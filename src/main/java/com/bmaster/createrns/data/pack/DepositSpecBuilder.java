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

    private final DepositBuildingContext ctx;
    private final List<String> scannerIconItemCandidates = new ArrayList<>();

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

        var candidate = new ConfiguredEntry(
                ctx.depositSpecId(),
                new ConfiguredSpec(
                        List.copyOf(scannerIconItemCandidates),
                        List.of(ctx.depositBlockId().toString()),
                        ctx.depositStructureId()
                ),
                ctx.isEnabled
        );
        var existing = SPECS.stream()
                .filter(spec -> spec.specId().equals(ctx.depositSpecId()))
                .findFirst()
                .orElse(null);

        if (existing == null) {
            SPECS.add(candidate);
            return;
        }
        if (!existing.equals(candidate)) {
            throw new IllegalStateException("Conflicting dynamic deposit spec definition already exists: " +
                    ctx.depositSpecId());
        }
    }

    private DepositSpecBuilder(DepositBuildingContext ctx) {
        this.ctx = ctx;
    }

    public record ConfiguredEntry(ResourceLocation specId, ConfiguredSpec spec, Supplier<Boolean> isEnabled) {
    }

    public record ConfiguredSpec(
            List<String> scannerIconCandidates,
            List<String> mapIconCandidates,
            ResourceLocation structureId
    ) {
    }
}
