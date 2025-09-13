package com.chunksmith.piscary.services;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyService {
    private final Plugin plugin;
    private Economy vault;

    public EconomyService(Plugin plugin) {
        this.plugin = plugin;
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) vault = rsp.getProvider();
        }
    }

    public void deposit(Player p, double amt) {
        if (amt <= 0) return;
        if (vault != null) vault.depositPlayer(p, amt);
        else p.sendMessage("+" + plugin.getConfig().getString("currency-symbol", "$") + amt);
    }

    public boolean has(Player p, double amt) {
        if (amt <= 0) return true;
        if (vault == null) return true; // free if no Vault
        return vault.getBalance(p) >= amt;
    }

    public boolean withdraw(Player p, double amt) {
        if (amt <= 0) return true;
        if (vault == null) {
            p.sendMessage("Vault not found — purchase is free this time.");
            return true;
        }
        if (!has(p, amt)) return false;
        vault.withdrawPlayer(p, amt);
        return true;
    }
}
