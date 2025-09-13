package com.chunksmith.piscary.commands;

import com.chunksmith.piscary.Piscary;
import com.chunksmith.piscary.model.Fish;
import com.chunksmith.piscary.util.Msg;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class FishCommand implements CommandExecutor, TabCompleter {
    private final Piscary plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public FishCommand(Piscary plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender, label); return true; }

        String sub = args[0].toLowerCase(Locale.ROOT);
        try {
            switch (sub) {
                case "menu" -> { require(sender, "piscary.menu"); openMenu(sender); }
                case "shop" -> { require(sender, "piscary.shop"); openShop(sender); }
                case "scales" -> { require(sender, "piscary.scales"); openScales(sender); }
                case "bag" -> { require(sender, "piscary.bag"); openBag(sender); }
                case "gut" -> { require(sender, "piscary.gut"); openGut(sender); }
                case "deliveries" -> { require(sender, "piscary.deliveries"); openDeliveries(sender); }
                case "augment" -> { require(sender, "piscary.augment"); openAugment(sender); }
                case "augments" -> { require(sender, "piscary.augments"); openAugments(sender); }
                case "totems" -> { require(sender, "piscary.totems"); openTotems(sender); } // NEW
                case "entropy" -> { require(sender, "piscary.entropy"); showEntropy(sender); }
                case "boosters" -> { require(sender, "piscary.boosters"); showBoosters(sender); }
                case "bait" -> { require(sender, "piscary.bait"); openBait(sender); }
                case "codex" -> { require(sender, "piscary.codex"); openCodex(sender); }
                case "removedelivery" -> { require(sender, "piscary.removedelivery"); removedelivery(sender, args); }
                case "reload" -> { require(sender, "piscary.reload"); doReload(sender); }
                case "give" -> { require(sender, "piscary.give"); doGive(sender, args); }
                case "addentropy" -> { require(sender, "piscary.addentropy"); addEntropy(sender, args); }
                case "addxp" -> { require(sender, "piscary.addxp"); addXp(sender, args); }
                default -> sendHelp(sender, label);
            }
        } catch (CommandException ignored) { }
        return true;
    }

    private void sendHelp(CommandSender s, String label) {
        s.sendMessage(mm.deserialize("""
            <gradient:#00e5ff:#0077ff><b>Piscary</b></gradient> <gray>— Commands</gray>
            <aqua>/%s</aqua> <gray>(help)</gray>
            <aqua>/%s menu</aqua> <gray>Main menu</gray>
            <aqua>/%s shop</aqua> <gray>Shop</gray>
            <aqua>/%s bag</aqua> <gray>Fish bag</gray>
            <aqua>/%s gut</aqua> <gray>Gutting</gray>
            <aqua>/%s scales</aqua> <gray>Scales</gray>
            <aqua>/%s deliveries</aqua> <gray>Deliveries</gray>
            <aqua>/%s augment</aqua> <gray>Augment GUI</gray>
            <aqua>/%s augments</aqua> <gray>All augments</gray>
            <aqua>/%s totems</aqua> <gray>Totems</gray>
            <aqua>/%s entropy</aqua> <gray>Entropy</gray>
            <aqua>/%s boosters</aqua> <gray>Boosters</gray>
            <aqua>/%s bait</aqua> <gray>Bait shop</gray>
            <aqua>/%s codex</aqua> <gray>Fish codex</gray>
            <aqua>/%s reload</aqua> <gray>Reload</gray>
            <aqua>/%s removedelivery [player]</aqua> <gray>Remove an active delivery</gray>
            """.formatted(label,label,label,label,label,label,label,label,label,label,label,label,label,label,label)));
    }

    private void openMenu(CommandSender s) { playerOnly(s, p -> plugin.menus().open("main", p)); }
    private void openShop(CommandSender s) { playerOnly(s, p -> plugin.menus().open("shop", p)); }
    private void openScales(CommandSender s) { playerOnly(s, p -> plugin.menus().open("scales", p)); }
    private void openBag(CommandSender s) { playerOnly(s, p -> plugin.menus().open("bag", p)); }
    private void openGut(CommandSender s) { playerOnly(s, p -> plugin.menus().open("gut", p)); }
    private void openDeliveries(CommandSender s) { playerOnly(s, p -> plugin.menus().open("deliveries", p)); }
    private void openAugment(CommandSender s) { playerOnly(s, p -> plugin.menus().open("augment", p)); }
    private void openAugments(CommandSender s) { playerOnly(s, p -> plugin.menus().open("augments", p)); }
    private void openTotems(CommandSender s) { playerOnly(s, p -> plugin.menus().open("totems", p)); } // NEW
    private void openBait(CommandSender s) { playerOnly(s, p -> plugin.menus().open("bait", p)); }
    private void openCodex(CommandSender s) { playerOnly(s, p -> plugin.menus().open("codex", p)); }

    private void showEntropy(CommandSender s) {
        playerOnly(s, p -> Msg.raw(p, "<gray>Entropy:</gray> <yellow>" + plugin.entropy().get(p) + "</yellow>"));
    }

    private void showBoosters(CommandSender s) {
        playerOnly(s, p -> {
            var ob = plugin.bait().getActive(p);
            if (ob.isEmpty()) {
                Msg.raw(p, "<gray>No active boosters.</gray>");
                return;
            }
            long ms = plugin.bait().getActiveRemainingMillis(p);
            long sec = Math.max(1, ms / 1000);
            Msg.raw(p, "<green>Active bait:</green> " + ob.get().name() + " <gray>(" + sec + "s remaining)</gray>");
        });
    }

    private void removedelivery(CommandSender s, String[] args) {
        if (args.length >= 2 && s.hasPermission("piscary.removedelivery")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) { s.sendMessage("Player not found."); return; }
            plugin.deliveries().abandon(target);
            s.sendMessage("Removed " + target.getName() + "'s active delivery.");
            return;
        }
        playerOnly(s, p -> {
            plugin.deliveries().abandon(p);
            Msg.raw(p, "<yellow>Your active delivery was removed.</yellow>");
        });
    }

    private void doReload(CommandSender s) {
        plugin.config().reload();
        plugin.lang().reload();
        plugin.fish().reload();
        plugin.bait().reload();
        plugin.menus().reload();
        plugin.deliveries().reload();
        plugin.totems().reload(); // NEW
        Msg.send(s, "reloaded");
    }

    private void doGive(CommandSender s, String[] args) {
        if (args.length < 4) { s.sendMessage("/fish give <player> <fishId> <amount>"); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { s.sendMessage("Player not found."); return; }
        String fishId = args[2];
        int amount;
        try { amount = Math.max(1, Integer.parseInt(args[3])); } catch (Exception e) { s.sendMessage("Invalid amount."); return; }

        Optional<Fish> fishOpt = plugin.fish().all().stream().filter(f -> f.id().equalsIgnoreCase(fishId)).findFirst();
        if (fishOpt.isEmpty()) { s.sendMessage("Unknown fish id."); return; }
        var item = fishOpt.get().toItem();
        item.setAmount(amount);
        target.getInventory().addItem(item);
        s.sendMessage("Gave " + amount + "x " + fishId + " to " + target.getName());
    }

    private void addEntropy(CommandSender s, String[] args) {
        if (args.length < 3) { s.sendMessage("/fish addentropy <player> <amount>"); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { s.sendMessage("Player not found."); return; }
        int amt;
        try { amt = Integer.parseInt(args[2]); } catch (Exception e) { s.sendMessage("Invalid amount."); return; }
        plugin.entropy().add(target, amt);
        s.sendMessage("Added " + amt + " entropy to " + target.getName());
    }

    private void addXp(CommandSender s, String[] args) {
        if (args.length < 3) { s.sendMessage("/fish addxp <player> <amount>"); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { s.sendMessage("Player not found."); return; }
        int amt;
        try { amt = Integer.parseInt(args[2]); } catch (Exception e) { s.sendMessage("Invalid amount."); return; }
        plugin.level().setXp(target, plugin.level().getXp(target) + amt);
        s.sendMessage("Added " + amt + " fishing XP to " + target.getName());
    }

    private void playerOnly(CommandSender s, java.util.function.Consumer<Player> run) {
        if (!(s instanceof Player p)) { Msg.send(s, "players-only"); throw new CommandException(); }
        run.accept(p);
    }
    private void require(CommandSender s, String perm) {
        if (!s.hasPermission(perm)) { Msg.send(s, "no-perm"); throw new CommandException(); }
    }

    private static final List<String> ROOT = List.of("menu","shop","scales","bag","gut","deliveries","augment","augments",
            "totems","entropy","boosters","bait","codex","removedelivery","reload","give","addentropy","addxp");

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
        if (args.length == 1) return ROOT.stream().filter(t -> t.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        if (args.length == 2 && args[0].equalsIgnoreCase("give"))
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        if (args.length == 3 && args[0].equalsIgnoreCase("give"))
            return plugin.fish().all().stream().map(Fish::id).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
