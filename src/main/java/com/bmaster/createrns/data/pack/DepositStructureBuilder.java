package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.data.pack.DynamicDatapackContent.Dimension;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositStructureBuilder {
    public static final ResourceLocation DEP_SMALL = ResourceLocation.fromNamespaceAndPath(CreateRNS.ID, "ore_deposit_small");
    public static final ResourceLocation DEP_MEDIUM = ResourceLocation.fromNamespaceAndPath(CreateRNS.ID, "ore_deposit_medium");
    public static final ResourceLocation DEP_LARGE = ResourceLocation.fromNamespaceAndPath(CreateRNS.ID, "ore_deposit_large");
    public static boolean dumpMode = false;

    private static final Object2ObjectOpenHashMap<Dimension, List<ConfiguredEntry>> DEPOSITS =
            new Object2ObjectOpenHashMap<>();

    public static DepositStructureBuilder create(String structureId) {
        return new DepositStructureBuilder(structureId);
    }

    public static DepositBlockBuilder blockOnly(String name) {
        return new DepositBlockBuilder(name, () -> true);
    }

    public static List<ConfiguredEntry> getDeposits() {
        return DEPOSITS.values().stream().flatMap(Collection::stream).toList();
    }

    public static List<ConfiguredEntry> getDeposits(Dimension dimension) {
        return Collections.unmodifiableList(DEPOSITS.computeIfAbsent(dimension, d -> new ArrayList<>()));
    }

    private final String structureId;
    private final List<WeightedTemplate> weightedTemplates = new ArrayList<>();
    private final ObjectOpenHashSet<String> compatIndicatorBlocks = new ObjectOpenHashSet<>();

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

    public DepositStructureBuilder enableWhenBlockPresent(String name) {
        compatIndicatorBlocks.add(name);
        return this;
    }

    public DepositStructureBuilder transform(UnaryOperator<DepositStructureBuilder> transform) {
        return transform.apply(this);
    }

    public DepositBlockBuilder block(String name) {
        var depositBlock = ResourceLocation.fromNamespaceAndPath(CreateRNS.ID, name);
        Supplier<Boolean> isEnabled;
        if (dumpMode) {
            isEnabled = () -> compatIndicatorBlocks.isEmpty() || DynamicDatapackDumpTool.includeCompat();
        } else {
            isEnabled = () -> ForgeRegistries.BLOCKS.getKeys().stream().anyMatch(rl ->
                    compatIndicatorBlocks.isEmpty() || compatIndicatorBlocks.contains(rl.getPath()));
        }

        if (weightedTemplates.isEmpty()) {
            throw new IllegalStateException("At least one template must be configured before registering");
        }
        var candidate = new ConfiguredEntry(structureId, depositBlock, depth, depthDeviation, weight,
                List.copyOf(weightedTemplates), isEnabled);
        var deposits = DEPOSITS.computeIfAbsent(dimension, d -> new ArrayList<>());
        var existing = deposits.stream()
                .filter(d -> d.name().equals(structureId))
                .findFirst()
                .orElse(null);
        if (existing == null) {
            deposits.add(candidate);
        } else if (!existing.equals(candidate)) {
            throw new IllegalStateException("Conflicting dynamic deposit definition already exists: " + structureId);
        }

        return new DepositBlockBuilder(name, isEnabled);
    }

    private DepositStructureBuilder(String structureId) {
        this.structureId = structureId;
    }

    public record ConfiguredEntry(
            String name, ResourceLocation depositBlock, int depth, int depthDeviation, int weight,
            List<WeightedTemplate> weightedTemplates, Supplier<Boolean> isEnabled
    ) {
    }

    public record WeightedTemplate(ResourceLocation template, int weight) {}

}
