package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.deposit.spec.DepositSpecLookup;
import com.bmaster.createrns.deposit.capability.IDepositIndex;
import com.bmaster.createrns.item.DepositScanner.DepositScannerClientHandler.AntennaStatus;
import com.bmaster.createrns.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class DepositScannerServerHandler {
    public static final int MIN_PING_INTERVAL = 3;
    public static final int MAX_PING_INTERVAL = 60;
    public static final float FOUND_DISTANCE = 10f;

    private static final int SEARCH_RADIUS_CHUNKS = 100;
    private static final int MAX_BLOCK_DISTANCE = SEARCH_RADIUS_CHUNKS * 16;

    public static void processScanRequest(ServerPlayer sp, Item yield, boolean recompute) {
        if (!(sp.level() instanceof ServerLevel sl)) return;
        var depIdxOpt = IDepositIndex.fromLevel(sl).resolve();
        if (depIdxOpt.isEmpty()) {
            CreateRNS.LOGGER.error("Deposit index is not present on level {}", sl.dimension());
            DepositScannerS2CPacket.send(sp, AntennaStatus.INACTIVE, 0, null);
            return;
        }
        var depIdx = depIdxOpt.get();

        var structure = DepositSpecLookup.getSpec(sl.registryAccess(), yield).structure();
        var depPos = depIdx.getNearest(structure.unwrapKey().orElseThrow(), sp, SEARCH_RADIUS_CHUNKS, !recompute);
        var state = getScannerState(sp, depPos);

        if (state.foundDepositCenter != null) depIdx.markAsFound(state.foundDepositCenter);

        DepositScannerS2CPacket.send(sp, state.antennaStatus, state.interval, state.foundDepositCenter);
    }

    private static ScannerState getScannerState(ServerPlayer sp, BlockPos targetPos) {
        var playerPos = sp.blockPosition();
        var distance = Math.sqrt(playerPos.distSqr(targetPos));
        if (distance > MAX_BLOCK_DISTANCE) {
            return new ScannerState(AntennaStatus.INACTIVE, 0, null);
        }

        AntennaStatus status;
        int interval;
        boolean found;

        // Calculate antenna status based player's look angle relative to target
        float curYaw = sp.getYRot();
        float targetYaw = getYaw(playerPos, targetPos);
        float diff = Mth.wrapDegrees(targetYaw - curYaw);
        if (Math.abs(diff) < 30) {
            status = AntennaStatus.BOTH_ACTIVE;
        } else if (diff <= 0) {
            status = AntennaStatus.LEFT_ACTIVE;
        } else {
            status = AntennaStatus.RIGHT_ACTIVE;
        }

        // Calculate ping interval based on distance to target and if distance is close enough to consider deposit found
        interval = MIN_PING_INTERVAL + (int) ((MAX_PING_INTERVAL - MIN_PING_INTERVAL) *
                Utils.easeOut((float) distance / MAX_BLOCK_DISTANCE, 2));
        found = (distance <= FOUND_DISTANCE);

        return new ScannerState(status, interval, found ? targetPos : null);
    }

    private static float getYaw(BlockPos from, BlockPos to) {
        Vec3 a = Vec3.atCenterOf(from);
        Vec3 b = Vec3.atCenterOf(to);
        Vec3 v = b.subtract(a);

        return (float) Mth.wrapDegrees(
                Math.toDegrees(Mth.atan2(-v.x, v.z))
        );
    }

    private record ScannerState(AntennaStatus antennaStatus, int interval, @Nullable BlockPos foundDepositCenter) {}
}
