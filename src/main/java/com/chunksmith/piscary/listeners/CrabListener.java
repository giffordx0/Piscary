package com.chunksmith.piscary.listeners;

import com.chunksmith.piscary.Piscary;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class CrabListener implements Listener {
    private final Piscary plugin;

    public CrabListener(Piscary plugin) { this.plugin = plugin; }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        plugin.crabs().onMove(p, e.getFrom(), e.getTo());
    }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        switch (e.getState()) {
            case FISHING -> plugin.crabs().onCast(p, e);
            case CAUGHT_FISH -> plugin.crabs().onCatch(p);
            default -> { }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (plugin.crabs().isCrab(e.getEntity()) && e.getEntity().getKiller() != null) {
            plugin.crabs().rewardForCrab(e.getEntity().getKiller());
            // Prevent weird drops from silverfish (optional): clear default drops
            e.getDrops().clear();
            e.setDroppedExp(0);
        }
    }
}
