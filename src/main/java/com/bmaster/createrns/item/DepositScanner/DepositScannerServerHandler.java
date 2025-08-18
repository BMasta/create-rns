package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.capability.orechunkdata.OreChunkClassifier;
import com.bmaster.createrns.capability.orechunkdata.OreChunkData;
import com.bmaster.createrns.util.Utils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DepositScannerServerHandler {
    public static final int MIN_PING_INTERVAL = 3;
    public static final int MAX_PING_INTERVAL = 60;

    private static final int MAX_CHESSBOARD_CHUNK_DISTANCE = 100;
    private static final int MAX_BLOCK_DISTANCE = MAX_CHESSBOARD_CHUNK_DISTANCE * 16;

    private static final Cache<UUID, Tuple<Item, Long>> oreChunkCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(20, TimeUnit.MINUTES)
            .build();

    public static void processScanRequest(ServerPlayer sp, Item ore, boolean recompute) {
        if (!(sp.level() instanceof ServerLevel sl)) return;

        Optional<ChunkPos> posOpt = getOreChunk(sp, ore, recompute);

        DepositScannerItemRenderer.AntennaStatus status = DepositScannerItemRenderer.AntennaStatus.INACTIVE;
        int interval = 0;
        boolean playerInChunk = false;

        if (posOpt.isPresent()) {
            var chunkPos = posOpt.get();
            var playerPos = sp.blockPosition();
            playerInChunk = Utils.isPosInChunk(playerPos, chunkPos);

            if (playerInChunk) {
                status = DepositScannerItemRenderer.AntennaStatus.BOTH_ACTIVE;
            } else {
                var chunkCenterPos = chunkPos.getMiddleBlockPosition(playerPos.getY());
                var distance = Math.min(MAX_BLOCK_DISTANCE, Math.sqrt(playerPos.distSqr(chunkCenterPos)));

                float curYaw = sp.getYRot();
                float targetYaw = getYaw(sp.blockPosition(), chunkCenterPos);
                float diff = Mth.wrapDegrees(targetYaw - curYaw);

                interval = MIN_PING_INTERVAL + (int) ((MAX_PING_INTERVAL - MIN_PING_INTERVAL) *
                        Utils.easeOut((float) distance / MAX_BLOCK_DISTANCE, 2));

                if (Math.abs(diff) < 30) {
                    status = DepositScannerItemRenderer.AntennaStatus.BOTH_ACTIVE;
                } else if (diff <= 0) {
                    status = DepositScannerItemRenderer.AntennaStatus.LEFT_ACTIVE;
                } else {
                    status = DepositScannerItemRenderer.AntennaStatus.RIGHT_ACTIVE;
                }
            }
        }
        DepositScannerS2CPacket.send(sp, status, interval, playerInChunk);
    }

    private static Optional<ChunkPos> getOreChunk(ServerPlayer sp, Item ore, boolean recompute) {
        var uuid = sp.getUUID();

        // Look up computed ore chunks in cache
        if (!recompute) {
            var hit = oreChunkCache.getIfPresent(uuid);
            if (hit != null && hit.getA() == ore) {
                var pos = new ChunkPos(hit.getB());
                CreateRNS.LOGGER.info("[Cache Hit] Ore chunk of type {} at {}, {}", ore, pos.getBlockX(8),
                        pos.getBlockZ(8));
                return Optional.of(pos);
            }
            return Optional.empty();
        }

        // Or compute one
        var l = sp.level();
        if (!(l instanceof ServerLevel sl)) return Optional.empty();
        var posOpt = OreChunkClassifier.INSTANCE.getNearestOreChunk(sp.chunkPosition(), sl.getSeed(),
                ore, MAX_CHESSBOARD_CHUNK_DISTANCE);
        posOpt.ifPresent(pos -> {
            oreChunkCache.put(uuid, new Tuple<>(ore, pos.toLong()));
        });
        return posOpt;
    }

    private static float getYaw(BlockPos from, BlockPos to) {
        Vec3 a = Vec3.atCenterOf(from);
        Vec3 b = Vec3.atCenterOf(to);
        Vec3 v = b.subtract(a);

        return (float) Mth.wrapDegrees(
                Math.toDegrees(Mth.atan2(-v.x, v.z))
        );
    }
}
