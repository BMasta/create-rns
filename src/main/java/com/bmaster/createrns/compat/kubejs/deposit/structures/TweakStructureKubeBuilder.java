package com.bmaster.createrns.compat.kubejs.deposit.structures;

import com.bmaster.createrns.content.deposit.worldgen.PlacementStrategy;
import com.bmaster.createrns.data.deposit.DepositResources;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class TweakStructureKubeBuilder extends StructureKubeBuilder<TweakStructureKubeBuilder> {
    private final DepositResources.Structure structure;
    private final DepositResources.Spec spec;

    TweakStructureKubeBuilder(DepositResources.Structure structure) {
        this(structure, DynamicDatapackContent.RESOURCES.getSpec(structure.id()));
    }

    private TweakStructureKubeBuilder(DepositResources.Structure structure, DepositResources.Spec spec) {
        super(structure, spec);
        this.structure = structure;
        this.spec = spec;
        blockId = DynamicDatapackContent.RESOURCES.getProcessor(structure.processorId()).targetBlock();
    }

    @Override
    public Integer structureSetWeight() {
        return configuredWeight != null ? configuredWeight : Objects.requireNonNull(structure.weight(),
                () -> "Missing weight for built-in structure " + structure.id());
    }

    boolean weightChanged() {
        return configuredWeight != null;
    }

    boolean defaultScannable() {
        return spec.isScannable().get();
    }

    @Override
    protected TweakStructureKubeBuilder self() {
        return this;
    }

    @Override
    protected ResourceLocation specId() {
        return spec.id();
    }

    @Override
    protected ResourceLocation processorId() {
        return structure.processorId();
    }

    @Override
    protected PlacementStrategy placementStrategy() {
        return structure.placementStrategy();
    }
}
