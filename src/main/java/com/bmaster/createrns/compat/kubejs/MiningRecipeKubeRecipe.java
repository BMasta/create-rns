package com.bmaster.createrns.compat.kubejs;

import com.google.gson.JsonSyntaxException;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentBuilderMap;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.util.ConsoleJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MiningRecipeKubeRecipe extends RecipeJS {
    @Nullable
    @Override
    public Recipe<?> createRecipe() {
        try {
            return super.createRecipe();
        } catch (JsonSyntaxException e) {
            ConsoleJS.SERVER.error(e.getMessage());
            type.event.failedCount.incrementAndGet();
            return null;
        }
    }

    @Info("Sets the deposit block mined by this mining recipe.")
    public MiningRecipeKubeRecipe block(String blockId) {
        setValue(MiningRecipeKubeSchema.DEPOSIT_BLOCK, normalizeId(blockId));
        return this;
    }

    @Info("""
            Makes this recipe functional in overworld. This is the default dimension unless specified otherwise.
            A recipe can only target a single dimension.
            """)
    public MiningRecipeKubeRecipe overworld() {
        setValue(MiningRecipeKubeSchema.DIMENSION, MiningRecipeKubeSchema.OVERWORLD_ID);
        return this;
    }

    @Info("Makes this recipe functional in the nether. A recipe can only target a single dimension.")
    public MiningRecipeKubeRecipe nether() {
        setValue(MiningRecipeKubeSchema.DIMENSION, MiningRecipeKubeSchema.NETHER_ID);
        return this;
    }

    @Info("""
            Deposits that run out of resources will be replaced with this block. Does not have to be a deposit block.
            Note that finite deposits must be enabled in the server config for this setting to take effect.
            """)
    public MiningRecipeKubeRecipe replaceWhenDepleted(String blockId) {
        setValue(MiningRecipeKubeSchema.REPLACE_WHEN_DEPLETED, normalizeId(blockId));
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

        var yields = getValue(MiningRecipeKubeSchema.YIELDS);
        var nextYield = builder.build();
        setValue(MiningRecipeKubeSchema.YIELDS,
                yields == null
                        ? new RecipeComponentBuilderMap[]{nextYield}
                        : MiningRecipeKubeSchema.YIELDS_COMPONENT.add(yields, nextYield));
        return this;
    }

    private static String normalizeId(String id) {
        return id.contains(":") ? id : "minecraft:" + id;
    }
}
