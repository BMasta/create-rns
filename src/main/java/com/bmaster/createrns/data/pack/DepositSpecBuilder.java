package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.data.pack.DepositBlockBuilder.DepositBuildingContext;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
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

    public static List<ConfiguredEntry> getSpecs() {
        return Collections.unmodifiableList(SPECS);
    }

    public static List<String> metalScannerIconCandidates(String material) {
        return List.of(
                "#c:raw_materials/" + material,
                "#c:ores/" + material,
                "#c:ingots/" + material,
                "#c:nuggets/" + material);
    }

    public static List<String> gemScannerIconCandidates(String material) {
        return List.of("#c:gems/" + material);
    }

    public static List<String> dustScannerIconCandidates(String material) {
        return List.of("#c:dusts/" + material);
    }

    private final DepositBuildingContext ctx;
    private final List<String> scannerIconItemCandidates = new ArrayList<>();
    private DepositDimension dimension = DepositDimension.OVERWORLD;

    public DepositSpecBuilder dimension(DepositDimension dimension) {
        this.dimension = dimension;
        return this;
    }

    public DepositSpecBuilder scannerIcon(String candidateId) {
        scannerIconItemCandidates.add(candidateId);
        return this;
    }

    public DepositSpecBuilder scannerIconMetal(String material) {
        scannerIconItemCandidates.addAll(metalScannerIconCandidates(material));
        return this;
    }

    public DepositSpecBuilder scannerIconGem(String dustKeyword) {
        scannerIconItemCandidates.addAll(gemScannerIconCandidates(dustKeyword));
        return this;
    }

    public DepositSpecBuilder scannerIconDust(String material) {
        scannerIconItemCandidates.addAll(dustScannerIconCandidates(material));
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
        var entry = new ConfiguredEntry(ctx.depositSpecId(), dimension, new ConfiguredSpec(
                List.copyOf(scannerIconItemCandidates),
                List.of(ctx.depositBlockId().toString()),
                ctx.depositStructureId(dimension),
                ctx.isEnabled));
        SPECS.add(entry);
    }

    private DepositSpecBuilder(DepositBuildingContext ctx) {
        this.ctx = ctx;
    }

    public record ConfiguredEntry(
            ResourceLocation specId, DepositDimension dimension, ConfiguredSpec spec
    ) {
    }

    public record ConfiguredSpec(
            List<String> scannerIconCandidates,
            List<String> mapIconCandidates,
            ResourceLocation structureId,
            Supplier<Boolean> scannable
    ) {
    }
}
