package com.bmaster.createrns.content.deposit.claiming;

import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer.ClaimerType;
import com.bmaster.createrns.content.deposit.operating.space.OperatingSpaceAdapter.OperatingSpace;
import com.bmaster.createrns.content.deposit.operating.space.OperatingSpaceAdapterHolder;
import com.simibubi.create.AllItems;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositClaimerOutlineRenderer {
    private static final int MAX_TTL = 120;
    private static final int OUTLINE_MAX_DIST = 64;

    private static final Object2ObjectOpenHashMap<OperatingSpace, ObjectOpenHashSet<BlockPos>> selectedClusters =
            new Object2ObjectOpenHashMap<>();
    private static boolean outlineActive = false;
    private static boolean outlineChanged = true;
    private static int ttl = 0;

    @ParametersAreNonnullByDefault
    public static void clearAndAddNearbyClaimers(ClaimerType type) {
        if (!outlineActive) return;
        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        var l = p.level();

        selectedClusters.clear();
        outlineChanged = true;
        DepositClaimerInstanceHolder.getInstancesWithinManhattanDistance(l, p.blockPosition(), OUTLINE_MAX_DIST, type)
                .forEach(DepositClaimerOutlineRenderer::addClaimer);
    }

    public static void addClaimer(IDepositBlockClaimer claimer) {
        if (!outlineActive) return;
        var p = Minecraft.getInstance().player;
        var level = claimer.getLevel();
        var anchor = claimer.getAnchor();
        var claimedBlocks = claimer.getClaimedDepositBlocks();
        if (p == null || level == null || anchor == null || claimedBlocks == null) return;
        var adapter = OperatingSpaceAdapterHolder.getAdapter();
        if (adapter.distManhattan(level, anchor, p.blockPosition()) > OUTLINE_MAX_DIST) return;

        var space = adapter.getOperatingSpace(level, anchor);
        var cluster = selectedClusters.computeIfAbsent(space, ignored -> new ObjectOpenHashSet<>());
        if (cluster.addAll(claimedBlocks)) outlineChanged = true;
    }

    public static void removeClaimer(IDepositBlockClaimer claimer) {
        if (!outlineActive) return;
        var claimedBlocks = claimer.getClaimedDepositBlocks();
        if (claimedBlocks == null) return;

        var iterator = selectedClusters.values().iterator();
        while (iterator.hasNext()) {
            var cluster = iterator.next();
            if (cluster.removeAll(claimedBlocks)) outlineChanged = true;
            if (cluster.isEmpty()) iterator.remove();
        }
    }

    public static void clearOutline() {
        outlineActive = false;
        selectedClusters.clear();
        outlineChanged = true;
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

        if (outlineChanged) {
            outlineChanged = false;
            selectedClusters.forEach((space, cluster) ->
                    Outliner.getInstance().showCluster(new OutlineSlot(space), cluster));
        } else {
            selectedClusters.keySet().forEach(space ->
                    Outliner.getInstance().keep(new OutlineSlot(space)));
        }
    }

    private static boolean holdingCorrectItem(Player p) {
        var mainHandItem = p.getMainHandItem();
        // Wrench
        if (AllItems.WRENCH.isIn(mainHandItem)) return true;

        if (!(mainHandItem.getItem() instanceof BlockItem mainHandBlockItem)) return false;

        // Or any block that acts as an outline target
        return IDepositClaimerOutlineTarget.class.isAssignableFrom(mainHandBlockItem.getBlock().getClass());
    }

    private static void activateOutlineIfNeeded() {
        if (outlineActive) return;

        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null) return;
        var l = p.level();

        if (!(mc.hitResult instanceof BlockHitResult ray) ||
                !(l.getBlockState(ray.getBlockPos()).getBlock() instanceof IDepositClaimerOutlineTarget target)) {
            return;
        }

        ttl = MAX_TTL;
        outlineActive = true;
        clearAndAddNearbyClaimers(target.getClaimerType());
    }

    private record OutlineSlot(OperatingSpace space) {}
}
