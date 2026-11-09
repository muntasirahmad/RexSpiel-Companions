package ai.rexspiel.companions;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class EconomyStore {
    private final JavaPlugin plugin;
    private final File file;
    private final FileConfiguration data;
    private final Map<Material, Double> prices = new HashMap<>();
    private final Map<Material, double[]> sellRanges = new HashMap<>();
    private final Map<Material, double[]> buyRanges = new HashMap<>();
    private final double startingMoney;

    public EconomyStore(JavaPlugin plugin) {
        this.plugin = plugin;
        startingMoney = plugin.getConfig().getDouble("starting-money", 100.0);
        file = new File(plugin.getDataFolder(), "data.yml");
        data = YamlConfiguration.loadConfiguration(file);
        loadPrices();
        registerDefaultRanges();
    }

    private void registerDefaultRanges() {
        range(Material.COBBLESTONE, 0.1, 0.5, 1, 2);
        range(Material.STONE, 0.1, 0.5, 1, 2);
        range(Material.COAL, 1, 2, 5, 5);
        range(Material.IRON_INGOT, 5, 8, 15, 20);
        range(Material.GOLD_INGOT, 10, 15, 30, 40);
        range(Material.DIAMOND, 40, 70, 100, 200);
        range(Material.EMERALD, 50, 80, 150, 250);
        range(Material.NETHERITE_INGOT, 400, 800, 1000, 2500);
        range(Material.WHEAT, 1, 2, 3, 5);
        range(Material.CARROT, 1, 2, 3, 5);
        range(Material.POTATO, 1, 2, 3, 5);
        range(Material.SUGAR_CANE, 2, 4, 5, 10);
        range(Material.BAMBOO, 2, 4, 5, 10);
        range(Material.PUMPKIN, 3, 6, 10, 15);
        range(Material.MELON, 3, 6, 10, 15);
        range(Material.NETHER_WART, 7, 10, 20, 20);
        range(Material.ROTTEN_FLESH, 1, 3, 5, 10);
        range(Material.BONE, 1, 3, 5, 10);
        range(Material.STRING, 1, 3, 5, 10);
        range(Material.GUNPOWDER, 8, 12, 25, 40);
        range(Material.ENDER_PEARL, 15, 30, 50, 100);
        range(Material.BLAZE_ROD, 35, 50, 100, 150);
        range(Material.GOLDEN_APPLE, 50, 100, 200, 500);
        range(Material.ENCHANTED_GOLDEN_APPLE, 1000, 3000, 5000, 15000);
        range(Material.TOTEM_OF_UNDYING, 500, 1500, 2500, 5000);
        range(Material.SPAWNER, 0, 0, 10000, 25000);
    }

    private void range(Material material, double sellMin, double sellMax, double buyMin, double buyMax) {
        sellRanges.put(material, new double[] {sellMin, sellMax});
        buyRanges.put(material, new double[] {buyMin, buyMax});
    }

    private void loadPrices() {
        for (String key : data.getConfigurationSection("prices") == null
            ? java.util.Set.<String>of()
            : data.getConfigurationSection("prices").getKeys(false)) {
            try { prices.put(Material.valueOf(key), data.getDouble("prices." + key)); }
            catch (IllegalArgumentException ignored) { }
        }
    }

    public double balance(UUID player) {
        return data.getDouble("balances." + player, startingMoney);
    }

    public void add(UUID player, double amount) {
        set(player, balance(player) + amount);
    }

    public boolean withdraw(UUID player, double amount) {
        if (amount < 0 || balance(player) < amount) return false;
        set(player, balance(player) - amount);
        return true;
    }

    public void set(UUID player, double amount) {
        data.set("balances." + player, Math.round(amount * 100.0) / 100.0);
        save();
    }

    public double price(Material material) {
        if (!prices.containsKey(material)) {
            double[] range = sellRanges.getOrDefault(material, new double[] {
                getConfigPrice("sell-price-min", 1), getConfigPrice("sell-price-max", 50)
            });
            prices.put(material, randomPrice(range[0], range[1]));
            data.set("prices." + material.name(), prices.get(material));
            save();
        }
        return prices.get(material);
    }

    public double buyPrice(Material material) {
        double[] range = buyRanges.get(material);
        return range == null ? -1 : randomPrice(range[0], range[1]);
    }

    public boolean isSellable(Material material) {
        return sellRanges.containsKey(material);
    }

    private double randomPrice(double min, double max) {
        if (min == max) return min;
        return Math.round((min + ThreadLocalRandom.current().nextDouble() * (max - min)) * 100.0) / 100.0;
    }

    private double getConfigPrice(String key, double fallback) {
        return plugin.getConfig().getDouble(key, fallback);
    }

    public Map<UUID, Double> balances() {
        Map<UUID, Double> result = new HashMap<>();
        if (data.getConfigurationSection("balances") == null) return result;
        for (String key : data.getConfigurationSection("balances").getKeys(false)) {
            try { result.put(UUID.fromString(key), data.getDouble("balances." + key)); }
            catch (IllegalArgumentException ignored) { }
        }
        return result;
    }

    public void save() {
        try {
            file.getParentFile().mkdirs();
            data.save(file);
        } catch (IOException error) {
            plugin.getLogger().warning("Could not save economy data: " + error.getMessage());
        }
    }
}
