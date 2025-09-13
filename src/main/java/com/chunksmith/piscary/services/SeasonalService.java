package com.chunksmith.piscary.services;

import com.chunksmith.piscary.Piscary;
import com.chunksmith.piscary.model.Rarity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.LocalDate;
import java.util.*;

public class SeasonalService {
    public static class SeasonalEvent {
        public final String id;
        public final String nameMini;
        public final Set<Integer> months;
        public final int dayStart;
        public final int dayEnd;
        public final Map<Rarity, Double> rarityMult;
        public final double xpMult;
        public final double sellMult;
        public final double entropyBonus;

        public SeasonalEvent(String id, String nameMini, Set<Integer> months, int dayStart, int dayEnd,
                             Map<Rarity, Double> rarityMult, double xpMult, double sellMult, double entropyBonus) {
            this.id = id;
            this.nameMini = nameMini;
            this.months = months;
            this.dayStart = dayStart;
            this.dayEnd = dayEnd;
            this.rarityMult = rarityMult;
            this.xpMult = xpMult;
            this.sellMult = sellMult;
            this.entropyBonus = entropyBonus;
        }

        public boolean matches(LocalDate date) {
            if (!months.contains(date.getMonthValue())) return false;
            int d = date.getDayOfMonth();
            return d >= dayStart && d <= dayEnd;
        }
    }

    private final Piscary plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final List<SeasonalEvent> events = new ArrayList<>();
    private SeasonalEvent active = null;
    private BukkitTask ticker;

    public SeasonalService(Piscary plugin) {
        this.plugin = plugin;
        reload();
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L * 60);
        tick();
    }

    public void shutdown() {
        if (ticker != null) ticker.cancel();
    }

    public void reload() {
        events.clear();
        File f = new File(plugin.getDataFolder(), "seasons.yml");
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        for (Map<?, ?> m : y.getMapList("events")) {
            try {
                String id = String.valueOf(m.get("id"));
                String name = m.containsKey("name") ? String.valueOf(m.get("name")) : id;

                Set<Integer> months = new HashSet<>();
                Object monthsObj = m.get("months");
                if (monthsObj instanceof List<?> list) {
                    for (Object o : list) {
                        try { months.add(Integer.parseInt(String.valueOf(o))); } catch (Exception ignored) {}
                    }
                }
                if (months.isEmpty()) continue;

                int dayStart = yInt(m, "dayStart", 1);
                int dayEnd   = Math.max(dayStart, yInt(m, "dayEnd", 31));

                Map<Rarity, Double> rmult = new EnumMap<>(Rarity.class);
                Object rm = m.get("rarityMultipliers");
                if (rm instanceof Map<?, ?> map) {
                    for (var e : map.entrySet()) {
                        try {
                            rmult.put(Rarity.valueOf(String.valueOf(e.getKey()).toUpperCase(Locale.ROOT)),
                                    Double.parseDouble(String.valueOf(e.getValue())));
                        } catch (Exception ignored) {}
                    }
                }
                double xpMult = yDouble(m, "xpMultiplier", 1.0);
                double sellMult = yDouble(m, "sellMultiplier", 1.0);
                double entBonus = yDouble(m, "entropyBonus", 0.0);

                events.add(new SeasonalEvent(id, name, months, dayStart, dayEnd, rmult, xpMult, sellMult, entBonus));
            } catch (Exception ex) {
                plugin.getLogger().warning("Season load error: " + ex.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + events.size() + " seasonal events.");
        tick();
    }

    private void tick() {
        LocalDate today = LocalDate.now();
        SeasonalEvent match = null;
        for (SeasonalEvent ev : events) {
            if (ev.matches(today)) { match = ev; break; }
        }
        if (!Objects.equals(active, match)) {
            active = match;
            if (active != null) {
                Component c = mm.deserialize("<gradient:#00e5ff:#00ffa8><b>Seasonal Event:</b></gradient> " + active.nameMini);
                Bukkit.getServer().sendMessage(c);
            } else {
                Bukkit.getServer().sendMessage(mm.deserialize("<gray>Seasonal event ended.</gray>"));
            }
        }
    }

    public boolean isActive() { return active != null; }
    public String activeId() { return active == null ? "NONE" : active.id; }
    public String activeNameMini() { return active == null ? "<gray>None</gray>" : active.nameMini; }
    public double rarityMultiplier(Rarity r) { return active == null ? 1.0 : active.rarityMult.getOrDefault(r, 1.0); }
    public double xpMultiplier() { return active == null ? 1.0 : Math.max(1.0, active.xpMult); }
    public double sellMultiplier() { return active == null ? 1.0 : Math.max(1.0, active.sellMult); }
    public double entropyBonus() { return active == null ? 0.0 : Math.max(0.0, active.entropyBonus); }

    private static int yInt(Map<?,?> m, String k, int def) {
        if (!m.containsKey(k)) return def;
        Object v = m.get(k);
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception ignored) { return def; }
    }
    private static double yDouble(Map<?,?> m, String k, double def) {
        if (!m.containsKey(k)) return def;
        Object v = m.get(k);
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception ignored) { return def; }
    }
}
