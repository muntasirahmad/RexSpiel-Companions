package ai.rexspiel.companions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class RexSpielCompanions extends JavaPlugin implements Listener {
    private final Map<UUID, Power> ownerPowers = new HashMap<>();
    private final Map<UUID, List<UUID>> companions = new HashMap<>();
    private final Map<UUID, Power> companionPowers = new HashMap<>();
    private NamespacedKey ownerKey;
    private NamespacedKey specialKey;
    private Power lesserA;
    private Power lesserB;
    private String lesserRecipeA;
    private String lesserRecipeB;
    private EconomyStore economy;
    private final Map<UUID, UUID> teleportRequests = new HashMap<>();
    private final Map<UUID, Long> teleportRequestTimes = new HashMap<>();
    private Scoreboard scoreboard;

    @Override public void onEnable() {
        saveDefaultConfig();
        economy = new EconomyStore(this);
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        ownerKey = new NamespacedKey(this, "owner");
        specialKey = new NamespacedKey(this, "special-power");
        lesserA = Power.randomLesser();
        do { lesserB = Power.randomLesser(); } while (lesserB.name().equals(lesserA.name()));
        getServer().getPluginManager().registerEvents(this, this);
        String[] commands = {"rex", "tpa", "tpaccept", "tpdeny", "sethome", "home", "delhome", "money", "check", "baltop", "economy", "pay", "sell", "coords"};
        for (String name : commands) getCommand(name).setExecutor((sender, command, label, args) -> command(sender, command.getName(), args));
        registerSpecialRecipes();
        getServer().getScheduler().runTaskTimer(this, this::tick, 20L, 10L);
    }

    private boolean command(CommandSender sender, String name, String[] args) {
        if (name.equalsIgnoreCase("economy")) return economyCommand(sender, args);
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        if (!name.equalsIgnoreCase("rex")) return utilityCommand(player, name.toLowerCase(), args);
        String sub = args.length == 0 ? "status" : args[0].toLowerCase();
        if (sub.equals("claim") || sub.equals("summon")) { claim(player); return true; }
        if (sub.equals("dismiss")) { dismiss(player); player.sendMessage(ChatColor.YELLOW + "Companions dismissed."); return true; }
        if (sub.equals("power") || sub.equals("status")) {
            player.sendMessage(ownerPower(player).color() + "Your power: " + ownerPower(player).name());
            return true;
        }
        if (sub.equals("powers")) {
            player.sendMessage(ChatColor.AQUA + "50 powers. Rex Prime: 5 wood. Lesser A: " + lesserRecipeA + ". Lesser B: " + lesserRecipeB + ".");
            return true;
        }
        player.sendMessage(ChatColor.YELLOW + "/rex claim, /rex dismiss, /rex power, /rex powers, /rex status");
        return true;
    }

    @EventHandler public void join(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ownerPower(player);
        updateMoneyTag(player);
        Bukkit.getScheduler().runTaskLater(this, () -> claim(player), 20L);
        player.sendMessage(ChatColor.AQUA + "RexSpiel AI companion granted. Use /rex claim for your second companion.");
    }

    @EventHandler public void quit(PlayerQuitEvent event) { dismiss(event.getPlayer()); }

    private boolean utilityCommand(Player player, String name, String[] args) {
        switch (name) {
            case "tpa" -> {
                if (args.length != 1) { player.sendMessage(ChatColor.YELLOW + "Use /tpa <player>"); return true; }
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null || target.equals(player)) { player.sendMessage(ChatColor.RED + "Player not found."); return true; }
                teleportRequests.put(target.getUniqueId(), player.getUniqueId());
                teleportRequestTimes.put(target.getUniqueId(), System.currentTimeMillis());
                target.sendMessage(ChatColor.AQUA + player.getName() + " requested teleport. Use /tpaccept or /tpdeny.");
                player.sendMessage(ChatColor.GREEN + "Teleport request sent.");
            }
            case "tpaccept", "tpdeny" -> {
                UUID requesterId = teleportRequests.remove(player.getUniqueId());
                Long created = teleportRequestTimes.remove(player.getUniqueId());
                if (requesterId == null || created == null || System.currentTimeMillis() - created > 60_000) {
                    player.sendMessage(ChatColor.RED + "No active teleport request."); return true;
                }
                Player requester = Bukkit.getPlayer(requesterId);
                if (requester == null) { player.sendMessage(ChatColor.RED + "Requester is offline."); return true; }
                if (name.equals("tpdeny")) { requester.sendMessage(ChatColor.YELLOW + "Teleport request denied."); player.sendMessage(ChatColor.YELLOW + "Denied."); return true; }
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (requester.isOnline() && player.isOnline()) requester.teleport(player.getLocation());
                }, 20L * getConfig().getInt("teleport-delay-seconds", 3));
                requester.sendMessage(ChatColor.GREEN + "Teleporting to " + player.getName() + "...");
            }
            case "sethome" -> setHome(player, args);
            case "home" -> home(player, args);
            case "delhome" -> deleteHome(player, args);
            case "money" -> showMoney(player, args);
            case "check" -> checkBalance(player, args);
            case "baltop" -> showBalanceTop(player);
            case "pay" -> pay(player, args);
            case "sell" -> sell(player, args);
            case "coords" -> player.sendMessage(ChatColor.AQUA + coordinates(player.getLocation()));
            default -> player.sendMessage(ChatColor.YELLOW + "Unknown command.");
        }
        return true;
    }

    private boolean economyCommand(CommandSender sender, String[] args) {
            if (args.length > 0 && args[0].equalsIgnoreCase("top")) {
                if (sender instanceof Player player) showBalanceTop(player);
                else sender.sendMessage("Use /baltop in-game.");
                return true;
            }
            if (!(sender instanceof Player player)) {
                if (!sender.hasPermission("rex.admin")) { sender.sendMessage("You need rex.admin."); return true; }
                return adminEconomy(sender, args);
            }
            if (args.length == 0) { showMoney(player, new String[0]); return true; }
            if (args[0].equalsIgnoreCase("balance")) { showMoney(player, java.util.Arrays.copyOfRange(args, 1, args.length)); return true; }
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take") || args[0].equalsIgnoreCase("set")) {
                if (!player.hasPermission("rex.admin")) { player.sendMessage(ChatColor.RED + "You need rex.admin."); return true; }
                return adminEconomy(player, args);
            }
            player.sendMessage(ChatColor.YELLOW + "Use /economy top, /economy balance, or /eco give|take|set <player> <amount>.");
            return true;
        }

    private boolean adminEconomy(CommandSender sender, String[] args) {
            if (args.length != 3) { sender.sendMessage("Use /eco give|take|set <player> <amount>"); return true; }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            double amount = parseAmount(args[2]);
            if (amount < 0) { sender.sendMessage("Amount must be non-negative."); return true; }
            double current = economy.balance(target.getUniqueId());
            double next = args[0].equalsIgnoreCase("give") ? current + amount
                : args[0].equalsIgnoreCase("take") ? Math.max(0, current - amount) : amount;
            economy.set(target.getUniqueId(), next);
            if (target.isOnline()) updateMoneyTag(target.getPlayer());
            sender.sendMessage(ChatColor.GREEN + "Updated " + target.getName() + " balance to $" + money(next) + ".");
            return true;
        }

    private void showMoney(Player player, String[] args) {
            OfflinePlayer target = args.length == 0 ? player : Bukkit.getOfflinePlayer(args[0]);
            player.sendMessage(ChatColor.GOLD + target.getName() + "'s balance: $" + money(economy.balance(target.getUniqueId())));
        }
    private void setHome(Player player, String[] args) {
        String home = args.length == 0 ? "home" : args[0].toLowerCase();
        if (!home.matches("[a-z0-9_-]{1,16}")) { player.sendMessage(ChatColor.RED + "Invalid home name."); return; }
        FileConfiguration data = getDataConfig();
        String base = "homes." + player.getUniqueId() + "." + home;
        data.set(base + ".world", player.getWorld().getName());
        data.set(base + ".x", player.getLocation().getX()); data.set(base + ".y", player.getLocation().getY());
        data.set(base + ".z", player.getLocation().getZ()); data.set(base + ".yaw", player.getLocation().getYaw());
        data.set(base + ".pitch", player.getLocation().getPitch()); saveData(data);
        player.sendMessage(ChatColor.GREEN + "Home '" + home + "' set at " + coordinates(player.getLocation()));
    }

    private void home(Player player, String[] args) {
        String home = args.length == 0 ? "home" : args[0].toLowerCase();
        String base = "homes." + player.getUniqueId() + "." + home;
        FileConfiguration data = getDataConfig();
        if (!data.contains(base)) { player.sendMessage(ChatColor.RED + "Home not found."); return; }
        org.bukkit.World world = Bukkit.getWorld(data.getString(base + ".world", ""));
        if (world == null) { player.sendMessage(ChatColor.RED + "Home world is unavailable."); return; }
        player.teleport(new Location(world, data.getDouble(base + ".x"), data.getDouble(base + ".y"), data.getDouble(base + ".z"),
            (float) data.getDouble(base + ".yaw"), (float) data.getDouble(base + ".pitch")));
    }

    private void deleteHome(Player player, String[] args) {
        String home = args.length == 0 ? "home" : args[0].toLowerCase();
        FileConfiguration data = getDataConfig(); data.set("homes." + player.getUniqueId() + "." + home, null); saveData(data);
        player.sendMessage(ChatColor.YELLOW + "Home deleted.");
    }

    private void pay(Player player, String[] args) {
        if (args.length != 2) { player.sendMessage(ChatColor.YELLOW + "Use /pay <player> <amount>"); return; }
        Player target = Bukkit.getPlayerExact(args[0]); double amount = parseAmount(args[1]);
        if (target == null || amount <= 0 || target.equals(player)) { player.sendMessage(ChatColor.RED + "Invalid player or amount."); return; }
        if (!economy.withdraw(player.getUniqueId(), amount)) { player.sendMessage(ChatColor.RED + "Not enough money."); return; }
        economy.add(target.getUniqueId(), amount); updateMoneyTag(player); updateMoneyTag(target);
        player.sendMessage(ChatColor.GREEN + "Paid $" + money(amount) + " to " + target.getName() + ".");
        target.sendMessage(ChatColor.GREEN + "Received $" + money(amount) + " from " + player.getName() + ".");
    }

    private void checkBalance(Player player, String[] args) {
        Player target = args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("balance"))
            ? player
            : Bukkit.getPlayerExact(args[args.length - 1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }
        player.sendMessage(ChatColor.GOLD + target.getName() + "'s balance: $" + money(economy.balance(target.getUniqueId())));
    }

    private void showBalanceTop(Player player) {
        List<Map.Entry<UUID, Double>> top = new ArrayList<>(economy.balances().entrySet());
        top.sort((left, right) -> Double.compare(right.getValue(), left.getValue()));
        player.sendMessage(ChatColor.GOLD + "---- Balance Top ----");
        int rank = 1;
        for (Map.Entry<UUID, Double> entry : top) {
            if (rank > 10) break;
            OfflinePlayer offline = Bukkit.getOfflinePlayer(entry.getKey());
            player.sendMessage(ChatColor.YELLOW + "#" + rank++ + " " + offline.getName() + ": $" + money(entry.getValue()));
        }
        if (top.isEmpty()) player.sendMessage(ChatColor.GRAY + "No balances recorded yet.");
    }

    private void sell(Player player, String[] args) {
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (args.length > 0 && !args[0].equalsIgnoreCase("hand")) {
            Material material = Material.matchMaterial(args[0]);
            if (material == null) { player.sendMessage(ChatColor.RED + "Unknown item."); return; }
            stack = new ItemStack(material);
        }
        if (stack.getType().isAir()) { player.sendMessage(ChatColor.RED + "Hold an item or use /sell <item> <amount>."); return; }
        int amount = args.length > 1 ? Math.max(1, parseInt(args[1], 1)) : stack.getAmount();
        int available = player.getInventory().containsAtLeast(stack, amount) ? amount : 0;
        if (available == 0) { player.sendMessage(ChatColor.RED + "You do not have enough of that item."); return; }
        player.getInventory().removeItem(new ItemStack(stack.getType(), available));
        double earned = economy.price(stack.getType()) * available;
        economy.add(player.getUniqueId(), earned); updateMoneyTag(player);
        player.sendMessage(ChatColor.GREEN + "Sold " + available + " " + stack.getType().name() + " for $" + money(earned) + ".");
    }

    private double parseAmount(String value) { try { return Math.round(Double.parseDouble(value) * 100.0) / 100.0; } catch (NumberFormatException e) { return -1; } }
    private int parseInt(String value, int fallback) { try { return Integer.parseInt(value); } catch (NumberFormatException e) { return fallback; } }
    private String money(double value) { return String.format(java.util.Locale.US, "%.2f", value); }
    private String coordinates(Location location) { return location.getWorld().getName() + " " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ(); }
    private FileConfiguration getDataConfig() { return YamlConfiguration.loadConfiguration(new java.io.File(getDataFolder(), "data.yml")); }
    private void saveData(FileConfiguration data) { try { getDataFolder().mkdirs(); data.save(new java.io.File(getDataFolder(), "data.yml")); } catch (java.io.IOException e) { getLogger().warning("Could not save data: " + e.getMessage()); } }
    private void updateMoneyTag(Player player) {
        String teamName = "rex_" + Integer.toHexString(player.getUniqueId().hashCode());
        Team team = scoreboard.getTeam(teamName);
        if (team == null) team = scoreboard.registerNewTeam(teamName);
        team.setPrefix(ChatColor.GOLD + "$" + money(economy.balance(player.getUniqueId())) + " " + ChatColor.RESET);
        if (!team.hasEntry(player.getName())) team.addEntry(player.getName());
    }

    @EventHandler public void ownerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player owner)) return;
        if (event.getDamager() instanceof LivingEntity attacker) {
            for (Wolf wolf : wolves(owner)) wolf.setTarget(attacker);
        }
    }

    @EventHandler public void companionTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Wolf wolf) {
            Player owner = owner(wolf);
            if (owner == null) return;
            if (event.getTarget() != null && event.getTarget().equals(owner)) event.setCancelled(true);
        }
    }

    @EventHandler public void specialCompanionUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getItem() == null) return;
        String powerName = event.getItem().getItemMeta() == null ? null :
            event.getItem().getItemMeta().getPersistentDataContainer().get(specialKey, PersistentDataType.STRING);
        if (powerName == null) return;
        Power selected = Power.ALL.stream().filter(power -> power.name().equals(powerName)).findFirst().orElse(null);
        if (selected == null || wolves(event.getPlayer()).size() >= getConfig().getInt("max-companions-per-player", 2)) return;
        event.setCancelled(true);
        event.getItem().setAmount(event.getItem().getAmount() - 1);
        summon(event.getPlayer(), selected);
        event.getPlayer().sendMessage(selected.color() + "Special companion summoned: " + selected.name());
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Power ownerPower = ownerPower(player);
            if (getConfig().getBoolean("owner-passive-power", true)) {
                player.addPotionEffect(new PotionEffect(ownerPower.passive(), 40, ownerPower.amplifier(), true, false, true));
            }
            for (Wolf wolf : wolves(player)) {
                if (wolf.getLocation().distanceSquared(player.getLocation()) > 4) wolf.teleport(player.getLocation().add(1, 0, 1));
                Power power = companionPowers.getOrDefault(wolf.getUniqueId(), ownerPower);
                wolf.getWorld().spawnParticle(power.particle(), wolf.getLocation().add(0, 1, 0), 2, .2, .3, .2, 0);
                LivingEntity target = findTarget(player, wolf);
                if (target != null) { wolf.setAngry(true); wolf.setTarget(target); } else wolf.setAngry(false);
            }
        }
    }

    private LivingEntity findTarget(Player owner, Wolf wolf) {
        double distance = getConfig().getDouble("max-distance", 32);
        for (Entity entity : wolf.getNearbyEntities(distance, distance, distance)) {
            if (!(entity instanceof LivingEntity living) || living.equals(owner) || living.equals(wolf)) continue;
            boolean hostile = entity.getType() == EntityType.ZOMBIE || entity.getType() == EntityType.SKELETON ||
                entity.getType() == EntityType.CREEPER || entity.getType() == EntityType.SPIDER ||
                entity.getType() == EntityType.WITHER || entity.getType() == EntityType.ENDER_DRAGON;
            if (hostile && !(entity instanceof Player)) return living;
        }
        return null;
    }

    private void claim(Player owner) {
        if (wolves(owner).size() >= getConfig().getInt("max-companions-per-player", 2)) return;
        summon(owner, ownerPower(owner));
    }

    private void summon(Player owner, Power power) {
        Wolf wolf = (Wolf) owner.getWorld().spawnEntity(owner.getLocation().add(1, 0, 1), EntityType.WOLF);
        wolf.setTamed(true); wolf.setOwner(owner); wolf.setAdult(); wolf.setSitting(false);
        wolf.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
        wolf.setCustomName(power.color() + power.aiName() + " [" + power.name() + "]");
        wolf.setCustomNameVisible(true);
        wolf.getAttribute(Attribute.MAX_HEALTH).setBaseValue(40); wolf.setHealth(40);
        companions.computeIfAbsent(owner.getUniqueId(), ignored -> new ArrayList<>()).add(wolf.getUniqueId());
        companionPowers.put(wolf.getUniqueId(), power);
    }

    private void dismiss(Player owner) {
        List<UUID> ids = companions.remove(owner.getUniqueId());
        if (ids == null) return;
        for (UUID id : ids) { Entity entity = Bukkit.getEntity(id); if (entity != null) entity.remove(); companionPowers.remove(id); }
    }

    private Power ownerPower(Player player) {
        return ownerPowers.computeIfAbsent(player.getUniqueId(), ignored -> Power.ALL.get(ThreadLocalRandom.current().nextInt(Power.ALL.size())));
    }

    private List<Wolf> wolves(Player owner) {
        List<Wolf> result = new ArrayList<>();
        for (UUID id : companions.getOrDefault(owner.getUniqueId(), List.of())) {
            Entity entity = Bukkit.getEntity(id);
            if (entity instanceof Wolf wolf && wolf.isValid()) result.add(wolf);
        }
        return result;
    }

    private Player owner(Wolf wolf) {
        String value = wolf.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        try { return value == null ? null : Bukkit.getPlayer(UUID.fromString(value)); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private void registerSpecialRecipes() {
        registerPrimeRecipe();
        registerRandomRecipe("rex_lesser_a", lesserA);
        registerRandomRecipe("rex_lesser_b", lesserB);
    }

    private void registerPrimeRecipe() {
        Power power = Power.strongest();
        Material[] planks = {
            Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS,
            Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS,
            Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS, Material.BAMBOO_PLANKS
        };
        for (Material plank : planks) {
            ItemStack result = specialItem(power);
            String key = "rex_prime_" + plank.name().toLowerCase();
            ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, key), result);
            recipe.shape(" W ", "WWW", " W ");
            recipe.setIngredient('W', plank);
            Bukkit.addRecipe(recipe);
        }
        getLogger().info("rex_prime recipes: 5 wood planks -> " + power.aiName());
    }

    private void registerRandomRecipe(String key, Power power) {
        Material[] pool = {
            Material.COAL, Material.IRON_INGOT, Material.GOLD_INGOT, Material.COPPER_INGOT,
            Material.REDSTONE, Material.LAPIS_LAZULI, Material.QUARTZ, Material.AMETHYST_SHARD,
            Material.PRISMARINE_SHARD, Material.BONE, Material.STRING, Material.GUNPOWDER,
            Material.ENDER_PEARL, Material.BLAZE_POWDER, Material.SLIME_BALL, Material.HONEYCOMB,
            Material.PAPER, Material.GLASS, Material.BRICK, Material.CLAY_BALL, Material.OBSIDIAN,
            Material.OAK_LOG, Material.COBBLESTONE, Material.MOSS_BLOCK, Material.ICE
        };
        Material[] ingredients = new Material[5];
        for (int i = 0; i < ingredients.length; i++) {
            Material selected;
            do {
                selected = pool[ThreadLocalRandom.current().nextInt(pool.length)];
            } while (contains(ingredients, selected));
            ingredients[i] = selected;
        }
        ItemStack result = specialItem(power);
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, key), result);
        recipe.shape("ABC", "D E", "   ");
        recipe.setIngredient('A', ingredients[0]);
        recipe.setIngredient('B', ingredients[1]);
        recipe.setIngredient('C', ingredients[2]);
        recipe.setIngredient('D', ingredients[3]);
        recipe.setIngredient('E', ingredients[4]);
        Bukkit.addRecipe(recipe);
        String recipeText = ingredients[0] + "+" + ingredients[1] + "+" + ingredients[2] + "+" +
            ingredients[3] + "+" + ingredients[4];
        if (key.equals("rex_lesser_b") && recipeText.equals(lesserRecipeA)) {
            registerRandomRecipe(key, power);
            return;
        }
        if (key.equals("rex_lesser_a")) lesserRecipeA = recipeText;
        else lesserRecipeB = recipeText;
        getLogger().info(key + " recipe power: " + power.aiName() + " | " +
            ingredients[0] + ", " + ingredients[1] + ", " + ingredients[2] + ", " +
            ingredients[3] + ", " + ingredients[4]);
    }

    private boolean contains(Material[] values, Material selected) {
        for (Material value : values) if (value == selected) return true;
        return false;
    }

    private ItemStack specialItem(Power power) {
        ItemStack result = new ItemStack(power.icon());
        ItemMeta meta = result.getItemMeta();
        meta.setDisplayName(power.color() + power.aiName() + " AI Companion");
        meta.setLore(List.of(ChatColor.GRAY + "Power: " + power.name(), ChatColor.YELLOW + "Right-click to summon"));
        meta.getPersistentDataContainer().set(specialKey, PersistentDataType.STRING, power.name());
        result.setItemMeta(meta);
        return result;
    }
}
