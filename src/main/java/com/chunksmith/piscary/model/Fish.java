package com.chunksmith.piscary.model;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public record Fish(
        String id,
        Component displayName,
        Material material,
        int model,
        Rarity rarity,
        double value,
        List<Component> lore,
        FishCondition condition
) {
    public ItemStack toItem() {
        ItemStack it = new ItemStack(material);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(displayName);
        if (model > 0) meta.setCustomModelData(model);
        if (lore != null && !lore.isEmpty()) meta.lore(lore);
        it.setItemMeta(meta);
        return it;
    }
}