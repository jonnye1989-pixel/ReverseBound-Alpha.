# ReverseBound Alpha 0.8 — Quest Tracking

Upload files into their matching GitHub paths. Do not upload Java files into the repository root.

- `src/main/java/com/reversebound/alpha/ReverseBoundAlpha.java`
- `src/main/java/com/reversebound/alpha/EndStoneGenerator.java`
- `src/main/resources/plugin.yml`
- `src/main/resources/config.yml` (only if absent; do not overwrite server-generated config)

Keep your existing GitHub Actions workflow and Gradle build files if already working.

In game: `/rbjournal` -> click book in slot 22 -> quest journal; `/rbquests` opens it directly; `/rbrp` shows RP. Complete the active objective and click the emerald at slot 22 to claim. Ten sequential quests. Progress and RP saved in config.yml.

Important testing limits: shift-click crafting is not tracked; craft single recipes normally. Smelting quests track furnace extraction, not smelting completion. The Void Lucky Block is a preview item, not an active lucky block. Quest 3 counts End Fragments consumed by the Hammering Station, quest 4 counts Void Dust consumed by the sieve.

Known issue: quest rewards are issued upon manual claim and older activity is not retroactively counted.
