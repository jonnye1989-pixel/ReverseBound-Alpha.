# ReverseBound Alpha 0.3 — Manual Sieve

Paper 1.21.4, Java 21. Includes all Alpha 0.2.1 features.

## Manual Sieve

Craft in a 3x3 table using **one custom End Fragments item in each corner** and **End Stone in the center** (4 fragments total, one in each corner). Place the resulting Manual Sieve (glass prototype) in `reversebound_end`. Hold at least 4 tagged Void Dust and right-click the placed sieve **four times**. Four Void Dust are consumed upon completion, and one reward drops above the sieve. The existing `/rbsieve` command remains available for testing.

The plugin records placed sieves in config.yml so they survive restarts. Break a placed sieve to retrieve its custom item. Existing Sieve 0.2.1 plugin can remain installed for testing, but its sieve is separate and will not process ReverseBound Void Dust correctly.

## Build

Upload **contents of this folder to the GitHub repository root**, preserving `.github/workflows/build.yml`; GitHub Actions will upload the `ReverseBound-Alpha-0.3.jar` build artifact. Stop the server, back up the world and old plugin, replace the old ReverseBound JAR (do not run two versions), restart, test.

This source has not been compiled or server-tested in this environment.
