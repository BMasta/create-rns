package com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance;

import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonator.ShatteringResonatorBlock;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ShatteringResonanceCatalyst extends AbstractResonanceCatalyst {
    public static @Nullable ShatteringResonanceCatalyst fromContraption(BearingContraption contraption) {
        var resCount = 0;
        for (var info : contraption.getBlocks().values()) {
            if (info.state().getBlock() instanceof ShatteringResonatorBlock) {
                resCount++;
            }
        }
        return (resCount > 0) ? new ShatteringResonanceCatalyst(resCount) : null;
    }

    public ShatteringResonanceCatalyst(int resonatorCount) {
        super(resonatorCount);
    }
}
