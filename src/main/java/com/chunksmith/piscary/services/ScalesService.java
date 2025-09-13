package com.chunksmith.piscary.services;

import com.chunksmith.piscary.model.Rarity;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public class ScalesService {
    private final Plugin plugin;

    public ScalesService(Plugin plugin) {
        this.plugin = plugin;
    }

    public double winChance(Rarity rarity) {
        // Default and per-rarity overrides
        double base = plugin.getConfig().getDouble("scales.base-win-chance", 0.45D);
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("scales.rarity");
        if (sec != null) {
            ConfigurationSection r = sec.getConfigurationSection(rarity.name());
            if (r != null) {
                return r.getDouble("chance", base);
            }
        }
        return base;
    }

    public double profitMultiplier(Rarity rarity) {
        double base = plugin.getConfig().getDouble("scales.base-profit-mult", 1.8D);
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("scales.rarity");
        if (sec != null) {
            ConfigurationSection r = sec.getConfigurationSection(rarity.name());
            if (r != null) {
                return r.getDouble("profit", base);
            }
        }
        return base;
    }
}
