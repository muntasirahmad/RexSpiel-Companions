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
    private final double startingMoney;

    public EconomyStore(JavaPlugin plugin) {
        this.plugin = plugin;
        startingMoney = plugin.getConfig().getDouble("starting-money", 100.0);
        file = new File(plugin.getDataFolder(), "data.yml");
        data = YamlConfiguration.loadConfiguration(file);
        loadPrices();
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
            prices.put(material, (double) ThreadLocalRandom.current().nextInt(1, 51));
            data.set("prices." + material.name(), prices.get(material));
            save();
        }
        return prices.get(material);
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
