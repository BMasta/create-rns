package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.data.pack.DepositBlockBuilder.DepositBuildingContext;
import com.bmaster.createrns.data.pack.DynamicDatapackContent.Dimension;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositStructureBuilder {
    public static final ResourceLocation DEP_SMALL = CreateRNS.asResource("ore_deposit_small");
    public static final ResourceLocation DEP_MEDIUM = CreateRNS.asResource("ore_deposit_medium");
    public static final ResourceLocation DEP_LARGE = CreateRNS.asResource("ore_deposit_large");

    private static final Object2ObjectOpenHashMap<Dimension, List<ConfiguredEntry>> DEPOSITS =
            new Object2ObjectOpenHashMap<>();

    public static DepositStructureBuilder create(DepositBuildingContext ctx) {
        return new DepositStructureBuilder(ctx);
    }

    public static List<ConfiguredEntry> getDeposits() {
        return DEPOSITS.object2ObjectEntrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().getSuffix()))
                .flatMap(e -> e.getValue().stream())
                .toList();
    }

    public static List<ConfiguredEntry> getDeposits(Dimension dimension) {
        return Collections.unmodifiableList(DEPOSITS.computeIfAbsent(dimension, d -> new ArrayList<>()));
    }

    private final DepositBuildingContext ctx;
    private final List<WeightedTemplate> weightedTemplates = new ArrayList<>();

    private Dimension dimension = Dimension.OVERWORLD;
    private int depth = 8;
    private int depthDeviation = 0;
    private int weight = 2;

    public DepositStructureBuilder dimension(Dimension dimension) {
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
        var candidate = new ConfiguredEntry(ctx.depositKeyword, ctx.depositBlockId(), depth, depthDeviation, weight,
                List.copyOf(weightedTemplates), ctx.isEnabled);
        var deposits = DEPOSITS.computeIfAbsent(dimension, d -> new ArrayList<>());
        var existing = deposits.stream()
                .filter(d -> d.name().equals(ctx.depositKeyword))
                .findFirst()
                .orElse(null);
        if (existing == null) {
            deposits.add(candidate);
        } else if (!existing.equals(candidate)) {
            throw new IllegalStateException("Conflicting dynamic deposit definition already exists: " + ctx.depositKeyword);
        }
    }

    private DepositStructureBuilder(DepositBuildingContext ctx) {
        this.ctx = ctx;
    }

    public record ConfiguredEntry(
            String name, ResourceLocation depositBlock, int depth, int depthDeviation, int weight,
            List<WeightedTemplate> weightedTemplates, Supplier<Boolean> isEnabled
    ) {
    }

    public record WeightedTemplate(ResourceLocation template, int weight) {}

}
