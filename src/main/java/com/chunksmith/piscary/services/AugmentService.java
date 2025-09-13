package com.chunksmith.piscary.services;

import com.chunksmith.piscary.Piscary;
import com.chunksmith.piscary.model.AugmentType;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class AugmentService {
    private final Piscary plugin;
    private final NamespacedKey key; // store as compact string "LUCK=2;DOUBLE_HOOK=1"
    private final Map<AugmentType, Integer> maxLevel = new EnumMap<>(AugmentType.class);
    private final Map<AugmentType, Integer> baseCost = new EnumMap<>(AugmentType.class);
    private final Map<AugmentType, Double> costMult = new EnumMap<>(AugmentType.class);
    private final Map<AugmentType, Integer> extraRollsPerLevel = new EnumMap<>(AugmentType.class);
    private final Map<AugmentType, Double> extraDropChancePerLevel = new EnumMap<>(AugmentType.class);
    private final Map<AugmentType, Double> gutBonusPerLevel = new EnumMap<>(AugmentType.class);

    public AugmentService(Piscary plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "piscary_augments");
        reload();
    }

    public void reload() {
        maxLevel.clear(); baseCost.clear(); costMult.clear();
        extraRollsPerLevel.clear(); extraDropChancePerLevel.clear(); gutBonusPerLevel.clear();

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("augments");
        for (AugmentType t : AugmentType.values()) {
            String path = tToPath(t);
            ConfigurationSection s = sec == null ? null : sec.getConfigurationSection(path);
            maxLevel.put(t, s == null ? 3 : s.getInt("max-level", 3));
            baseCost.put(t, s == null ? 100 : s.getInt("base-cost", 100));
            costMult.put(t, s == null ? 1.6 : s.getDouble("cost-mult", 1.6));

            // behavior params
            switch (t) {
                case LUCK -> extraRollsPerLevel.put(t, s == null ? 1 : s.getInt("extra-rolls-per-level", 1));
                case DOUBLE_HOOK -> extraDropChancePerLevel.put(t, s == null ? 0.10 : s.getDouble("extra-drop-chance-per-level", 0.10));
                case ENTROPY -> gutBonusPerLevel.put(t, s == null ? 0.10 : s.getDouble("gut-bonus-per-level", 0.10));
            }
        }
    }

    public int getLevel(ItemStack rod, AugmentType type) {
        if (rod == null || !rod.hasItemMeta()) return 0;
        ItemMeta im = rod.getItemMeta();
        String raw = im.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (raw == null || raw.isEmpty()) return 0;
        for (String part : raw.split(";")) {
            int idx = part.indexOf('=');
            if (idx <= 0) continue;
            String k = part.substring(0, idx);
            String v = part.substring(idx + 1);
            if (k.equalsIgnoreCase(type.name())) {
                try { return Math.max(0, Integer.parseInt(v)); } catch (Exception ignored) { return 0; }
            }
        }
        return 0;
    }

    public void setLevel(ItemStack rod, AugmentType type, int level) {
        if (rod == null) return;
        ItemMeta im = rod.getItemMeta();
        String raw = im.getPersistentDataContainer().getOrDefault(key, PersistentDataType.STRING, "");
        java.util.Map<String, Integer> map = decode(raw);
        int capped = Math.max(0, Math.min(level, maxLevel.getOrDefault(type, 3)));
        map.put(type.name(), capped);
        im.getPersistentDataContainer().set(key, PersistentDataType.STRING, encode(map));
        rod.setItemMeta(im);
    }

    public int maxLevel(AugmentType t) { return maxLevel.getOrDefault(t, 3); }
    public int baseCost(AugmentType t) { return baseCost.getOrDefault(t, 100); }
    public double costMult(AugmentType t) { return costMult.getOrDefault(t, 1.6); }

    public int nextCost(AugmentType t, int currentLevel) {
        int next = currentLevel + 1;
        if (next > maxLevel(t)) return -1;
        double cost = baseCost(t) * Math.pow(costMult(t), currentLevel);
        return (int) Math.ceil(cost);
    }

    public int extraRollsPerLevel(AugmentType t) { return extraRollsPerLevel.getOrDefault(t, 1); }
    public double extraDropChancePerLevel(AugmentType t) { return extraDropChancePerLevel.getOrDefault(t, 0.0); }
    public double gutBonusPerLevel(AugmentType t) { return gutBonusPerLevel.getOrDefault(t, 0.0); }

    // --- utils ---

    private static Map<String,Integer> decode(String raw) {
        java.util.Map<String,Integer> map = new java.util.LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) return map;
        for (String part : raw.split(";")) {
            int idx = part.indexOf('=');
            if (idx <= 0) continue;
            try { map.put(part.substring(0,idx), Integer.parseInt(part.substring(idx+1))); } catch (Exception ignored) {}
        }
        return map;
    }

    private static String encode(Map<String,Integer> map) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (var e : map.entrySet()) {
            if (!first) sb.append(';'); first = false;
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    private static String tToPath(AugmentType t) {
        return switch (t) {
            case LUCK -> "luck";
            case DOUBLE_HOOK -> "double_hook";
            case ENTROPY -> "entropy";
        };
    }
}
