package com.chunksmith.piscary.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class Text {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    public static Component mm(String input) { return MM.deserialize(input == null ? "" : input); }
}
