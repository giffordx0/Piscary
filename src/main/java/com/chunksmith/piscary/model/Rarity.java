package com.chunksmith.piscary.model;

public enum Rarity {
    COMMON, UNCOMMON, RARE, EPIC, LEGENDARY;

    public double defaultXpMultiplier() {
        return switch (this) {
            case COMMON -> 1.0;
            case UNCOMMON -> 1.2;
            case RARE -> 1.5;
            case EPIC -> 2.0;
            case LEGENDARY -> 3.0;
        };
    }
}
