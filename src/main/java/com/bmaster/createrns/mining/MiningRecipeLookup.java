package com.bmaster.createrns.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSRecipeTypes;
import com.bmaster.createrns.mining.recipe.MiningRecipe;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

public class MiningRecipeLookup {
    private static Object2ObjectOpenHashMap<Block, MiningRecipe> depBlockToRecipe;

    public static @Nullable MiningRecipe find(Level l, Block depositBlock) {
        if (depBlockToRecipe == null) build(l);
        var res = depBlockToRecipe.get(depositBlock);
        if (res == null) {
            CreateRNS.LOGGER.error("Could not get mining recipe for deposit block {}",
                    ForgeRegistries.BLOCKS.getKey(depositBlock));
        }
        return res;
    }

    public static boolean isDepositMineable(Level l, Block depositBlock, int minerTier) {
        if (depBlockToRecipe == null) build(l);
        var recipe = depBlockToRecipe.get(depositBlock);
        if (recipe == null) return false;
        return minerTier >= recipe.getTier();
    }

    public static void build(Level l) {
        var recipes = l.getRecipeManager().getAllRecipesFor(RNSRecipeTypes.MINING_RECIPE_TYPE.get());
        depBlockToRecipe = recipes.stream().collect(Collectors.toMap(
                MiningRecipe::getDepositBlock,
                r -> r,
                (o, n) -> n,
                Object2ObjectOpenHashMap::new));
    }
}
