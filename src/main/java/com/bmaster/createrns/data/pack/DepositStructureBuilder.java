package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.CreateRNS;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositStructureBuilder {
    public static final ResourceLocation DEP_SMALL = CreateRNS.asResource("ore_deposit_small");
    public static final ResourceLocation DEP_MEDIUM = CreateRNS.asResource("ore_deposit_medium");
    public static final ResourceLocation DEP_LARGE = CreateRNS.asResource("ore_deposit_large");
    public static boolean dumpMode = false;

    private static final List<ConfiguredEntry> DEPOSITS = new ArrayList<>();

    public static DepositStructureBuilder create(String structureId) {
        return new DepositStructureBuilder(structureId);
    }

    public static DepositBlockBuilder blockOnly(String name) {
        return new DepositBlockBuilder(name, () -> true);
    }

    public static List<ConfiguredEntry> getDeposits() {
        return Collections.unmodifiableList(DEPOSITS);
    }

    private final String structureId;
    private final List<WeightedTemplate> weightedTemplates = new ArrayList<>();
    private final ObjectOpenHashSet<String> compatIndicatorBlocks = new ObjectOpenHashSet<>();

    private int depth = 8;
    private int weight = 2;

    public DepositStructureBuilder depth(int depth) {
        this.depth = depth;
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
        var depositBlock = CreateRNS.asResource(name);
        Supplier<Boolean> isEnabled;
        if (dumpMode) {
            isEnabled = () -> compatIndicatorBlocks.isEmpty() || DynamicDatapackDumpTool.includeCompat();
        } else {
            isEnabled = () -> BuiltInRegistries.BLOCK.keySet().stream().anyMatch(rl ->
                    compatIndicatorBlocks.isEmpty() || compatIndicatorBlocks.contains(rl.getPath()));
        }

        if (weightedTemplates.isEmpty()) {
            throw new IllegalStateException("At least one template must be configured before registering");
        }
        var candidate = new ConfiguredEntry(structureId, depositBlock, depth, weight, List.copyOf(weightedTemplates),
                isEnabled);
        var existing = DEPOSITS.stream()
                .filter(d -> d.name().equals(structureId))
                .findFirst()
                .orElse(null);
        if (existing == null) {
            DEPOSITS.add(candidate);
        } else if (!existing.equals(candidate)) {
            throw new IllegalStateException("Conflicting dynamic deposit definition already exists: " + structureId);
        }

        return new DepositBlockBuilder(name, isEnabled);
    }

    private DepositStructureBuilder(String structureId) {
        this.structureId = structureId;
    }

    public record ConfiguredEntry(
            String name, ResourceLocation depositBlock, int depth, int weight,
            List<WeightedTemplate> weightedTemplates, Supplier<Boolean> isEnabled
    ) {
    }

    public record WeightedTemplate(ResourceLocation template, int weight) {}

}
