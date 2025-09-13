package com.chunksmith.piscary.gui;

import com.chunksmith.piscary.gui.components.Component;
import com.chunksmith.piscary.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MenuView {
    private final GuiEngine engine;
    private final String id;
    private final String titleMini;
    private int size;
    private final List<Component> components = new ArrayList<>();

    public MenuView(GuiEngine engine, String id, File file) {
        this.engine = engine;
        this.id = id;

        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        this.titleMini = engine.resolveLangRefs(y.getString("title", "<white>Menu</white>"));

        int configSize = clampSize(y.getInt("size", 27));
        int maxSlot = -1;

        // Peek at configured slots to auto-expand size if needed
        List<?> rawList = y.getList("components");
        if (rawList != null) {
            for (Object obj : rawList) {
                if (obj instanceof Map<?, ?> m) {
                    Object s = m.get("slot");
                    if (s instanceof Number n) maxSlot = Math.max(maxSlot, n.intValue());
                    // Also check rows[*].slot for list components
                    Object rows = m.get("rows");
                    if (rows instanceof List<?> lr) {
                        for (Object ro : lr) {
                            if (ro instanceof Map<?, ?> rm) {
                                Object rs = rm.get("slot");
                                if (rs instanceof Number rn) maxSlot = Math.max(maxSlot, rn.intValue());
                            }
                        }
                    }
                }
            }
        }
        int needed = maxSlot >= 0 ? roundUpToInv(maxSlot + 1) : configSize;
        this.size = Math.max(configSize, needed);

        // Build components
        if (rawList != null) {
            for (Object obj : rawList) {
                if (obj instanceof Map<?, ?> m) {
                    YamlConfiguration sec = new YamlConfiguration();
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        sec.set(String.valueOf(e.getKey()), e.getValue());
                    }
                    components.add(Component.fromConfig(sec, engine));
                }
            }
        }
    }

    private static int clampSize(int s) {
        if (s < 9) s = 9;
        if (s > 54) s = 54;
        // must be multiple of 9
        int rem = s % 9;
        if (rem != 0) s += 9 - rem;
        return s;
    }
    private static int roundUpToInv(int n) {
        if (n < 9) return 9;
        if (n > 54) return 54;
        int rem = n % 9;
        if (rem == 0) return n;
        int s = n + (9 - rem);
        return Math.min(54, s);
    }

    public String id() { return id; }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(p, size, Text.mm(titleMini));
        for (Component c : components) c.render(inv, p);
        p.openInventory(inv);
    }

    public void handle(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().title().equals(Text.mm(titleMini))) return;

        // Cancel clicks in the top inventory by default so players can't
        // grab or place items in empty slots. Components may still re-enable
        // interaction for their own slots if needed.
        boolean top = e.getRawSlot() < e.getView().getTopInventory().getSize();
        if (top) {
            e.setCancelled(true);
        } else {
            // Also block shift clicks or hotbar swaps originating from the
            // player's inventory that would move items into the menu.
            switch (e.getAction()) {
                case MOVE_TO_OTHER_INVENTORY, HOTBAR_SWAP, HOTBAR_MOVE_AND_READD,
                        SWAP_WITH_CURSOR, COLLECT_TO_CURSOR -> e.setCancelled(true);
                default -> {}
            }
        }

        for (Component c : components) c.click(e);

        // Re-render the menu after handling the click if the click was in the
        // menu itself. This keeps component visuals in sync after actions.
        if (top) {
            for (Component c : components) c.render(e.getView().getTopInventory(), p);
        }
    }

    public List<Component> components() { return components; }
}
