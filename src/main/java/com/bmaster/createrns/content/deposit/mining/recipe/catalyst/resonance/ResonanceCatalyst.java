package com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance;

import com.bmaster.createrns.content.deposit.mining.multiblock.attachment.resonator.AbstractResonatorBlock;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ResonanceCatalyst extends AbstractResonanceCatalyst {
    public static @Nullable ResonanceCatalyst fromContraption(BearingContraption contraption) {
        var resCount = 0;
        for (var info : contraption.getBlocks().values()) {
            if (info.state().getBlock() instanceof AbstractResonatorBlock) {
                resCount++;
            }
        }
        return (resCount > 0) ? new ResonanceCatalyst(resCount) : null;
    }

    public ResonanceCatalyst(int resonatorCount) {
        super(resonatorCount);
    }
}
