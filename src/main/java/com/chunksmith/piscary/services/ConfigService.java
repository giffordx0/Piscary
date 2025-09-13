package com.chunksmith.piscary.services;

import com.chunksmith.piscary.util.YamlUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class ConfigService {
    private final Plugin plugin;
    private YamlConfiguration messages;
    private YamlConfiguration fishIndex;

    public ConfigService(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        messages = YamlUtil.load(new File(plugin.getDataFolder(), "messages.yml"));
        fishIndex = YamlUtil.load(new File(plugin.getDataFolder(), "fish/index.yml"));
    }

    public FileConfiguration main() { return plugin.getConfig(); }
    public FileConfiguration messages() { return messages; }
    public FileConfiguration fishIndex() { return fishIndex; }

    public YamlConfiguration loadYaml(String path) { return YamlUtil.load(new File(plugin.getDataFolder(), path)); }
}
