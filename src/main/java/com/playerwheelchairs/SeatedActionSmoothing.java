package com.playerwheelchairs;

import java.util.*;
import java.util.function.IntPredicate;
import net.runelite.api.Client;
import net.runelite.api.Player;

/** Uses native skeletal interpolation, rather than averaging deformed meshes.
 * Leave another plugin's interpolation filter entirely under its ownership. */
final class SeatedActionSmoothing
{
    private final Client client;
    private volatile Set<Integer> actions=Collections.emptySet();
    private final IntPredicate filter=id->actions.contains(id);
    private boolean installed;
    SeatedActionSmoothing(Client client){this.client=client;}
    void update(Collection<Player> players,boolean enabled)
    {
        if(!enabled){stop();return;}
        if(installed && client.getAnimationInterpolationFilter()!=filter)installed=false;
        if(!installed)
        {
            if(client.getAnimationInterpolationFilter()!=null)return;
            client.setAnimationInterpolationFilter(filter);installed=true;
        }
        Set<Integer> observed=new HashSet<>(actions);
        for(Player player:players)
        {
            int animation=player.getAnimation();
            if(animation>=0)observed.add(animation);
        }
        actions=Collections.unmodifiableSet(observed);
    }
    void stop()
    {
        if(installed && client.getAnimationInterpolationFilter()==filter)client.setAnimationInterpolationFilter(null);
        installed=false;actions=Collections.emptySet();
    }
}
