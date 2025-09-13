package com.chunksmith.piscary.integrations;

import com.chunksmith.piscary.Piscary;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PiscaryPlaceholders extends PlaceholderExpansion {
    private final Piscary plugin;

    public PiscaryPlaceholders(Piscary plugin) { this.plugin = plugin; }

    @Override public @NotNull String getIdentifier() { return "piscary"; }
    @Override public @NotNull String getAuthor() { return "Chunksmith"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer p, @NotNull String params) {
        if (p == null) return "";
        String key = params.toLowerCase();

        // Use a normal switch statement to avoid "return inside switch expression" errors.
        switch (key) {
            case "level":
                return p.isOnline() ? String.valueOf(level(plugin.level().getXp(p.getPlayer()))) : "0";
            case "entropy":
                return p.isOnline() ? String.valueOf(plugin.entropy().get(p.getPlayer())) : "0";
            case "currentxp":
                return p.isOnline() ? String.valueOf(plugin.level().getXp(p.getPlayer())) : "0";
            case "requiredxp":
                return "1000";

            // Tournaments
            case "istournament":
                return plugin.tournaments().isActive() ? "true" : "false";
            case "tournamenttype":
                return plugin.tournaments().isActive() ? plugin.tournaments().getType().name() : "NONE";
            case "timetilltournament":
                if (plugin.tournaments().isActive()) {
                    return String.valueOf(plugin.tournaments().secondsRemaining());
                } else {
                    var cal = java.util.Calendar.getInstance();
                    int minute = cal.get(java.util.Calendar.MINUTE);
                    int interval = Math.max(1, plugin.getConfig().getInt("auto.interval-minutes", 60));
                    int rem = (interval - (minute % interval)) % interval;
                    return String.valueOf(rem * 60);
                }

                // Crabs
            case "crabskilled":
                return p.isOnline()
                        ? String.valueOf(plugin.crabs().getCrabsKilled(p.getPlayer()))
                        : "0";

            // Seasonal
            case "isseason":
                return plugin.seasons().isActive() ? "true" : "false";
            case "seasonid":
                return plugin.seasons().activeId();
            case "seasonname":
                return plugin.seasons().activeNameMini().replaceAll("<[^>]+>","");

            default:
                return "";
        }
    }

    private int level(int xp) { return (int)Math.floor(Math.sqrt(xp / 25.0)); }
}
