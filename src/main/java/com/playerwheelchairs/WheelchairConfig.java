package com.playerwheelchairs;

import net.runelite.client.config.*;

@ConfigGroup("playerwheelchairs")
public interface WheelchairConfig extends Config
{
    @ConfigItem(keyName="otherPlayers", name="Other players", description="Also seat other players in the current world view", position=0)
    default boolean otherPlayers() { return true; }

    @Range(min=1,max=64)
    @ConfigItem(keyName="distance", name="Distance (tiles)", description="Limit chair rendering distance for performance", position=1)
    default int distance() { return 32; }

    @Range(min=0,max=20000)
    @ConfigItem(keyName="seatAnimation", name="Sitting animation", description="4073 is CHAIR_SIT_READY_CHAIR1; experimental alignment setting", position=2)
    default int seatAnimation() { return 4073; }

    @Range(min=-100,max=100)
    @ConfigItem(keyName="height", name="Chair height offset", description="Positive moves the chair down; requires in-game calibration", position=3)
    default int height() { return 0; }

    @Range(min=-100,max=100)
    @ConfigItem(keyName="forward", name="Chair front/back offset", description="Adjust chair relative to the seated player", position=4)
    default int forward() { return 0; }

    @Range(min=0,max=2047)
    @ConfigItem(keyName="rotation", name="Chair rotation offset", description="2048 angle units per full turn; 1024 reverses the chair", position=5)
    default int rotation() { return 0; }

    @ConfigItem(keyName="smoothSeatedActions", name="Smooth seated actions", description="Use native animation interpolation for observed player actions and hit reactions. Leaves an existing Animation Smoothing filter unchanged.", position=8)
    default boolean smoothSeatedActions() { return true; }

    @ConfigItem(keyName="rollingWheels", name="Rolling wheels", description="Rotate rear wheels and front casters according to each player's travelled distance. Running rolls faster; teleports are ignored.", position=9)
    default boolean rollingWheels() { return true; }

    @Range(min=-20,max=40)
    @ConfigItem(keyName="seatLift", name="Seat lift", description="Raise the cushion, back and armrests without moving the wheels. Alignment estimate from the side-view recording.", position=10)
    default int seatLift() { return 16; }

    @Range(min=-20,max=40)
    @ConfigItem(keyName="footLift", name="Footrest lift", description="Raise the footrests independently to close the gap under the boots.", position=11)
    default int footLift() { return 12; }

    @ConfigItem(keyName="fittedAnimations", name="Fitted seated animations", description="First live fitted-mesh pass for the standard GPU renderer. Untextured outfits only; unsupported gear/effects keep native rendering. Disable if visual or clicking issues occur.", position=13)
    default boolean fittedAnimations() { return false; }

    @ConfigItem(keyName="pushingArms", name="Pushing arms", description="Push-cycle preview for moving empty-handed players with a matching short-sleeve arm layout. Attacks and hit reactions take priority.", position=14)
    default boolean pushingArms() { return false; }

    @Range(min=-20,max=40)
    @ConfigItem(keyName="cushionDrop", name="Cushion drop", description="Lower only the cushion relative to the upper chair frame. Default fits the underside of the captured seated hips.", position=15)
    default int cushionDrop() { return 18; }
}
