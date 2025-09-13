package com.chunksmith.piscary.gui.actions;

import com.chunksmith.piscary.gui.GuiEngine;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;

public interface Action {
    void run(InventoryClickEvent e);

    static Action fromMap(GuiEngine engine, Map<?, ?> map) {
        Object t = map.get("type");
        String type = (t == null ? "openMenu" : String.valueOf(t)).toLowerCase();

        return switch (type) {
            case "sellall" -> new SellAllAction(engine, map);
            case "gutall" -> new GutAllAction(engine, map);
            case "buyapplybait" -> new BuyApplyBaitAction(engine, map);
            case "buytotem" -> new BuyTotemAction(engine, map);           // NEW
            case "gambleone" -> new GambleOneAction(engine, map);
            case "acceptdelivery" -> new AcceptRandomDeliveryAction(engine, map);
            case "claimgoods" -> new ClaimDeliveryAction(engine, map);
            case "abandondelivery" -> new AbandonDeliveryAction(engine, map);
            case "upgradeaugment" -> new UpgradeAugmentAction(engine, map);
            case "depositbag" -> new DepositAllToBagAction(engine, map);
            case "withdrawbag" -> new WithdrawAllFromBagAction(engine, map);
            case "openmenu" -> new OpenMenuAction(engine, map);
            default -> new OpenMenuAction(engine, map);
        };
    }
}
