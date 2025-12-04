package com.bmaster.createrns.util;

import com.bmaster.createrns.CreateRNS;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;

import java.text.DecimalFormat;
import java.util.function.Function;

public class Utils {
    protected static DecimalFormat df = new DecimalFormat("0.##");

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

    public static Codec<Long> longRangeCodec(long minInclusive, long maxInclusive) {
        final Function<Long, DataResult<Long>> checker = value -> {
            if (value.compareTo(minInclusive) >= 0 && value.compareTo(maxInclusive) <= 0) {
                return DataResult.success(value);
            }
            return DataResult.error(() -> "Value " + value + " outside of range [" + minInclusive + ":" + maxInclusive + "]");
        };
        return Codec.LONG.flatXmap(checker, checker);
    }

    /// Flips non-zero vector components to zero, and zero components to either -1 or 1
    public static Vec3i normalVecFlip(Direction dir, boolean positive) {
        var n = dir.getNormal();
        var flipVal = (positive ? 1 : -1);
        Function<Integer, Integer> flip = v -> v == 0 ? flipVal : 0;
        return new Vec3i(flip.apply(n.getX()), flip.apply(n.getY()), flip.apply(n.getZ()));
    }

    public static int dot(Vec3i a, Vec3i b) {
        return a.getX() * b.getX() + a.getY() * b.getY() + a.getZ() * b.getZ();
    }

    public static long murmur64(long h) {
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }

    public static LangBuilder fancyArg(Object arg) {
        return CreateRNS.lang().text("" + arg);
    }

    public static LangBuilder fancyChanceArg(float c) {
        return fancyArg(df.format(Math.min(c * 100, 100)) + "%");
    }
}
