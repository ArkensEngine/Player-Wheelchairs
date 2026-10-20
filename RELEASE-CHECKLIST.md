# Release candidate checklist

Preparation completed:

- [x] Public author: Arkens Engine; plugin metadata and BSD license included.
- [x] Standard Plugin Hub build metadata; Java 11 Gradle project and wrapper.
- [x] Portable tests independent of local samples or profiles.
- [x] Model recorder and retired GPU-wrapper implementation removed.
- [x] Runtime source uses no reflection, network calls or profile access.
- [x] Fitted animations and pushing disabled by default.
- [x] Ignore rules exclude local profiles, logs, credentials and build outputs.
- [x] GitHub build workflow prepared.
- [x] Full Java 11 Gradle `check build` passed for Plugin Hub version 1.13.2 and `latest.release` on October 20, 2026.
- [x] Clean source archive audited by an explicit file allowlist; no local profiles or captured models included.

Still required before requesting public Plugin Hub publication:

- [ ] Create the public GitHub repository and run its Gradle workflow successfully.
- [ ] Test a fresh normal RuneLite profile with default settings.
- [ ] Repeated camera movement/zoom, walking/running, region loads and world hops without freezes or terrain/minimap corruption.
- [ ] Repeated death/respawn, logout/login and plugin enable/disable cleanup.
- [ ] Fitted visuals: player menus, attack/follow/trade targeting and overlapping crowds work correctly.
- [ ] Fitted visuals: bow, whip, spear, punch, kick and hit reactions remain understandable; no misleading combat indicators.
- [ ] Clothing/gear changes and spell effects fall back cleanly without duplicate, missing or broken models.
- [ ] Verify cushion/foot contact and short-sleeve pushing in game.
- [ ] Test animation smoothing, other appearance plugins and unsupported renderer fallback.
- [ ] Ask RuneLite reviewers to assess temporary pose/action/equipment sampling and model replacement under current rules.
- [ ] Set the real public repository URL and full commit hash in the Plugin Hub marker.

Review preparation is not approval or a claim that live bugs are resolved. If the fitted feature cannot meet these checks, remove it from the submission rather than describing it as finished.
