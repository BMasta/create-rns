package com.bmaster.createrns.content.deposit.scanning;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.info.ServerDepositLocation;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerClientHandler.AntennaStatus;
import com.bmaster.createrns.content.deposit.spec.DepositSpecLookup;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.bmaster.createrns.util.Utils;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositScannerServerHandler {
    public enum RequestType {
        DISCOVER, TRACK
    }

    public static final int MIN_PING_INTERVAL = 3;
    public static final int MAX_PING_INTERVAL = 60;
    public static final float FOUND_DISTANCE = 5f;
    public static final int DEFAULT_SEARCH_RADIUS_CHUNKS = 300;

    public static void processScanRequest(ServerPlayer sp, Item icon, RequestType rt) {
        if (!(sp.level() instanceof ServerLevel sl)) return;

        var structKey = DepositSpecLookup.getStructureKey(sl.registryAccess(), icon);
        if (rt == RequestType.DISCOVER) {
            CreateRNS.LOGGER.trace("[Scanner discover] Player {} searching for {}", sp.getScoreboardName(),
                    structKey.location());
        }
        var nearest = switch (rt) {
            case DISCOVER -> ServerDepositLocation.getNearest(sp, structKey, false, ServerConfig.MAX_SCAN_DISTANCE.get(), false);
            case TRACK -> ServerDepositLocation.getNearest(sp, structKey, false, ServerConfig.MAX_SCAN_DISTANCE.get(), true);
        };

        var state = getScannerState(sp, (nearest != null) ? nearest.getLocation() : null);
        if (state.found) {
            assert nearest != null;
            nearest.setFound(sl, true);
        }
        PacketDistributor.sendToPlayer(sp, new DepositScannerS2CPayload(state.antennaStatus, state.interval, state.found, rt));
    }

    private static ScannerState getScannerState(ServerPlayer sp, @Nullable BlockPos targetPos) {
        if (targetPos == null) return new ScannerState(AntennaStatus.INACTIVE, MAX_PING_INTERVAL, false);
        var playerPos = sp.blockPosition();
        var maxBlockDistance = SectionPos.sectionToBlockCoord(ServerConfig.MAX_SCAN_DISTANCE.get());
        var distance = Math.min(maxBlockDistance, Math.sqrt(playerPos.distSqr(targetPos)));

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
                Utils.easeOut((float) distance / maxBlockDistance, 2));
        found = (distance <= FOUND_DISTANCE);

        return new ScannerState(status, interval, found);
    }

    private static float getYaw(BlockPos from, BlockPos to) {
        Vec3 a = Vec3.atCenterOf(from);
        Vec3 b = Vec3.atCenterOf(to);
        Vec3 v = b.subtract(a);

        return (float) Mth.wrapDegrees(
                Math.toDegrees(Mth.atan2(-v.x, v.z))
        );
    }

    private record ScannerState(AntennaStatus antennaStatus, int interval, boolean found) {}
}
