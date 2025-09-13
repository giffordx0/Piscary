package com.chunksmith.piscary.services;

import com.chunksmith.piscary.Piscary;
import com.chunksmith.piscary.model.Rarity;
import com.chunksmith.piscary.model.TotemType;
import com.chunksmith.piscary.util.YamlUtil;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TotemService {
    private final Piscary plugin;

    private final NamespacedKey totemItemKey;
    private final NamespacedKey totemOwnerKey;
    private final Map<String, TotemType> types = new LinkedHashMap<>();

    private final File placedFile;
    private final Map<UUID, List<Placed>> placedByWorld = new HashMap<>();

    public TotemService(Piscary plugin) {
        this.plugin = plugin;
        this.totemItemKey = new NamespacedKey(plugin, "piscary_totem_type");
        this.totemOwnerKey = new NamespacedKey(plugin, "piscary_totem_owner");
        this.placedFile = new File(plugin.getDataFolder(), "placed-totems.yml");
        reload();
        loadAll();
    }

    public void reload() {
        types.clear();
        File f = new File(plugin.getDataFolder(), "totems.yml");
        if (!f.exists()) return;
        YamlConfiguration y = YamlUtil.load(f);
        for (Map<?,?> m : y.getMapList("totems")) {
            try {
                String id = String.valueOf(m.get("id"));
                if (id == null || id.isEmpty()) continue;

                Object nameObj = m.containsKey("name") ? m.get("name") : id;
                String name = String.valueOf(nameObj);

                Object matObj = m.containsKey("material") ? m.get("material") : "BEACON";
                String mat = String.valueOf(matObj);
                Material material = Material.matchMaterial(mat.toUpperCase(Locale.ROOT));
                if (material == null) material = Material.BEACON;

                double priceMoney = safeDouble(m.get("priceMoney"), 0.0);
                int priceEntropy = safeInt(m.get("priceEntropy"), 0);
                int radius = Math.max(1, safeInt(m.get("radius"), 16));

                Map<Rarity,Double> rmult = new EnumMap<>(Rarity.class);
                Object rm = m.get("rarityMultipliers");
                if (rm instanceof Map<?,?> map) {
                    for (var e : map.entrySet()) {
                        try { rmult.put(Rarity.valueOf(String.valueOf(e.getKey()).toUpperCase(Locale.ROOT)), safeDouble(e.getValue(), 1.0)); } catch (Exception ignored) {}
                    }
                }
                double xpMult = safeDouble(m.get("xpMultiplier"), 1.0);
                double sellMult = safeDouble(m.get("sellMultiplier"), 1.0);
                double entBonus = safeDouble(m.get("entropyBonus"), 0.0);

                TotemType type = new TotemType(id, name, material, priceMoney, priceEntropy, radius, rmult, xpMult, sellMult, entBonus);
                types.put(id.toLowerCase(Locale.ROOT), type);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to load totem type: " + ex.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + types.size() + " totem types.");
    }

    public Collection<TotemType> allTypes() { return Collections.unmodifiableCollection(types.values()); }
    public Optional<TotemType> byId(String id) { return id == null ? Optional.empty() : Optional.ofNullable(types.get(id.toLowerCase(Locale.ROOT))); }

    public ItemStack totemItem(TotemType type) {
        ItemStack it = new ItemStack(type.itemMaterial());
        ItemMeta im = it.getItemMeta();
        im.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(type.nameMini()));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<gray>Radius:</gray> <yellow>" + type.radius() + "</yellow>"));
        if (!type.rarityMultipliers().isEmpty())
            lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<gray>Rarity buffs enabled.</gray>"));
        if (type.xpMultiplier() > 1.0)
            lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<gray>XP x</gray><yellow>" + type.xpMultiplier() + "</yellow>"));
        if (type.sellMultiplier() > 1.0)
            lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<gray>Sell x</gray><yellow>" + type.sellMultiplier() + "</yellow>"));
        if (type.entropyBonus() > 0.0)
            lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<gray>Gutting +</gray><yellow>" + (int)Math.round(type.entropyBonus()*100) + "%</yellow>"));

        im.lore(lore);
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        it.setItemMeta(im);
        it.editMeta(meta -> meta.getPersistentDataContainer().set(totemItemKey, PersistentDataType.STRING, type.id()));
        return it;
    }

    public Optional<TotemType> fromItem(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return Optional.empty();
        String id = it.getItemMeta().getPersistentDataContainer().get(totemItemKey, PersistentDataType.STRING);
        if (id == null || id.isEmpty()) return Optional.empty();
        return byId(id);
    }

    public boolean place(Player owner, Location loc, TotemType type) {
        List<Placed> list = placedByWorld.computeIfAbsent(loc.getWorld().getUID(), k -> new ArrayList<>());
        for (Placed p : list) if (p.x == loc.getBlockX() && p.y == loc.getBlockY() && p.z == loc.getBlockZ()) return false;
        list.add(new Placed(owner.getUniqueId(), type.id(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        saveAll();
        return true;
    }

    public boolean remove(Location loc, boolean allowOthers, UUID breaker) {
        List<Placed> list = placedByWorld.getOrDefault(loc.getWorld().getUID(), Collections.emptyList());
        for (Iterator<Placed> it = list.iterator(); it.hasNext(); ) {
            Placed p = it.next();
            if (p.x == loc.getBlockX() && p.y == loc.getBlockY() && p.z == loc.getBlockZ()) {
                if (!allowOthers && !p.owner.equals(breaker)) return false;
                it.remove();
                saveAll();
                return true;
            }
        }
        return false;
    }

    public void loadAll() {
        placedByWorld.clear();
        if (!placedFile.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(placedFile);
        for (String w : y.getKeys(false)) {
            try {
                UUID wid = UUID.fromString(w);
                List<Map<?,?>> list = y.getMapList(w);
                List<Placed> out = new ArrayList<>();
                for (Map<?,?> m : list) {
                    UUID owner = UUID.fromString(String.valueOf(m.get("owner")));
                    String type = String.valueOf(m.get("type"));
                    int x = Integer.parseInt(String.valueOf(m.get("x")));
                    int yv = Integer.parseInt(String.valueOf(m.get("y")));
                    int z = Integer.parseInt(String.valueOf(m.get("z")));
                    out.add(new Placed(owner, type, x, yv, z));
                }
                placedByWorld.put(wid, out);
            } catch (Exception ignored) {}
        }
        plugin.getLogger().info("Loaded " + placedByWorld.values().stream().mapToInt(List::size).sum() + " placed totems.");
    }

    public void saveAll() {
        YamlConfiguration y = new YamlConfiguration();
        for (var e : placedByWorld.entrySet()) {
            List<Map<String,Object>> list = new ArrayList<>();
            for (Placed p : e.getValue()) {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("owner", p.owner.toString());
                m.put("type", p.typeId);
                m.put("x", p.x);
                m.put("y", p.y);
                m.put("z", p.z);
                list.add(m);
            }
            y.set(e.getKey().toString(), list);
        }
        try { y.save(placedFile); } catch (IOException ex) {
            plugin.getLogger().warning("Failed saving placed totems: " + ex.getMessage());
        }
    }

    public double rarityMultiplier(Player p, Rarity r) {
        TotemType t = strongestInRange(p);
        if (t == null) return 1.0;
        return t.rarityMultipliers().getOrDefault(r, 1.0);
    }

    public double xpMultiplier(Player p) {
        TotemType t = strongestInRange(p);
        return t == null ? 1.0 : Math.max(1.0, t.xpMultiplier());
    }

    public double sellMultiplier(Player p) {
        TotemType t = strongestInRange(p);
        return t == null ? 1.0 : Math.max(1.0, t.sellMultiplier());
    }

    public double entropyBonus(Player p) {
        TotemType t = strongestInRange(p);
        return t == null ? 0.0 : Math.max(0.0, t.entropyBonus());
    }

    private TotemType strongestInRange(Player p) {
        World w = p.getWorld();
        List<Placed> list = placedByWorld.get(w.getUID());
        if (list == null || list.isEmpty()) return null;
        TotemType best = null;
        int bestRadius = -1;
        int px = p.getLocation().getBlockX();
        int py = p.getLocation().getBlockY();
        int pz = p.getLocation().getBlockZ();

        for (Placed pl : list) {
            TotemType t = byId(pl.typeId).orElse(null);
            if (t == null) continue;
            int dx = px - pl.x, dy = py - pl.y, dz = pz - pl.z;
            int dist2 = dx*dx + dy*dy + dz*dz;
            if (dist2 <= t.radius() * t.radius()) {
                if (t.radius() > bestRadius) { best = t; bestRadius = t.radius(); }
            }
        }
        return best;
    }

    private static int safeInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return def; }
    }
    private static double safeDouble(Object o, double def) {
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return def; }
    }

    private static class Placed {
        final UUID owner;
        final String typeId;
        final int x, y, z;
        Placed(UUID owner, String typeId, int x, int y, int z) {
            this.owner = owner; this.typeId = typeId; this.x = x; this.y = y; this.z = z;
        }
    }
}
