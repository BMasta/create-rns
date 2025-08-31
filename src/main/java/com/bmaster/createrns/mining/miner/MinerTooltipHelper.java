package com.bmaster.createrns.mining.miner;

import com.bmaster.createrns.CreateRNS;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public class MinerTooltipHelper {
    public static final int PROGRESS_SEGMENT_COUNT = 10;
    private static MinerBlockEntity lastInspectedMiner = null;

//    public static void tick() {
//        var l = Minecraft.getInstance().level;
//        if (l != null && GoggleOverlayRenderer.hoverTicks > 0 &&
//                (l.getBlockEntity(GoggleOverlayRenderer.lastHovered) instanceof MinerBlockEntity minerBE)) {
//            // Request server to send progress info to its client counterpart when tooltip for it is being rendered
//            if (lastInspectedMiner == null || minerBE == lastInspectedMiner) {
//                CreateRNS.LOGGER.info("Keep render at {}, {}", GoggleOverlayRenderer.lastHovered.getX(),
//                        GoggleOverlayRenderer.lastHovered.getZ());
//                // Still rendering tooltip for the same miner (or first miner for this render)
//                MinerInfoC2SPacket.send(GoggleOverlayRenderer.lastHovered);
//            } else {
//                // Still rendering a miner tooltip, but for a different miner
////                CreateRNS.LOGGER.info("Render different at {}, {}, invalidate {}, {}", GoggleOverlayRenderer.lastHovered.getX(),
////                        GoggleOverlayRenderer.lastHovered.getZ(), lastInspectedMiner.getBlockPos().getX(), lastInspectedMiner.getBlockPos().getZ());
////                lastInspectedMiner.invalidateProgressInfo();
//                MinerInfoC2SPacket.forceSend(GoggleOverlayRenderer.lastHovered);
//            }
//            lastInspectedMiner = minerBE;
//        } else if (lastInspectedMiner != null) {
//            CreateRNS.LOGGER.info("Render end, invalidate {}, {}", lastInspectedMiner.getBlockPos().getX(), lastInspectedMiner.getBlockPos().getZ());
//            // Stopped rendering a miner tooltip
//            lastInspectedMiner.invalidateProgressInfo();
//            lastInspectedMiner = null;
//        }
//    }

    public static boolean segmentsChanged(float progressBefore, float progressAfter) {
        var segBefore = Math.round(progressBefore * PROGRESS_SEGMENT_COUNT);
        var segAfter = Math.round(progressAfter * PROGRESS_SEGMENT_COUNT);
        if (segBefore != segAfter) CreateRNS.LOGGER.info("Segments changed {} -> {}", segBefore, segAfter);
        return segBefore != segAfter;
    }
}
