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
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.BookMeta;
import java.util.Arrays;
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
    private final Set<UUID> journalReaders = new HashSet<>();
    private final Set<String> sieves = new HashSet<>();
    private final Set<String> hammerStations = new HashSet<>();
    private final Map<UUID, Integer> sieveClicks = new HashMap<>();

    private static final String[] QUEST_NAMES = {"First Fragments", "Breaking It Down", "The Hammering Age", "Hidden Within", "A Spark of Crystal", "A Stronger Tool", "Forged in the Void", "The First Flame", "Metal From Nothing", "Beyond Survival"};
    private static final String[] QUEST_GOALS = {"Mine 16 End Stone", "Craft 16 End Fragments", "Hammer 16 End Fragments", "Sieve 16 Void Dust", "Craft 2 Resonant Crystals", "Craft 1 Primitive Pickaxe", "Place 1 Voidforge", "Craft 8 Void Briquettes", "Smelt 1 raw metal", "Smelt 8 raw metals"};
    private static final int[] QUEST_TARGETS = {16,16,16,16,2,1,1,8,1,8};
    private static final int[] QUEST_RP = {5,5,5,10,10,10,15,10,15,20};
    private String questPath(Player p) { return "quests." + p.getUniqueId(); }
    private int questStage(Player p) { return Math.min(10, Math.max(0, getConfig().getInt(questPath(p)+".stage",0))); }
    private int questProgress(Player p) { return getConfig().getInt(questPath(p)+".progress",0); }
    private int researchPoints(Player p) { return getConfig().getInt(questPath(p)+".research-points",0); }
    private void questEvent(Player p, int stage, int amount) {
        if (p.getWorld() != testWorld || p.getGameMode() == GameMode.CREATIVE || questStage(p) != stage || amount <= 0) return;
        int next = Math.min(QUEST_TARGETS[stage], questProgress(p)+amount);
        getConfig().set(questPath(p)+".progress", next);
        saveConfig();
        if (next == QUEST_TARGETS[stage]) p.sendMessage("§aQuest ready to claim: §e"+QUEST_NAMES[stage]+" §7— open /rbjournal");
        else p.sendActionBar("§d"+QUEST_NAMES[stage]+" §7"+next+"/"+QUEST_TARGETS[stage]);
    }
    private void giveReward(Player p, ItemStack item) {
        p.getInventory().addItem(item).values().forEach(extra -> p.getWorld().dropItemNaturally(p.getLocation(),extra));
    }
    private void claimQuest(Player p) {
        int stage = questStage(p);
        if (stage >= 10) { p.sendMessage("§aAll Alpha 0.8 quests complete!"); return; }
        if (questProgress(p) < QUEST_TARGETS[stage]) { p.sendMessage("§eComplete the objective before claiming."); return; }
        getConfig().set(questPath(p)+".stage",stage+1);
        getConfig().set(questPath(p)+".progress",0);
        getConfig().set(questPath(p)+".research-points",researchPoints(p)+QUEST_RP[stage]);
        saveConfig();
        switch(stage) {
            case 0 -> giveReward(p,fragments(8));
            case 1 -> giveReward(p,voidDust(4));
            case 2 -> giveReward(p,voidDust(8));
            case 3 -> giveReward(p,resource(Material.BROWN_MUSHROOM,"§aOrganic Spores","organic_spores",4));
            case 4 -> giveReward(p,resource(Material.AMETHYST_SHARD,"§dCrystal Dust","crystal_dust",4));
            case 5 -> giveReward(p,fragments(8));
            case 6 -> giveReward(p,named(Material.CHARCOAL,"§6Void Briquette",4));
            case 7 -> giveReward(p,resource(Material.BROWN_MUSHROOM,"§aOrganic Spores","organic_spores",4));
            case 8 -> giveReward(p,new ItemStack(Material.RAW_IRON,4));
            case 9 -> giveReward(p,resource(Material.CHEST,"§dVoid Lucky Block (preview)","void_lucky_block",1));
        }
        p.sendMessage("§aQuest claimed: §e"+QUEST_NAMES[stage]+" §7+"+QUEST_RP[stage]+" Research Points!");
        if (stage == 9) p.sendMessage("§5End Stone Age chapter complete! More chapters are coming.");
    }
    private void showQuestJournal(Player p) {
        Inventory menu = Bukkit.createInventory(null,27,"§5ReverseBound • Quest Journal");
        int stage=questStage(p);
        for (int i=0;i<10;i++) {
            String status=i<stage?"§aCompleted":i==stage?"§eActive":"§8Locked";
            String progress=i==stage?"§7Progress: "+questProgress(p)+"/"+QUEST_TARGETS[i]:"§7"+QUEST_GOALS[i];
            menu.setItem(i+9,menuIcon(i<stage?Material.LIME_DYE:i==stage?Material.WRITABLE_BOOK:Material.GRAY_DYE,"§d"+(i+1)+". "+QUEST_NAMES[i],"§7"+QUEST_GOALS[i],progress,status,"§bReward: "+QUEST_RP[i]+" RP"));
        }
        menu.setItem(22,menuIcon(Material.EMERALD,"§aClaim current quest",stage>=10?"§aChapter completed": "§7Click when objective is complete"));
        menu.setItem(26,menuIcon(Material.EXPERIENCE_BOTTLE,"§bResearch Points: "+researchPoints(p),"§7Research unlocks arrive in a later update"));
        journalReaders.add(p.getUniqueId());
        p.openInventory(menu);
    }
    @EventHandler
    public void onQuestCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || e.isCancelled()) return;
        ItemStack result=e.getRecipe().getResult();
        // Shift-crafting is not counted here to avoid awarding unverified amounts.
        if (e.isShiftClick()) return;
        if (isTagged(result,"end_fragments")) questEvent(p,1,result.getAmount());
        if (isTagged(result,"resonant_crystal")) questEvent(p,4,result.getAmount());
        if (isTagged(result,"primitive_pickaxe")) questEvent(p,5,1);
        if (result.getType()==Material.CHARCOAL && "§6Void Briquette".equals(result.getItemMeta()!=null?result.getItemMeta().getDisplayName():"")) questEvent(p,7,result.getAmount());
    }
    @EventHandler
    public void onQuestSmelt(FurnaceExtractEvent e) {
        if (e.getItemType()==Material.IRON_INGOT || e.getItemType()==Material.COPPER_INGOT || e.getItemType()==Material.GOLD_INGOT) {
            questEvent(e.getPlayer(),8,e.getItemAmount()); questEvent(e.getPlayer(),9,e.getItemAmount());
        }
    }
    @Override
    public void onEnable() {
        saveDefaultConfig();
        sieves.addAll(getConfig().getStringList("placed-sieves"));
        hammerStations.addAll(getConfig().getStringList("placed-hammer-stations"));
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
        // Restore visuals for stations in chunks already loaded at startup.
        for (org.bukkit.Chunk chunk : testWorld.getLoadedChunks()) restoreStationDisplays(chunk);
        getLogger().info("ReverseBound Alpha 0.9 quest tracking ready in reversebound_end.");
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
            player.sendMessage("§5Welcome to ReverseBound Alpha 0.9! §7Use /rbhelp to begin.");
        });
    }

    private ItemStack named(Material type, String name, int count) {
        ItemStack stack = new ItemStack(type, count);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack primitivePickaxe() {
        ItemStack item = named(Material.WOODEN_PICKAXE, "§dPrimitive Pickaxe", 1);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(new NamespacedKey(this, "primitive_pickaxe"), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack voidforgeItem() {
        ItemStack item = named(Material.FURNACE, "§5Voidforge", 1);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(this, "voidforge"), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    // Protect previously issued Primitive Pickaxes as well as new ones.
    @EventHandler
    public void onPrimitivePickaxeDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() != Material.WOODEN_PICKAXE || !item.hasItemMeta()) return;
        if ("§dPrimitive Pickaxe".equals(item.getItemMeta().getDisplayName()) || isTagged(item, "primitive_pickaxe")) {
            event.setCancelled(true);
        }
    }

    private void giveStarter(Player player) {
        if (player.getPersistentDataContainer().has(starterKey, PersistentDataType.BYTE)) {
            player.sendMessage("§eYour one-time starter kit has already been claimed.");
            return;
        }
        player.getPersistentDataContainer().set(starterKey, PersistentDataType.BYTE, (byte) 1);
        for (ItemStack item : new ItemStack[]{
                primitivePickaxe(),
                named(Material.WOODEN_HOE, "§dPrimitive Hammer (prototype)", 1),
                new ItemStack(Material.CRAFTING_TABLE, 1),
                fieldJournal(),
                named(Material.BROWN_MUSHROOM, "§aOrganic Spores", 2)}) {
            java.util.Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            overflow.values().forEach(extra -> player.getWorld().dropItemNaturally(player.getLocation(), extra));
        }
        player.sendMessage("§aReverseBound starter kit received! Type /rbhelp.");
    }

    private ItemStack fieldJournal() {
        ItemStack book = named(Material.WRITTEN_BOOK, "§5Field Journal", 1);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("ReverseBound Field Journal");
        meta.setAuthor("ReverseBound");
        meta.addPage("§5REVERSEBOUND\n§0The End Stone Age\n\n§0Mine End Stone and craft End Fragments.\n\n§0Right-click this journal to view your progress.");
        meta.addPage("§5FIRST MACHINES\n\n§0Hammering Station: End Fragments in four corners; End Stone elsewhere.\n\n§0Manual Sieve: Fragments in corners, End Stone center.");
        meta.addPage("§5VOIDFORGE\n\n§0Surround a Resonant Crystal with 8 End Stone.\n\n§0Use Void Briquettes as fuel to smelt raw metals.");
        meta.getPersistentDataContainer().set(new NamespacedKey(this, "field_journal"), PersistentDataType.BYTE, (byte)1);
        book.setItemMeta(meta);
        return book;
    }

    private ItemStack menuIcon(Material material, String title, String... lore) {
        ItemStack item = named(material, title, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private void openJournal(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, "§5ReverseBound • Field Journal");
        menu.setItem(10, menuIcon(Material.END_STONE, "§e1. End Stone", "§7Mine End Stone using your", "§7unbreakable Primitive Pickaxe."));
        menu.setItem(11, menuIcon(Material.END_STONE_BRICKS, "§e2. End Fragments", "§7Four End Stone in crafting corners", "§7produce four End Fragments."));
        menu.setItem(12, menuIcon(Material.SMITHING_TABLE, "§d3. Hammering Station", "§7Hold your Primitive Hammer and right-click.", "§7Sneak for batch processing."));
        menu.setItem(13, menuIcon(Material.GLASS, "§d4. Manual Sieve", "§7Hold four Void Dust and right-click", "§7four times to receive resources."));
        menu.setItem(14, menuIcon(Material.AMETHYST_SHARD, "§d5. Resonant Crystal", "§7Craft using four Crystal Dust."));
        menu.setItem(15, menuIcon(Material.FURNACE, "§5 6. Voidforge", "§7Eight End Stone around one", "§7Resonant Crystal. Smelts raw metals."));
        menu.setItem(16, menuIcon(Material.CHARCOAL, "§6Fuel: Void Briquettes", "§7Four Organic Spores yield two briquettes."));
        menu.setItem(22, menuIcon(Material.BOOK, "§aChapter 1: End Stone Age", "§7Click to view tracked quests", "§7and claim Research Points."));
        journalReaders.add(player.getUniqueId());
        player.openInventory(menu);
    }

    @EventHandler
    public void onJournalClick(InventoryClickEvent event) {
        if (!journalReaders.contains(event.getWhoClicked().getUniqueId())) return;
        if (!"§5ReverseBound • Field Journal".equals(event.getView().getTitle()) && !"§5ReverseBound • Quest Journal".equals(event.getView().getTitle())) {
            journalReaders.remove(event.getWhoClicked().getUniqueId());
            return;
        }
        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player p)) return;
            int slot=event.getRawSlot();
            if ("§5ReverseBound • Field Journal".equals(event.getView().getTitle()) && slot==22) showQuestJournal(p);
            else if ("§5ReverseBound • Quest Journal".equals(event.getView().getTitle()) && slot==22) {
                claimQuest(p); showQuestJournal(p);
            }
        }
    }

    @EventHandler
    public void onJournalUse(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!isTagged(event.getItem(), "field_journal")) return;
        event.setCancelled(true);
        openJournal(event.getPlayer());
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

    private ItemStack hammerStationItem() {
        ItemStack item = resource(Material.SMITHING_TABLE, "§5Void Hammering Station", "hammer_station", 1);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(new NamespacedKey("reversebound", "hammering_station"));
        item.setItemMeta(meta);
        return item;
    }

    private NamespacedKey stationDisplayKey() { return new NamespacedKey(this, "station_display"); }

    private void removeStationDisplay(Block block) {
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        for (Entity entity : block.getWorld().getNearbyEntities(center, 1.25, 1.25, 1.25)) {
            if (entity instanceof ItemDisplay && entity.getPersistentDataContainer()
                    .has(stationDisplayKey(), PersistentDataType.STRING)) {
                String stored = entity.getPersistentDataContainer().get(stationDisplayKey(), PersistentDataType.STRING);
                if (sieveLocation(block).equals(stored)) entity.remove();
            }
        }
    }

    private void spawnStationDisplay(Block block) {
        if (!hammerStations.contains(sieveLocation(block))) return;
        removeStationDisplay(block);
        // The display is a visual; the barrier remains the solid interaction block.
        Location location = block.getLocation().add(0.5, 0.5, 0.5);
        block.getWorld().spawn(location, ItemDisplay.class, display -> {
            display.setItemStack(hammerStationItem());
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f), new Quaternionf(),
                new Vector3f(1.0f, 1.0f, 1.0f), new Quaternionf()));
            display.getPersistentDataContainer().set(stationDisplayKey(), PersistentDataType.STRING, sieveLocation(block));
            display.setPersistent(true);
        });
    }

    private void restoreStationDisplays(org.bukkit.Chunk chunk) {
        if (testWorld == null || chunk.getWorld() != testWorld) return;
        for (String key : new HashSet<>(hammerStations)) {
            String[] parts = key.split(":");
            if (parts.length != 4 || !parts[0].equals(testWorld.getUID().toString())) continue;
            try {
                int x = Integer.parseInt(parts[1]), y = Integer.parseInt(parts[2]), z = Integer.parseInt(parts[3]);
                if ((x >> 4) != chunk.getX() || (z >> 4) != chunk.getZ()) continue;
                Block block = testWorld.getBlockAt(x, y, z);
                if (block.getType() == Material.SMITHING_TABLE || block.getType() == Material.BARRIER) {
                    block.setType(Material.BARRIER, false);
                    spawnStationDisplay(block);
                }
            } catch (NumberFormatException ignored) { }
        }
    }

    @EventHandler
    public void onStationChunkLoad(ChunkLoadEvent event) {
        if (event.getWorld() == testWorld)
            Bukkit.getScheduler().runTask(this, () -> restoreStationDisplays(event.getChunk()));
    }

    private boolean isPrimitiveHammer(ItemStack item) {
        return item != null && item.getType() == Material.WOODEN_HOE && item.hasItemMeta()
            && "§dPrimitive Hammer (prototype)".equals(item.getItemMeta().getDisplayName());
    }

    private ItemStack voidDust(int amount) {
        return resource(Material.GUNPOWDER, "§5Void Dust", "void_dust", amount);
    }

    private void saveStations() {
        getConfig().set("placed-hammer-stations", new java.util.ArrayList<>(hammerStations));
        saveConfig();
    }

    private String sieveLocation(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private void saveSieves() {
        getConfig().set("placed-sieves", new java.util.ArrayList<>(sieves));
        saveConfig();
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getWorld() != testWorld) return;
        if (isTagged(event.getItemInHand(), "hammer_station")) {
            hammerStations.add(sieveLocation(event.getBlockPlaced()));
            saveStations();
            Block placed = event.getBlockPlaced();
            Bukkit.getScheduler().runTask(this, () -> {
                if (hammerStations.contains(sieveLocation(placed))) {
                    placed.setType(Material.BARRIER, false);
                    spawnStationDisplay(placed);
                }
            });
            event.getPlayer().sendMessage("§dHammering Station placed! Hold your Primitive Hammer and right-click it. Sneak-right-click for batches.");
        }
        if (isTagged(event.getItemInHand(), "voidforge")) questEvent(event.getPlayer(),6,1);
        if (isTagged(event.getItemInHand(), "manual_sieve")) {
            sieves.add(sieveLocation(event.getBlockPlaced()));
            saveSieves();
            event.getPlayer().sendMessage("§aManual Sieve placed. Hold 4 Void Dust and right-click four times.");
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!event.isCancelled() && event.getBlock().getType()==Material.END_STONE) questEvent(event.getPlayer(),0,1);
        String key = sieveLocation(event.getBlock());
        if (hammerStations.contains(key) && !event.isCancelled()) {
            hammerStations.remove(key);
            removeStationDisplay(event.getBlock());
            event.setDropItems(false);
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE)
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), hammerStationItem());
            saveStations();
            return;
        }
        if (!event.isCancelled() && sieves.remove(key)) {
            event.setDropItems(false);
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE)
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), sieveItem());
            saveSieves();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onHammerStationClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null || !hammerStations.contains(sieveLocation(block))) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!isPrimitiveHammer(player.getInventory().getItemInMainHand())) {
            player.sendActionBar("§cHold your Primitive Hammer to use this station.");
            return;
        }
        int available = countFragments(player);
        int batches = player.isSneaking() ? Math.min(available / 4, 32) : 1;
        if (batches < 1 || !takeFragments(player, batches * 4)) {
            player.sendActionBar("§cYou need at least 4 custom End Fragments.");
            return;
        }
        questEvent(player,2,batches*4);
        ItemStack dust = voidDust(batches * 2);
        java.util.Map<Integer, ItemStack> overflow = player.getInventory().addItem(dust);
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        Location fx = block.getLocation().add(0.5, 1.0, 0.5);
        block.getWorld().spawnParticle(Particle.PORTAL, fx, 18, 0.35, 0.2, 0.35, 0.1);
        block.getWorld().playSound(block.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 0.55f, 1.5f);
        player.sendActionBar("§dHammered " + (batches * 4) + " End Fragments §7→ §5" + (batches * 2) + " Void Dust");
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
        questEvent(player,3,4);
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
        // Renewable pickaxe: three custom fragments across the top, End Stone handle.
        ShapedRecipe pickaxe = new ShapedRecipe(new NamespacedKey(this, "primitive_pickaxe_recipe"), primitivePickaxe());
        pickaxe.shape("FFF", " E ", " E ");
        pickaxe.setIngredient('F', new RecipeChoice.ExactChoice(fragments(1)));
        pickaxe.setIngredient('E', Material.END_STONE);
        Bukkit.addRecipe(pickaxe);

        // A functional vanilla furnace with a custom identity; model requires a resource pack.
        ShapedRecipe forge = new ShapedRecipe(new NamespacedKey(this, "voidforge_recipe"), voidforgeItem());
        forge.shape("EEE", "ECE", "EEE");
        forge.setIngredient('E', Material.END_STONE);
        forge.setIngredient('C', new RecipeChoice.ExactChoice(resource(Material.AMETHYST_SHARD, "§dResonant Crystal", "resonant_crystal", 1)));
        Bukkit.addRecipe(forge);
        ShapedRecipe station = new ShapedRecipe(new NamespacedKey(this, "hammer_station_recipe"), hammerStationItem());
        station.shape("FEF", "EEE", "FEF");
        station.setIngredient('F', new RecipeChoice.ExactChoice(fragments(1)));
        station.setIngredient('E', Material.END_STONE);
        Bukkit.addRecipe(station);
        registerResourceRecipes();
        // Void Dust is processed using /rbhammer, not generic End Stone Bricks.
    }

    private int countFragments(Player player) {
        int available = 0;
        for (ItemStack stack : player.getInventory().getStorageContents())
            if (isTagged(stack, "end_fragments")) available += stack.getAmount();
        return available;
    }

    private boolean takeFragments(Player player, int count) {
        if (countFragments(player) < count) return false;
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
        if (command.getName().equalsIgnoreCase("rbquests")) {
            if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
            showQuestJournal(player); return true;
        }
        if (command.getName().equalsIgnoreCase("rbjournal")) {
            if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
            openJournal(player);
            return true;
        }
        if (command.getName().equalsIgnoreCase("rbrp")) {
            if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
            player.sendMessage("§bResearch Points: "+researchPoints(player)); return true;
        }
        if (command.getName().equalsIgnoreCase("rbhelp")) {
            sender.sendMessage("§5ReverseBound Alpha 0.9 §7Use /rbquests for tracked quests and /rbrp for Research Points. Right-click your Field Journal or use /rbjournal for the in-game guide. Mine End Stone. Put 4 End Stone in the four CORNERS of a 3x3 crafting table to make 4 End Fragments. Craft a Void Hammering Station with custom End Fragments in all 4 corners and End Stone in the other 5 slots. Place it, hold the Primitive Hammer and right-click to turn 4 End Fragments into 2 Void Dust. Sneak-right-click to process up to 32 batches. Craft a Manual Sieve with custom End Fragments in the 4 corners and End Stone in the center. Place it and right-click 4 times holding 4 Void Dust. Combine 4 matching fragments into Raw Iron, Raw Copper or Raw Gold. Combine 4 Organic Spores into 2 Void Briquettes (charcoal fuel); 4 Crystal Dust into 1 Resonant Crystal. Craft a renewable Primitive Pickaxe with 3 custom End Fragments across the top and 2 End Stone in the center column. Craft a Voidforge with 8 End Stone surrounding 1 custom Resonant Crystal. Voidforge smelts like a furnace using Void Briquettes as fuel. /rbsieve remains a test command.");
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
