package com.chunksmith.piscary.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record Delivery(
        String id,
        String nameMini,                 // MiniMessage string
        Map<String, Integer> objectives, // fishId -> amount
        double rewardMoney,
        int rewardEntropy,
        double weight
) {
    public Map<String,Integer> copyObjectives() {
        return new LinkedHashMap<>(objectives);
    }
}
