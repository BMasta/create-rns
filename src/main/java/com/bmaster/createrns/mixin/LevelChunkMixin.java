package com.bmaster.createrns.mixin;

import com.bmaster.createrns.RNSTags;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer;
import com.bmaster.createrns.content.deposit.info.DepositDurabilityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public class LevelChunkMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void create_rns$onBlockChanged(
            BlockPos pos, BlockState newState, boolean isMoving, CallbackInfoReturnable<BlockState> cir
    ) {
        var oldState = cir.getReturnValue();
        if (oldState == null || oldState.getBlock() == newState.getBlock()) return;

        var level = ((LevelChunk) (Object) this).getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!oldState.is(RNSTags.RNSBlockTags.DEPOSIT_BLOCKS) && !newState.is(RNSTags.RNSBlockTags.DEPOSIT_BLOCKS)) return;

        DepositDurabilityManager.removeDepositBlockDurability(serverLevel, pos);
        IDepositBlockClaimer.reclaimBlock(level, pos);
    }
}
