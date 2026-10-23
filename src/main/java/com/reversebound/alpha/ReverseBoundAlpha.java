package com.reversebound.alpha;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ReverseBoundAlpha extends JavaPlugin implements Listener {
    private World testWorld;
    private static final int CAVE_Y = 64;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        WorldCreator creator = new WorldCreator("reversebound_end");
        creator.environment(World.Environment.NORMAL);
        creator.generator(new EndStoneGenerator());
        creator.generateStructures(false);
        testWorld = Bukkit.createWorld(creator);
        if (testWorld == null) {
            getLogger().severe("Unable to create reversebound_end; disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        testWorld.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
        testWorld.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        testWorld.setTime(6000L);
        buildStarterCave();
        testWorld.setSpawnLocation(0, CAVE_Y, 0);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("ReverseBound Alpha 0.1 test cave ready in reversebound_end.");
    }

    private Location caveSpawn() {
        return new Location(testWorld, 0.5, CAVE_Y + 0.0, 0.5, 0, 0);
    }

    private void buildStarterCave() {
        // Cave is 7x7 wide, five blocks high (y=64..68), floor at y=63.
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = CAVE_Y; y < CAVE_Y + 5; y++) {
                    Block block = testWorld.getBlockAt(x, y, z);
                    if (block.getType() == Material.END_STONE) block.setType(Material.AIR, false);
                }
                testWorld.getBlockAt(x, CAVE_Y - 1, z).setType(Material.END_STONE, false);
            }
        }
        for (int[] pos : new int[][]{{-2,-2},{-2,2},{2,-2},{2,2}}) {
            testWorld.getBlockAt(pos[0], CAVE_Y + 4, pos[1]).setType(Material.SEA_LANTERN, false);
        }
        testWorld.getBlockAt(2, CAVE_Y, 0).setType(Material.CRAFTING_TABLE, false);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!getConfig().getBoolean("send-new-players-to-cave", false)) return;
        if (player.hasPlayedBefore()) return;
        Bukkit.getScheduler().runTask(this, () -> {
            player.teleport(caveSpawn());
            player.setGameMode(GameMode.SURVIVAL);
            player.sendMessage("§5Welcome to ReverseBound Alpha! §7This is a world-generation test.");
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("rbspawn")) return false;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /rbspawn.");
            return true;
        }
        player.teleport(caveSpawn());
        player.sendMessage("§5ReverseBound §7— teleported to the End Stone test cave.");
        return true;
    }
}
