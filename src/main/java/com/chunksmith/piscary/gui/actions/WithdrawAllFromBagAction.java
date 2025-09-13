package com.chunksmith.piscary.gui.actions;

import com.chunksmith.piscary.gui.GuiEngine;
import com.chunksmith.piscary.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;

public class WithdrawAllFromBagAction implements Action {
    private final GuiEngine engine;

    public WithdrawAllFromBagAction(GuiEngine engine, Map<?, ?> map) {
        this.engine = engine;
    }

    @Override
    public void run(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        int out = engine.plugin().bag().withdrawAll(p);
        if (out > 0) {
            Msg.raw(p, "<green>Withdrew <yellow>" + out + "</yellow> fish from your bag.</green>");
        } else {
            Msg.raw(p, "<gray>No fish in your bag or inventory full.</gray>");
        }
        // Refresh the GUI
        e.getWhoClicked().closeInventory();
        engine.open("bag", p);
    }
}
