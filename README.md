# ReverseBound Alpha 0.1 — Paper 1.21.4

**Development prototype only.** Creates a separate world `reversebound_end` with an enclosed End Stone mass and a 7x7x5 starter cave. Existing `world`, `world_nether`, and `world_the_end` are not changed. It does **not** yet implement private team worlds, quests, machines, or a real End dimension.

## Build using GitHub (no local Java setup)

1. Create a new GitHub repository (for example `ReverseBound-Alpha`).
2. Upload the **contents** of this folder to the repository root, including `.github/workflows/build.yml` (hidden folders must be included).
3. Open **Actions** > **Build ReverseBound Alpha** > **Run workflow** (or push a commit to main).
4. Wait for a green build. Under the run's **Artifacts**, download `ReverseBound-Alpha-0.1`.
5. Extract the downloaded artifact ZIP to get `ReverseBound-Alpha-0.1.jar`.
6. Stop the TickHosting Paper 1.21.4 server and back up the server.
7. Upload the `.jar` into `/home/container/plugins` (NOT the ZIP). Start the server.
8. Look for `ReverseBound Alpha 0.1 test cave ready` in the console, and use `/plugins` to verify it is enabled.
9. As an OP, run `/rbspawn` in game to enter the test cave. Use `/tp` to return to the original world if needed.

The first version deliberately does **not** teleport players automatically. `plugins/ReverseBoundAlpha/config.yml` has `send-new-players-to-cave: false`.

## Notes

- The generator is a prototype and uses a **NORMAL** dimension environment to avoid unwanted End environmental rules. It uses End Stone blocks, not the vanilla End world type.
- The world is not literally infinite solid stone vertically: it has a bedrock roof and floor. This prevents players from easily escaping the underground test area.
- Generating chunks filled with solid blocks is resource-intensive; use only for a small development world until performance is measured.
- No plugin JAR is bundled: the source must be compiled against the Paper API by GitHub Actions. Build success and actual server startup still need testing.
