package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.RNSTags.RNSBlockTags;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.CustomObjectRecipeComponent;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MiningRecipeKubeRecipe extends KubeRecipe {
    private boolean hasFallbackSourceLine = false;
    private boolean hasErrorSourceLine = false;

    @Info("Sets the deposit block mined by this mining recipe.")
    public MiningRecipeKubeRecipe block(Context cx, String blockId) {
        var coarseSourceLine = SourceLine.of(cx);
        return block(blockId, KubeJSSourceLine.expressionStart(coarseSourceLine),
                KubeJSSourceLine.methodCall(coarseSourceLine, "block"));
    }

    @HideFromJS
    public MiningRecipeKubeRecipe block(String blockId) {
        return block(blockId, SourceLine.UNKNOWN, SourceLine.UNKNOWN);
    }

    MiningRecipeKubeRecipe block(String blockId, SourceLine callSourceLine) {
        return block(blockId, callSourceLine, callSourceLine);
    }

    private MiningRecipeKubeRecipe block(
            String blockId, SourceLine fallbackSourceLine, SourceLine errorSourceLine
    ) {
        captureFallbackSourceLine(fallbackSourceLine);
        var normalizedId = normalizeId(blockId);
        var parsedId = ResourceLocation.tryParse(normalizedId);
        var block = parsedId == null ? null : BuiltInRegistries.BLOCK.getOptional(parsedId).orElse(null);
        if (block == null) {
            captureErrorSourceLine(errorSourceLine);
            json.addProperty(MiningRecipeKubeSchema.DEPOSIT_BLOCK.name, normalizedId);
            return this;
        }

        if (!block.defaultBlockState().is(RNSBlockTags.DEPOSIT_BLOCKS)) captureErrorSourceLine(errorSourceLine);

        setValue(MiningRecipeKubeSchema.DEPOSIT_BLOCK, block);
        return this;
    }

    @Info("""
            Makes this recipe functional in overworld. This is the default dimension unless specified otherwise.
            A recipe can only target a single dimension.
            """)
    public MiningRecipeKubeRecipe overworld(Context cx) {
        captureFallbackSourceLine(KubeJSSourceLine.expressionStart(SourceLine.of(cx)));
        return overworld();
    }

    @HideFromJS
    public MiningRecipeKubeRecipe overworld() {
        setValue(MiningRecipeKubeSchema.DIMENSION, Level.OVERWORLD);
        return this;
    }

    @Info("Makes this recipe functional in the nether. A recipe can only target a single dimension.")
    public MiningRecipeKubeRecipe nether(Context cx) {
        captureFallbackSourceLine(KubeJSSourceLine.expressionStart(SourceLine.of(cx)));
        return nether();
    }

    @HideFromJS
    public MiningRecipeKubeRecipe nether() {
        setValue(MiningRecipeKubeSchema.DIMENSION, Level.NETHER);
        return this;
    }

    @Info("""
            Deposits that run out of resources will be replaced with this block. Does not have to be a deposit block.
            Note that finite deposits must be enabled in the server config for this setting to take effect.
            """)
    public MiningRecipeKubeRecipe replaceWhenDepleted(Context cx, String blockId) {
        captureFallbackSourceLine(KubeJSSourceLine.expressionStart(SourceLine.of(cx)));
        return replaceWhenDepleted(blockId);
    }

    @HideFromJS
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
    public MiningRecipeKubeRecipe durability(Context cx, long core, long edge, float randomSpread) {
        var coarseSourceLine = SourceLine.of(cx);
        return durability(core, edge, randomSpread, KubeJSSourceLine.expressionStart(coarseSourceLine),
                KubeJSSourceLine.methodCall(coarseSourceLine, "durability"));
    }

    @HideFromJS
    public MiningRecipeKubeRecipe durability(long core, long edge, float randomSpread) {
        return durability(core, edge, randomSpread, SourceLine.UNKNOWN, SourceLine.UNKNOWN);
    }

    MiningRecipeKubeRecipe durability(long core, long edge, float randomSpread, SourceLine errorSourceLine) {
        return durability(core, edge, randomSpread, errorSourceLine, errorSourceLine);
    }

    private MiningRecipeKubeRecipe durability(
            long core, long edge, float randomSpread, SourceLine fallbackSourceLine, SourceLine errorSourceLine
    ) {
        captureFallbackSourceLine(fallbackSourceLine);
        if (!errorSourceLine.isUnknown()
                && (core <= 0 || edge <= 0 || Float.isNaN(randomSpread) || randomSpread < 0 || randomSpread > 1)) {
            captureErrorSourceLine(errorSourceLine);
        }

        setValue(MiningRecipeKubeSchema.DURABILITY, MiningRecipeKubeSchema.durability(core, edge, randomSpread));
        return this;
    }

    @Info("Adds a yield entry to this mining recipe. Each yield is independent of other yields.")
    public MiningRecipeKubeRecipe yield(Context cx, Consumer<YieldKubeBuilder> yield) {
        return this.yield(yield, KubeJSSourceLine.expressionStart(SourceLine.of(cx)));
    }

    @HideFromJS
    public MiningRecipeKubeRecipe yield(Consumer<YieldKubeBuilder> yield) {
        return this.yield(yield, SourceLine.UNKNOWN);
    }

    MiningRecipeKubeRecipe yield(Consumer<YieldKubeBuilder> yield, SourceLine callSourceLine) {
        captureFallbackSourceLine(callSourceLine);
        var builder = new YieldKubeBuilder();
        yield.accept(builder);
        if (!builder.hasItems()) return this;

        var itemErrorSourceLine = builder.recipeErrorSourceLine();
        captureErrorSourceLine(itemErrorSourceLine);

        var yields = getValue(MiningRecipeKubeSchema.YIELDS);
        var mutableYields = yields == null
                ? new ArrayList<List<CustomObjectRecipeComponent.Value>>()
                : new ArrayList<>(yields);
        mutableYields.add(builder.build());
        setValue(MiningRecipeKubeSchema.YIELDS, mutableYields);
        return this;
    }

    private void captureFallbackSourceLine(SourceLine callSourceLine) {
        if (hasFallbackSourceLine || callSourceLine.isUnknown()) return;

        sourceLine = callSourceLine;
        hasFallbackSourceLine = true;
    }

    private void captureErrorSourceLine(SourceLine callSourceLine) {
        if (hasErrorSourceLine || callSourceLine.isUnknown()) return;

        sourceLine = callSourceLine;
        hasErrorSourceLine = true;
    }

    private RecipeScriptContext recipeContext() {
        return new RecipeScriptContext.Impl(null, this);
    }

    private static String normalizeId(String id) {
        return id.contains(":") ? id : "minecraft:" + id;
    }
}
