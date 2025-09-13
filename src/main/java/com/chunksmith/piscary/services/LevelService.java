package com.chunksmith.piscary.services;

import com.chunksmith.piscary.Piscary;
import com.chunksmith.piscary.model.Rarity;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class LevelService {
    private final Piscary plugin;
    private final NamespacedKey key;
    private int baseXpPerFish;
    private final Map<Rarity, Double> rarityMult = new EnumMap<>(Rarity.class);

    public LevelService(Piscary plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "piscary_fishing_xp");
        reload();
    }

    public void reload() {
        FileConfiguration c = plugin.getConfig();
        baseXpPerFish = Math.max(1, c.getInt("leveling.xp-per-fish", 10));
        rarityMult.clear();
        var sec = c.getConfigurationSection("leveling.xp-rarity-multipliers");
        for (Rarity r : Rarity.values()) {
            double v = 1.0;
            if (sec != null) v = sec.getDouble(r.name(), 1.0);
            rarityMult.put(r, v);
        }
    }

    public int getXp(Player p) {
        return p.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    public void setXp(Player p, int xp) {
        p.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, Math.max(0, xp));
    }

    public void addCatchXp(Player p, Rarity r) {
        int base = baseXpPerFish;
        double mult = rarityMult.getOrDefault(r, 1.0);

        // Totem XP multiplier
        mult *= plugin.totems().xpMultiplier(p);

        // Seasonal XP multiplier
        mult *= plugin.seasons().xpMultiplier();

        int add = (int)Math.max(1, Math.round(base * mult));
        setXp(p, getXp(p) + add);
    }
}
