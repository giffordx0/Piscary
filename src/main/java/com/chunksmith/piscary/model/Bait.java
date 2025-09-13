package com.chunksmith.piscary.model;

import net.kyori.adventure.text.Component;

import java.util.EnumMap;
import java.util.Map;

public record Bait(
        String id,
        Component name,
        double price,
        int durationSeconds,
        Map<Rarity, Double> rarityMultipliers
) {}
