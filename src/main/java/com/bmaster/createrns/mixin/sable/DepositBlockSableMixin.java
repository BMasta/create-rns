package com.bmaster.createrns.mixin.sable;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.compat.sable.DepositDurabilitySublevelTransfer;
import com.bmaster.createrns.content.deposit.DepositBlock;
import com.bmaster.createrns.content.deposit.info.DepositDurabilityManager;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DepositBlock.class)
public abstract class DepositBlockSableMixin implements BlockSubLevelAssemblyListener {
    @Unique
    private @Nullable DepositDurabilitySublevelTransfer create_rns$pendingDurabilityTransfer;

    @Override
    public void beforeMove(
            ServerLevel oldLevel, ServerLevel newLevel, BlockState state, BlockPos oldPos, BlockPos newPos
    ) {
        if (create_rns$pendingDurabilityTransfer != null) {
            CreateRNS.LOGGER.error("Discarding unfinished deposit durability transfer {}",
                    create_rns$pendingDurabilityTransfer);
        }
        create_rns$pendingDurabilityTransfer = DepositDurabilitySublevelTransfer.capture(
                oldLevel, newLevel, state, oldPos, newPos);
    }

    @Override
    public void afterMove(
            ServerLevel oldLevel, ServerLevel newLevel, BlockState state, BlockPos oldPos, BlockPos newPos
    ) {
        var transfer = create_rns$pendingDurabilityTransfer;
        if (transfer == null) {
            CreateRNS.LOGGER.error("Received deposit durability afterMove without a matching beforeMove from {} to {}",
                    oldPos, newPos);
            return;
        }
        if (!transfer.matches(oldLevel, newLevel, state, oldPos, newPos)) {
            CreateRNS.LOGGER.error(
                    "Ignored unrelated deposit durability afterMove from {} to {}; pending transfer is {}",
                    oldPos, newPos, transfer);
            return;
        }

        create_rns$pendingDurabilityTransfer = null;
        var replaced = DepositDurabilityManager.setRaw(
                newLevel, newPos, transfer.durability());
        if (replaced.isPresent()) {
            CreateRNS.LOGGER.error("Replaced conflicting deposit durability {} at {} in {} with source durability {}",
                    replaced.getAsLong(), newPos, newLevel.dimension().location(), transfer.durability());
        }
    }
}
