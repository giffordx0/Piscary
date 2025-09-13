package com.chunksmith.piscary.gui.actions;

import com.chunksmith.piscary.gui.GuiEngine;
import com.chunksmith.piscary.model.Fish;
import com.chunksmith.piscary.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class GambleOneAction implements Action {
    private final GuiEngine engine;
    private final Mode mode;

    private enum Mode { RANDOM, HIGHEST }

    public GambleOneAction(GuiEngine engine, Map<?, ?> map) {
        this.engine = engine;
        String raw = String.valueOf(map.containsKey("mode") ? map.get("mode") : "RANDOM").toUpperCase(Locale.ROOT);
        Mode m;
        try { m = Mode.valueOf(raw); } catch (Exception e) { m = Mode.RANDOM; }
        this.mode = m;
    }

    @Override
    public void run(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || !hand.hasItemMeta()) { Msg.raw(p, "<gray>Hold a Piscary fish to gamble.</gray>"); return; }

        List<Fish> db = engine.plugin().fish().all();
        Fish match = null;
        for (Fish f : db) {
            if (hand.getType() == f.material()
                    && hand.getItemMeta().hasCustomModelData()
                    && hand.getItemMeta().getCustomModelData() == f.model()) {
                match = f; break;
            }
        }
        if (match == null) { Msg.raw(p, "<gray>That doesn't look like a Piscary fish.</gray>"); return; }

        var conf = engine.plugin().getConfig();
        double baseChance = conf.getDouble("scales.base-win-chance", 0.45);
        double baseProfit = conf.getDouble("scales.base-profit-mult", 1.80);
        var rsec = conf.getConfigurationSection("scales.rarity." + match.rarity().name());
        if (rsec != null) {
            baseChance = rsec.getDouble("chance", baseChance);
            baseProfit = rsec.getDouble("profit", baseProfit);
        }

        boolean win = new Random().nextDouble() < baseChance;
        if (win) {
            double valEach = engine.plugin().fish().sellValue(match);
            double payout = valEach * baseProfit * hand.getAmount();
            engine.plugin().eco().deposit(p, payout);
            p.getInventory().setItemInMainHand(null);
            Msg.raw(p, "<green>You won!</green> Sold for <yellow>" + String.format(java.util.Locale.US, "%.2f", payout) + "</yellow>.");
        } else {
            p.getInventory().setItemInMainHand(null);
            Msg.raw(p, "<red>You lost the gamble.</red>");
        }
    }
}
