package com.chunksmith.piscary.commands;

import com.chunksmith.piscary.Piscary;
import com.chunksmith.piscary.model.TournamentType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FtCommand implements CommandExecutor, TabCompleter {
    private final Piscary plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public FtCommand(Piscary plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { help(sender, label); return true; }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "start" -> {
                require(sender, "piscary.tournaments");
                if (args.length < 2) { sender.sendMessage("/" + label + " start <type> [duration]"); return true; }
                TournamentType type = TournamentType.safe(args[1], TournamentType.MOST_FISH);
                int duration = (args.length >= 3) ? parseDuration(args[2]) : 0;
                boolean ok = plugin.tournaments().start(type, duration);
                sender.sendMessage(ok ? "Tournament started: " + type : "Tournament already active.");
            }
            case "stop" -> {
                require(sender, "piscary.tournaments");
                boolean ok = plugin.tournaments().stop();
                sender.sendMessage(ok ? "Tournament stopped." : "No active tournament.");
            }
            case "info" -> {
                if (!plugin.tournaments().isActive()) { sender.sendMessage("No active tournament."); return true; }
                var t = plugin.tournaments();
                sender.sendMessage("Type: " + t.getType());
                sender.sendMessage("Time remaining: " + t.secondsRemaining() + "s");
                var top = t.topList(3);
                int i = 1;
                for (var e : top) {
                    String name = (Bukkit.getPlayer(e.getKey()) != null) ? Bukkit.getPlayer(e.getKey()).getName() : e.getKey().toString();
                    sender.sendMessage(i + ") " + name);
                    i++;
                }
            }
            case "types" -> {
                sender.sendMessage("Tournament types: " + String.join(", ", plugin.tournaments().listTypes()));
            }
            case "time" -> {
                if (!plugin.tournaments().isActive()) { sender.sendMessage("No active tournament."); return true; }
                sender.sendMessage("Time remaining: " + plugin.tournaments().secondsRemaining() + "s");
            }
            case "remove" -> {
                require(sender, "piscary.tournaments");
                if (args.length < 2) { sender.sendMessage("/" + label + " remove <player>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage("Player not found."); return true; }
                plugin.tournaments().removePlayer(target.getUniqueId());
                sender.sendMessage("Removed " + target.getName() + " from the tournament.");
            }
            default -> help(sender, label);
        }
        return true;
    }

    private void help(CommandSender s, String label) {
        s.sendMessage(mm.deserialize("""
            <gradient:#00e5ff:#0077ff><b>Piscary Tournaments</b></gradient>
            <gray>/"%s start <type> [duration]"</gray>
            <gray>/"%s stop"</gray>
            <gray>/"%s info"</gray>
            <gray>/"%s types"</gray>
            <gray>/"%s time"</gray>
            <gray>/"%s remove <player>"</gray>
            """.replace("\"", "")
                .formatted(label,label,label,label,label,label)));
    }

    private void require(CommandSender s, String perm) {
        if (!s.hasPermission(perm)) throw new CommandException("No permission.");
    }

    private int parseDuration(String arg) {
        try {
            if (arg.endsWith("m")) return Integer.parseInt(arg.substring(0, arg.length()-1)) * 60;
            if (arg.endsWith("s")) return Integer.parseInt(arg.substring(0, arg.length()-1));
            return Integer.parseInt(arg);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String opt : List.of("start","stop","info","types","time","remove")) {
                if (opt.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(opt);
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            for (TournamentType t : TournamentType.values()) {
                String n = t.name().toLowerCase(Locale.ROOT);
                if (n.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(n);
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(p.getName());
            }
            return out;
        }
        return out;
    }
}
