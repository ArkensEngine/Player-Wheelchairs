package com.playerwheelchairs;

import net.runelite.api.Player;

/** A temporary secondary pose, also used alongside primary attack animations.
 * Does not overwrite action animation IDs, frames, timing or spell effects. */
final class PoseOverride
{
    private final Player player;
    private final int originalPose, originalFrame, seatedPose;
    PoseOverride(Player player, int seatedPose)
    {
        this.player=player;
        this.originalPose=player.getPoseAnimation();
        this.originalFrame=player.getPoseAnimationFrame();
        this.seatedPose=seatedPose;
        player.setPoseAnimation(seatedPose);
        player.setPoseAnimationFrame(0);
    }
    void restore()
    {
        // Respect newer game/plugin state rather than overwriting it.
        if(player.getPoseAnimation()==seatedPose)
        {
            player.setPoseAnimation(originalPose);
            player.setPoseAnimationFrame(originalFrame);
        }
    }
}
