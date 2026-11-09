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

    @Override public void onEnable() {
        saveDefaultConfig();
        ownerKey = new NamespacedKey(this, "owner");
        specialKey = new NamespacedKey(this, "special-power");
        lesserA = Power.randomLesser();
        do { lesserB = Power.randomLesser(); } while (lesserB.name().equals(lesserA.name()));
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("rex").setExecutor((sender, command, label, args) -> command(sender, args));
        registerSpecialRecipes();
        getServer().getScheduler().runTaskTimer(this, this::tick, 20L, 10L);
    }

    private boolean command(org.bukkit.command.CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
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
        Bukkit.getScheduler().runTaskLater(this, () -> claim(player), 20L);
        player.sendMessage(ChatColor.AQUA + "RexSpiel AI companion granted. Use /rex claim for your second companion.");
    }

    @EventHandler public void quit(PlayerQuitEvent event) { dismiss(event.getPlayer()); }

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
