package com.bmaster.createrns.mixin.simulated;

import com.bmaster.createrns.RNSTags.RNSBlockTags;
import com.bmaster.createrns.infrastructure.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.simulated_team.simulated.util.assembly.SimAssemblyContraption", remap = false)
public abstract class SimAssemblyContraptionMixin {
    @Inject(method = "movementAllowed", at = @At("HEAD"), cancellable = true, remap = false)
    private void create_rns$preventDepositMovement(
            BlockState state, Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir
    ) {
        if (ServerConfig.MOVABLE_DEPOSITS.get() || !state.is(RNSBlockTags.DEPOSIT_BLOCKS)) return;
        cir.setReturnValue(false);
    }
}
