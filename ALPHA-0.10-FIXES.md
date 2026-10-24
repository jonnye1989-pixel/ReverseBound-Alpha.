# ReverseBound Alpha 0.10 — Hammering Station fixes

- Enlarges the 3D world display from scale 1.0 to 1.45.
- Allows left-clicking the station barrier with any pickaxe in Survival to collect it.
- Calls a BlockBreakEvent before removal, allowing other protection plugins to veto the break.
- Preserves the existing station item drop, display cleanup and saved station registry.

## Testing

1. Stop server, back up the existing plugin JAR and configuration, then install the newly built JAR.
2. Keep ReverseBound Resources v5 enabled.
3. Use a pickaxe to left-click the invisible block around the station.
4. Confirm one station item drops, the display disappears, and the item can be placed again.
5. Confirm the station still processes fragments when right-clicked with the Primitive Hammer.

Note: This fixes the barrier interaction, not ordinary vanilla mining progress; breaking is immediate on a pickaxe left-click. Verify cave protection before allowing other players access.
