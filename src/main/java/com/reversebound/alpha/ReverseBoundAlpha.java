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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;
import java.util.Random;
import org.bukkit.plugin.java.JavaPlugin;

public final class ReverseBoundAlpha extends JavaPlugin implements Listener {
    private World testWorld;
    private static final int CAVE_Y = 64;
    private NamespacedKey starterKey;
    private final Random rng = new Random();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        starterKey = new NamespacedKey(this, "starter_received");
        registerRecipes();
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
            giveStarter(player);
            player.sendMessage("§5Welcome to ReverseBound Alpha 0.2! §7Use /rbhelp to begin.");
        });
    }

    private ItemStack named(Material type, String name, int count) {
        ItemStack stack = new ItemStack(type, count);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        stack.setItemMeta(meta);
        return stack;
    }

    private void giveStarter(Player player) {
        if (player.getPersistentDataContainer().has(starterKey, PersistentDataType.BYTE)) {
            player.sendMessage("§eYour one-time starter kit has already been claimed.");
            return;
        }
        player.getPersistentDataContainer().set(starterKey, PersistentDataType.BYTE, (byte) 1);
        for (ItemStack item : new ItemStack[]{
                named(Material.WOODEN_PICKAXE, "§dPrimitive Pickaxe", 1),
                named(Material.WOODEN_HOE, "§dPrimitive Hammer (prototype)", 1),
                new ItemStack(Material.CRAFTING_TABLE, 1),
                named(Material.WRITTEN_BOOK, "§5Field Journal (prototype)", 1),
                named(Material.BROWN_MUSHROOM, "§aOrganic Spores", 2)}) {
            java.util.Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            overflow.values().forEach(extra -> player.getWorld().dropItemNaturally(player.getLocation(), extra));
        }
        player.sendMessage("§aReverseBound starter kit received! Type /rbhelp.");
    }

    private void registerRecipes() {
        ItemStack fragments = named(Material.END_STONE_BRICKS, "§eEnd Fragments", 4);
        ItemMeta fm = fragments.getItemMeta();
        fm.getPersistentDataContainer().set(new NamespacedKey(this, "end_fragments"), PersistentDataType.BYTE, (byte) 1);
        fragments.setItemMeta(fm);
        ShapelessRecipe fragmentRecipe = new ShapelessRecipe(new NamespacedKey(this, "end_fragments_recipe"), fragments);
        fragmentRecipe.addIngredient(Material.END_STONE);
        Bukkit.addRecipe(fragmentRecipe);
        ItemStack dust = named(Material.GUNPOWDER, "§5Void Dust", 2);
        ItemMeta dm = dust.getItemMeta();
        dm.getPersistentDataContainer().set(new NamespacedKey(this, "void_dust"), PersistentDataType.BYTE, (byte) 1);
        dust.setItemMeta(dm);
        ShapelessRecipe dustRecipe = new ShapelessRecipe(new NamespacedKey(this, "void_dust_recipe"), dust);
        dustRecipe.addIngredient(4, Material.END_STONE_BRICKS);
        Bukkit.addRecipe(dustRecipe);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("rbhelp")) {
            sender.sendMessage("§5ReverseBound §7Mine End Stone with your pickaxe. Craft End Fragments from End Stone, then Void Dust from 4 fragments. Use /rbsieve while holding at least 4 Void Dust. /rbkit grants a one-time starter kit.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("rbkit")) {
            if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
            giveStarter(player);
            return true;
        }
        if (command.getName().equalsIgnoreCase("rbsieve")) {
            if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
            if (player.getWorld() != testWorld) { player.sendMessage("§cSieving is only available in the ReverseBound test world."); return true; }
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType() != Material.GUNPOWDER || !held.hasItemMeta() || !held.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(this, "void_dust"), PersistentDataType.BYTE) || held.getAmount() < 4) {
                player.sendMessage("§cHold at least 4 custom Void Dust in your main hand."); return true;
            }
            held.setAmount(held.getAmount() - 4);
            int roll = rng.nextInt(100);
            ItemStack reward;
            if (roll < 28) reward = named(Material.IRON_NUGGET, "§7Iron Fragments", 2);
            else if (roll < 50) reward = named(Material.COPPER_INGOT, "§6Copper Fragments (prototype)", 2);
            else if (roll < 70) reward = named(Material.BROWN_MUSHROOM, "§aOrganic Spores", 2);
            else if (roll < 85) reward = named(Material.AMETHYST_SHARD, "§dCrystal Dust", 2);
            else if (roll < 95) reward = new ItemStack(Material.FLINT, 2);
            else if (roll < 99) reward = named(Material.GOLD_NUGGET, "§6Gold Fragments", 2);
            else reward = named(Material.ECHO_SHARD, "§5Rift Shard", 1);
            java.util.Map<Integer, ItemStack> overflow = player.getInventory().addItem(reward);
            overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            player.sendMessage("§dSieved 4 Void Dust and found " + reward.getItemMeta().getDisplayName() + "§d!");
            return true;
        }
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
