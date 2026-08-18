package com.bmaster.createrns.mixin;

import com.bmaster.createrns.RNSTags.RNSBlockTags;
import com.bmaster.createrns.infrastructure.ServerConfig;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Inject(method = "getPistonPushReaction", at = @At("HEAD"), cancellable = true)
    private void create_rns$getDepositPistonPushReaction(CallbackInfoReturnable<PushReaction> cir) {
        var state = (BlockBehaviour.BlockStateBase) (Object) this;
        if (!state.is(RNSBlockTags.DEPOSIT_BLOCKS)) return;

        cir.setReturnValue(ServerConfig.MOVABLE_DEPOSITS.get() ? PushReaction.NORMAL : PushReaction.BLOCK);
    }
}
