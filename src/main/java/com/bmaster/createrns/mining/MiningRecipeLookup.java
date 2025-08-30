package com.bmaster.createrns.mining;

import com.bmaster.createrns.RNSRecipes;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.stream.Collectors;

public class MiningRecipeLookup {
    private static Object2ObjectOpenHashMap<Block, Item> depBlockToYield;

    public static Item getYield(Level l, Block depositBlock) {
        if (depBlockToYield == null) build(l);
        return depBlockToYield.get(depositBlock);
    }

    public static void build(Level l) {
        var recipes = l.getRecipeManager().getAllRecipesFor(RNSRecipes.MINING_TYPE.get());
        depBlockToYield = recipes.stream().collect(Collectors.toMap(
                MiningRecipe::getDepositBlock,
                MiningRecipe::getYield,
                (o, n) -> n,
                Object2ObjectOpenHashMap::new));
    }
}
