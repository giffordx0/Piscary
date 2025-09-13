package com.chunksmith.piscary.listeners;

import com.chunksmith.piscary.Piscary;
import com.chunksmith.piscary.model.Fish;
import com.chunksmith.piscary.model.Rarity;
import com.chunksmith.piscary.model.AugmentType;
import com.chunksmith.piscary.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.Optional;
import java.util.Random;

public class FishingListener implements Listener {
    private final Piscary plugin;
    private final Random rng = new Random();

    public FishingListener(Piscary plugin) { this.plugin = plugin; }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!(e.getCaught() instanceof Item)) return;

        var p = e.getPlayer();

        // LUCK augment: extra rolls, pick best rarity
        int luckLvl = plugin.augments().getLevel(p.getInventory().getItemInMainHand(), AugmentType.LUCK);
        int rolls = Math.max(1, 1 + plugin.augments().extraRollsPerLevel(AugmentType.LUCK) * luckLvl);

        Fish best = null;
        for (int i = 0; i < rolls; i++) {
            Optional<Fish> roll = plugin.fish().rollCatch(p);
            if (roll.isEmpty()) continue;
            Fish f = roll.get();
            if (best == null || rarityRank(f.rarity()) > rarityRank(best.rarity())) best = f;
        }
        if (best == null) return;

        // Replace vanilla
        e.getCaught().remove();
        ItemStack caughtItem = best.toItem();
        p.getInventory().addItem(caughtItem);

        // DOUBLE_HOOK: chance to duplicate
        int dblLvl = plugin.augments().getLevel(p.getInventory().getItemInMainHand(), AugmentType.DOUBLE_HOOK);
        double chance = plugin.augments().extraDropChancePerLevel(AugmentType.DOUBLE_HOOK) * dblLvl;
        if (dblLvl > 0 && rng.nextDouble() < chance) {
            ItemStack dup = best.toItem();
            // Avoid overflow of stacks for certain materials
            if (dup.getType() != Material.AIR) p.getInventory().addItem(dup);
            Msg.raw(p, "<yellow>Double Hook!</yellow> You pulled a second fish.");
        }

        // XP, deliveries, tournaments
        plugin.level().addCatchXp(p, best.rarity());
        plugin.deliveries().onCatch(p, best.id());
        plugin.tournaments().onCatch(p, best);

        double val = plugin.fish().sellValue(best);
        Msg.sendf(p, "caught", java.util.Map.of(
                "fish", best.displayName().toString(),
                "rarity", best.rarity().name(),
                "value", String.valueOf(val)
        ));
    }

    private int rarityRank(Rarity r) {
        return switch (r) {
            case COMMON -> 0;
            case UNCOMMON -> 1;
            case RARE -> 2;
            case EPIC -> 3;
            case LEGENDARY -> 4;
        };
    }
}
