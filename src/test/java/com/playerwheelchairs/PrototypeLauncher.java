package com.playerwheelchairs;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public final class PrototypeLauncher
{
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(WheelchairPlugin.class);
        RuneLite.main(args);
    }
}
