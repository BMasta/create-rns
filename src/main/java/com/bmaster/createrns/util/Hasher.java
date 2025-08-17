package com.bmaster.createrns.util;

import com.bmaster.createrns.CreateRNS;
import com.google.common.hash.Hashing;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.world.level.ChunkPos;

public class Hasher {
    private long hash = 0;
    private int idx = 1;

    public Hasher(long seed1, long seed2) {
        reset(seed1, seed2);
    }

    public void reset(long seed1, long seed2) {
        idx = 1;
        hash = Hashing.murmur3_128().newHasher()
                .putLong(seed1)
                .putLong(seed2)
                .hash().asLong();
    }

    public float roll() {
        // Leave only 24 left bits, then divide by 2^24 to get a range [0, 1)
        float roll = (float) ((HashCommon.mix(hash * idx++) & 0xFFFFFFL) * (1.0 / (1 << 24)));
        return roll;
    }
}
