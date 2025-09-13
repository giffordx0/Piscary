package com.chunksmith.piscary.services;

import com.chunksmith.piscary.model.Rarity;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class EntropyService {
    private final Plugin plugin;
    private final NamespacedKey key;

    public EntropyService(Plugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "piscary_entropy");
    }

    public int get(Player p) {
        return p.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    public void set(Player p, int amount) {
        p.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, Math.max(0, amount));
    }

    public void add(Player p, int delta) {
        if (delta == 0) return;
        set(p, get(p) + delta);
    }

    /** Base amount per fish from config + rarity multipliers. */
    public int gutAmountFor(Rarity rarity) {
        double base = plugin.getConfig().getDouble("entropy.per-fish-base", 5.0);
        double mult = plugin.getConfig().getDouble("entropy.rarity-multipliers." + rarity.name(), 1.0);
        return (int)Math.max(0, Math.round(base * mult));
    }
}
