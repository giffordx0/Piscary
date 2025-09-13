package com.chunksmith.piscary.util;

import com.chunksmith.piscary.services.LangService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public class Msg {
    private static Plugin plugin;
    private static LangService lang;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void init(Plugin p, LangService l) { plugin = p; lang = l; }
    private static String prefix() { return plugin.getConfig().getString("messages.prefix",""); }

    public static void send(CommandSender to, String key) {
        String raw = plugin.getConfig().getString("messages."+key, "<gray>...</gray>");
        to.sendMessage(MM.deserialize(prefix() + raw));
    }
    public static void sendf(CommandSender to, String key, Map<String,String> ph) {
        String msg = plugin.getConfig().getString("messages."+key, "");
        if (msg == null) msg = "";
        for (var e : ph.entrySet()) msg = msg.replace("<"+e.getKey()+">", e.getValue());
        to.sendMessage(MM.deserialize(prefix() + msg));
    }
    public static void raw(CommandSender to, String message) { to.sendMessage(MM.deserialize(prefix() + message)); }
}
