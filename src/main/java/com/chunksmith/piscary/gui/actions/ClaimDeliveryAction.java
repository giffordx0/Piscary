package com.chunksmith.piscary.gui.actions;

import com.chunksmith.piscary.gui.GuiEngine;
import com.chunksmith.piscary.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;

public class ClaimDeliveryAction implements Action {
    private final GuiEngine engine;

    public ClaimDeliveryAction(GuiEngine engine, Map<?, ?> map) {
        this.engine = engine;
    }

    @Override
    public void run(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (!engine.plugin().deliveries().hasActive(p)) {
            Msg.raw(p, "<yellow>No active delivery.</yellow>");
            return;
        }
        if (!engine.plugin().deliveries().isComplete(p)) {
            Msg.raw(p, "<yellow>You don't have all required fish yet.</yellow>");
            return;
        }
        boolean ok = engine.plugin().deliveries().claim(p);
        if (ok) {
            Msg.raw(p, "<green>Delivery complete! Rewards granted.</green>");
            engine.open("deliveries", p);
        } else {
            Msg.raw(p, "<red>Could not claim delivery.</red>");
        }
    }
}
