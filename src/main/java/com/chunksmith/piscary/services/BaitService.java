package com.chunksmith.piscary.services;

import com.chunksmith.piscary.Piscary;
import com.chunksmith.piscary.model.Bait;
import com.chunksmith.piscary.model.Rarity;
import com.chunksmith.piscary.util.YamlUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.time.Instant;
import java.util.*;

public class BaitService {
    private final Piscary plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<String, Bait> baits = new HashMap<>();

    private final NamespacedKey baitIdKey;
    private final NamespacedKey baitUntilKey;

    public BaitService(Piscary plugin) {
        this.plugin = plugin;
        this.baitIdKey = new NamespacedKey(plugin, "piscary_bait_id");
        this.baitUntilKey = new NamespacedKey(plugin, "piscary_bait_until"); // epoch millis
        reload();
    }

    public void reload() {
        baits.clear();
        File file = new File(plugin.getDataFolder(), "baits.yml");
        if (!file.exists()) return;
        YamlConfiguration y = YamlUtil.load(file);
        List<Map<?, ?>> list = y.getMapList("baits");
        for (Map<?, ?> m : list) {
            try {
                String id = safeString(m.get("id"), "");
                if (id.isEmpty()) continue;
                String nameRaw = safeString(m.get("name"), id);
                double price = safeDouble(m.get("price"), 0.0);
                int duration = safeInt(m.get("durationSeconds"), 300);

                Map<Rarity, Double> mults = new EnumMap<>(Rarity.class);
                Object rm = m.get("rarityMultipliers");
                if (rm instanceof Map<?,?> map) {
                    for (var e : map.entrySet()) {
                        String k = String.valueOf(e.getKey());
                        double v = safeDouble(e.getValue(), 1.0);
                        try { mults.put(Rarity.valueOf(k.toUpperCase(Locale.ROOT)), v); } catch (Exception ignored) { }
                    }
                }
                Bait bait = new Bait(id, mm.deserialize(nameRaw), price, duration, mults);
                baits.put(id.toLowerCase(Locale.ROOT), bait);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to load bait: " + ex.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + baits.size() + " baits.");
    }

    public Collection<Bait> all() { return Collections.unmodifiableCollection(baits.values()); }

    public Optional<Bait> byId(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(baits.get(id.toLowerCase(Locale.ROOT)));
    }

    public Optional<Bait> getActive(Player p) {
        var pdc = p.getPersistentDataContainer();
        String id = pdc.get(baitIdKey, PersistentDataType.STRING);
        Long until = pdc.get(baitUntilKey, PersistentDataType.LONG);
        long now = Instant.now().toEpochMilli();

        if (id == null || until == null || until <= now) {
            // Expired or none
            pdc.remove(baitIdKey);
            pdc.remove(baitUntilKey);
            return Optional.empty();
        }
        return byId(id);
    }

    public long getActiveRemainingMillis(Player p) {
        var pdc = p.getPersistentDataContainer();
        Long until = pdc.get(baitUntilKey, PersistentDataType.LONG);
        if (until == null) return 0L;
        long rem = until - Instant.now().toEpochMilli();
        return Math.max(rem, 0L);
    }

    public void apply(Player p, Bait bait) {
        var pdc = p.getPersistentDataContainer();
        long until = Instant.now().toEpochMilli() + (long) bait.durationSeconds() * 1000L;
        pdc.set(baitIdKey, PersistentDataType.STRING, bait.id());
        pdc.set(baitUntilKey, PersistentDataType.LONG, until);
    }

    private static String safeString(Object o, String def) { return o instanceof String s ? s : def; }
    private static int safeInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception ignored) { return def; }
    }
    private static double safeDouble(Object o, double def) {
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception ignored) { return def; }
    }
}
