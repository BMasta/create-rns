package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.data.pack.DepositBlockBuilder.DepositBuildingContext;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositStructureBuilder {
    public static final ResourceLocation PROCESSOR_INPUT_BLOCK = ResourceLocation.withDefaultNamespace("end_stone");
    public static final ResourceLocation DEP_SMALL = CreateRNS.asResource("ore_deposit_small");
    public static final ResourceLocation DEP_MEDIUM = CreateRNS.asResource("ore_deposit_medium");
    public static final ResourceLocation DEP_LARGE = CreateRNS.asResource("ore_deposit_large");

    public static final Preset OVERWORLD_COMMON = new Preset(
            DepositDimension.OVERWORLD, 8, 50,
            List.of(new WeightedTemplate(DEP_MEDIUM, 70), new WeightedTemplate(DEP_LARGE, 30)));
    public static final Preset OVERWORLD_UNCOMMON = new Preset(
            DepositDimension.OVERWORLD, 10, 35,
            List.of(new WeightedTemplate(DEP_SMALL, 30), new WeightedTemplate(DEP_MEDIUM, 60),
                    new WeightedTemplate(DEP_LARGE, 10)));
    public static final Preset OVERWORLD_RARE = new Preset(
            DepositDimension.OVERWORLD, 12, 20,
            List.of(new WeightedTemplate(DEP_SMALL, 70), new WeightedTemplate(DEP_MEDIUM, 28),
                    new WeightedTemplate(DEP_LARGE, 2)));
    public static final Preset NETHER_COMMON = new Preset(
            DepositDimension.NETHER, 4, 50, OVERWORLD_COMMON.weightedTemplates());
    public static final Preset NETHER_UNCOMMON = new Preset(
            DepositDimension.NETHER, 4, 35, OVERWORLD_UNCOMMON.weightedTemplates());
    public static final Preset NETHER_RARE = new Preset(
            DepositDimension.NETHER, 4, 20, OVERWORLD_RARE.weightedTemplates());

    private static final Map<String, Preset> PRESETS = Map.of(
            "overworld_common", OVERWORLD_COMMON,
            "overworld_uncommon", OVERWORLD_UNCOMMON,
            "overworld_rare", OVERWORLD_RARE,
            "nether_common", NETHER_COMMON,
            "nether_uncommon", NETHER_UNCOMMON,
            "nether_rare", NETHER_RARE
    );

    private static final List<ConfiguredEntry> DEPOSITS = new ArrayList<>();

    public static DepositStructureBuilder create(DepositBuildingContext ctx) {
        return new DepositStructureBuilder(ctx);
    }

    public static List<ConfiguredEntry> getDeposits() {
        return Collections.unmodifiableList(DEPOSITS);
    }

    public static List<ConfiguredEntry> getDeposits(DepositDimension dimension) {
        return DEPOSITS.stream()
                .filter(d -> dimension == d.structure.dimension)
                .toList();
    }

    public static List<ConfiguredEntry> getScannableDeposits(DepositDimension dimension) {
        return DEPOSITS.stream()
                .filter(d -> dimension == d.structure.dimension)
                .filter(d -> d.scannable.get())
                .toList();
    }

    public static ResourceLocation structureId(ConfiguredEntry entry) {
        return structureId(entry.name, entry.structure.dimension);
    }

    public static ResourceLocation structureId(String depositName, DepositDimension dimension) {
        return CreateRNS.asResource("deposit_" + dimension.prefix() + depositName);
    }

    public static Preset getPreset(String presetId) {
        var preset = PRESETS.get(presetId);
        if (preset != null) return preset;

        throw new IllegalArgumentException("Unknown deposit preset: " + presetId + "\n" +
                "Available presets: " + String.join(", ", PRESETS.keySet()));
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

    public DepositStructureBuilder preset(Preset preset) {
        dimension(preset.dimension);
        depth(preset.depth);
        weight(preset.weight);
        for (var template : preset.weightedTemplates) {
            nbt(template.template(), template.weight());
        }
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
            String name, Supplier<Boolean> scannable, ConfiguredStructure structure
    ) {
    }

    public record ConfiguredStructure(
            ResourceLocation depositBlock, DepositDimension dimension, int depth, int depthDeviation,
            int weight, List<WeightedTemplate> weightedTemplates
    ) {
    }

    public record WeightedTemplate(ResourceLocation template, int weight) {}

    public record Preset(
            DepositDimension dimension, int depth, int weight, List<WeightedTemplate> weightedTemplates
    ) {
    }

}
