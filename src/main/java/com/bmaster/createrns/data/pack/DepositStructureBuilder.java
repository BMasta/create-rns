package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.CreateRNS;
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
public class DepositStructureBuilder {
    public static final ResourceLocation DEP_SMALL = CreateRNS.asResource("ore_deposit_small");
    public static final ResourceLocation DEP_MEDIUM = CreateRNS.asResource("ore_deposit_medium");
    public static final ResourceLocation DEP_LARGE = CreateRNS.asResource("ore_deposit_large");

    private static final List<ConfiguredEntry> DEPOSITS = new ArrayList<>();

    public static DepositStructureBuilder create(DepositBuildingContext ctx) {
        return new DepositStructureBuilder(ctx);
    }

    public static List<ConfiguredEntry> getEnabledDeposits() {
        return DEPOSITS.stream().filter(d -> d.isEnabled.get()).toList();
    }

    public static List<ConfiguredEntry> getEnabledDeposits(DepositDimension dimension) {
        return DEPOSITS.stream()
                .filter(d -> d.isEnabled.get())
                .filter(d -> dimension == d.structure.dimension)
                .toList();
    }

    private final DepositBuildingContext ctx;
    private final List<WeightedTemplate> weightedTemplates = new ArrayList<>();

    private DepositDimension dimension = DepositDimension.OVERWORLD;
    private int depth = 8;
    private int depthDeviation = 0;
    private int weight = 2;

    public DepositStructureBuilder dimension(DepositDimension dimension) {
        this.dimension = dimension;
        return this;
    }

    public DepositStructureBuilder depth(int depth) {
        this.depth = depth;
        return this;
    }

    public DepositStructureBuilder depthDeviation(int delta) {
        this.depthDeviation = delta;
        return this;
    }

    public DepositStructureBuilder weight(int weight) {
        if (weight <= 0) throw new IllegalArgumentException("Deposit weight must be positive");
        this.weight = weight;
        return this;
    }

    public DepositStructureBuilder nbt(ResourceLocation template, int weight) {
        if (weight <= 0) throw new IllegalArgumentException("Template weight must be positive");
        weightedTemplates.add(new WeightedTemplate(template, weight));
        return this;
    }

    public DepositStructureBuilder transform(UnaryOperator<DepositStructureBuilder> transform) {
        return transform.apply(this);
    }

    public void save() {
        if (weightedTemplates.isEmpty()) {
            throw new IllegalStateException("At least one template must be configured before registering");
        }
        for (var existing : DEPOSITS) {
            if (existing.structure.depositBlock == ctx.depositBlockId() && existing.structure.dimension == dimension) {
                throw new IllegalStateException("Conflicting deposit structure entry already exists: " +
                        ctx.depositKeyword + " (" + dimension.getSerializedName() + ")");
            }
        }
        var entry = new ConfiguredEntry(ctx.depositKeyword, ctx.isEnabled, new ConfiguredStructure(
                ctx.depositBlockId(), dimension, depth, depthDeviation, weight, List.copyOf(weightedTemplates)));
        DEPOSITS.add(entry);
    }

    private DepositStructureBuilder(DepositBuildingContext ctx) {
        this.ctx = ctx;
    }

    public record ConfiguredEntry(
            String name, Supplier<Boolean> isEnabled, ConfiguredStructure structure
    ) {
    }

    public record ConfiguredStructure(
            ResourceLocation depositBlock, DepositDimension dimension, int depth, int depthDeviation,
            int weight, List<WeightedTemplate> weightedTemplates
    ) {
    }

    public record WeightedTemplate(ResourceLocation template, int weight) {}

}
