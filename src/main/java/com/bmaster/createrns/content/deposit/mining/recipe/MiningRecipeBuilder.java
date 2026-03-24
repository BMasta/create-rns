package com.bmaster.createrns.content.deposit.mining.recipe;

import com.bmaster.createrns.data.pack.DynamicDatapackDepositEntry;
import com.bmaster.createrns.data.pack.DynamicDatapackDepositEntry.DepositBlockBuildingContext;
import com.bmaster.createrns.data.pack.DynamicDatapackDumpTool;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MiningRecipeBuilder {
    private static final List<ConfiguredEntry> RECIPES = new ArrayList<>();

    public static MiningRecipeBuilder create(DepositBlockBuildingContext ctx) {
        return new MiningRecipeBuilder(ctx);
    }

    public static List<ConfiguredEntry> getRecipes() {
        return Collections.unmodifiableList(RECIPES);
    }

    private final ResourceLocation recipeId;
    private final ResourceLocation depositBlockId;
    private final List<YieldBuilder.ConfiguredYield> yields = new ArrayList<>();

    private @Nullable ResourceLocation replacementBlockId;
    private @Nullable DepositDurability durability;
    private @Nullable String requiredModId;

    public MiningRecipeBuilder requireMod(String modId) {
        if (modId.isBlank()) throw new IllegalArgumentException("Required mod id cannot be blank");
        requiredModId = modId;
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

    public void save() {
        if (yields.isEmpty()) throw new IllegalStateException("Mining recipe must define at least one yield");

        var candidate = new ConfiguredEntry(
                recipeId,
                requiredModId,
                new ConfiguredRecipe(depositBlockId, replacementBlockId, durability, List.copyOf(yields))
        );
        var existing = RECIPES.stream()
                .filter(recipe -> recipe.recipeId().equals(recipeId))
                .findFirst()
                .orElse(null);

        if (existing == null) {
            RECIPES.add(candidate);
            return;
        }
        if (!existing.equals(candidate)) {
            throw new IllegalStateException("Conflicting dynamic mining recipe definition already exists: " + recipeId);
        }
    }

    private MiningRecipeBuilder(DepositBlockBuildingContext ctx) {
        this.recipeId = ctx.depositBlockId();
        this.depositBlockId = ctx.depositBlockId();
        this.requiredModId = ctx.requiredModId();
    }

    public record ConfiguredEntry(ResourceLocation recipeId, @Nullable String requiredModId, ConfiguredRecipe recipe) {
        public boolean isEnabled() {
            if (DynamicDatapackDepositEntry.dumpMode) {
                var enabledMods = DynamicDatapackDumpTool.getEnabledMods();
                return requiredModId == null || enabledMods == null || enabledMods.contains(requiredModId);
            }

            var modList = ModList.get();
            return requiredModId == null || modList.isLoaded(requiredModId);
        }
    }

    public record ConfiguredRecipe(
            ResourceLocation depositBlockId,
            @Nullable ResourceLocation replacementBlockId,
            @Nullable DepositDurability durability,
            List<YieldBuilder.ConfiguredYield> yields
    ) {
    }
}
