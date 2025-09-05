package com.bmaster.createrns.compat.jei;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.RNSRecipes;
import com.bmaster.createrns.mining.MiningRecipe;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.compat.jei.ItemIcon;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MiningRecipeCategory extends CreateRecipeCategory<MiningRecipe> {
    public static final RecipeType<MiningRecipe> MINING_RECIPE_TYPE = RecipeType.create(CreateRNS.MOD_ID,
            "mining", MiningRecipe.class);

    private static final IDrawable BASIC_SLOT = asDrawable(AllGuiTextures.JEI_SLOT);
    private static final Info<MiningRecipe> info = new Info<>(
            MINING_RECIPE_TYPE, Component.translatable("%s.recipe.mining.basic".formatted(CreateRNS.MOD_ID)),
            new EmptyBackground(177, 90),
            new ItemIcon(() -> new ItemStack(RNSContent.MINER_MK1_BLOCK)),
            (() -> {
                var level = Minecraft.getInstance().level;
                if (level == null) return List.of();
                return level.getRecipeManager().getAllRecipesFor(RNSRecipes.MINING_TYPE.get());
            }),
            List.of(() -> new ItemStack(RNSContent.MINER_MK1_BLOCK.get().asItem()),
                    () -> new ItemStack(RNSContent.MINER_MK2_BLOCK.get().asItem()))
    );
    private final AnimatedMiner miner = new AnimatedMiner(RNSContent.MINER_MK1_BLOCK.get(), RNSContent.MINER_MK1_DRILL);

    public MiningRecipeCategory() {
        super(info);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MiningRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 34, 60)
                .addItemStack(new ItemStack(recipe.getDepositBlock().asItem()));

        // TODO: When multiple yields are implemented, draw n slots programmatically
        builder.addSlot(RecipeIngredientRole.OUTPUT, 90, 45)
                .setBackground(BASIC_SLOT, -1, -1)
                .addItemStack(new ItemStack(recipe.getYield()));
    }

    @Override
    public void draw(MiningRecipe r, IRecipeSlotsView rsv, GuiGraphics gui, double mX, double mY) {
        miner.draw(gui, 25, 45);
        AllGuiTextures.JEI_SHADOW.render(gui, 15, 67);
        AllGuiTextures.JEI_DOWN_ARROW.render(gui, 85, 23);
    }
}
