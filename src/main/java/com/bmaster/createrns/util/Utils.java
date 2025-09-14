package com.bmaster.createrns.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public class Utils {
    public static boolean isPosInChunk(BlockPos pos, ChunkPos chunkPos) {
        return (pos.getX() >> 4) == chunkPos.x && (pos.getZ() >> 4) == chunkPos.z;
    }

    public static float easeInOut(float val, float deg) {
        return (float) ((val < 0.5) ? (Math.pow(2, deg - 1) * Math.pow(val, deg)) : (1 - Math.pow(-2 * val + 2, deg) / 2));
    }

    public static float easeInOutBack(float val) {
        float c1 = 1.70158f;
        float c2 = c1 * 1.525f;

        return val < 0.5
                ? (float) (Math.pow(2 * val, 2) * ((c2 + 1) * 2 * val - c2)) / 2
                : (float) (Math.pow(2 * val - 2, 2) * ((c2 + 1) * (val * 2 - 2) + c2) + 2) / 2;
    }

    public static float easeOut(float val, float deg) {
        return 1 - (float) Math.pow(1 - val, deg);
    }
}
