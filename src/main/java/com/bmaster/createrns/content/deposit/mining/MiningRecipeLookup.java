package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSRecipeTypes;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystHandler;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
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

    public static boolean isDepositMineable(Level l, Block depositBlock, Set<Catalyst> catalysts) {
        if (depBlockToRecipe == null) build(l);
        var recipe = depBlockToRecipe.get(depositBlock);
        if (recipe == null) return false;

        return new CatalystHandler(l.registryAccess(), recipe, catalysts).isMiningPossible();
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
