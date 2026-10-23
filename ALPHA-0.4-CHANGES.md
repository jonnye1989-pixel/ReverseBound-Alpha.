# ReverseBound Alpha 0.4 — Resource Progression

Build from repository root with GitHub Actions. Install only one ReverseBoundAlpha JAR; stop the server first and back up the world.

## Sieve drops (per 4 Void Dust)
- Iron Fragments x2: 28%
- Copper Fragments x2: 22%
- Organic Spores x2: 20%
- Crystal Dust x2: 15%
- Flint x2: 10%
- Gold Fragments x2: 4%
- Rift Shard x1: 1%

## Crafting (shapeless, 3x3 crafting table)
- 4 Iron Fragments → 1 Raw Iron
- 4 Copper Fragments → 1 Raw Copper
- 4 Gold Fragments → 1 Raw Gold
- 4 Organic Spores → 2 Void Briquettes (charcoal, furnace fuel)
- 4 Crystal Dust → 1 Resonant Crystal

Old Alpha 0.3 fragment/spore/crystal items have compatibility recipes. Raw metals require a furnace to smelt; obtaining/crafting a furnace is NOT yet implemented as part of ReverseBound's End-world progression. The 0.4 milestone is resource conversion, not a complete self-contained metal economy.

## Testing
1. Back up the world and replace Alpha 0.3 JAR with Alpha 0.4. Restart.
2. `/plugins`, `/rbhelp` and `/rbspawn`.
3. Mine End Stone → End Fragments → `/rbhammer` → Void Dust.
4. Sieve 4 Void Dust and collect resources.
5. Craft four matching fragments/spores/crystals into the output above.

**Do not upload the nested old `ReverseBound-Alpha-0.1-Source` folder as the active source; the GitHub Actions build uses the root `src/` directory.**
