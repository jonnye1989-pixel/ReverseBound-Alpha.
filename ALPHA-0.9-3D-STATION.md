# ReverseBound Alpha 0.9 — 3D Hammering Station

Requires Paper 1.21.4, Java 21 and **ReverseBound Resources v5** enabled by the client.

This update preserves Alpha 0.8 crafting, quests, sieve, forge and hammer processing.

- Crafted Hammering Station items receive `minecraft:item_model=reversebound:hammering_station`.
- Placing a tagged Hammering Station creates a barrier collision/interaction block and an ItemDisplay showing the v5 model.
- Right-click the station with the existing Primitive Hammer to process End Fragments as before.
- Breaking the station removes its visual and returns its custom item in Survival.
- Visuals are recreated when station chunks load.

## Install
1. Back up your server world and plugins/ReverseBoundAlpha config.
2. Build the Gradle project with Java 21 (GitHub Actions build workflow is included).
3. Stop the server and replace the previous ReverseBoundAlpha JAR with the built Alpha 0.9 JAR (do not keep both).
4. Keep ReverseBound Resources v5 active in Minecraft.
5. Start the server and place a freshly crafted Hammering Station. Existing stations should be converted when their chunks load.

## Important
The source has not been compiled or tested in a live Paper server in this environment. The visual uses an ItemDisplay over a barrier block, so without the resource pack players will not see the station. The item model's fixed display orientation/scale may need adjustment after a screenshot. This update does not introduce a new GUI; the existing hammer interaction remains intact.
