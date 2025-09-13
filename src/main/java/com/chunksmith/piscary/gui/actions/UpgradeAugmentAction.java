package com.chunksmith.piscary.gui.actions;

import com.chunksmith.piscary.gui.GuiEngine;
import com.chunksmith.piscary.model.AugmentType;
import com.chunksmith.piscary.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Map;

public class UpgradeAugmentAction implements Action {
    private final GuiEngine engine;
    private final AugmentType type;

    public UpgradeAugmentAction(GuiEngine engine, Map<?, ?> map) {
        this.engine = engine;
        String raw = String.valueOf(map.containsKey("augment") ? map.get("augment") : "LUCK");
        AugmentType t;
        try { t = AugmentType.valueOf(raw.toUpperCase(Locale.ROOT)); } catch (Exception e) { t = AugmentType.LUCK; }
        this.type = t;
    }

    @Override
    public void run(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        ItemStack main = p.getInventory().getItemInMainHand();
        if (main == null || main.getType() != Material.FISHING_ROD) {
            Msg.raw(p, "<red>Hold the fishing rod you want to upgrade.</red>");
            return;
        }

        int current = engine.plugin().augments().getLevel(main, type);
        int max = engine.plugin().augments().maxLevel(type);
        if (current >= max) {
            Msg.raw(p, "<yellow>That augment is already maxed.</yellow>");
            return;
        }

        int cost = engine.plugin().augments().nextCost(type, current);
        if (engine.plugin().entropy().get(p) < cost) {
            Msg.raw(p, "<red>Not enough entropy.</red> <gray>Need:</gray> <yellow>" + cost + "</yellow>");
            return;
        }

        engine.plugin().entropy().add(p, -cost);
        engine.plugin().augments().setLevel(main, type, current + 1);
        Msg.raw(p, "<green>Upgraded " + type.name() + " to level " + (current + 1) + "!</green>");
        e.getWhoClicked().closeInventory();
        engine.open("augment", p);
    }
}
