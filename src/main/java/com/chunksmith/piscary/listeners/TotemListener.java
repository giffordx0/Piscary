package com.chunksmith.piscary.listeners;

import com.chunksmith.piscary.Piscary;
import com.chunksmith.piscary.util.Msg;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class TotemListener implements Listener {
    private final Piscary plugin;

    public TotemListener(Piscary plugin) { this.plugin = plugin; }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        ItemStack inHand = e.getItemInHand();
        var typeOpt = plugin.totems().fromItem(inHand);
        if (typeOpt.isEmpty()) return;

        // Require placing the exact item
        var type = typeOpt.get();

        // Force block type to the item's material to avoid confusion
        Block b = e.getBlockPlaced();
        b.setType(type.itemMaterial());

        boolean ok = plugin.totems().place(e.getPlayer(), b.getLocation(), type);
        if (!ok) {
            e.setCancelled(true);
            Msg.raw(e.getPlayer(), "<red>There is already a totem here.</red>");
            return;
        }

        Msg.raw(e.getPlayer(), "<green>Placed totem:</green> " + type.nameMini());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        var w = b.getWorld();
        // Try removing; allowOthers depends on permission
        boolean allowOthers = e.getPlayer().hasPermission("fish.totems.break.others") || e.getPlayer().hasPermission("piscary.totems.break.others");
        boolean removed = plugin.totems().remove(b.getLocation(), allowOthers, e.getPlayer().getUniqueId());
        if (removed) {
            Msg.raw(e.getPlayer(), "<yellow>Totem removed.</yellow>");
            // Drop the totem item back?
            // Keep it simple: not returning item to avoid duplication edge cases.
        }
    }

    // Nice-to-have: right click any totem block to ping current buff
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null) return;
        // If block matches any totem material, show current buff
        if (b.getType() == Material.BEACON || b.getType() == Material.LODESTONE || b.getType() == Material.CONDUIT || b.getType() == Material.AMETHYST_BLOCK) {
            Player p = e.getPlayer();
            double sell = plugin.totems().sellMultiplier(p);
            double xp = plugin.totems().xpMultiplier(p);
            double ent = plugin.totems().entropyBonus(p);
            Msg.raw(p, "<aqua>Totem aura:</aqua> <gray>Sell x</gray><yellow>" + sell + "</yellow> <gray>XP x</gray><yellow>" + xp + "</yellow> <gray>Gut +</gray><yellow>" + (int)Math.round(ent*100) + "%</yellow>");
        }
    }
}
