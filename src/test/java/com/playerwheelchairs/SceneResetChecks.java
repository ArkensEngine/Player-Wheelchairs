package com.playerwheelchairs;

import java.lang.reflect.*;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;

final class SceneResetChecks
{
    static void run()
    {
        try
        {
            Client client=(Client)Proxy.newProxyInstance(Client.class.getClassLoader(),new Class<?>[]{Client.class},(p,m,a)->{
                throw new AssertionError("Scene cleanup must not change the renderer, scene or minimap: "+m.getName());
            });
            Scene[] scene={stub(Scene.class)};int[] base={3200,3200};
            WorldView world=(WorldView)Proxy.newProxyInstance(WorldView.class.getClassLoader(),new Class<?>[]{WorldView.class},(p,m,a)->{
                switch(m.getName())
                {
                    case "getScene":return scene[0];
                    case "getBaseX":return base[0];
                    case "getBaseY":return base[1];
                    default:throw new AssertionError("Unexpected world operation");
                }
            });
            Player player=player(world);
            WheelchairPlugin plugin=new WheelchairPlugin();
            Field clientField=WheelchairPlugin.class.getDeclaredField("client");clientField.setAccessible(true);clientField.set(plugin,client);
            plugin.startUp();
            require(plugin.synchronizeScene(player),"Initial scene starts fresh");
            require(!plugin.synchronizeScene(player),"Stable scene does not rebuild chairs every frame");
            base[0]+=8;require(plugin.synchronizeScene(player),"Region-base change discards stale local coordinates");
            scene[0]=stub(Scene.class);require(plugin.synchronizeScene(player),"Rebuilt scene resets chairs even with reused world view");
            Player respawned=player(world);require(plugin.synchronizeScene(respawned),"New local player after respawn resets chairs");
            GameStateChanged loading=new GameStateChanged();loading.setGameState(GameState.LOADING);
            plugin.onGameStateChanged(loading);
            require(plugin.synchronizeScene(respawned),"Loading clears state even when scene and actor identities are reused");
            System.out.println("PASS: loading/respawn/region scene reset, stable-scene reuse and no GPU/scene/minimap calls during cleanup.");
        }
        catch(ReflectiveOperationException ex){throw new AssertionError(ex);}
    }
    private static Player player(WorldView world)
    {
        return (Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->{
            if(m.getName().equals("getWorldView"))return world;
            throw new AssertionError("Unexpected actor operation");
        });
    }
    @SuppressWarnings("unchecked") private static <T> T stub(Class<T> type)
    {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->{throw new AssertionError("Scene must not be mutated");});
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
