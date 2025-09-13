package com.chunksmith.piscary.model;

public enum TournamentType {
    MOST_FISH,        // Highest number of catches
    HIGHEST_VALUE,    // Highest single-fish sell value
    RARITY_POINTS;    // Sum of points per rarity (configurable)

    public static TournamentType safe(String s, TournamentType def) {
        if (s == null) return def;
        try { return TournamentType.valueOf(s.toUpperCase()); } catch (Exception e) { return def; }
    }
}
