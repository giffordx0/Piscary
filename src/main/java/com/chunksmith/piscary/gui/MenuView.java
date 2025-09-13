package com.chunksmith.piscary.gui;

import com.chunksmith.piscary.gui.components.Component;
import com.chunksmith.piscary.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MenuView {
    private final GuiEngine engine;
    private final String id;
    private final String titleMini;
    private final int size;
    private final List<Component> components = new ArrayList<>();

    public MenuView(GuiEngine engine, String id, File file) {
        this.engine = engine;
        this.id = id;

        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        this.titleMini = engine.resolveLangRefs(y.getString("title", "<white>Menu</white>"));
        this.size = Math.max(9, Math.min(54, y.getInt("size", 27)));

        // Components (no unchecked warnings)
        List<?> rawList = y.getList("components");
        if (rawList != null) {
            for (Object obj : rawList) {
                if (obj instanceof Map<?, ?> m) {
                    // Build a tiny in-memory section and pass to factory
                    YamlConfiguration sec = new YamlConfiguration();
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        sec.set(String.valueOf(e.getKey()), e.getValue());
                    }
                    components.add(Component.fromConfig(sec, engine));
                }
            }
        }
    }

    public String id() { return id; }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(p, size, Text.mm(titleMini));
        for (Component c : components) c.render(inv, p);
        p.openInventory(inv);
    }

    /** Handle a click and re-render to reflect any state changes. */
    public void handle(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        // Only process if this is our top inventory
        if (!e.getView().title().equals(Text.mm(titleMini))) return;

        for (Component c : components) c.click(e);
        // Re-render after actions
        for (Component c : components) c.render(e.getView().getTopInventory(), p);
    }

    public List<Component> components() { return components; }
}
