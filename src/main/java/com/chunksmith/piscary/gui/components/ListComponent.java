package com.chunksmith.piscary.gui.components;

import com.chunksmith.piscary.gui.GuiEngine;
import com.chunksmith.piscary.gui.actions.Action;
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

public class ListComponent implements com.chunksmith.piscary.gui.components.Component {
    private final GuiEngine engine;
    private final int start;
    private final List<Row> rows = new ArrayList<>();

    private static class Row {
        final int slot;
        final Material mat;
        final String name;
        final List<String> lore;
        final List<Action> actions;
        Row(int slot, Material mat, String name, List<String> lore, List<Action> actions) {
            this.slot = slot; this.mat = mat; this.name = name; this.lore = lore; this.actions = actions;
        }
    }

    public ListComponent(GuiEngine engine, ConfigurationSection sec) {
        this.engine = engine;
        this.start = sec.getInt("start", 10);
        for (var r : sec.getMapList("rows")) {
            try {
                int slot = r.containsKey("slot") ? asInt(r.get("slot"), start) : start;
                String matName = r.containsKey("material") ? String.valueOf(r.get("material")) : "PAPER";
                Material mat = Material.matchMaterial(matName.toUpperCase(Locale.ROOT));
                String name = engine.resolveLangRefs(r.containsKey("name")
                        ? String.valueOf(r.get("name"))
                        : "<white>Item</white>");
                @SuppressWarnings("unchecked")
                List<String> lore = r.get("lore") instanceof List<?> lr ? (List<String>) lr : Collections.emptyList();

                List<Action> acts = new ArrayList<>();
                Object ao = r.get("actions");
                if (ao instanceof List<?> al) {
                    for (Object a : al) if (a instanceof Map<?,?> am) acts.add(Action.fromMap(engine, (Map<?, ?>) am));
                }
                rows.add(new Row(slot, mat == null ? Material.PAPER : mat, name, lore, acts));
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void render(Inventory inv, Player viewer) {
        for (Row r : rows) {
            ItemStack it = new ItemStack(r.mat);
            ItemMeta im = it.getItemMeta();
            im.displayName(Text.mm(r.name));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (String s : r.lore) lore.add(Text.mm(engine.resolveLangRefs(s)));
            if (!lore.isEmpty()) im.lore(lore);
            im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            it.setItemMeta(im);
            inv.setItem(r.slot, it);
        }
    }

    @Override
    public void click(InventoryClickEvent e) {
        for (Row r : rows) {
            if (e.getSlot() == r.slot) {
                e.setCancelled(true);
                for (Action a : r.actions) a.run(e);
                return;
            }
        }
    }

    private static int asInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception ignored) { return def; }
    }
}
