package com.chunksmith.piscary.services;

import com.chunksmith.piscary.Piscary;
import com.chunksmith.piscary.model.Fish;
import com.chunksmith.piscary.model.Rarity;
import com.chunksmith.piscary.model.TournamentType;
import com.chunksmith.piscary.util.Msg;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class TournamentService {
    private final Piscary plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    // Active state
    private boolean active = false;
    private TournamentType type = TournamentType.MOST_FISH;
    private Instant endsAt = Instant.EPOCH;
    private final Map<UUID, Score> scores = new HashMap<>();
    private BossBar bossBar;
    private BukkitTask ticker;
    private BukkitTask autoStarter;

    // Config cache
    private boolean autoEnabled;
    private int autoIntervalMinutes;
    private int defaultDurationSec;
    private List<TournamentType> enabledTypes = new ArrayList<>();
    private Map<Rarity,Integer> rarityPoints = new EnumMap<>(Rarity.class);
    private double reward1Money, reward2Money, reward3Money;
    private int reward1Entropy, reward2Entropy, reward3Entropy;

    public TournamentService(Piscary plugin) {
        this.plugin = plugin;
        reload();
        // auto-start checker, every minute
        autoStarter = Bukkit.getScheduler().runTaskTimer(plugin, this::autoStartTick, 20L, 20L * 60);
    }

    public void reload() {
        var y = plugin.getConfig(); // keep global config free; using dedicated file:
        var conf = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File(plugin.getDataFolder(), "tournaments.yml"));

        this.autoEnabled = conf.getBoolean("auto.enabled", true);
        this.autoIntervalMinutes = Math.max(5, conf.getInt("auto.interval-minutes", 60));
        this.defaultDurationSec = Math.max(60, conf.getInt("defaults.duration-seconds", 900));

        this.enabledTypes.clear();
        for (String s : conf.getStringList("types.enabled")) {
            enabledTypes.add(TournamentType.safe(s, TournamentType.MOST_FISH));
        }
        if (enabledTypes.isEmpty()) enabledTypes = List.of(TournamentType.MOST_FISH, TournamentType.HIGHEST_VALUE, TournamentType.RARITY_POINTS);

        rarityPoints.clear();
        var rp = conf.getConfigurationSection("types.rarity_points.points");
        if (rp != null) {
            for (String k : rp.getKeys(false)) {
                try {
                    rarityPoints.put(Rarity.valueOf(k.toUpperCase(Locale.ROOT)), rp.getInt(k, 1));
                } catch (Exception ignored) {}
            }
        } else {
            rarityPoints.put(Rarity.COMMON, 1);
            rarityPoints.put(Rarity.UNCOMMON, 2);
            rarityPoints.put(Rarity.RARE, 3);
            rarityPoints.put(Rarity.EPIC, 5);
            rarityPoints.put(Rarity.LEGENDARY, 8);
        }

        reward1Money = conf.getDouble("rewards.1st.money", 1000.0);
        reward2Money = conf.getDouble("rewards.2nd.money", 500.0);
        reward3Money = conf.getDouble("rewards.3rd.money", 250.0);
        reward1Entropy = conf.getInt("rewards.1st.entropy", 100);
        reward2Entropy = conf.getInt("rewards.2nd.entropy", 50);
        reward3Entropy = conf.getInt("rewards.3rd.entropy", 25);
    }

    // --- Public API ---

    public boolean isActive() { return active; }
    public TournamentType getType() { return type; }
    public long secondsRemaining() { return Math.max(0, endsAt.getEpochSecond() - Instant.now().getEpochSecond()); }

    public boolean start(TournamentType type, int durationSeconds) {
        if (active) return false;
        this.type = type;
        this.endsAt = Instant.now().plusSeconds(durationSeconds <= 0 ? defaultDurationSec : durationSeconds);
        this.active = true;
        this.scores.clear();

        // boss bar
        if (bossBar != null) hideBossbarAll();
        bossBar = BossBar.bossBar(mm.deserialize("<gradient:#00e5ff:#0077ff><b>" + typeName() + "</b></gradient>"), 1.0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        showBossbarAll();

        // ticker
        if (ticker != null) ticker.cancel();
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        broadcast("<yellow>Tournament started:</yellow> " + typeName() + " <gray>(" + formatDuration(secondsRemaining()) + ")</gray>");
        return true;
    }

    public boolean stop() {
        if (!active) return false;
        // sort winners
        var top = topList(3);
        announceWinners(top);
        giveRewards(top);
        // cleanup
        active = false;
        scores.clear();
        if (ticker != null) ticker.cancel();
        hideBossbarAll();
        return true;
    }

    public void shutdown() {
        if (ticker != null) ticker.cancel();
        if (autoStarter != null) autoStarter.cancel();
        hideBossbarAll();
    }

    public void onCatch(Player p, Fish fish) {
        if (!active) return;
        Score s = scores.computeIfAbsent(p.getUniqueId(), k -> new Score());
        switch (type) {
            case MOST_FISH -> s.count++;
            case HIGHEST_VALUE -> {
                double v = plugin.fish().sellValue(fish);
                if (v > s.bestValue) s.bestValue = v;
            }
            case RARITY_POINTS -> s.points += rarityPoints.getOrDefault(fish.rarity(), 1);
        }
        updateBar();
    }

    public List<String> listTypes() {
        return enabledTypes.stream().map(Enum::name).collect(Collectors.toList());
    }

    public List<Map.Entry<UUID, Score>> topList(int n) {
        Comparator<Map.Entry<UUID, Score>> cmp = comparatorFor(type);
        return scores.entrySet().stream().sorted(cmp.reversed()).limit(n).toList();
    }

    public void removePlayer(UUID uuid) {
        scores.remove(uuid);
    }

    // --- Internals ---

    private void tick() {
        if (!active) return;
        long remaining = secondsRemaining();
        if (remaining <= 0) {
            stop();
            return;
        }
        updateBar();
    }

    private void updateBar() {
        if (bossBar == null) return;
        long total = Math.max(1, Duration.between(Instant.now(), endsAt).abs().getSeconds());
        long remaining = secondsRemaining();
        float prog = Math.max(0f, Math.min(1f, remaining / (float) (total)));
        bossBar.progress(prog);

        // Leader string
        var leader = topList(1);
        String lead = leader.isEmpty() ? "<gray>No entries yet</gray>" : formatEntry(leader.get(0));
        bossBar.name(mm.deserialize("<gradient:#00e5ff:#0077ff><b>" + typeName() + "</b></gradient> <gray>·</gray> " + lead + " <gray>·</gray> <yellow>" + formatDuration(remaining) + "</yellow>"));
    }

    private void autoStartTick() {
        if (!autoEnabled || active) return;
        // start when current minute divisible by interval
        java.util.Calendar c = java.util.Calendar.getInstance();
        int minute = c.get(java.util.Calendar.MINUTE);
        if (minute % Math.max(1, autoIntervalMinutes) != 0) return;

        // choose a random enabled type
        TournamentType chosen = enabledTypes.get(new Random().nextInt(enabledTypes.size()));
        start(chosen, defaultDurationSec);
    }

    private void showBossbarAll() {
        if (bossBar == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) p.showBossBar(bossBar);
    }

    private void hideBossbarAll() {
        if (bossBar == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(bossBar);
        bossBar = null;
    }

    private String typeName() {
        return switch (type) {
            case MOST_FISH -> "Most Fish";
            case HIGHEST_VALUE -> "Highest Value";
            case RARITY_POINTS -> "Rarity Points";
        };
    }

    private Comparator<Map.Entry<UUID, Score>> comparatorFor(TournamentType t) {
        return switch (t) {
            case MOST_FISH -> Comparator.comparingInt(e -> e.getValue().count);
            case HIGHEST_VALUE -> Comparator.comparingDouble(e -> e.getValue().bestValue);
            case RARITY_POINTS -> Comparator.comparingInt(e -> e.getValue().points);
        };
    }

    private String formatEntry(Map.Entry<UUID, Score> e) {
        String name = Optional.ofNullable(Bukkit.getPlayer(e.getKey())).map(Player::getName).orElse("Player");
        return switch (type) {
            case MOST_FISH -> "<yellow>" + name + "</yellow> <gray>(" + e.getValue().count + ")";
            case HIGHEST_VALUE -> "<yellow>" + name + "</yellow> <gray>(" + String.format(java.util.Locale.US, "%.2f", e.getValue().bestValue) + ")";
            case RARITY_POINTS -> "<yellow>" + name + "</yellow> <gray>(" + e.getValue().points + " pts)";
        };
    }

    private void announceWinners(List<Map.Entry<UUID, Score>> top) {
        broadcast("<gradient:#00e5ff:#0077ff><b>" + typeName() + " finished!</b></gradient>");
        if (top.isEmpty()) {
            broadcast("<gray>No participants.</gray>");
            return;
        }
        int place = 1;
        for (var e : top) {
            broadcast(switch (place) {
                case 1 -> "<gold>1st:</gold> " + formatEntry(e);
                case 2 -> "<yellow>2nd:</yellow> " + formatEntry(e);
                default -> "<white>3rd:</white> " + formatEntry(e);
            });
            place++;
        }
    }

    private void giveRewards(List<Map.Entry<UUID, Score>> top) {
        if (top.size() >= 1) pay(top.get(0).getKey(), reward1Money, reward1Entropy);
        if (top.size() >= 2) pay(top.get(1).getKey(), reward2Money, reward2Entropy);
        if (top.size() >= 3) pay(top.get(2).getKey(), reward3Money, reward3Entropy);
    }

    private void pay(UUID uuid, double money, int entropy) {
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) return;
        if (money > 0) plugin.eco().deposit(p, money);
        if (entropy > 0) plugin.entropy().add(p, entropy);
        Msg.raw(p, "<green>Tournament reward:</green> <yellow>+" + money + "</yellow> money, <yellow>+" + entropy + "</yellow> entropy.");
    }

    private void broadcast(String mini) {
        Component c = mm.deserialize(mini);
        Bukkit.getServer().sendMessage(c);
    }

    private static String formatDuration(long seconds) {
        long m = seconds / 60;
        long s = seconds % 60;
        if (m <= 0) return s + "s";
        return m + "m " + s + "s";
    }

    // Score struct
    public static class Score {
        public int count = 0;
        public double bestValue = 0.0;
        public int points = 0;
    }
}
