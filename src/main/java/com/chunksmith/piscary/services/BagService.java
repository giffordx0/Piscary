package com.chunksmith.piscary.services;

import com.chunksmith.piscary.Piscary;
import com.chunksmith.piscary.model.Fish;
import com.chunksmith.piscary.util.Msg;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Simple virtual storage for Piscary fish.
 * Data is kept in data/bags/<uuid>.yml as:
 * bag:
 *   <fishId>: <count>
 */
public class BagService {
    private final Piscary plugin;
    private final File dir;

    public BagService(Piscary plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "bags");
        if (!dir.exists()) dir.mkdirs();
    }

    // --- public API ---

    public Map<String,Integer> getBag(Player p) {
        return load(p.getUniqueId());
    }

    public int getTotalCount(Player p) {
        return load(p.getUniqueId()).values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getSpeciesCount(Player p) {
        return load(p.getUniqueId()).size();
    }

    /**
     * Move all Piscary fish from player inventory into their bag.
     * @return number of fish moved
     */
    public int depositAll(Player p) {
        ItemStack[] contents = p.getInventory().getContents();
        List<Fish> db = plugin.fish().all();
        Map<String,Integer> bag = load(p.getUniqueId());
        int moved = 0;

        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null || !it.hasItemMeta()) continue;

            Fish match = matchFish(db, it);
            if (match == null) continue;

            int amt = it.getAmount();
            contents[i] = null;
            bag.merge(match.id(), amt, Integer::sum);
            moved += amt;
        }
        p.getInventory().setContents(contents);
        save(p.getUniqueId(), bag);
        return moved;
    }

    /**
     * Withdraw as many fish as possible from the bag to the player's inventory.
     * Keeps leftovers if inventory fills.
     * @return number withdrawn
     */
    public int withdrawAll(Player p) {
        Map<String,Integer> bag = load(p.getUniqueId());
        if (bag.isEmpty()) return 0;

        int withdrawn = 0;
        for (var it = bag.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            String fishId = e.getKey();
            int remain = e.getValue();

            Optional<Fish> of = plugin.fish().all().stream().filter(f -> f.id().equalsIgnoreCase(fishId)).findFirst();
            if (of.isEmpty()) { it.remove(); continue; }
            Fish f = of.get();

            while (remain > 0) {
                int take = Math.min(remain, f.material().getMaxStackSize());
                ItemStack item = f.toItem();
                item.setAmount(take);
                Map<Integer, ItemStack> leftovers = p.getInventory().addItem(item);
                if (leftovers.isEmpty()) {
                    withdrawn += take;
                    remain -= take;
                } else {
                    // inventory full (some or all returned)
                    int notPlaced = leftovers.values().stream().mapToInt(ItemStack::getAmount).sum();
                    int placed = take - notPlaced;
                    withdrawn += Math.max(0, placed);
                    remain -= Math.max(0, placed);
                    // Put back the remainder into bag and bail
                    e.setValue(remain);
                    save(p.getUniqueId(), bag);
                    return withdrawn;
                }
            }
            // fully drained this species
            it.remove();
        }
        save(p.getUniqueId(), bag);
        return withdrawn;
    }

    // --- internals ---

    private Fish matchFish(List<Fish> db, ItemStack it) {
        for (Fish f : db) {
            if (it.getType() == f.material()
                    && it.getItemMeta().hasCustomModelData()
                    && it.getItemMeta().getCustomModelData() == f.model()) {
                return f;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String,Integer> load(UUID uuid) {
        File f = file(uuid);
        if (!f.exists()) return new LinkedHashMap<>();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        Map<String,Object> raw = (Map<String,Object>) y.getConfigurationSection("bag") == null
                ? Map.of() : y.getConfigurationSection("bag").getValues(false);
        Map<String,Integer> out = new LinkedHashMap<>();
        for (var e : raw.entrySet()) {
            try { out.put(e.getKey(), Integer.parseInt(String.valueOf(e.getValue()))); } catch (Exception ignored) {}
        }
        return out;
    }

    private void save(UUID uuid, Map<String,Integer> bag) {
        File f = file(uuid);
        YamlConfiguration y = new YamlConfiguration();
        for (var e : bag.entrySet()) {
            y.set("bag." + e.getKey(), e.getValue());
        }
        try { y.save(f); } catch (IOException ex) { plugin.getLogger().warning("Bag save failed: " + ex.getMessage()); }
    }

    private File file(UUID uuid) {
        return new File(dir, uuid.toString() + ".yml");
    }
}
