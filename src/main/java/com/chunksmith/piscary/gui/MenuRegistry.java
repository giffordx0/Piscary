package com.chunksmith.piscary.gui;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Map;

public class MenuRegistry {
    private final Map<String, MenuView> menus = new HashMap<>();

    public void put(String id, MenuView view) { menus.put(id.toLowerCase(), view); }
    public MenuView get(String id) { return menus.get(id.toLowerCase()); }
    public void clear() { menus.clear(); }
}
