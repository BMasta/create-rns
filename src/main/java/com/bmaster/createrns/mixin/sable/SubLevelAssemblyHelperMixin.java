package com.bmaster.createrns.mixin.sable;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSTags.RNSBlockTags;
import com.bmaster.createrns.compat.sable.DepositDurabilitySublevelTransfer;
import com.bmaster.createrns.content.deposit.info.DepositDurabilityManager;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import java.util.ArrayList;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.api.SubLevelAssemblyHelper", remap = false)
public abstract class SubLevelAssemblyHelperMixin {
    @WrapMethod(method = "moveBlocks", remap = false)
    private static void create_rns$moveDepositDurability(
            ServerLevel oldLevel,
            SubLevelAssemblyHelper.AssemblyTransform transform,
            Iterable<BlockPos> positions,
            Operation<Void> original
    ) {
        var newLevel = transform.getLevel();
        var transfers = new ArrayList<DepositDurabilitySublevelTransfer>();
        for (var oldPos : positions) {
            var state = oldLevel.getBlockState(oldPos);
            if (!state.is(RNSBlockTags.DEPOSIT_BLOCKS)) continue;

            transfers.add(DepositDurabilitySublevelTransfer.capture(
                    oldLevel, newLevel, state, oldPos, transform.apply(oldPos)));
        }

        original.call(oldLevel, transform, positions);

        for (var transfer : transfers) {
            var destinationState = newLevel.getBlockState(transfer.newPos());
            if (destinationState.getBlock() != transfer.state().getBlock()) {
                CreateRNS.LOGGER.error(
                        "Skipped deposit durability transfer from {} in {} to {} in {} because destination contains {}",
                        transfer.oldPos(), transfer.oldDimension().location(), transfer.newPos(),
                        transfer.newDimension().location(), destinationState);
                continue;
            }

            var replaced = DepositDurabilityManager.setRaw(newLevel, transfer.newPos(), transfer.durability());
            if (replaced.isPresent()) {
                CreateRNS.LOGGER.error(
                        "Replaced conflicting deposit durability {} at {} in {} with source durability {}",
                        replaced.getAsLong(), transfer.newPos(), transfer.newDimension().location(),
                        transfer.durability());
            }
        }
    }
}
