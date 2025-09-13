package com.chunksmith.piscary.model;

import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

public record TotemType(
        String id,
        String nameMini,                    // MiniMessage
        Material itemMaterial,
        double priceMoney,
        int priceEntropy,
        int radius,
        Map<Rarity, Double> rarityMultipliers, // per-rarity catch weight multiplier
        double xpMultiplier,                // e.g. 1.10 => +10% XP
        double sellMultiplier,              // e.g. 1.05 => +5% sell price
        double entropyBonus                 // e.g. 0.10 => +10% gut entropy
) {
    public static Map<Rarity, Double> emptyRarityMult() {
        return new EnumMap<>(Rarity.class);
    }
}
