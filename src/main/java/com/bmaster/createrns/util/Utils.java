package com.bmaster.createrns.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public class Utils {
    public static boolean isPosInChunk(BlockPos pos, ChunkPos chunkPos) {
        return (pos.getX() >> 4) == chunkPos.x && (pos.getZ() >> 4) == chunkPos.z;
    }

    public static float easeOut(float val, int deg) {
        return 1 - (float) Math.pow(1 - val, deg);
    }
}
