# ReverseBound Alpha 0.2.1 — crafting hotfix

For Paper 1.21.4 / Java 21. This is source code, not a compiled JAR. Build using the existing GitHub Actions workflow.

## Changes
- Four End Stone blocks in the four corners of a **3x3 crafting table** give four tagged End Fragments. Vanilla 2x2 End Stone Bricks remain untouched.
- Hold the **Primitive Hammer (prototype)** in your main hand and run `/rbhammer` to convert four custom End Fragments into two tagged Void Dust.
- Hold at least four tagged Void Dust and run `/rbsieve` to get an early resource roll.
- Ordinary End Stone Bricks cannot be used as End Fragments.

## Safe installation
Stop server, back up worlds and plugins, remove old Alpha 0.2 JAR (keep local copy), upload newly built Alpha 0.2.1 JAR, then start. Do not delete the `reversebound_end` world or player data.

## Test
`/plugins`, `/rbspawn`, craft End Fragments using 3x3 corners, `/rbhammer` holding hammer, `/rbsieve` holding 4 Void Dust. `/rbkit` is one-time; players who already claimed it keep their existing items.

**Known limitations:** Test-only shared world, prototype tools and resource items, no full machine interface, and no survival progression balance yet. Not runtime tested on TickHosting.
