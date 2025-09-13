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

public class ButtonComponent implements com.chunksmith.piscary.gui.components.Component {
    private final GuiEngine engine;
    private final int slot;
    private final ItemStack item;
    private final List<Action> actions = new ArrayList<>();

    public ButtonComponent(GuiEngine engine, ConfigurationSection sec) {
        this.engine = engine;
        this.slot = Math.max(0, sec.getInt("slot", 0));

        // Read nested "item" section if present
        ConfigurationSection itemSec = sec.getConfigurationSection("item");

        String name = "<white>Button</white>";
        List<String> lore = new ArrayList<>();
        Material mat = Material.PAPER;

        if (itemSec != null) {
            String matName = itemSec.getString("material", "PAPER");
            Material m = Material.matchMaterial(String.valueOf(matName).toUpperCase(Locale.ROOT));
            if (m != null) mat = m;
            name = itemSec.getString("name", name);
            lore = itemSec.getStringList("lore");
        }

        // Root-level fallbacks for older/other schemas
        if (sec.isString("material")) {
            Material m2 = Material.matchMaterial(sec.getString("material", "PAPER").toUpperCase(Locale.ROOT));
            if (m2 != null) mat = m2;
        }
        if (sec.isString("name")) name = sec.getString("name", name);
        if (sec.isList("lore")) lore = sec.getStringList("lore");

        // Resolve langs
        name = engine.resolveLangRefs(name);
        List<net.kyori.adventure.text.Component> ad = new ArrayList<>();
        for (String line : lore) ad.add(Text.mm(engine.resolveLangRefs(line)));

        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        im.displayName(Text.mm(name));
        if (!ad.isEmpty()) im.lore(ad);
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        it.setItemMeta(im);
        this.item = it;

        for (Map<?, ?> m : sec.getMapList("actions")) {
            actions.add(Action.fromMap(engine, m));
        }
    }

    @Override
    public void render(Inventory inv, Player viewer) {
        if (slot < 0 || slot >= inv.getSize()) {
            engine.plugin().getLogger().warning("[GUI] Button slot " + slot + " is out of bounds for inventory size " + inv.getSize());
            return;
        }
        inv.setItem(slot, item);
    }

    @Override
    public void click(InventoryClickEvent e) {
        if (e.getSlot() != slot) return;
        e.setCancelled(true);
        for (Action a : actions) a.run(e);
    }
}
