package com.playerwheelchairs;
import net.runelite.api.hooks.DrawCallbacks;
/** Read-only renderer compatibility check. Never installs a renderer wrapper. */
final class RendererSupport
{
    static boolean supported(DrawCallbacks renderer)
    {
        return renderer!=null && renderer.getClass().getName().equals("net.runelite.client.plugins.gpu.GpuPlugin");
    }
}
