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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventPriority;
import org.bukkit.block.BlockFace;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.Particle;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.persistence.PersistentDataType;
import java.util.Random;
import org.bukkit.plugin.java.JavaPlugin;

public final class ReverseBoundAlpha extends JavaPlugin implements Listener {
    private World testWorld;
    private static final int CAVE_Y = 64;
    private NamespacedKey starterKey;
    private final Random rng = new Random();
    private final Set<String> sieves = new HashSet<>();
    private final Set<String> forges = new HashSet<>();
    private final Map<UUID, Integer> sieveClicks = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        sieves.addAll(getConfig().getStringList("placed-sieves"));
        forges.addAll(getConfig().getStringList("placed-voidforges"));
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
        getLogger().info("ReverseBound Alpha 0.5 bootstrap and Voidforge ready in reversebound_end.");
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
            player.sendMessage("§5Welcome to ReverseBound Alpha 0.4! §7Use /rbhelp to begin.");
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

    private ItemStack fragments(int amount) {
        ItemStack result = named(Material.END_STONE_BRICKS, "§eEnd Fragments", amount);
        ItemMeta meta = result.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(this, "end_fragments"), PersistentDataType.BYTE, (byte) 1);
        result.setItemMeta(meta);
        return result;
    }

    private boolean isTagged(ItemStack stack, String key) {
        return stack != null && stack.hasItemMeta() &&
                stack.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(this, key), PersistentDataType.BYTE);
    }

    private ItemStack sieveItem() {
        ItemStack item = named(Material.GLASS, "§dManual Sieve", 1);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(this, "manual_sieve"), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private String sieveLocation(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private void saveSieves() {
        getConfig().set("placed-sieves", new java.util.ArrayList<>(sieves));
        getConfig().set("placed-voidforges", new java.util.ArrayList<>(forges));
        saveConfig();
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getWorld() != testWorld) return;
        if (isTagged(event.getItemInHand(), "voidforge")) {
            forges.add(sieveLocation(event.getBlockPlaced()));
            saveSieves();
            event.getPlayer().sendMessage("§5Voidforge placed! Use Void Briquettes as fuel and raw ores as input.");
        }
        if (isTagged(event.getItemInHand(), "manual_sieve")) {
            sieves.add(sieveLocation(event.getBlockPlaced()));
            saveSieves();
            event.getPlayer().sendMessage("§aManual Sieve placed. Hold 4 Void Dust and right-click four times.");
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (forges.remove(sieveLocation(event.getBlock()))) {
            event.setDropItems(false);
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), voidforgeItem());
            saveSieves();
            return;
        }
        if (!sieves.remove(sieveLocation(event.getBlock()))) return;
        event.setDropItems(false);
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), sieveItem());
        saveSieves();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSieveClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null || !sieves.contains(sieveLocation(block))) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!isTagged(held, "void_dust") || held.getAmount() < 4) {
            sieveClicks.remove(player.getUniqueId());
            player.sendMessage("§cHold at least 4 custom Void Dust to use this sieve.");
            return;
        }
        int clicks = sieveClicks.getOrDefault(player.getUniqueId(), 0) + 1;
        if (clicks < 4) {
            sieveClicks.put(player.getUniqueId(), clicks);
            player.sendActionBar("§dSieving Void Dust: " + clicks + "/4");
            player.getWorld().playSound(block.getLocation(), org.bukkit.Sound.BLOCK_GRAVEL_HIT, 0.6f, 1.2f);
            player.getWorld().spawnParticle(Particle.CLOUD, block.getLocation().add(0.5, 1, 0.5), 3, 0.15, 0.1, 0.15, 0.01);
            return;
        }
        sieveClicks.remove(player.getUniqueId());
        held.setAmount(held.getAmount() - 4);
        ItemStack reward = sieveReward();
        player.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1.0, 0.5), reward);
        player.sendMessage("§aSieved 4 Void Dust and found " + rewardName(reward) + "§a!");
        player.getWorld().playSound(block.getLocation(), org.bukkit.Sound.BLOCK_GRAVEL_BREAK, 0.7f, 1f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 1.1, 0.5), 12, 0.25, 0.2, 0.25);
    }

    private String rewardName(ItemStack reward) {
        return reward.hasItemMeta() && reward.getItemMeta().hasDisplayName()
            ? reward.getItemMeta().getDisplayName() : reward.getType().name();
    }

    private ItemStack resource(Material material, String display, String key, int amount) {
        ItemStack stack = named(material, display, amount);
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(this, key), PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack sieveReward() {
        int roll = rng.nextInt(100);
        if (roll < 28) return resource(Material.IRON_NUGGET, "§7Iron Fragments", "iron_fragments", 2);
        if (roll < 50) return resource(Material.COPPER_INGOT, "§6Copper Fragments", "copper_fragments", 2);
        if (roll < 70) return resource(Material.BROWN_MUSHROOM, "§aOrganic Spores", "organic_spores", 2);
        if (roll < 85) return resource(Material.AMETHYST_SHARD, "§dCrystal Dust", "crystal_dust", 2);
        if (roll < 95) return new ItemStack(Material.FLINT, 2);
        if (roll < 99) return resource(Material.GOLD_NUGGET, "§6Gold Fragments", "gold_fragments", 2);
        return resource(Material.ECHO_SHARD, "§5Rift Shard", "rift_shard", 1);
    }

    private void addConversion(String id, ItemStack input, ItemStack output) {
        ShapelessRecipe recipe = new ShapelessRecipe(new NamespacedKey(this, id), output);
        for (int i = 0; i < 4; i++) recipe.addIngredient(new RecipeChoice.ExactChoice(input));
        Bukkit.addRecipe(recipe);
    }

    private void registerResourceRecipes() {
        addConversion("iron_fragments_to_raw_iron", resource(Material.IRON_NUGGET, "§7Iron Fragments", "iron_fragments", 1), new ItemStack(Material.RAW_IRON));
        addConversion("copper_fragments_to_raw_copper", resource(Material.COPPER_INGOT, "§6Copper Fragments", "copper_fragments", 1), new ItemStack(Material.RAW_COPPER));
        addConversion("gold_fragments_to_raw_gold", resource(Material.GOLD_NUGGET, "§6Gold Fragments", "gold_fragments", 1), new ItemStack(Material.RAW_GOLD));
        addConversion("organic_spores_to_fuel", resource(Material.BROWN_MUSHROOM, "§aOrganic Spores", "organic_spores", 1), named(Material.CHARCOAL, "§6Void Briquette", 2));
        addConversion("crystal_dust_to_crystal", resource(Material.AMETHYST_SHARD, "§dCrystal Dust", "crystal_dust", 1), resource(Material.AMETHYST_SHARD, "§dResonant Crystal", "resonant_crystal", 1));
        // Backwards compatibility for resource items obtained from Alpha 0.3.
        addConversion("legacy_iron_to_raw", named(Material.IRON_NUGGET, "§7Iron Fragments", 1), new ItemStack(Material.RAW_IRON));
        addConversion("legacy_copper_to_raw", named(Material.COPPER_INGOT, "§6Copper Fragments (prototype)", 1), new ItemStack(Material.RAW_COPPER));
        addConversion("legacy_gold_to_raw", named(Material.GOLD_NUGGET, "§6Gold Fragments", 1), new ItemStack(Material.RAW_GOLD));
        addConversion("legacy_spores_to_fuel", named(Material.BROWN_MUSHROOM, "§aOrganic Spores", 1), named(Material.CHARCOAL, "§6Void Briquette", 2));
        addConversion("legacy_crystal_to_crystal", named(Material.AMETHYST_SHARD, "§dCrystal Dust", 1), resource(Material.AMETHYST_SHARD, "§dResonant Crystal", "resonant_crystal", 1));
    }

    private ItemStack voidforgeItem() {
        return resource(Material.FURNACE, "§5Voidforge §7(Primitive End Furnace)", "voidforge", 1);
    }

    private void registerBootstrapRecipes() {
        // These recipes use only End Stone and are recoverable after every starter tool breaks.
        ShapelessRecipe sticks = new ShapelessRecipe(new NamespacedKey(this, "end_stone_to_sticks"), new ItemStack(Material.STICK, 4));
        sticks.addIngredient(Material.END_STONE);
        Bukkit.addRecipe(sticks);

        ShapedRecipe pick = new ShapedRecipe(new NamespacedKey(this, "renewable_primitive_pickaxe"), named(Material.WOODEN_PICKAXE, "§dPrimitive Pickaxe", 1));
        pick.shape("FFF", " S ", " S ");
        pick.setIngredient('F', new RecipeChoice.ExactChoice(fragments(1)));
        pick.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(pick);

        ShapedRecipe hammer = new ShapedRecipe(new NamespacedKey(this, "renewable_primitive_hammer"), named(Material.WOODEN_HOE, "§dPrimitive Hammer (prototype)", 1));
        hammer.shape("FF ", " S ", " S ");
        hammer.setIngredient('F', new RecipeChoice.ExactChoice(fragments(1)));
        hammer.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(hammer);

        ShapedRecipe forge = new ShapedRecipe(new NamespacedKey(this, "voidforge_recipe"), voidforgeItem());
        forge.shape("EEE", "E E", "EEE");
        forge.setIngredient('E', Material.END_STONE);
        Bukkit.addRecipe(forge);
    }

    private void registerRecipes() {
        // Corners of a 3x3 crafting table, not the vanilla 2x2 End Stone Bricks recipe.
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "end_fragments_recipe"), fragments(4));
        recipe.shape("E E", "   ", "E E");
        recipe.setIngredient('E', Material.END_STONE);
        Bukkit.addRecipe(recipe);
        ShapedRecipe sieve = new ShapedRecipe(new NamespacedKey(this, "manual_sieve_recipe"), sieveItem());
        sieve.shape("F F", " E ", "F F");
        sieve.setIngredient('F', new RecipeChoice.ExactChoice(fragments(1)));
        sieve.setIngredient('E', Material.END_STONE);
        Bukkit.addRecipe(sieve);
        registerResourceRecipes();
        registerBootstrapRecipes();
        // Void Dust is processed using /rbhammer, not generic End Stone Bricks.
    }

    private boolean takeFragments(Player player, int count) {
        int available = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (isTagged(stack, "end_fragments")) available += stack.getAmount();
        }
        if (available < count) return false;
        int remaining = count;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (!isTagged(stack, "end_fragments")) continue;
            int used = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - used);
            remaining -= used;
            if (remaining == 0) break;
        }
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("rbhelp")) {
            sender.sendMessage("§5ReverseBound §7Mine End Stone. Put 4 End Stone in the four CORNERS of a 3x3 crafting table to make 4 End Fragments. Hold the Primitive Hammer and use /rbhammer to turn 4 tagged fragments into 2 Void Dust. Craft a Manual Sieve with custom End Fragments in the 4 corners and End Stone in the center. Place it and right-click 4 times holding 4 Void Dust. Combine 4 matching fragments into Raw Iron, Raw Copper or Raw Gold. 1 End Stone crafts 4 sticks; 3 tagged End Fragments plus 2 sticks crafts a new Primitive Pickaxe. 2 fragments and 2 sticks crafts a new Hammer. 8 End Stone around an empty center crafts a Voidforge (currently a functional furnace block). Combine 4 Organic Spores into 2 Void Briquettes (charcoal fuel); 4 Crystal Dust into 1 Resonant Crystal. /rbsieve remains a test command.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("rbkit")) {
            if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
            giveStarter(player);
            return true;
        }
        if (command.getName().equalsIgnoreCase("rbhammer")) {
            if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
            if (player.getWorld() != testWorld) { player.sendMessage("§cHammer processing is only available in the ReverseBound test world."); return true; }
            ItemStack hammer = player.getInventory().getItemInMainHand();
            if (hammer.getType() != Material.WOODEN_HOE || !hammer.hasItemMeta() ||
                    !"§dPrimitive Hammer (prototype)".equals(hammer.getItemMeta().getDisplayName())) {
                player.sendMessage("§cHold your Primitive Hammer in your main hand."); return true;
            }
            if (!takeFragments(player, 4)) { player.sendMessage("§cYou need 4 custom End Fragments (not vanilla End Stone Bricks)."); return true; }
            ItemStack dust = named(Material.GUNPOWDER, "§5Void Dust", 2);
            ItemMeta meta = dust.getItemMeta();
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "void_dust"), PersistentDataType.BYTE, (byte) 1);
            dust.setItemMeta(meta);
            java.util.Map<Integer, ItemStack> overflow = player.getInventory().addItem(dust);
            overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            player.sendMessage("§aProcessed 4 End Fragments into 2 Void Dust.");
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
            ItemStack reward = sieveReward();
            java.util.Map<Integer, ItemStack> overflow = player.getInventory().addItem(reward);
            overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            player.sendMessage("§dSieved 4 Void Dust and found " + rewardName(reward) + "§d!");
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
