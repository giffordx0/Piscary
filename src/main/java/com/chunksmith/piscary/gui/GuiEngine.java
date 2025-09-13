package com.chunksmith.piscary.gui;

import com.chunksmith.piscary.Piscary;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class GuiEngine {
    private final Piscary plugin;
    private final Map<String, MenuView> registry = new HashMap<>();
    private final Map<UUID, MenuView> openByPlayer = new HashMap<>();

    public GuiEngine(Piscary plugin) {
        this.plugin = plugin;
    }

    /** Expose the main plugin to actions/components. */
    public Piscary plugin() { return plugin; }

    /** Simple pass-through. If you add a Lang service later, resolve keys here. */
    public String resolveLangRefs(String s) {
        return s == null ? "" : s;
    }

    public void reload() {
        registry.clear();

        File dir = new File(plugin.getDataFolder(), "menus");
        if (!dir.exists()) dir.mkdirs();

        // Load *.yml directly under menus/ (skip subfolders like themes/)
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                String id = f.getName().substring(0, f.getName().length() - 4); // strip .yml
                try {
                    // pass *this* engine, never call back into plugin.menus() during load
                    MenuView view = new MenuView(this, id, f);
                    registry.put(id.toLowerCase(Locale.ROOT), view);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed to load menu '" + id + "': " + ex.getMessage());
                }
            }
        }
        plugin.getLogger().info("Loaded " + registry.size() + " menus.");
    }

    public Optional<MenuView> get(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(registry.get(id.toLowerCase(Locale.ROOT)));
    }

    public void open(String id, Player p) {
        MenuView v = get(id).orElse(null);
        if (v == null) {
            plugin.getLogger().warning("Menu not found: " + id);
            return;
        }
        v.open(p);
        openByPlayer.put(p.getUniqueId(), v);
    }

    public MenuView current(Player p) {
        return openByPlayer.get(p.getUniqueId());
    }

    public void closed(Player p) {
        openByPlayer.remove(p.getUniqueId());
    }
}
