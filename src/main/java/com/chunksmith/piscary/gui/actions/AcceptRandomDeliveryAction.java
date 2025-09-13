package com.chunksmith.piscary.gui.actions;

import com.chunksmith.piscary.gui.GuiEngine;
import com.chunksmith.piscary.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;

public class AcceptRandomDeliveryAction implements Action {
    private final GuiEngine engine;

    public AcceptRandomDeliveryAction(GuiEngine engine, Map<?, ?> map) {
        this.engine = engine;
    }

    @Override
    public void run(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (engine.plugin().deliveries().hasActive(p)) {
            Msg.raw(p, "<yellow>You already have an active delivery.</yellow>");
            return;
        }
        boolean ok = engine.plugin().deliveries().acceptRandom(p);
        if (ok) {
            Msg.raw(p, "<green>New delivery accepted!</green>");
            engine.open("deliveries", p);
        } else {
            Msg.raw(p, "<red>No deliveries available.</red>");
        }
    }
}
