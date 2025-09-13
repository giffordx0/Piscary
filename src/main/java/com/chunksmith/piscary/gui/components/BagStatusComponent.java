package com.chunksmith.piscary.gui.components;

import com.chunksmith.piscary.gui.GuiEngine;
import com.chunksmith.piscary.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BagStatusComponent implements com.chunksmith.piscary.gui.components.Component {
    private final GuiEngine engine;
    private final int slot;
    private final Material material;
    private final String namePattern;
    private final List<String> lorePattern;

    public BagStatusComponent(GuiEngine engine, ConfigurationSection sec) {
        this.engine = engine;
        this.slot = sec.getInt("slot", 13);
        String mat = String.valueOf(sec.contains("material") ? sec.get("material") : "BUNDLE");
        this.material = Material.matchMaterial(mat.toUpperCase());
        this.namePattern = engine.resolveLangRefs(String.valueOf(sec.contains("name") ? sec.get("name") : "<aqua>Fish Bag</aqua>"));
        this.lorePattern = new ArrayList<>(sec.getStringList("lore"));
    }

    @Override
    public void render(Inventory inv, Player viewer) {
        int species = engine.plugin().bag().getSpeciesCount(viewer);
        int total = engine.plugin().bag().getTotalCount(viewer);

        ItemStack it = new ItemStack(material == null ? Material.BUNDLE : material);
        ItemMeta im = it.getItemMeta();
        im.displayName(Text.mm(namePattern));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lorePattern) {
            String s = line.replace("<species>", String.valueOf(species))
                    .replace("<total>", String.valueOf(total));
            lore.add(Text.mm(s));
        }
        im.lore(lore);
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
