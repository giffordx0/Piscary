package com.chunksmith.piscary.services;

import com.chunksmith.piscary.Piscary;
import com.chunksmith.piscary.model.Fish;
import com.chunksmith.piscary.model.FishCondition;
import com.chunksmith.piscary.model.Rarity;
import com.chunksmith.piscary.util.YamlUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class FishService {
    private final Piscary plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final List<Fish> allFish = new ArrayList<>();
    private Map<Rarity, Integer> rarityWeights = Map.of();

    public FishService(Piscary plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        allFish.clear();

        // rarity weights
        var sec = plugin.config().fishIndex().getConfigurationSection("rarity-weights");
        Map<Rarity,Integer> tmp = new EnumMap<>(Rarity.class);
        if (sec != null) {
            for (String k : sec.getKeys(false)) {
                try { tmp.put(Rarity.valueOf(k), sec.getInt(k)); } catch (Exception ignored) {}
            }
        }
        rarityWeights = tmp.isEmpty()
                ? Map.of(Rarity.COMMON,60,Rarity.UNCOMMON,25,Rarity.RARE,10,Rarity.EPIC,4,Rarity.LEGENDARY,1)
                : tmp;

        // sets
        List<Map<?, ?>> sets = plugin.config().fishIndex().getMapList("sets");
        for (Map<?, ?> s : sets) {
            String setId = safeString(s.get("id"), "default");
            File f = new File(plugin.getDataFolder(), "fish/sets/" + setId + ".yml");
            if (!f.exists()) continue;

            YamlConfiguration y = YamlUtil.load(f);
            List<Map<?, ?>> list = y.getMapList("fish");
            for (Map<?, ?> fm : list) {
                try {
                    String fid = safeString(fm.get("id"), "unknown");
                    String name = safeString(fm.get("name"), fid);

                    String materialRaw = safeString(fm.get("material"), "COD");
                    Material mat = Material.matchMaterial(materialRaw.toUpperCase(Locale.ROOT));
                    if (mat == null) mat = Material.COD;

                    int model = safeInt(fm.get("model"), 0);
                    String rarityRaw = safeString(fm.get("rarity"), "COMMON");
                    Rarity rarity = Rarity.valueOf(rarityRaw.toUpperCase(Locale.ROOT));
                    double value = safeDouble(fm.get("value"), 10.0);

                    @SuppressWarnings("unchecked")
                    List<String> loreRaw = fm.get("lore") instanceof List<?> lr
                            ? (List<String>) lr
                            : Collections.emptyList();

                    Map<?, ?> cond = fm.get("conditions") instanceof Map<?,?> c ? c : Collections.emptyMap();

                    @SuppressWarnings("unchecked")
                    List<String> biomeNames = cond.get("biomes") instanceof List<?> bl
                            ? (List<String>) bl
                            : Collections.emptyList();

                    String time = safeString(cond.get("time"), "ANY");
                    String weather = safeString(cond.get("weather"), "ANY");

                    Map<?, ?> yr = cond.get("y") instanceof Map<?,?> ymap ? ymap : Map.of();
                    int yMin = safeInt(yr.get("min"), 0);
                    int yMax = safeInt(yr.get("max"), 320);

                    FishCondition condition = new FishCondition(
                            parseBiomes(biomeNames),
                            FishCondition.Time.valueOf(time),
                            FishCondition.Weather.valueOf(weather),
                            yMin, yMax
                    );

                    Fish fish = new Fish(
                            fid,
                            mm.deserialize(name),
                            mat,
                            model,
                            rarity,
                            value,
                            loreRaw.stream().map(mm::deserialize).toList(),
                            condition
                    );
                    allFish.add(fish);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed loading fish in set " + setId + ": " + ex.getMessage());
                }
            }
        }
        plugin.getLogger().info("Loaded " + allFish.size() + " fish.");
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

    /**
     * Converts biome names from config to Biome set.
     * Suppressed to keep the build clean on 1.21+, where the legacy enum accessors are deprecated.
     */
    @SuppressWarnings({"deprecation", "removal"})
    private Set<Biome> parseBiomes(Iterable<String> names) {
        Set<Biome> set = new HashSet<>();
        if (names == null) return set;
        for (String s : names) {
            if (s == null) continue;
            for (Biome b : Biome.values()) {
                if (b.name().equalsIgnoreCase(s)) { set.add(b); break; }
            }
        }
        return set;
    }

    public Optional<Fish> rollCatch(Player p) {
        List<Fish> pool = allFish.stream().filter(f -> f.condition().matches(p)).toList();
        if (pool.isEmpty()) return Optional.empty();

        var baitOpt = plugin.bait().getActive(p);

        List<Fish> bag = new ArrayList<>();
        for (Fish f : pool) {
            int baseW = rarityWeights.getOrDefault(f.rarity(), 1);
            double mult = 1.0;

            // Bait
            if (baitOpt.isPresent()) {
                mult *= baitOpt.get().rarityMultipliers().getOrDefault(f.rarity(), 1.0);
            }

            // Totem
            mult *= plugin.totems().rarityMultiplier(p, f.rarity());

            // Seasonal
            mult *= plugin.seasons().rarityMultiplier(f.rarity());

            int w = Math.max(1, (int)Math.round(baseW * mult));
            for (int i = 0; i < w; i++) bag.add(f);
        }
        if (bag.isEmpty()) return Optional.empty();
        return Optional.of(bag.get(new Random().nextInt(bag.size())));
    }

    public double sellValue(Fish f) {
        double mult = plugin.getConfig().getDouble("sell-multiplier", 1.0);
        return Math.max(0, f.value() * mult);
    }

    public List<Fish> all() { return Collections.unmodifiableList(allFish); }
}
