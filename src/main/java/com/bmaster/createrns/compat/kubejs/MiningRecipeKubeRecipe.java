package com.bmaster.createrns.compat.kubejs;

import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.CustomObjectRecipeComponent;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MiningRecipeKubeRecipe extends KubeRecipe {
    private final List<List<String>> unresolvedRequiredItems = new ArrayList<>();

    @Info("Sets the deposit block mined by this mining recipe.")
    public MiningRecipeKubeRecipe block(String blockId) {
        setValue(MiningRecipeKubeSchema.DEPOSIT_BLOCK,
                MiningRecipeKubeSchema.DEPOSIT_BLOCK.component.wrap(recipeContext(), normalizeId(blockId)));
        return this;
    }

    @Info("""
            Makes this recipe functional in overworld. This is the default dimension unless specified otherwise.
            A recipe can only target a single dimension.
            """)
    public MiningRecipeKubeRecipe overworld() {
        setValue(MiningRecipeKubeSchema.DIMENSION, Level.OVERWORLD);
        return this;
    }

    @Info("Makes this recipe functional in the nether. A recipe can only target a single dimension.")
    public MiningRecipeKubeRecipe nether() {
        setValue(MiningRecipeKubeSchema.DIMENSION, Level.NETHER);
        return this;
    }

    @Info("""
            Deposits that run out of resources will be replaced with this block. Does not have to be a deposit block.
            Note that finite deposits must be enabled in the server config for this setting to take effect.
            """)
    public MiningRecipeKubeRecipe replaceWhenDepleted(String blockId) {
        setValue(MiningRecipeKubeSchema.REPLACE_WHEN_DEPLETED,
                MiningRecipeKubeSchema.REPLACE_WHEN_DEPLETED.component.wrap(recipeContext(), normalizeId(blockId)));
        return this;
    }

    @Info("""
            Sets the number of times a block can be mined by a mining contraption.
            Blocks at the horizontal center of the vein get the core durability,
            and it gradually decreases towards the edges.
            """)
    public MiningRecipeKubeRecipe durability(long core, long edge, float randomSpread) {
        setValue(MiningRecipeKubeSchema.DURABILITY, MiningRecipeKubeSchema.durability(core, edge, randomSpread));
        return this;
    }

    @Info("Adds a yield entry to this mining recipe. Each yield is independent of other yields.")
    public MiningRecipeKubeRecipe yield(Consumer<YieldKubeBuilder> yield) {
        var builder = new YieldKubeBuilder();
        yield.accept(builder);
        if (!builder.hasItems()) return this;

        unresolvedRequiredItems.addAll(builder.unresolvedRequiredItems());
        var yields = getValue(MiningRecipeKubeSchema.YIELDS);
        var mutableYields = yields == null
                ? new ArrayList<List<CustomObjectRecipeComponent.Value>>()
                : new ArrayList<>(yields);
        mutableYields.add(builder.build());
        setValue(MiningRecipeKubeSchema.YIELDS, mutableYields);
        return this;
    }

    @Override
    public KubeRecipe serializeChanges() {
        var depositBlock = getValue(MiningRecipeKubeSchema.DEPOSIT_BLOCK);
        if (depositBlock == null || depositBlock == Blocks.AIR) {
            throw new IllegalStateException("Mining recipe must specify a deposit block via block(...)");
        }

        var yields = getValue(MiningRecipeKubeSchema.YIELDS);
        if (yields == null || yields.isEmpty()) {
            throw new IllegalStateException("Mining recipe must define at least one yield");
        }
        if (!unresolvedRequiredItems.isEmpty()) {
            throw new IllegalStateException("Mining recipe item(...) candidates do not resolve to a registered item: "
                    + unresolvedRequiredItems);
        }

        return super.serializeChanges();
    }

    private RecipeScriptContext recipeContext() {
        return new RecipeScriptContext.Impl(null, this);
    }

    private static String normalizeId(String id) {
        return id.contains(":") ? id : "minecraft:" + id;
    }
}
