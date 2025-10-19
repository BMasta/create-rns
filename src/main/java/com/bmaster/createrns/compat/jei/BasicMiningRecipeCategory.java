//package com.bmaster.createrns.compat.jei;
//
//import com.bmaster.createrns.CreateRNS;
//import com.bmaster.createrns.RNSContent;
//import com.bmaster.createrns.RNSRecipeTypes;
//import com.bmaster.createrns.mining.MiningLevel;
//import com.bmaster.createrns.mining.recipe.BasicMiningRecipe;
//import com.simibubi.create.compat.jei.EmptyBackground;
//import com.simibubi.create.compat.jei.ItemIcon;
//import mezz.jei.api.recipe.RecipeType;
//import net.minecraft.MethodsReturnNonnullByDefault;
//import net.minecraft.client.Minecraft;
//import net.minecraft.world.item.ItemStack;
//
//import java.util.List;
//
//@MethodsReturnNonnullByDefault
//public class BasicMiningRecipeCategory extends MiningRecipeCategory<BasicMiningRecipe> {
//    public static final RecipeType<BasicMiningRecipe> JEI_RECIPE_TYPE = RecipeType.create(CreateRNS.MOD_ID,
//            MiningLevel.BASIC.getRecipeID(), BasicMiningRecipe.class);
//
//    private final AnimatedMiner miner = new AnimatedMiner(RNSContent.MINER_MK1_BLOCK.get(), RNSContent.MINER_MK1_DRILL);
//    private static final Info<BasicMiningRecipe> info = new Info<>(
//            JEI_RECIPE_TYPE, MiningLevel.BASIC.getTitle(),
//            new EmptyBackground(177, 90),
//            new ItemIcon(() -> new ItemStack(RNSContent.MINER_MK1_BLOCK)),
//            (() -> {
//                var level = Minecraft.getInstance().level;
//                if (level == null) return List.of();
//                return level.getRecipeManager().getAllRecipesFor(RNSRecipeTypes.BASIC_MINING_TYPE.get());
//            }),
//            List.of(() -> new ItemStack(RNSContent.MINER_MK1_BLOCK.get().asItem()))
//    );
//
//    public BasicMiningRecipeCategory() {
//        super(info);
//    }
//
//    @Override
//    public AnimatedMiner getAnimatedMiner() {
//        return miner;
//    }
//}
