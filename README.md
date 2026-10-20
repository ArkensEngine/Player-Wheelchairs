# Player Wheelchairs

By **Arkens Engine**. Release candidate **0.2.3-rc1**.

A cosmetic RuneLite plugin that displays player wheelchairs on your own client.
Other players must install the plugin themselves to see these visuals. NPCs are excluded.

**Experimental and not Plugin Hub approved.** Public release is pending live validation and RuneLite review. This candidate has not been submitted.

## Features

- Original wooden-and-iron 3D chair geometry.
- Seated idle/movement pose for eligible players in the current world view.
- Rolling rear wheels and front casters based on each player's travel distance.
- Adjustable range, seat height, cushion drop, footrest height and orientation.
- Optional fitted seated attacks, stable fitted idle, unarmed kick and empty-hand pushing.

**Fitted seated animations and Pushing arms default off.** They are experimental and require the standard RuneLite GPU plugin. Unsupported textures, outfit topology and active effects fall back to normal player drawing. Arm fitting supports a limited short-sleeve component layout, not every outfit. Player targeting, menus and death/respawn need further verification with fitted visuals enabled.

## Install

The plugin is not currently available in Plugin Hub. After acceptance, users will be able to install **Player Wheelchairs** through RuneLite's Plugin Hub. There is no need for a custom launcher or shared login credentials for normal Plugin Hub installation.

## Development

Use a Java 11 JDK and the included Gradle wrapper:

```
./gradlew check build
./gradlew run
```

Windows users can use `gradlew.bat` instead. A RuneLite version can be selected with `-PruneliteVersion=1.13.2`; the default follows `latest.release`. `check` includes portable geometry, pose restoration, motion and drawing-filter checks; no game login or captured models are required.

For development login, follow [RuneLite's official instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). Never commit credentials or runtime profiles. Authentication is not handled by this plugin.

## Implementation and limitations

This plugin uses public RuneLite APIs. It does not replace global GPU DrawCallbacks or use runtime reflection. Fitted visuals use separate models and the ordinary object-drawing filter; the original player remains in the scene. Retaining the native actor does not prove that interaction hitboxes align with the changed pose.

The source contains temporary, restored pose/action/equipment sampling for fitting. This behaviour requires explicit reviewer attention, particularly for combat clarity and interaction compatibility. It is not a claim that RuneLite or Jagex permits every proposed animation feature.

An earlier prototype produced a GPU sorting assertion and a reported map issue. Conservative bounds guards and scene cleanup were added. The release candidate still needs repeated live camera, region, world-hop and death/respawn tests. Automated checks do not establish that those failures are fully resolved.

No network requests, account-data handling, local model recorder, captured game assets or profile files are included in this release source. The plugin does not upload player data.

See [RELEASE-CHECKLIST.md](RELEASE-CHECKLIST.md) for outstanding validation. License: BSD-2-Clause; see [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
