package com.chunksmith.piscary.gui.actions;

import com.chunksmith.piscary.gui.GuiEngine;
import com.chunksmith.piscary.model.AugmentType;
import com.chunksmith.piscary.model.Fish;
import com.chunksmith.piscary.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class GutAllAction implements Action {
    private final GuiEngine engine;

    public GutAllAction(GuiEngine engine, Map<?, ?> map) {
        this.engine = engine;
    }

    @Override
    public void run(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        ItemStack[] contents = p.getInventory().getContents();
        List<Fish> db = engine.plugin().fish().all();

        int totalEntropy = 0;
        int totalFish = 0;

        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null || !it.hasItemMeta()) continue;

            for (Fish f : db) {
                if (it.getType() == f.material()
                        && it.getItemMeta().hasCustomModelData()
                        && it.getItemMeta().getCustomModelData() == f.model()) {
                    int count = it.getAmount();
                    totalFish += count;
                    totalEntropy += engine.plugin().entropy().gutAmountFor(f.rarity()) * count;
                    contents[i] = null; // remove
                    break;
                }
            }
        }

        // Augment bonus
        ItemStack main = p.getInventory().getItemInMainHand();
        int lvl = engine.plugin().augments().getLevel(main, AugmentType.ENTROPY);
        double augBonusRate = engine.plugin().augments().gutBonusPerLevel(AugmentType.ENTROPY);
        int bonusAug = (int)Math.floor(totalEntropy * (augBonusRate * lvl));

        // Totem bonus
        double totemBonusRate = engine.plugin().totems().entropyBonus(p);
        int bonusTotem = (int)Math.floor(totalEntropy * totemBonusRate);

        // Seasonal bonus
        double seasonalBonusRate = engine.plugin().seasons().entropyBonus();
        int bonusSeason = (int)Math.floor(totalEntropy * seasonalBonusRate);

        p.getInventory().setContents(contents);

        int grant = totalEntropy + Math.max(0, bonusAug) + Math.max(0, bonusTotem) + Math.max(0, bonusSeason);
        if (grant > 0) {
            engine.plugin().entropy().add(p, grant);
            String msg = "<green>Gutted <yellow>" + totalFish + "</yellow> fish for <yellow>" + totalEntropy + "</yellow> entropy";
            if (bonusAug > 0) msg += " <gray>+ aug:</gray> <yellow>" + bonusAug + "</yellow>";
            if (bonusTotem > 0) msg += " <gray>+ totem:</gray> <yellow>" + bonusTotem + "</yellow>";
            if (bonusSeason > 0) msg += " <gray>+ season:</gray> <yellow>" + bonusSeason + "</yellow>";
            msg += ".";
            Msg.raw(p, msg);
        } else {
            Msg.raw(p, "<gray>No Piscary fish to gut.</gray>");
        }
    }
}
