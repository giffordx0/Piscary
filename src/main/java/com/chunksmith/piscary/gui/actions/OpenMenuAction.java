package com.chunksmith.piscary.gui.actions;

import com.chunksmith.piscary.gui.GuiEngine;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;

public class OpenMenuAction implements Action {
    private final GuiEngine engine;
    private final String target;

    public OpenMenuAction(GuiEngine engine, Map<?, ?> map) {
        this.engine = engine;
        Object m = map.get("menu");
        this.target = (m == null ? "main" : String.valueOf(m));
    }

    @Override
    public void run(InventoryClickEvent e) {
        engine.open(target, (Player) e.getWhoClicked());
    }
}
