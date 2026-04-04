package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.data.pack.DepositBlockBuilder.DepositBuildingContext;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositSpecBuilder {
    private static final List<ConfiguredEntry> SPECS = new ArrayList<>();

    public static DepositSpecBuilder create(DepositBuildingContext ctx) {
        return new DepositSpecBuilder(ctx);
    }

    public static List<ConfiguredEntry> getEnabledSpecs() {
        return SPECS.stream().filter(s -> s.isEnabled.get()).toList();
    }

    private final DepositBuildingContext ctx;
    private final List<String> scannerIconItemCandidates = new ArrayList<>();
    private DepositDimension dimension = DepositDimension.OVERWORLD;

    public DepositSpecBuilder dimension(DepositDimension dimension) {
        this.dimension = dimension;
        return this;
    }

    public DepositSpecBuilder scannerIconItem(String candidateId) {
        scannerIconItemCandidates.add(candidateId);
        return this;
    }

    public DepositSpecBuilder scannerIconItem(List<String> candidateIds) {
        scannerIconItemCandidates.addAll(candidateIds);
        return this;
    }

    public DepositSpecBuilder transform(UnaryOperator<DepositSpecBuilder> transform) {
        return transform.apply(this);
    }

    public void save() {
        if (scannerIconItemCandidates.isEmpty()) {
            throw new IllegalStateException("Deposit spec must define a scanner icon");
        }
        for (var existing : SPECS) {
            if (existing.spec.structureId == ctx.depositStructureId(dimension)) {
                throw new IllegalStateException("Conflicting deposit spec entry already exists: " +
                        ctx.depositStructureId(dimension));
            }
        }
        var entry = new ConfiguredEntry(ctx.depositSpecId(), dimension, ctx.isEnabled, new ConfiguredSpec(
                List.copyOf(scannerIconItemCandidates), List.of(ctx.depositBlockId().toString()), ctx.depositStructureId(dimension)));
        SPECS.add(entry);
    }

    private DepositSpecBuilder(DepositBuildingContext ctx) {
        this.ctx = ctx;
    }

    public record ConfiguredEntry(
            ResourceLocation specId, DepositDimension dimension, Supplier<Boolean> isEnabled, ConfiguredSpec spec
    ) {
    }

    public record ConfiguredSpec(
            List<String> scannerIconCandidates,
            List<String> mapIconCandidates,
            ResourceLocation structureId
    ) {
    }
}
