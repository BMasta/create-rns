package com.bmaster.createrns.content.deposit.claiming;

import com.bmaster.createrns.RNSTextures;
import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapterHolder;
import com.bmaster.createrns.util.Utils;
import com.simibubi.create.AllItems;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositClaimerOutlineRenderer {
    private static final int MAX_TTL = 30;
    private static final int OUTLINE_MAX_DIST = 64;

    private static final Set<IDepositBlockClaimer> keptClaimers = new HashSet<>();
    private static final Set<IDepositBlockClaimer> addedClaimers = new HashSet<>();

    private static boolean outlineActive = false;
    private static int ttl = 0;

    public static void addClaimer(IDepositBlockClaimer claimer) {
        var player = Minecraft.getInstance().player;
        if (!outlineActive || player == null || addedClaimers.contains(claimer)) return;
        keptClaimers.remove(claimer);
        var adapter = OperatingSublevelAdapterHolder.getAdapter();
        if (adapter.distManhattan(player.level(), player.blockPosition(), claimer.getBlockPos()) > OUTLINE_MAX_DIST)
            return;
        addedClaimers.add(claimer);
    }

    public static void removeClaimer(IDepositBlockClaimer claimer) {
        if (!outlineActive) return;
        addedClaimers.remove(claimer);
        keptClaimers.remove(claimer);
    }

    public static void clearOutline() {
        keptClaimers.clear();
        addedClaimers.clear();
        outlineActive = false;
    }

    public static void tick() {
        refreshOutline();
        if (!outlineActive) return;

        if (addedClaimers.isEmpty() && keptClaimers.isEmpty()) {
            clearOutline();
            return;
        }

        ttl--;
        if (ttl <= 0) {
            clearOutline();
            return;
        }

        for (var claimer : keptClaimers) {
            Outliner.getInstance().keep(claimer);
        }

        for (var claimer : addedClaimers) {
            var claimedBlocks = claimer.getClaimedDepositBlocks();
            if (claimedBlocks == null) continue;
            Outliner.getInstance()
                    .showCluster(claimer, claimedBlocks)
                    .lineWidth(0)
                    .withFaceTexture(RNSTextures.BLOCKFACE_TINTED)
                    .colored(Utils.colorFromObject(claimer.getBlockPos(), 0.5f, 0.4f, false));
            keptClaimers.add(claimer);
        }
        addedClaimers.clear();
    }

    private static void refreshOutline() {
        if (!isHoldingCorrectItem()) return;
        ttl = MAX_TTL;

        if (outlineActive || !isLookingAtOutlineTarget()) return;

        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        var l = p.level();

        keptClaimers.clear();
        addedClaimers.clear();
        addedClaimers.addAll(DepositClaimerInstanceHolder.getInstancesWithinManhattanDistance(
                l, p.blockPosition(), OUTLINE_MAX_DIST));
        outlineActive = true;
    }

    private static boolean isLookingAtOutlineTarget() {
        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null) return false;
        var l = p.level();

        return mc.hitResult instanceof BlockHitResult hit &&
                l.getBlockState(hit.getBlockPos()).getBlock() instanceof IDepositClaimerOutlineTarget;
    }

    private static boolean isHoldingCorrectItem() {
        var p = Minecraft.getInstance().player;
        if (p == null) return false;

        var mainHandItem = p.getMainHandItem();
        var offhandItem = p.getOffhandItem();

        // Wrench
        if (AllItems.WRENCH.isIn(mainHandItem) || AllItems.WRENCH.isIn(offhandItem)) return true;

        // Or any block that acts as an outline target
        if (mainHandItem.getItem() instanceof BlockItem mainHandBlockItem &&
                IDepositClaimerOutlineTarget.class.isAssignableFrom(mainHandBlockItem.getBlock().getClass())) {
            return true;
        }
        if (offhandItem.getItem() instanceof BlockItem offhandBlockItem &&
                IDepositClaimerOutlineTarget.class.isAssignableFrom(offhandBlockItem.getBlock().getClass())) {
            return true;
        }

        return false;
    }
}
