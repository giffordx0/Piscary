package com.chunksmith.piscary.gui.components;

import com.chunksmith.piscary.gui.GuiEngine;
import com.chunksmith.piscary.gui.actions.Action;
import com.chunksmith.piscary.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ButtonComponent implements Component {
    private final GuiEngine engine;
    private final int slot;
    private final ItemStack item;
    private final List<Action> actions = new ArrayList<>();

    public ButtonComponent(GuiEngine engine, ConfigurationSection sec) {
        this.engine = engine;
        this.slot = sec.getInt("slot", 0);

        ConfigurationSection itemSec = sec.getConfigurationSection("item");

        // Fallbacks if "item:" section is missing/malformed
        Material mat = Material.BOOK;
        String name = "Button";
        List<String> lore = new ArrayList<>();
        int model = 0;

        if (itemSec != null) {
            Material m = Material.matchMaterial(itemSec.getString("material", "BOOK"));
            if (m != null) mat = m;
            name = engine.resolveLangRefs(itemSec.getString("name", name));
            lore = itemSec.getStringList("lore");
            model = itemSec.getInt("model", 0);
        }

        this.item = new ItemStack(mat);
        ItemMeta im = item.getItemMeta();
        im.displayName(Text.mm(name));
        if (!lore.isEmpty()) im.lore(lore.stream().map(engine::resolveLangRefs).map(Text::mm).toList());
        if (model > 0) im.setCustomModelData(model);
        item.setItemMeta(im);

        var acts = sec.getMapList("actions");
        for (var a : acts) actions.add(Action.fromMap(engine, a));
    }

    @Override
    public void render(Inventory inv, Player viewer) {
        inv.setItem(slot, item);
    }

    @Override
    public void click(InventoryClickEvent e) {
        if (e.getSlot() != slot) return;
        e.setCancelled(true);
        for (Action a : actions) a.run(e);
    }
}
