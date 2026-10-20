package com.playerwheelchairs;

import java.lang.reflect.*;
import java.util.*;
import net.runelite.api.*;
import net.runelite.client.callback.RenderCallbackManager;

final class FittedDrawChecks
{
    static void run() throws Exception
    {
        Client client=(Client)Proxy.newProxyInstance(Client.class.getClassLoader(),new Class<?>[]{Client.class},(p,m,a)->{
            throw new AssertionError("Drawing filter must not replace GPU callbacks or mutate the scene");
        });
        Player player=(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->{
            throw new AssertionError("Drawing filter must not mutate the native player");
        });
        GameObject object=(GameObject)Proxy.newProxyInstance(GameObject.class.getClassLoader(),new Class<?>[]{GameObject.class},(p,m,a)->{
            if(m.getName().equals("getRenderable"))return player;
            throw new AssertionError("Drawing filter must retain the native scene object");
        });
        RenderCallbackManager callbacks=new RenderCallbackManager();FittedAnimations fitted=new FittedAnimations(client,callbacks);
        Field hidden=FittedAnimations.class.getDeclaredField("hidden");hidden.setAccessible(true);
        Set<Player> ready=Collections.newSetFromMap(new IdentityHashMap<>());ready.add(player);hidden.set(fitted,ready);
        PrototypeChecks.require(callbacks.addEntity(player,false)&&callbacks.addEntity(player,true),"Native player stays in the scene and overhead UI path");
        PrototypeChecks.require(!callbacks.drawObject(null,object),"Only duplicate object drawing is filtered");
        PrototypeChecks.require(callbacks.drawTile(null,null),"Terrain filtering is unchanged");
        fitted.clear();PrototypeChecks.require(callbacks.drawObject(null,object),"Clear immediately restores native drawing");
        fitted.stop();PrototypeChecks.require(callbacks.drawObject(null,object),"Stop unregisters fitted drawing filter");
        System.out.println("PASS: ordinary draw filter, native scene/UI retention, terrain unaffected and cleanup (live click/menu behaviour unverified).");
    }
}
