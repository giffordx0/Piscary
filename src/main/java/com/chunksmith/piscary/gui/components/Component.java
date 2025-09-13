package com.chunksmith.piscary.gui.components;

import com.chunksmith.piscary.gui.GuiEngine;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public interface Component {
    void render(Inventory inv, Player viewer);
    void click(InventoryClickEvent e);

    static Component fromConfig(ConfigurationSection sec, GuiEngine engine) {
        String type = sec.getString("type", "button").toLowerCase();
        return switch (type) {
            case "button" -> new ButtonComponent(engine, sec);
            case "list" -> new ListComponent(engine, sec);
            case "delivery_status" -> new DeliveryStatusComponent(engine, sec);
            case "augment_button" -> new AugmentButtonComponent(engine, sec);
            case "bag_status" -> new BagStatusComponent(engine, sec);
            default -> new ButtonComponent(engine, sec);
        };
    }
}
