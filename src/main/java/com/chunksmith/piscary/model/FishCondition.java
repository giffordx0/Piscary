package com.chunksmith.piscary.model;

import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.util.Set;

public record FishCondition(
        Set<Biome> biomes,
        Time time,
        Weather weather,
        int yMin,
        int yMax
) {
    public enum Time { DAY, NIGHT, ANY }
    public enum Weather { CLEAR, RAIN, ANY }

    public boolean matches(Player p) {
        var loc = p.getLocation();
        var world = loc.getWorld();

        if (biomes != null && !biomes.isEmpty() && !biomes.contains(world.getBiome(loc))) return false;

        long t = world.getTime() % 24000;
        boolean isDay = t < 12300 || t > 23850;
        if (time == Time.DAY && !isDay) return false;
        if (time == Time.NIGHT && isDay) return false;

        boolean raining = world.hasStorm();
        if (weather == Weather.CLEAR && raining) return false;
        if (weather == Weather.RAIN && !raining) return false;

        int y = loc.getBlockY();
        return y >= yMin && y <= yMax;
    }
}
