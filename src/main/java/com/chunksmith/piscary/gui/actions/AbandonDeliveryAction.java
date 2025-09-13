package com.chunksmith.piscary.gui.actions;

import com.chunksmith.piscary.gui.GuiEngine;
import com.chunksmith.piscary.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;

public class AbandonDeliveryAction implements Action {
    private final GuiEngine engine;

    public AbandonDeliveryAction(GuiEngine engine, Map<?, ?> map) {
        this.engine = engine;
    }

    @Override
    public void run(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (!engine.plugin().deliveries().hasActive(p)) {
            Msg.raw(p, "<yellow>No active delivery to abandon.</yellow>");
            return;
        }
        engine.plugin().deliveries().abandon(p);
        Msg.raw(p, "<gray>Delivery abandoned.</gray>");
        engine.open("deliveries", p);
    }
}
