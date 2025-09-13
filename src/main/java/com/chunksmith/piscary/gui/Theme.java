package com.chunksmith.piscary.gui;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Theme {
    public final boolean frame;
    public final String fillToken;

    public Theme(boolean frame, String fillToken) {
        this.frame = frame;
        this.fillToken = fillToken;
    }

    public static Theme fromYaml(YamlConfiguration y) {
        boolean frame = y.getBoolean("frame", true);
        String fill = y.getString("fill", "pane.gray");
        return new Theme(frame, fill);
    }

    public ItemStack makeFillItem() {
        ItemStack it = new ItemStack(paneFor(fillToken));
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(" ");
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(im);
        return it;
    }

    private Material paneFor(String token) {
        String t = token == null ? "" : token.toLowerCase();
        if (!t.startsWith("pane.")) return Material.GRAY_STAINED_GLASS_PANE;
        String color = t.substring("pane.".length());
        return switch (color) {
            case "black" -> Material.BLACK_STAINED_GLASS_PANE;
            case "blue" -> Material.BLUE_STAINED_GLASS_PANE;
            case "brown" -> Material.BROWN_STAINED_GLASS_PANE;
            case "cyan" -> Material.CYAN_STAINED_GLASS_PANE;
            case "green" -> Material.GREEN_STAINED_GLASS_PANE;
            case "light_blue" -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case "lightgray", "light_grey", "light-gray", "silver" -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            case "lime" -> Material.LIME_STAINED_GLASS_PANE;
            case "magenta" -> Material.MAGENTA_STAINED_GLASS_PANE;
            case "orange" -> Material.ORANGE_STAINED_GLASS_PANE;
            case "pink" -> Material.PINK_STAINED_GLASS_PANE;
            case "purple" -> Material.PURPLE_STAINED_GLASS_PANE;
            case "red" -> Material.RED_STAINED_GLASS_PANE;
            case "white" -> Material.WHITE_STAINED_GLASS_PANE;
            case "yellow" -> Material.YELLOW_STAINED_GLASS_PANE;
            default -> Material.GRAY_STAINED_GLASS_PANE;
        };
    }
}
