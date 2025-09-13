package com.chunksmith.piscary.gui.actions;

import com.chunksmith.piscary.gui.GuiEngine;
import com.chunksmith.piscary.model.Bait;
import com.chunksmith.piscary.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;
import java.util.Optional;

public class BuyApplyBaitAction implements Action {
    private final GuiEngine engine;
    private final String baitId;

    public BuyApplyBaitAction(GuiEngine engine, Map<?, ?> map) {
        this.engine = engine;
        Object id = map.get("bait");
        this.baitId = id == null ? "" : String.valueOf(id);
    }

    @Override
    public void run(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        Optional<Bait> ob = engine.plugin().bait().byId(baitId);
        if (ob.isEmpty()) {
            Msg.raw(p, "<red>Unknown bait: " + baitId + "</red>");
            return;
        }
        Bait bait = ob.get();

        double price = bait.price();
        boolean paid = engine.plugin().eco().withdraw(p, price);
        if (!paid) {
            Msg.raw(p, "<red>Not enough money.</red>");
            return;
        }

        engine.plugin().bait().apply(p, bait);
        Msg.raw(p, "<green>Applied bait: </green>" + bait.name());
    }
}
