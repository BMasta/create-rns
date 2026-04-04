package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.content.deposit.mining.recipe.DepositDurability;
import com.bmaster.createrns.data.pack.DepositBlockBuilder.DepositBuildingContext;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MiningRecipeBuilder {
    private static final List<ConfiguredEntry> RECIPES = new ArrayList<>();

    public static MiningRecipeBuilder create(DepositBuildingContext ctx) {
        return new MiningRecipeBuilder(ctx);
    }

    public static List<ConfiguredEntry> getRecipes() {
        return Collections.unmodifiableList(RECIPES);
    }

    public static List<ConfiguredEntry> getEnabledRecipes() {
        return RECIPES.stream().filter(r -> r.isEnabled.get()).toList();
    }

    private final DepositBuildingContext ctx;
    private final List<YieldBuilder.ConfiguredYield> yields = new ArrayList<>();

    private DepositDimension dimension = DepositDimension.OVERWORLD;
    private @Nullable ResourceLocation replacementBlockId;
    private @Nullable DepositDurability durability;

    public MiningRecipeBuilder dimension(DepositDimension dimension) {
        this.dimension = dimension;
        return this;
    }

    public MiningRecipeBuilder replaceWhenDepleted(String blockId) {
        replacementBlockId = ResourceLocation.parse(blockId);
        return this;
    }

    public MiningRecipeBuilder durability(long core, long edge, float randomSpread) {
        if (core <= 0) throw new IllegalArgumentException("Core durability must be positive");
        if (edge <= 0) throw new IllegalArgumentException("Edge durability must be positive");
        if (randomSpread < 0 || randomSpread > 1) {
            throw new IllegalArgumentException("Random spread must be between 0 and 1");
        }

        durability = new DepositDurability(core, edge, randomSpread);
        return this;
    }

    public MiningRecipeBuilder yield(Consumer<YieldBuilder> yield) {
        var builder = new YieldBuilder();
        yield.accept(builder);
        // Skip adding an empty yield
        if (builder.items.isEmpty()) return this;
        yields.add(builder.build());
        return this;
    }

    public MiningRecipeBuilder transform(UnaryOperator<MiningRecipeBuilder> transform) {
        return transform.apply(this);
    }

    public MiningRecipeBuilder transformIf(boolean condition, UnaryOperator<MiningRecipeBuilder> transform) {
        if (!condition) return this;
        return transform.apply(this);
    }

    public void save() {
        if (yields.isEmpty()) throw new IllegalStateException("Mining recipe must define at least one yield");
        for (var existing : RECIPES) {
            if (ctx.depositBlockId() == existing.recipe.depositBlockId && dimension == existing.recipe.dimension) {
                throw new IllegalStateException("Conflicting mining recipe entry already exists: " +
                        ctx.depositBlockId() + " (" + dimension.getSerializedName() + ")");
            }
        }
        var entry = new ConfiguredEntry(ctx.depositBlockId(), ctx.isEnabled, new ConfiguredRecipe(
                ctx.depositBlockId(), dimension, replacementBlockId, durability, List.copyOf(yields)));
        RECIPES.add(entry);
    }

    private MiningRecipeBuilder(DepositBuildingContext ctx) {
        this.ctx = ctx;
    }

    public record ConfiguredEntry(ResourceLocation recipeId, Supplier<Boolean> isEnabled, ConfiguredRecipe recipe) {
    }

    public record ConfiguredRecipe(
            ResourceLocation depositBlockId,
            DepositDimension dimension,
            @Nullable ResourceLocation replacementBlockId,
            @Nullable DepositDurability durability,
            List<YieldBuilder.ConfiguredYield> yields
    ) {
    }
}
