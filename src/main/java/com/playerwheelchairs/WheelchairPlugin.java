package com.playerwheelchairs;

import com.google.inject.Provides;
import java.util.*;
import javax.inject.Inject;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(name="Player Wheelchairs", description="Experimental seated players with custom 3D wheelchairs", tags={"cosmetic","wheelchair","players"}, enabledByDefault=false)
public class WheelchairPlugin extends Plugin
{
    private static final Logger log=LoggerFactory.getLogger(WheelchairPlugin.class);
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private WheelchairConfig config;
    @Inject private net.runelite.client.callback.RenderCallbackManager renderCallbacks;
    private FittedAnimations fittedAnimations;
    private final Map<Player,RuneLiteObject> chairs=new IdentityHashMap<>();
    private final Map<Player,ChairMotion> chairMotions=new IdentityHashMap<>();
    private final List<PoseOverride> poses=new ArrayList<>();
    private Model chairModel;
    private ChairMesh chairMesh;
    private boolean failed, running;
    private SeatedActionSmoothing actionSmoothing;
    private WorldView activeWorld;
    private Scene activeScene;
    private Player activeLocal;
    private int activeBaseX,activeBaseY;

    @Provides WheelchairConfig provideConfig(ConfigManager manager) { return manager.getConfig(WheelchairConfig.class); }

    @Override protected void startUp()
    {
        running=true; failed=false;
        actionSmoothing=new SeatedActionSmoothing(client);
        if(renderCallbacks!=null)fittedAnimations=new FittedAnimations(client,renderCallbacks);
        // Safety rollback: the global renderer adapter is quarantined following
        // a reported world/minimap corruption after death and respawn. Saved
        // config values must not reactivate it in this build.
        log.info("Player Wheelchairs 0.2.3-rc1 started");
    }
    @Override protected void shutDown()
    {
        running=false;
        FittedAnimations oldVisuals=fittedAnimations;fittedAnimations=null;
        clientThread.invoke(()->{if(oldVisuals!=null)oldVisuals.stop();clear();});
    }

    @Subscribe public void onClientTick(ClientTick event) { restorePoses(); }

    @Subscribe public void onGameStateChanged(GameStateChanged event)
    {
        if(event.getGameState()!=GameState.LOGGED_IN) clear();
    }

    @Subscribe public void onConfigChanged(ConfigChanged event)
    {
        if("playerwheelchairs".equals(event.getGroup())) clientThread.invoke(this::clear);
    }

    @Subscribe public void onBeforeRender(BeforeRender event)
    {
        // Restore before taking the next snapshot, including uncapped render frames.
        restorePoses();
        if(!running || failed || client.getGameState()!=GameState.LOGGED_IN) return;
        Player local=client.getLocalPlayer();
        if(local==null || local.getWorldView()==null) return;
        try
        {
            synchronizeScene(local);
            if(chairModel==null)
            {
                chairMesh=ChairMesh.create(config.height(),config.forward(),config.seatLift(),config.footLift(),config.cushionDrop());
                chairModel=ChairModelFactory.build(client,chairMesh);
                if(chairModel==null) return;
                log.info("Wheelchair chair model built successfully");
            }
            Animation seated=client.loadAnimation(config.seatAnimation());
            if(seated==null || seated.getNumFrames()<=0) return;
            Set<Player> visible=Collections.newSetFromMap(new IdentityHashMap<>());
            LocalPoint origin=local.getLocalLocation();
            if(origin==null) return;
            for(Player player:local.getWorldView().players())
            {
                if(player==null || (!config.otherPlayers() && player!=local)) continue;
                LocalPoint point=player.getLocalLocation();
                if(point==null || point.distanceTo(origin)>config.distance()*128) continue;
                // Keep the chair and seated secondary pose during attacks and
                // spell effects. The client's existing primary action animation
                // continues unchanged and can blend with this secondary pose.
                if(!canUseChair(player)) continue;
                poses.add(new PoseOverride(player,config.seatAnimation()));
                RuneLiteObject chair=chairs.get(player);
                if(chair==null)
                {
                    // Moving chairs must own their vertices: sharing would make
                    // one player's movement rotate everybody else's wheels.
                    Model model=config.rollingWheels()?ChairModelFactory.build(client,chairMesh):chairModel;
                    if(model==null)continue;
                    chair=new RuneLiteObject(client);
                    chair.setModel(model);
                    chairs.put(player,chair);
                    chairMotions.put(player,new ChairMotion(chairMesh));
                }
                ChairMotion motion=chairMotions.get(player);
                if(motion!=null)
                {
                    motion.move(point.getX(),point.getY(),player.getWorldView().getPlane(),System.nanoTime());
                    if(config.rollingWheels())motion.apply(chair.getModel());
                }
                chair.setLocation(point,player.getWorldView().getPlane());
                chair.setOrientation((player.getCurrentOrientation()+config.rotation())&2047);
                if(!chair.isActive()) chair.setActive(true);
                visible.add(player);
            }
            Iterator<Map.Entry<Player,RuneLiteObject>> it=chairs.entrySet().iterator();
            while(it.hasNext())
            {
                Map.Entry<Player,RuneLiteObject> e=it.next();
                if(!visible.contains(e.getKey())) { e.getValue().setActive(false);chairMotions.remove(e.getKey());it.remove(); }
            }
            actionSmoothing.update(visible,config.smoothSeatedActions());
            if(fittedAnimations!=null)fittedAnimations.update(visible,chairMotions,config);
            // Keep the native GPU renderer and native player models untouched.
            // Full-body attacks may temporarily override the seated pose until
            // a scene-safe posture implementation has been verified.
        }
        catch(RuntimeException ex)
        {
            clear(); failed=true;
            log.error("Wheelchair prototype stopped after a rendering error; toggle it to retry",ex);
        }
    }

    private void restorePoses()
    {
        for(PoseOverride pose:poses) pose.restore();
        poses.clear();
    }

    static boolean canUseChair(Player player)
    {
        PlayerComposition appearance=player.getPlayerComposition();
        // RuneLite's dead marker can outlive a PvP respawn on the same actor.
        // It is not a reliable cosmetic eligibility test. Do not change that
        // marker: keep the chair through death and let actor removal clean up.
        return appearance!=null && appearance.getTransformedNpcId()==-1;
    }

    boolean synchronizeScene(Player local)
    {
        WorldView world=local.getWorldView();
        Scene scene=world.getScene();
        int baseX=world.getBaseX(),baseY=world.getBaseY();
        boolean changed=world!=activeWorld || scene!=activeScene || local!=activeLocal
            || baseX!=activeBaseX || baseY!=activeBaseY;
        if(changed)clear();
        activeWorld=world;activeScene=scene;activeLocal=local;activeBaseX=baseX;activeBaseY=baseY;
        return changed;
    }

    static SeatedReference captureSeatedReference(Player player)
    {
        return captureSeatedReference(player,-1,-1);
    }

    static SeatedReference captureSeatedReference(Player player,int left,int right)
    {
        // Sample the native sitting model, even when first seen mid-attack.
        // This is synchronous on the client thread; restore before returning.
        int action=player.getAnimation();
        PlayerComposition composition=(left>=0 || right>=0)?player.getPlayerComposition():null;
        int[] equipment=composition==null?null:composition.getEquipmentIds();
        int weapon=net.runelite.api.kit.KitType.WEAPON.getIndex();
        int shield=net.runelite.api.kit.KitType.SHIELD.getIndex();
        int oldWeapon=equipment==null?0:equipment[weapon],oldShield=equipment==null?0:equipment[shield];
        try
        {
            if(equipment!=null)
            {
                if(left>=0)equipment[shield]=left;
                if(right>=0)equipment[weapon]=right;
                composition.setHash();
            }
            player.setAnimation(-1);
            Model model=player.getModel();
            if(model==null)throw new IllegalStateException("Player model not ready");
            return new SeatedReference(model);
        }
        finally
        {
            try
            {
                if(equipment!=null)
                {
                    equipment[weapon]=oldWeapon;equipment[shield]=oldShield;
                    composition.setHash();
                }
            }
            finally {player.setAnimation(action);}
        }
    }

    private void clear()
    {
        restorePoses();
        if(fittedAnimations!=null)fittedAnimations.clear();
        if(actionSmoothing!=null)actionSmoothing.stop();
        for(RuneLiteObject chair:chairs.values()) chair.setActive(false);
        chairs.clear();chairMotions.clear();chairModel=null;chairMesh=null;failed=false;
        activeWorld=null;activeScene=null;activeLocal=null;activeBaseX=0;activeBaseY=0;
    }
}
