package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AttachmentCatalystRequirement extends CatalystRequirement {
    public static final MapCodec<AttachmentCatalystRequirement> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("attachment")
                                    .forGetter(c -> c.attachment),
                    Codec.intRange(0, Integer.MAX_VALUE).fieldOf("count")
                            .orElse(1)
                            .forGetter(c -> c.count))
            .apply(i, AttachmentCatalystRequirement::new));

    protected static final Set<Class<? extends Catalyst>> RELEVANT_CATALYST_TYPES = Set.of(AttachmentCatalyst.class);

    public final HolderSet<Block> attachment;
    public final int count;

    public AttachmentCatalystRequirement(HolderSet<Block> attachment, int count) {
        this.attachment = attachment;
        this.count = count;
    }

    @Override
    public CatalystRequirementType<?> type() {
        return CatalystRequirementType.ATTACHMENT;
    }

    @Override
    public Set<Class<? extends Catalyst>> relevantCatalystTypes() {
        return RELEVANT_CATALYST_TYPES;
    }

    @Override
    public boolean isSatisfiedBy(Collection<Catalyst> catalysts) {
        int actualCount = 0;
        for (var c : catalysts) {
            if (!(c instanceof AttachmentCatalyst attachmentCatalyst)) continue;
            if (!attachmentCatalyst.attachmentBlock.defaultBlockState().is(this.attachment)) continue;
            actualCount += attachmentCatalyst.count;
            if (actualCount >= this.count) return true;
        }
        return false;
    }

    /// For performance reasons assumes the catalysts are already validated with isSatisfiedBy.
    @Override
    public boolean useCatalysts(Collection<Catalyst> catalysts, boolean simulate) {
        return true;
    }
}
