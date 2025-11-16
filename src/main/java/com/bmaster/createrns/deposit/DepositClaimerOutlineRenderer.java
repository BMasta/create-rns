package com.bmaster.createrns.deposit;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.CreateRNS;
import com.simibubi.create.AllItems;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.ParametersAreNonnullByDefault;

public class DepositClaimerOutlineRenderer {
    private static final String OUTLINER_SLOT = "%s:miningAreaOutline".formatted(CreateRNS.MOD_ID);
    private static final int MAX_TTL = 30;
    private static final int OUTLINE_MAX_DIST = 64;

    private static boolean outlineActive = false;
    private static boolean outlineChanged = true;
    private static final ObjectOpenHashSet<BlockPos> selectedCluster = new ObjectOpenHashSet<>();
    private static int ttl = 0;

    @ParametersAreNonnullByDefault
    public static void clearAndAddNearbyMiningBEs() {
        if (!outlineActive) return;
        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        var l = p.level();

        selectedCluster.clear();
        outlineChanged = true;
        DepositClaimerInstanceHolder.getInstancesWithinManhattanDistance(l, p.blockPosition(), OUTLINE_MAX_DIST)
                .forEach(DepositClaimerOutlineRenderer::addClaimer);
    }

    public static void addClaimer(IDepositBlockClaimer claimer) {
        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        var l = p.level();
        if (!outlineActive) return;
        if (Math.sqrt(claimer.getAnchor().distManhattan(p.blockPosition())) > OUTLINE_MAX_DIST) return;

        if (selectedCluster.addAll(claimer.getClaimedDepositBlocks())) outlineChanged = true;
    }

    public static void removeClaimer(IDepositBlockClaimer claimer) {
        if (!outlineActive) return;
        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        var l = p.level();
        if (Math.sqrt(claimer.getAnchor().distManhattan(p.blockPosition())) > OUTLINE_MAX_DIST) return;

        if (selectedCluster.removeAll(claimer.getClaimedDepositBlocks())) outlineChanged = true;
    }

    public static void clearOutline() {
        outlineActive = false;
    }

    public static void tick() {
        activateOutlineIfNeeded();
        if (!outlineActive) return;

        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null) return;

        if (holdingCorrectItem(p)) ttl = MAX_TTL;
        else ttl--;

        if (ttl <= 0) {
            outlineActive = false;
            return;
        }

        if (!selectedCluster.isEmpty()) {
            if (outlineChanged) {
                outlineChanged = false;
                Outliner.getInstance().showCluster(OUTLINER_SLOT, selectedCluster);
            } else {
                Outliner.getInstance().keep(OUTLINER_SLOT);
            }
        }
    }

    private static boolean holdingCorrectItem(Player p) {
        var mainHandItem = p.getMainHandItem();
        return AllItems.WRENCH.isIn(mainHandItem) ||
                RNSContent.MINER_MK1_BLOCK.isIn(mainHandItem) ||
                RNSContent.MINER_MK2_BLOCK.isIn(mainHandItem);
    }

    private static void activateOutlineIfNeeded() {
        if (outlineActive) return;

        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null) return;
        var l = p.level();

        boolean lookingAtMiningBE = mc.hitResult instanceof BlockHitResult ray &&
                l.getBlockEntity(ray.getBlockPos()) instanceof IDepositBlockClaimer;

        if (holdingCorrectItem(p) && lookingAtMiningBE) {
            ttl = MAX_TTL;
            outlineActive = true;
            clearAndAddNearbyMiningBEs();
        }
    }
}
