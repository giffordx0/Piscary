package com.chunksmith.piscary.services;

import com.chunksmith.piscary.Piscary;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class CrabsService {
    private final Piscary plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final NamespacedKey crabTag;
    private final NamespacedKey crabKillKey;

    private boolean enabled;
    private int windowCatches;
    private double maxMoveDistance;
    private float maxRotate;
    private double maxIntervalJitter;
    private double chancePerCatch;
    private int minCrabs, maxCrabs;
    private int rewardEntropy;
    private double crabHealth;
    private double crabDamage;
    private double crabSpeed;

    private static class State {
        final Deque<Long> catchTimes = new ArrayDeque<>();
        Location firstLoc = null;
        float firstYaw, firstPitch;
        long lastMoveAt = 0L;
        double totalRot = 0.0;
    }
    private final Map<UUID, State> states = new HashMap<>();
    private final Random rng = new Random();

    public CrabsService(Piscary plugin) {
        this.plugin = plugin;
        this.crabTag = new NamespacedKey(plugin, "piscary_crab");
        this.crabKillKey = new NamespacedKey(plugin, "piscary_crabs_killed");
        reload();
    }

    public void reload() {
        var y = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File(plugin.getDataFolder(), "crabs.yml"));
        this.enabled = y.getBoolean("enabled", true);
        this.windowCatches = Math.max(3, y.getInt("window-catches", 4));
        this.maxMoveDistance = Math.max(0.1, y.getDouble("max-move-distance", 0.6));
        this.maxRotate = Math.max(1f, (float) y.getDouble("max-rotate-deg", 10.0));
        this.maxIntervalJitter = Math.max(0.05, y.getDouble("max-interval-jitter", 1.2));
        this.chancePerCatch = Math.min(1.0, Math.max(0.0, y.getDouble("chance-per-catch", 0.65)));
        this.minCrabs = Math.max(1, y.getInt("spawn.min", 1));
        this.maxCrabs = Math.max(minCrabs, y.getInt("spawn.max", 2));
        this.rewardEntropy = Math.max(0, y.getInt("reward.entropy", 5));
        this.crabHealth = Math.max(1.0, y.getDouble("crab.health", 12.0));
        this.crabDamage = Math.max(0.0, y.getDouble("crab.damage", 2.0));
        this.crabSpeed = Math.max(0.05, y.getDouble("crab.speed", 0.30));
    }

    public void shutdown() { states.clear(); }

    public void onMove(Player p, Location from, Location to) {
        if (!enabled) return;
        if (from == null || to == null) return;
        State st = states.computeIfAbsent(p.getUniqueId(), k -> new State());
        st.lastMoveAt = System.currentTimeMillis();
        float dyaw = Math.abs(to.getYaw() - from.getYaw());
        float dpitch = Math.abs(to.getPitch() - from.getPitch());
        st.totalRot += dyaw + dpitch;
    }

    public void onCast(Player p, PlayerFishEvent e) {
        if (!enabled) return;
        State st = states.computeIfAbsent(p.getUniqueId(), k -> new State());
        st.firstLoc = p.getLocation().clone();
        st.firstYaw = p.getLocation().getYaw();
        st.firstPitch = p.getLocation().getPitch();
        st.totalRot = 0.0;
    }

    public void onCatch(Player p) {
        if (!enabled) return;
        State st = states.computeIfAbsent(p.getUniqueId(), k -> new State());
        st.catchTimes.addLast(System.currentTimeMillis());
        while (st.catchTimes.size() > windowCatches) st.catchTimes.removeFirst();
        if (st.firstLoc == null) st.firstLoc = p.getLocation().clone();

        if (st.catchTimes.size() >= windowCatches && isSuspicious(p, st)) {
            if (rng.nextDouble() < chancePerCatch) spawnCrabsNear(p);
        }
    }

    private boolean isSuspicious(Player p, State st) {
        double moved = distance2D(st.firstLoc, p.getLocation());
        boolean idleMove = moved <= maxMoveDistance;
        boolean idleLook = st.totalRot <= maxRotate;
        if (st.catchTimes.size() < 2) return false;
        double stdev = intervalStdDev(st.catchTimes);
        boolean robotic = stdev <= maxIntervalJitter;
        return idleMove && idleLook && robotic;
    }

    private double distance2D(Location a, Location b) {
        if (a == null || b == null || a.getWorld() != b.getWorld()) return Double.MAX_VALUE;
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx*dx + dz*dz);
    }

    private double intervalStdDev(Deque<Long> times) {
        if (times.size() < 2) return 999;
        long[] arr = times.stream().mapToLong(Long::longValue).toArray();
        double[] iv = new double[arr.length - 1];
        for (int i = 1; i < arr.length; i++) iv[i-1] = (arr[i] - arr[i-1]) / 1000.0;
        double mean = 0;
        for (double v : iv) mean += v;
        mean /= iv.length;
        double var = 0;
        for (double v : iv) var += (v - mean)*(v - mean);
        var /= iv.length;
        return Math.sqrt(var);
    }

    private void spawnCrabsNear(Player p) {
        int count = rng.nextInt(maxCrabs - minCrabs + 1) + minCrabs;
        Location base = p.getLocation();

        for (int i = 0; i < count; i++) {
            Location at = base.clone().add(rngOffset(), 0, rngOffset());
            at.setY(Math.max(Math.min(base.getWorld().getHighestBlockYAt(at), base.getBlockY() + 2), base.getBlockY() - 1));

            Silverfish crab = (Silverfish) p.getWorld().spawnEntity(at, EntityType.SILVERFISH, false);
            crab.customName(mm.deserialize("<gold>Crab</gold>"));
            crab.setCustomNameVisible(true);
            crab.getPersistentDataContainer().set(crabTag, PersistentDataType.BYTE, (byte) 1);

            // Use potion effects to simulate tougher/faster/stronger crabs (1.21-safe)
            if (crabHealth > 12.0) {
                crab.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 60, 0, false, false, false));
            }
            if (crabDamage > 2.0) {
                crab.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 60, 0, false, false, false));
            }
            if (crabSpeed > 0.30) {
                crab.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60, 0, false, false, false));
            }

            crab.setTarget(p);
        }

        p.sendMessage(mm.deserialize("<yellow>Crabs scuttle out of the sand!</yellow> <gray>(Anti-AFK)</gray>"));
        p.playSound(p.getLocation(), Sound.ENTITY_SILVERFISH_AMBIENT, 1f, 1f);
    }

    private double rngOffset() { return (rng.nextDouble() * 6.0) - 3.0; }

    public boolean isCrab(org.bukkit.entity.Entity e) {
        return e != null && e.getPersistentDataContainer().has(crabTag, PersistentDataType.BYTE);
    }

    public void rewardForCrab(Player killer) {
        if (rewardEntropy > 0) plugin.entropy().add(killer, rewardEntropy);
        int killed = killer.getPersistentDataContainer().getOrDefault(crabKillKey, PersistentDataType.INTEGER, 0);
        killer.getPersistentDataContainer().set(crabKillKey, PersistentDataType.INTEGER, killed + 1);
        killer.sendMessage(mm.deserialize("<green>+<yellow>" + rewardEntropy + "</yellow> entropy for slaying a Crab.</green>"));
        killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
    }

    public int getCrabsKilled(Player p) {
        return p.getPersistentDataContainer().getOrDefault(crabKillKey, PersistentDataType.INTEGER, 0);
    }
}
