package com.chunksmith.piscary.gui.components;

import com.chunksmith.piscary.gui.GuiEngine;
import com.chunksmith.piscary.gui.actions.Action;
import com.chunksmith.piscary.model.AugmentType;
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
import java.util.Locale;
import java.util.Map;

public class AugmentButtonComponent implements com.chunksmith.piscary.gui.components.Component {
    private final GuiEngine engine;
    private final int slot;
    private final Material material;
    private final AugmentType type;
    private final String namePattern;
    private final List<String> lorePattern;
    private final List<Action> actions = new ArrayList<>();

    public AugmentButtonComponent(GuiEngine engine, ConfigurationSection sec) {
        this.engine = engine;
        this.slot = sec.getInt("slot", 0);
        String matName = String.valueOf(sec.contains("item.material") ? sec.get("item.material") : "BOOK");
        this.material = Material.matchMaterial(matName.toUpperCase(Locale.ROOT));
        String raw = String.valueOf(sec.contains("augment") ? sec.get("augment") : "LUCK");
        AugmentType t;
        try { t = AugmentType.valueOf(raw.toUpperCase(Locale.ROOT)); } catch (Exception e) { t = AugmentType.LUCK; }
        this.type = t;
        this.namePattern = engine.resolveLangRefs(String.valueOf(sec.contains("item.name") ? sec.get("item.name") : "<white>Augment</white>"));
        this.lorePattern = new ArrayList<>(sec.getStringList("item.lore"));

        var acts = sec.getMapList("actions");
        for (var a : acts) actions.add(Action.fromMap(engine, a));
    }

    @Override
    public void render(Inventory inv, Player viewer) {
        ItemStack main = viewer.getInventory().getItemInMainHand();
        int level = engine.plugin().augments().getLevel(main, type);
        int max = engine.plugin().augments().maxLevel(type);
        int cost = engine.plugin().augments().nextCost(type, level);

        ItemStack it = new ItemStack(material == null ? Material.BOOK : material);
        ItemMeta im = it.getItemMeta();

        String name = namePattern
                .replace("<augment>", type.name())
                .replace("<level>", String.valueOf(level))
                .replace("<max>", String.valueOf(max))
                .replace("<next_cost>", cost < 0 ? "MAX" : String.valueOf(cost));
        im.displayName(Text.mm(name));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lorePattern) {
            lore.add(Text.mm(
                    line.replace("<augment>", type.name())
                            .replace("<level>", String.valueOf(level))
                            .replace("<max>", String.valueOf(max))
                            .replace("<next_cost>", cost < 0 ? "MAX" : String.valueOf(cost))
            ));
        }
        if (!lore.isEmpty()) im.lore(lore);
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        it.setItemMeta(im);

        inv.setItem(slot, it);
    }

    @Override
    public void click(InventoryClickEvent e) {
        if (e.getSlot() != slot) return;
        e.setCancelled(true);
        for (Action a : actions) a.run(e);
    }
}
