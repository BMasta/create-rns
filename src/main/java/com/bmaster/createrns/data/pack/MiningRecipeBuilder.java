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

    private final DepositBuildingContext ctx;
    private final List<YieldBuilder.ConfiguredYield> yields = new ArrayList<>();

    private @Nullable ResourceLocation replacementBlockId;
    private @Nullable DepositDurability durability;

    public MiningRecipeBuilder compat() {
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

        var candidate = new ConfiguredEntry(
                ctx.depositBlockId(),
                new ConfiguredRecipe(ctx.depositBlockId(), replacementBlockId, durability, List.copyOf(yields)),
                ctx.isEnabled
        );
        var existing = RECIPES.stream()
                .filter(recipe -> recipe.recipeId().equals(ctx.depositBlockId()))
                .findFirst()
                .orElse(null);

        if (existing == null) {
            RECIPES.add(candidate);
            return;
        }
        if (!existing.equals(candidate)) {
            throw new IllegalStateException("Conflicting dynamic mining recipe definition already exists: " +
                    ctx.depositBlockId());
        }
    }

    private MiningRecipeBuilder(DepositBuildingContext ctx) {
        this.ctx = ctx;
    }

    public record ConfiguredEntry(ResourceLocation recipeId, ConfiguredRecipe recipe, Supplier<Boolean> isEnabled) {
    }

    public record ConfiguredRecipe(
            ResourceLocation depositBlockId,
            @Nullable ResourceLocation replacementBlockId,
            @Nullable DepositDurability durability,
            List<YieldBuilder.ConfiguredYield> yields
    ) {
    }
}
