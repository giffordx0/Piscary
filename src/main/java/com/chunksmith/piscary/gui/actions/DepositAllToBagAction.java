package com.chunksmith.piscary.gui.actions;

import com.chunksmith.piscary.gui.GuiEngine;
import com.chunksmith.piscary.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;

public class DepositAllToBagAction implements Action {
    private final GuiEngine engine;

    public DepositAllToBagAction(GuiEngine engine, Map<?, ?> map) {
        this.engine = engine;
    }

    @Override
    public void run(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        int moved = engine.plugin().bag().depositAll(p);
        if (moved > 0) {
            Msg.raw(p, "<green>Deposited <yellow>" + moved + "</yellow> fish into your bag.</green>");
        } else {
            Msg.raw(p, "<gray>No Piscary fish to deposit.</gray>");
        }
        // Refresh the GUI
        e.getWhoClicked().closeInventory();
        engine.open("bag", p);
    }
}
