# ReverseBound Alpha 0.5 — renewable tools and Voidforge foundation

## Added
- 1 End Stone (shapeless) -> 4 sticks, ensuring recovery after starter tools break.
- 3 tagged End Fragments + 2 sticks in vanilla pickaxe shape -> Primitive Pickaxe.
- 2 tagged End Fragments + 2 sticks in vanilla hoe shape -> Primitive Hammer.
- 8 End Stone in furnace shape -> tagged Voidforge.
- Voidforge placement/break tracking; breaking returns the custom tagged item. Furnace mechanics remain vanilla-compatible, including smelting raw metals and charcoal-based Void Briquettes.
- Existing sieve, resource, starter-kit, commands and world logic preserved.

## Important limitation
This version intentionally uses the **vanilla furnace appearance** as the functional foundation. The illustrated 3D Voidforge requires a separate model/texture resource pack and a server-side display/interaction system, which is NOT included in this ZIP. Do not claim it already looks like the concept image.

## Testing
1. Back up the server and world. Stop the server before replacing the JAR.
2. Build with the existing GitHub Actions workflow and download the JAR artifact.
3. Install the JAR under plugins, remove the old ReverseBound JAR, and restart.
4. In a crafting table, craft 4 End Fragments from 4 End Stone in the four corners.
5. Craft 4 sticks from 1 End Stone; craft Primitive Pickaxe and Hammer using tagged fragments.
6. Craft Voidforge using 8 End Stone, place it, and open the normal furnace UI.
7. Smelt Raw Iron using Void Briquettes (4 Organic Spores -> 2 Void Briquettes).

## Notes
- Custom named items are PDC-tagged where appropriate; do not substitute vanilla End Stone Bricks for End Fragments.
- The old nested `ReverseBound-Alpha-0.1-Source` directory is historical; GitHub builds the top-level `src` directory.
