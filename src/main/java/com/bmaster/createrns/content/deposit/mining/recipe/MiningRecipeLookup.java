package com.bmaster.createrns.content.deposit.mining.recipe;

import com.bmaster.createrns.RNSRecipeTypes;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystHandler;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MiningRecipeLookup {
    private static Object2ObjectOpenHashMap<Block, Object2ObjectOpenHashMap<ResourceKey<Level>, MiningRecipe>> depBlockToDimToRecipe;
    private static List<ResourceKey<Level>> allDimensions = null;

    public static @Nullable MiningRecipe find(Level l, Block depositBlock) {
        MiningRecipe result = null;
        if (depBlockToDimToRecipe == null) build(l);
        var recipes = depBlockToDimToRecipe.get(depositBlock);
        if (recipes != null) result = recipes.get(l.dimension());

        // If there aren't any candidates for the current dimension,
        // fall back to any other dimension for which the recipe is defined.
        if (result == null && recipes != null && !recipes.isEmpty()) {
            var resolvedDim = recipes.keySet().stream()
                    .min(Comparator.comparing(d -> d.location().toString()))
                    .orElse(null);
            if (resolvedDim != null) result = recipes.get(resolvedDim);
        }
        return result;
    }

    public static List<ResourceKey<Level>> getAllRelevantDimensions(Level l) {
        if (allDimensions == null) build(l);
        return allDimensions;
    }

    public static boolean isDepositMineable(Level l, Block depositBlock, Set<Catalyst> catalysts) {
        var recipe = find(l, depositBlock);
        if (recipe == null) return false;
        return CatalystHandler.isMiningPossible(l.registryAccess(), recipe, catalysts);
    }

    public static void build(Level l) {
        var recipes = l.getRecipeManager().getAllRecipesFor(RNSRecipeTypes.MINING_RECIPE_TYPE.get());
        depBlockToDimToRecipe = recipes.stream()
                .map(RecipeHolder::value)
                .filter(r -> r.initialize(l.registryAccess()))
                .collect(Collectors.toMap(
                        MiningRecipe::getDepositBlock,
                        r -> {
                            var res = new Object2ObjectOpenHashMap<ResourceKey<Level>, MiningRecipe>();
                            res.put(r.getDimension(), r);
                            return res;
                        },
                        (o, n) -> {
                            for (var e : n.object2ObjectEntrySet()) {
                                var existing = o.put(e.getKey(), e.getValue());
                                if (existing != null) {
                                    throw new IllegalStateException("Found existing mining recipe with the same " +
                                            "deposit block and dimension: " + e.getValue().getDepositBlock() + ", " +
                                            e.getValue().getDimension());
                                }
                            }
                            return o;
                        },
                        Object2ObjectOpenHashMap::new));

        allDimensions = depBlockToDimToRecipe.values().stream()
                .flatMap(dToR -> dToR.keySet().stream())
                .distinct()
                .sorted(Comparator.comparing(d -> d.location().toString()))
                .collect(Collectors.toList());
    }
}
