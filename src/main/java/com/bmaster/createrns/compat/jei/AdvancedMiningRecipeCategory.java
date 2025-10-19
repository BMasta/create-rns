//package com.bmaster.createrns.compat.jei;
//
//import com.bmaster.createrns.CreateRNS;
//import com.bmaster.createrns.RNSContent;
//import com.bmaster.createrns.RNSRecipeTypes;
//import com.bmaster.createrns.mining.MiningLevel;
//import com.bmaster.createrns.mining.recipe.AdvancedMiningRecipe;
//import com.simibubi.create.compat.jei.EmptyBackground;
//import com.simibubi.create.compat.jei.ItemIcon;
//import mezz.jei.api.recipe.RecipeType;
//import net.minecraft.MethodsReturnNonnullByDefault;
//import net.minecraft.client.Minecraft;
//import net.minecraft.world.item.ItemStack;
//
//import java.util.Collections;
//import java.util.List;
//
//@MethodsReturnNonnullByDefault
//public class AdvancedMiningRecipeCategory extends MiningRecipeCategory<AdvancedMiningRecipe> {
//    public static final RecipeType<AdvancedMiningRecipe> JEI_RECIPE_TYPE = RecipeType.create(CreateRNS.MOD_ID,
//            MiningLevel.ADVANCED.getRecipeID(), AdvancedMiningRecipe.class);
//
//    private final AnimatedMiner miner = new AnimatedMiner(RNSContent.MINER_MK2_BLOCK.get(), RNSContent.MINER_MK2_DRILL);
//    private static final Info<AdvancedMiningRecipe> info = new Info<>(
//            JEI_RECIPE_TYPE, MiningLevel.ADVANCED.getTitle(),
//            new EmptyBackground(177, 90),
//            new ItemIcon(() -> new ItemStack(RNSContent.MINER_MK2_BLOCK)),
//            (() -> {
//                var level = Minecraft.getInstance().level;
//                if (level == null) return Collections.emptyList();
//                return level.getRecipeManager().getAllRecipesFor(RNSRecipeTypes.ADVANCED_MINING_TYPE.get());
//            }),
//            List.of(() -> new ItemStack(RNSContent.MINER_MK2_BLOCK.get().asItem()))
//    );
//
//    public AdvancedMiningRecipeCategory() {
//        super(info);
//    }
//
//    @Override
//    public AnimatedMiner getAnimatedMiner() {
//        return miner;
//    }
//}
