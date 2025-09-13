package com.chunksmith.piscary.gui.actions;

import com.chunksmith.piscary.gui.GuiEngine;
import com.chunksmith.piscary.model.TotemType;
import com.chunksmith.piscary.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;
import java.util.Optional;

public class BuyTotemAction implements Action {
    private final GuiEngine engine;
    private final String totemId;

    public BuyTotemAction(GuiEngine engine, Map<?, ?> map) {
        this.engine = engine;
        this.totemId = String.valueOf(map.containsKey("totem") ? map.get("totem") : "");
    }

    @Override
    public void run(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        Optional<TotemType> ot = engine.plugin().totems().byId(totemId);
        if (ot.isEmpty()) { Msg.raw(p, "<red>Unknown totem.</red>"); return; }
        TotemType t = ot.get();

        if (t.priceEntropy() > 0 && engine.plugin().entropy().get(p) < t.priceEntropy()) {
            Msg.raw(p, "<red>Not enough entropy.</red> Need: <yellow>" + t.priceEntropy() + "</yellow>");
            return;
        }
        if (t.priceMoney() > 0 && !engine.plugin().eco().has(p, t.priceMoney())) {
            Msg.raw(p, "<red>Not enough money.</red> Need: <yellow>" + t.priceMoney() + "</yellow>");
            return;
        }
        if (t.priceEntropy() > 0) engine.plugin().entropy().add(p, -t.priceEntropy());
        if (t.priceMoney() > 0) engine.plugin().eco().withdraw(p, t.priceMoney());

        var item = engine.plugin().totems().totemItem(t);
        p.getInventory().addItem(item);
        Msg.raw(p, "<green>Purchased:</green> " + t.nameMini());
    }
}
