package com.bmaster.createrns.mining;

import com.bmaster.createrns.CreateRNS;
import net.minecraft.network.chat.Component;

public enum MiningLevel {
    BASIC(1, "basic"),
    ADVANCED(2, "advanced");

    private final int level;

    MiningLevel(int level, String id) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
