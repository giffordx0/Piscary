package com.chunksmith.piscary.gui.actions;

import com.chunksmith.piscary.gui.GuiEngine;
import com.chunksmith.piscary.model.Fish;
import com.chunksmith.piscary.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class SellAllAction implements Action {
    private final GuiEngine engine;

    public SellAllAction(GuiEngine engine, Map<?, ?> map) {
        this.engine = engine;
    }

    @Override
    public void run(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        ItemStack[] contents = p.getInventory().getContents();
        List<Fish> db = engine.plugin().fish().all();

        int sold = 0;
        double total = 0.0;

        double totemSellMult = engine.plugin().totems().sellMultiplier(p);
        double seasonalSellMult = engine.plugin().seasons().sellMultiplier();

        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null || !it.hasItemMeta()) continue;

            for (Fish f : db) {
                if (it.getType() == f.material()
                        && it.getItemMeta().hasCustomModelData()
                        && it.getItemMeta().getCustomModelData() == f.model()) {
                    int count = it.getAmount();
                    contents[i] = null; // remove
                    sold += count;
                    double valEach = engine.plugin().fish().sellValue(f) * totemSellMult * seasonalSellMult;
                    total += valEach * count;
                    break;
                }
            }
        }

        p.getInventory().setContents(contents);

        if (sold > 0 && total > 0) {
            engine.plugin().eco().deposit(p, total);
            Msg.raw(p, "<green>Sold <yellow>" + sold + "</yellow> fish for <yellow>" + String.format(java.util.Locale.US, "%.2f", total) + "</yellow>.</green>");
        } else {
            Msg.raw(p, "<gray>No Piscary fish to sell.</gray>");
        }
    }
}
