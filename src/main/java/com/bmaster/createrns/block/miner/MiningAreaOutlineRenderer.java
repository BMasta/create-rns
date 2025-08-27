package com.bmaster.createrns.block.miner;

import com.bmaster.createrns.AllContent;
import com.bmaster.createrns.CreateRNS;
import com.simibubi.create.AllItems;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public class MiningAreaOutlineRenderer {
    private static final String OUTLINER_SLOT = "%s:minerOutline".formatted(CreateRNS.MOD_ID);
    private static final int MAX_TTL = 30;
    private static final int OUTLINE_MAX_DIST = 64;

    private static boolean outlineActive = false;
    private static boolean outlineChanged = true;
    private static final ObjectOpenHashSet<BlockPos> selectedCluster = new ObjectOpenHashSet<>();
    private static int ttl = 0;

    public static void clearAndAddNearbyMiners() {
        if (!outlineActive) return;
        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        var l = p.level();

        selectedCluster.clear();
        outlineChanged = true;
        MinerBlockEntityInstanceHolder.getInstancesWithinManhattanDistance(l, p.blockPosition(), OUTLINE_MAX_DIST)
                .forEach(MiningAreaOutlineRenderer::addMiner);
    }

    public static void addMiner(MinerBlockEntity be) {
        if (!outlineActive) return;
        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        if (Math.sqrt(be.getBlockPos().distManhattan(p.blockPosition())) > OUTLINE_MAX_DIST) return;

        if (selectedCluster.addAll(be.reservedDepositBlocks)) outlineChanged = true;
    }

    public static void removeMiner(MinerBlockEntity be) {
        if (!outlineActive) return;
        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        if (Math.sqrt(be.getBlockPos().distManhattan(p.blockPosition())) > OUTLINE_MAX_DIST) return;

        if (selectedCluster.removeAll(be.reservedDepositBlocks)) outlineChanged = true;
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
        var l = p.level();

        var mainHandItem = p.getMainHandItem();
        boolean holdingCorrectItem = AllItems.WRENCH.isIn(mainHandItem) || AllContent.MINER_BLOCK.isIn(mainHandItem);

        if (holdingCorrectItem) ttl = MAX_TTL;
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

    private static void activateOutlineIfNeeded() {
        if (outlineActive) return;

        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null) return;
        var l = p.level();

        var mainHandItem = p.getMainHandItem();
        boolean holdingCorrectItem = AllItems.WRENCH.isIn(mainHandItem) || AllContent.MINER_BLOCK.isIn(mainHandItem);
        boolean lookingAtMiner = mc.hitResult instanceof BlockHitResult ray && l.getBlockEntity(ray.getBlockPos()) instanceof MinerBlockEntity;

        if (holdingCorrectItem && lookingAtMiner) {
            ttl = MAX_TTL;
            outlineActive = true;
            clearAndAddNearbyMiners();
        }
    }
}
