package com.chunksmith.piscary.services;

import com.chunksmith.piscary.util.YamlUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class LangService {
    private final Plugin plugin;
    private YamlConfiguration lang;

    public LangService(Plugin plugin) { this.plugin = plugin; reload(); }

    public void reload() { lang = YamlUtil.load(new File(plugin.getDataFolder(), "lang/en.yml")); }

    public String get(String key, String def) {
        String v = lang.getString(key);
        return v != null ? v : def;
    }
}
