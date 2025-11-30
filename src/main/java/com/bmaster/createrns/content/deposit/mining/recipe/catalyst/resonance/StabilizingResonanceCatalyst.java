package com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance;

import com.bmaster.createrns.content.deposit.mining.multiblock.attachment.resonator.StabilizingResonatorBlock;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class StabilizingResonanceCatalyst extends AbstractResonanceCatalyst {
    public static @Nullable StabilizingResonanceCatalyst fromContraption(BearingContraption contraption) {
        var resCount = 0;
        for (var info : contraption.getBlocks().values()) {
            if (info.state().getBlock() instanceof StabilizingResonatorBlock) {
                resCount++;
            }
        }
        return (resCount > 0) ? new StabilizingResonanceCatalyst(resCount) : null;
    }

    public StabilizingResonanceCatalyst(int resonatorCount) {
        super(resonatorCount);
    }
}
