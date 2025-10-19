//package com.bmaster.createrns.mining;
//
//import com.bmaster.createrns.RNSRecipeTypes;
//import com.bmaster.createrns.mining.recipe.AdvancedMiningRecipe;
//import com.bmaster.createrns.mining.recipe.BasicMiningRecipe;
//import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
//import net.minecraft.world.item.Item;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.Block;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.stream.Collectors;
//
//public class MiningRecipeLookup {
//    private static Object2ObjectOpenHashMap<Block, Item> depBlockToYieldBasic;
//    private static Object2ObjectOpenHashMap<Block, Item> depBlockToYieldAdvanced;
//
//    public static @Nullable Item getYield(Level l, MiningLevel ml, Block depositBlock) {
//        if (depBlockToYieldBasic == null) build(l);
//        var res = depBlockToYieldBasic.get(depositBlock);
//        return (res != null) ? res : depBlockToYieldAdvanced.get(depositBlock);
//    }
//
//    @SuppressWarnings("RedundantIfStatement")
//    public static boolean isDepositMineable(Level l, Block depositBlock, MiningLevel ml) {
//        if (depBlockToYieldBasic == null) build(l);
//        if (ml.getLevel() >= MiningLevel.BASIC.getLevel() && depBlockToYieldBasic.containsKey(depositBlock)) {
//            return true;
//        }
//        if (ml.getLevel() >= MiningLevel.ADVANCED.getLevel() && depBlockToYieldAdvanced.containsKey(depositBlock)) {
//            return true;
//        }
//        return false;
//    }
//
//    public static void build(Level l) {
//        var basicRecipes = l.getRecipeManager().getAllRecipesFor(RNSRecipeTypes.BASIC_MINING_TYPE.get());
//        var advancedRecipes = l.getRecipeManager().getAllRecipesFor(RNSRecipeTypes.ADVANCED_MINING_TYPE.get());
//        depBlockToYieldBasic = basicRecipes.stream().collect(Collectors.toMap(
//                BasicMiningRecipe::getDepositBlock,
//                BasicMiningRecipe::getYield,
//                (o, n) -> n,
//                Object2ObjectOpenHashMap::new));
//        depBlockToYieldAdvanced = advancedRecipes.stream().collect(Collectors.toMap(
//                AdvancedMiningRecipe::getDepositBlock,
//                AdvancedMiningRecipe::getYield,
//                (o, n) -> n,
//                Object2ObjectOpenHashMap::new));
//    }
//}
