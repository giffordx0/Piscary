package com.chunksmith.piscary.gui.components;

import com.chunksmith.piscary.gui.GuiEngine;
import com.chunksmith.piscary.services.DeliveriesService;
import com.chunksmith.piscary.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class DeliveryStatusComponent implements com.chunksmith.piscary.gui.components.Component {
    private final GuiEngine engine;
    private final int slot;

    public DeliveryStatusComponent(GuiEngine engine, ConfigurationSection sec) {
        this.engine = engine;
        this.slot = Math.max(0, sec.getInt("slot", 22));
    }

    @Override
    public void render(Inventory inv, Player viewer) {
        if (slot < 0 || slot >= inv.getSize()) {
            engine.plugin().getLogger().warning("[GUI] Delivery status slot " + slot + " is out of bounds for inventory size " + inv.getSize());
            return;
        }

        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta im = it.getItemMeta();

        Optional<DeliveriesService.Delivery> od = engine.plugin().deliveries().getActive(viewer);
        if (od.isEmpty()) {
            im.displayName(Text.mm(engine.resolveLangRefs("<yellow>No Active Delivery</yellow>")));
            im.lore(java.util.List.of(Text.mm(engine.resolveLangRefs("<gray>Use the button to accept a random delivery.</gray>"))));
        } else {
            var d = od.get();
            im.displayName(Text.mm(engine.resolveLangRefs("<aqua>Delivery:</aqua> <yellow>" + d.name + "</yellow>")));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(Text.mm(engine.resolveLangRefs("<gray>Needs:</gray>")));
            for (var e : d.needs.entrySet()) {
                lore.add(Text.mm(engine.resolveLangRefs(" - <white>" + e.getKey() + "</white> x<yellow>" + e.getValue() + "</yellow>")));
            }
            lore.add(Text.mm(engine.resolveLangRefs("<gray>Rewards:</gray>")));
            if (d.rewardMoney > 0) lore.add(Text.mm(engine.resolveLangRefs(" - <yellow>$" + d.rewardMoney + "</yellow>")));
            if (d.rewardEntropy > 0) lore.add(Text.mm(engine.resolveLangRefs(" - <yellow>" + d.rewardEntropy + " entropy</yellow>")));
            im.lore(lore);
        }
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(im);
        inv.setItem(slot, it);
    }

    @Override
    public void click(InventoryClickEvent e) {
        if (e.getSlot() != slot) return;
        e.setCancelled(true);
    }
}
