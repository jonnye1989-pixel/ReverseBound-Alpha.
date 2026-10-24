# ReverseBound Alpha 0.7 — Field Journal preview

Based on the uploaded working Alpha 0.6 source.

## Added
- A real written Field Journal in new starter kits. Right-click it to open a 27-slot progression guide.
- `/rbjournal` opens the same guide for existing players whose one-time kit was already claimed.
- Journal crafting and machine progression information; the inventory guide cannot be looted.
- Version bumped in `plugin.yml`, `build.gradle`, and output JAR filename.

## Preserved
- Unbreakable Primitive Pickaxe
- Void Hammering Station, manual sieve, Voidforge and crafting recipes
- Existing config/world names and stored machine locations

## Important limitations
- The journal shows the intended progression, but does NOT yet track quest completions or machine unlocks automatically.
- Custom 3D machine models and a server resource pack are NOT included in this increment; they need a separate model and block-display integration pass.
- This source has not been compiled in this environment; GitHub Actions is the build verification step.

## Install
1. Keep a backup of your server and world.
2. On GitHub, replace `src/main/java/com/reversebound/alpha/ReverseBoundAlpha.java`, `src/main/resources/plugin.yml` and `build.gradle` with the corresponding files in this ZIP.
3. Let GitHub Actions finish successfully and download its JAR artifact.
4. Stop the server, replace only the previous ReverseBound Alpha JAR in `plugins`, and start the server. Do not delete your `plugins/ReverseBoundAlpha` data folder.
5. Confirm `/version ReverseBoundAlpha` reports 0.7, then test `/rbjournal` and the original stations.
