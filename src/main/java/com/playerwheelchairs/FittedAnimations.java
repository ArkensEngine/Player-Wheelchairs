package com.playerwheelchairs;

import java.util.*;
import net.runelite.api.*;
import net.runelite.client.callback.*;

/** Uses the ordinary object drawing filter, never replaces DrawCallbacks.
 * Native actors stay in the scene; only their duplicate 3D draw is filtered. */
final class FittedAnimations
{
    private final Client client;
    private final RenderCallbackManager callbacks;
    private final Map<Player,State> states=new IdentityHashMap<>();
    private Set<Player> hidden=Collections.emptySet();
    private static final class State
    {
        int signature;SeatedReference reference;PlayerArmRig rig;
        FittedPlayerMesh mesh;RuneLiteObject object;double previousDistance,travel,blend;long lastTime,lastMove;
    }
    private final RenderCallback callback=new RenderCallback()
    {
        @Override public boolean drawObject(Scene scene,TileObject object)
        {
            return !(object instanceof GameObject) || !hidden.contains(((GameObject)object).getRenderable());
        }
    };
    FittedAnimations(Client client,RenderCallbackManager callbacks)
    {this.client=client;this.callbacks=callbacks;callbacks.register(callback);}
    void update(Set<Player> visible,Map<Player,ChairMotion> motions,WheelchairConfig config)
    {
        net.runelite.api.hooks.DrawCallbacks renderer=client.getDrawCallbacks();
        if(!config.fittedAnimations() || !RendererSupport.supported(renderer)){clear();return;}
        Set<Player> next=Collections.newSetFromMap(new IdentityHashMap<>());
        Iterator<Map.Entry<Player,State>> old=states.entrySet().iterator();
        while(old.hasNext())
        {Map.Entry<Player,State> e=old.next();if(!visible.contains(e.getKey())){deactivate(e.getValue());old.remove();}}
        int allocations=0;
        for(Player player:visible)
        {
            State state=states.computeIfAbsent(player,p->new State());
            try
            {
                if(player.getSpotAnims().iterator().hasNext()){deactivate(state);continue;}
                PlayerComposition appearance=player.getPlayerComposition();
                Animation action=player.getAnimation()<0?null:client.loadAnimation(player.getAnimation());
                int left=action==null?-1:action.getLeftHandItem(),right=action==null?-1:action.getRightHandItem();
                int signature=31*(31*Arrays.hashCode(appearance.getEquipmentIds())+Arrays.hashCode(appearance.getColors()))+appearance.getGender();
                int[] currentEquipment=appearance.getEquipmentIds();
                // Equivalent hand overrides must not rebuild an otherwise
                // identical sitting reference on an idle/action transition.
                signature=31*(31*signature+(left<0?currentEquipment[5]:left))+(right<0?currentEquipment[3]:right);
                if(state.reference==null || state.signature!=signature)
                {
                    deactivate(state);state.mesh=null;state.object=null;
                    state.reference=WheelchairPlugin.captureSeatedReference(player,left,right);
                    state.rig=new PlayerArmRig(state.reference);state.signature=signature;state.blend=0;
                }
                Model source=player.getModel();
                if(source==null || !state.reference.matches(source) || !FittedPlayerMesh.supported(source)){deactivate(state);continue;}
                if(state.mesh==null)
                {
                    if(allocations++>=2)continue;
                    state.mesh=FittedPlayerMesh.create(client,source);
                    if(state.mesh==null)continue;
                    state.object=new RuneLiteObject(client);state.object.setModel(state.mesh.model);
                }
                if(source.getVerticesCount()!=state.mesh.layout.vertices.size() || source.getFaceCount()!=state.mesh.layout.faces.size())
                {deactivate(state);state.mesh=null;continue;}
                state.mesh.copy(source);
                if(player.getAnimation()==-1)
                    state.reference.posture.holdSeated(state.mesh.model.getVerticesX(),state.mesh.model.getVerticesY(),state.mesh.model.getVerticesZ());
                else state.reference.posture.correct(state.mesh.model.getVerticesX(),state.mesh.model.getVerticesY(),state.mesh.model.getVerticesZ());
                if(player.getAnimation()==SeatedKick.ACTION && action!=null)
                    SeatedKick.apply(state.reference,state.mesh.model.getVerticesX(),state.mesh.model.getVerticesY(),state.mesh.model.getVerticesZ(),player.getAnimationFrame(),action.getNumFrames());
                long now=System.nanoTime();double dt=state.lastTime==0?0:Math.min(.1,(now-state.lastTime)/1e9);state.lastTime=now;
                ChairMotion motion=motions.get(player);double distance=motion==null?state.previousDistance:motion.getDistance();
                double step=distance-state.previousDistance;if(step<0)step+=ChairMotion.PERIOD;
                state.previousDistance=distance;state.travel=(state.travel+Math.min(step,128))%WheelPushCycle.TRAVEL_PER_CYCLE;
                int[] equipment=appearance.getEquipmentIds();
                if(step>.05)state.lastMove=now;
                boolean pushing=config.pushingArms() && state.rig.usable() && WheelPushCycle.mayPush(state.lastMove!=0 && now-state.lastMove<100_000_000L,player.getAnimation(),equipment[3]==0&&equipment[5]==0);
                if(player.getAnimation()!=-1)state.blend=0;
                else state.blend+=((pushing?1:0)-state.blend)*(1-Math.exp(-dt/.08));
                if(state.blend>.001)state.rig.push(state.mesh.model.getVerticesX(),state.mesh.model.getVerticesY(),state.mesh.model.getVerticesZ(),state.travel,state.blend);
                state.mesh.finish();
                state.object.setLocation(player.getLocalLocation(),player.getWorldView().getPlane());state.object.setOrientation(player.getCurrentOrientation());
                if(!state.object.isActive())state.object.setActive(true);
                next.add(player);
            }
            catch(RuntimeException ex)
            {deactivate(state);org.slf4j.LoggerFactory.getLogger(FittedAnimations.class).debug("Fitted player visual unavailable; using native draw",ex);}
        }
        hidden=next;
    }
    private static void deactivate(State state){if(state.object!=null)state.object.setActive(false);}
    void clear(){hidden=Collections.emptySet();for(State state:states.values())deactivate(state);states.clear();}
    void stop(){clear();callbacks.unregister(callback);}
}
