package com.chunksmith.piscary.services;

import com.chunksmith.piscary.Piscary;
import com.chunksmith.piscary.model.Fish;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class DeliveriesService {
    private final Piscary plugin;

    public static class Delivery {
        public final String id;
        public final String name;
        public final Map<String,Integer> needs;
        public final double rewardMoney;
        public final int rewardEntropy;

        public Delivery(String id, String name, Map<String,Integer> needs, double rewardMoney, int rewardEntropy) {
            this.id = id; this.name = name; this.needs = needs;
            this.rewardMoney = rewardMoney; this.rewardEntropy = rewardEntropy;
        }
    }

    private final List<Delivery> pool = new ArrayList<>();
    private final Map<UUID, Delivery> active = new HashMap<>();

    public DeliveriesService(Piscary plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        pool.clear();
        File f = new File(plugin.getDataFolder(), "deliveries.yml");
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        for (Map<?,?> m : y.getMapList("deliveries")) {
            try {
                String id = String.valueOf(m.get("id"));
                String name = m.containsKey("name") ? String.valueOf(m.get("name")) : id;

                Map<String,Integer> needs = new LinkedHashMap<>();
                Object fishObj = m.get("needs");
                if (fishObj instanceof Map<?,?> map) {
                    for (var e : map.entrySet()) {
                        needs.put(String.valueOf(e.getKey()), safeInt(e.getValue(), 1));
                    }
                }

                double money = safeDouble(m.get("rewardMoney"), 0.0);
                int entropy = safeInt(m.get("rewardEntropy"), 0);

                pool.add(new Delivery(id, name, needs, money, entropy));
            } catch (Exception ex) {
                plugin.getLogger().warning("Delivery parse error: " + ex.getMessage());
            }
        }
    }

    public boolean hasActive(Player p) {
        return active.containsKey(p.getUniqueId());
    }

    public Optional<Delivery> getActive(Player p) {
        return Optional.ofNullable(active.get(p.getUniqueId()));
    }

    public boolean acceptRandom(Player p) {
        if (pool.isEmpty()) return false;
        Delivery d = pool.get(new Random().nextInt(pool.size()));
        active.put(p.getUniqueId(), d);
        return true;
    }

    public void abandon(Player p) {
        active.remove(p.getUniqueId());
    }

    public boolean isComplete(Player p) {
        Delivery d = active.get(p.getUniqueId());
        if (d == null) return false;
        return hasAll(p, d.needs);
    }

    public boolean claim(Player p) {
        Delivery d = active.get(p.getUniqueId());
        if (d == null) return false;
        if (!hasAll(p, d.needs)) return false;

        removeAll(p, d.needs);
        if (d.rewardMoney > 0) plugin.eco().deposit(p, d.rewardMoney);
        if (d.rewardEntropy > 0) plugin.entropy().add(p, d.rewardEntropy);
        active.remove(p.getUniqueId());
        return true;
    }

    public void onCatch(Player p, String fishId) {
        // Hook point for progress updates.
    }

    // ---- inventory helpers ----

    private boolean hasAll(Player p, Map<String,Integer> needs) {
        for (var e : needs.entrySet()) {
            int have = countFish(p, e.getKey());
            if (have < e.getValue()) return false;
        }
        return true;
    }

    private void removeAll(Player p, Map<String,Integer> needs) {
        for (var e : needs.entrySet()) {
            removeFish(p, e.getKey(), e.getValue());
        }
    }

    private int countFish(Player p, String fishId) {
        int total = 0;
        for (var it : p.getInventory().getContents()) {
            if (it == null || !it.hasItemMeta()) continue;
            for (Fish f : plugin.fish().all()) {
                if (f.id().equalsIgnoreCase(fishId)
                        && it.getType() == f.material()
                        && it.getItemMeta().hasCustomModelData()
                        && it.getItemMeta().getCustomModelData() == f.model()) {
                    total += it.getAmount();
                }
            }
        }
        return total;
    }

    private void removeFish(Player p, String fishId, int amt) {
        var inv = p.getInventory().getContents();
        for (int i = 0; i < inv.length && amt > 0; i++) {
            var it = inv[i];
            if (it == null || !it.hasItemMeta()) continue;
            for (Fish f : plugin.fish().all()) {
                if (f.id().equalsIgnoreCase(fishId)
                        && it.getType() == f.material()
                        && it.getItemMeta().hasCustomModelData()
                        && it.getItemMeta().getCustomModelData() == f.model()) {
                    int take = Math.min(amt, it.getAmount());
                    it.setAmount(it.getAmount() - take);
                    if (it.getAmount() <= 0) inv[i] = null;
                    amt -= take;
                    break;
                }
            }
        }
        p.getInventory().setContents(inv);
    }

    private static int safeInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return def; }
    }
    private static double safeDouble(Object o, double def) {
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return def; }
    }
}
