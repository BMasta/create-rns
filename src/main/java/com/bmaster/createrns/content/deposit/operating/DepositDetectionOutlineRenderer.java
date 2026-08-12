package com.bmaster.createrns.content.deposit.operating;

import com.bmaster.createrns.content.deposit.claiming.DepositClaimerInstanceHolder;
import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapter.OperatingSublevel;
import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapterHolder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class DepositDetectionOutlineRenderer {
    private static final int OUTLINE_MAX_DIST = 64;
    private static final int DETECTION_AREA_COLOR = 0x4dc3ff;
    private static final float DETECTION_AREA_LINE_WIDTH = 1 / 32f;

    private static final ObjectOpenHashSet<DetectionAreaSlot> ACTIVE_SLOTS = new ObjectOpenHashSet<>();
    private static boolean enabled = false;

    private DepositDetectionOutlineRenderer() {}

    public static void setEnabled(boolean enabled) {
        DepositDetectionOutlineRenderer.enabled = enabled;
        if (!enabled) clearOutlines();
    }

    public static void reset() {
        setEnabled(false);
    }

    public static void tick() {
        if (!enabled) return;

        var player = Minecraft.getInstance().player;
        if (player == null) {
            clearOutlines();
            return;
        }

        var level = player.level();
        var adapter = OperatingSublevelAdapterHolder.getAdapter();
        var currentSlots = new ObjectOpenHashSet<DetectionAreaSlot>();
        for (var claimer : DepositClaimerInstanceHolder.getInstancesWithinManhattanDistance(
                level, player.blockPosition(), OUTLINE_MAX_DIST)) {
            if (!(claimer instanceof HybridOperatingBehaviour operatingBehaviour)) continue;

            var anchor = operatingBehaviour.getOperatingAnchor();
            var direction = operatingBehaviour.getOperatingDirection();
            var dimensions = operatingBehaviour.getCrossSublevelOperatingDimensions();
            if (anchor == null || dimensions == null) continue;
            var area = IDepositBlockOperator.createCrossSublevelOperatingArea(anchor, direction, dimensions);

            var sublevel = adapter.getOperatingSublevel(level, anchor);
            var slot = new DetectionAreaSlot(operatingBehaviour.getBlockPos(), sublevel);
            currentSlots.add(slot);
            Outliner.getInstance()
                    .showAABB(slot, area)
                    .colored(DETECTION_AREA_COLOR)
                    .lineWidth(DETECTION_AREA_LINE_WIDTH)
                    .disableCull();
        }

        for (var slot : ACTIVE_SLOTS) {
            if (!currentSlots.contains(slot)) Outliner.getInstance().remove(slot);
        }
        ACTIVE_SLOTS.clear();
        ACTIVE_SLOTS.addAll(currentSlots);
    }

    private static void clearOutlines() {
        for (var slot : ACTIVE_SLOTS) Outliner.getInstance().remove(slot);
        ACTIVE_SLOTS.clear();
    }

    private record DetectionAreaSlot(BlockPos ownerPos, OperatingSublevel space) {}
}
