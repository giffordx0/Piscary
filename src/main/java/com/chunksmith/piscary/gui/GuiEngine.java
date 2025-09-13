package com.chunksmith.piscary.gui;

import com.chunksmith.piscary.Piscary;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuiEngine {
    private final Piscary plugin;
    private final Map<String, MenuView> registry = new HashMap<>();
    private final Map<UUID, MenuView> openByPlayer = new HashMap<>();
    private YamlConfiguration lang;

    private static final Pattern LANG_TOKEN_INLINE = Pattern.compile("@?lang:([A-Za-z0-9_.-]+)");
    private static final String[] DEFAULT_MENUS = {
            "main","bag","gut","deliveries","scales","shop","bait","augment","augments","totems","codex"
    };

    public GuiEngine(Piscary plugin) { this.plugin = plugin; }
    public Piscary plugin() { return plugin; }

    public void reload() {
        loadLang();
        ensureDefaultMenus();
        loadMenus();
    }

    private void loadLang() {
        File f = new File(plugin.getDataFolder(), "lang.yml");
        if (!f.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                try { plugin.saveResource("lang.yml", false); } catch (IllegalArgumentException ignored) {}
                if (!f.exists()) f.createNewFile();
            } catch (Exception ignored) {}
        }
        lang = YamlConfiguration.loadConfiguration(f);
    }

    private void ensureDefaultMenus() {
        File dir = new File(plugin.getDataFolder(), "menus");
        if (!dir.exists()) dir.mkdirs();
        for (String name : DEFAULT_MENUS) {
            File f = new File(dir, name + ".yml");
            if (!f.exists()) {
                try { plugin.saveResource("menus/" + name + ".yml", false); } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    private void loadMenus() {
        registry.clear();
        File dir = new File(plugin.getDataFolder(), "menus");
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                String id = f.getName().substring(0, f.getName().length() - 4);
                try {
                    MenuView view = new MenuView(this, id, f);
                    registry.put(id.toLowerCase(Locale.ROOT), view);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed to load menu '" + id + "': " + ex.getMessage());
                }
            }
        }
        plugin.getLogger().info("Loaded " + registry.size() + " menus.");
    }

    /** Replace @lang: tokens using lang.yml; missing keys prettified. */
    public String resolveLangRefs(String input) {
        if (input == null || input.isEmpty()) return "";
        if (input.startsWith("@lang:") || input.startsWith("lang:")) {
            String key = input.substring(input.indexOf(':') + 1);
            String val = lang != null ? lang.getString(key) : null;
            return val != null ? val : prettifyKey(key);
        }
        Matcher m = LANG_TOKEN_INLINE.matcher(input);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            String val = lang != null ? lang.getString(key) : null;
            if (val == null) val = prettifyKey(key);
            m.appendReplacement(out, Matcher.quoteReplacement(val));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String prettifyKey(String key) {
        String last = key;
        int dot = key.lastIndexOf('.');
        if (dot >= 0 && dot + 1 < key.length()) last = key.substring(dot + 1);
        last = last.replace('_',' ').replace('-',' ').trim();
        if (last.isEmpty()) last = key;
        String[] parts = last.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<parts.length;i++){
            String p = parts[i];
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length()>1) sb.append(p.substring(1).toLowerCase(Locale.ROOT));
            if (i+1<parts.length) sb.append(' ');
        }
        return sb.toString();
    }

    public Optional<MenuView> get(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(registry.get(id.toLowerCase(Locale.ROOT)));
    }

    public void open(String id, Player p) {
        MenuView v = get(id).orElse(null);
        if (v == null) { plugin.getLogger().warning("Menu not found: " + id); return; }
        v.open(p);
        openByPlayer.put(p.getUniqueId(), v);
    }

    public MenuView current(Player p) { return openByPlayer.get(p.getUniqueId()); }
    public void closed(Player p) { openByPlayer.remove(p.getUniqueId()); }
}
