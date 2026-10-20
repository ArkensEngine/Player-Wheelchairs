package com.playerwheelchairs;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.IntPredicate;
import net.runelite.api.*;

final class SeatedSmoothingChecks
{
    static void run()
    {
        IntPredicate[] current={null};
        Client client=(Client)Proxy.newProxyInstance(Client.class.getClassLoader(),new Class<?>[]{Client.class},(p,m,a)->{
            if(m.getName().equals("getAnimationInterpolationFilter"))return current[0];
            if(m.getName().equals("setAnimationInterpolationFilter")){current[0]=(IntPredicate)a[0];return null;}
            throw new AssertionError("Smoothing must not edit other client state");
        });
        int[] action={424};
        Player player=(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->{
            if(m.getName().equals("getAnimation"))return action[0];
            throw new AssertionError("Smoothing must not alter action frames or player state");
        });
        SeatedActionSmoothing smoothing=new SeatedActionSmoothing(client);
        smoothing.update(Collections.singleton(player),true);
        require(current[0]!=null && current[0].test(424),"Hit reactions use native interpolation");
        require(!current[0].test(-1)&&!current[0].test(123456),"Unobserved animations remain unchanged");
        action[0]=836;smoothing.update(Collections.singleton(player),true);
        require(current[0].test(836)&&current[0].test(424),"Death and prior action interpolation remain available");
        smoothing.stop();require(current[0]==null,"Disable restores the absent interpolation filter");
        IntPredicate other=id->id==426;current[0]=other;
        smoothing.update(Collections.singleton(player),true);
        require(current[0]==other,"Existing Animation Smoothing filter is preserved");
        smoothing.stop();require(current[0]==other,"Stopping does not clear another plugin's filter");
        current[0]=null;smoothing.update(Collections.singleton(player),true);current[0]=other;
        smoothing.stop();require(current[0]==other,"Renderer/plugin ownership changes are respected");
        current[0]=null;smoothing.update(Collections.singleton(player),true);
        smoothing.update(Collections.emptyList(),false);require(current[0]==null,"Config toggle removes owned interpolation");
        System.out.println("PASS: native hit/action/death interpolation, no action-state edits, config cleanup and existing-filter ownership.");
    }
    private static void require(boolean test,String message){if(!test)throw new AssertionError(message);}
}
