package com.chunksmith.piscary.util;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class YamlUtil {
    public static YamlConfiguration load(File file) {
        YamlConfiguration y = new YamlConfiguration();
        try { y.load(file); }
        catch (IOException | InvalidConfigurationException e) { throw new RuntimeException("YAML load failed: " + file.getPath(), e); }
        return y;
    }
}
