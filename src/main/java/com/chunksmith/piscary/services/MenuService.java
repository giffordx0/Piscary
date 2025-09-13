package com.chunksmith.piscary.services;

import com.chunksmith.piscary.Piscary;
import com.chunksmith.piscary.gui.GuiEngine;
import com.chunksmith.piscary.gui.MenuView;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class MenuService implements Listener {
    private final Piscary plugin;
    private final GuiEngine engine;

    public MenuService(Piscary plugin) {
        this.plugin = plugin;
        this.engine = new GuiEngine(plugin);
        reload();
    }

    public void reload() {
        engine.reload();
    }

    /** Open a menu by id. */
    public void open(String id, Player p) {
        engine.open(id, p);
    }

    /** Resolve simple lang refs if you use keys like {@code lang:main.title}. */
    public String resolveLangRefs(String s) {
        // Minimal pass-through; hook your LangService here if you use keys.
        return s == null ? "" : s;
    }

    /** Expose the engine to components. */
    public GuiEngine engine() { return engine; }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        MenuView v = engine.current(p);
        if (v != null) v.handle(e);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        engine.closed(p);
    }
}
