package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.data.pack.DepositStructureBuilder.DepositBuildingContext;
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
    private final List<ResourceLocation> scannerIconItemCandidates = new ArrayList<>();
    private final List<ResourceLocation> scannerIconTagCandidates = new ArrayList<>();

    public DepositSpecBuilder scannerIconVanillaItem(String name) {
        if (name.contains(":")) throw new IllegalArgumentException("Item name cannot contain namespace");
        scannerIconItemCandidates.add(ResourceLocation.parse(name));
        return this;
    }

    public DepositSpecBuilder scannerIconItem(String namespace, String name) {
        scannerIconItemCandidates.add(ResourceLocation.parse(namespace + ":" + name));
        return this;
    }

    public DepositSpecBuilder scannerIconCommonTag(String tagId) {
        scannerIconTagCandidates.add(ResourceLocation.parse("c:" + tagId));
        return this;
    }

    public DepositSpecBuilder scannerIconVanillaTag(String tagId) {
        scannerIconTagCandidates.add(ResourceLocation.parse(tagId));
        return this;
    }

    public DepositSpecBuilder scannerIconTag(String namespace, String tagId) {
        scannerIconTagCandidates.add(ResourceLocation.parse(namespace + ":" + tagId));
        return this;
    }

    public DepositSpecBuilder transform(UnaryOperator<DepositSpecBuilder> transform) {
        return transform.apply(this);
    }

    public void save() {
        if (scannerIconItemCandidates.isEmpty() && scannerIconTagCandidates.isEmpty()) {
            throw new IllegalStateException("Deposit spec must define at least one scanner icon item or tag");
        }

        var candidate = new ConfiguredEntry(
                ctx.depositSpecId(),
                new ConfiguredSpec(
                        List.copyOf(scannerIconItemCandidates),
                        List.copyOf(scannerIconTagCandidates),
                        ctx.depositBlockId(),
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
            List<ResourceLocation> scannerIconItemCandidates,
            List<ResourceLocation> scannerIconTagCandidates,
            ResourceLocation mapIconItemId,
            ResourceLocation structureId
    ) {
    }
}
