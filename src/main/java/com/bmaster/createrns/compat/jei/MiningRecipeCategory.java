package com.bmaster.createrns.compat.jei;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.RNSRecipes;
import com.bmaster.createrns.mining.recipe.MiningRecipe;
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
public class MiningRecipeCategory<R extends MiningRecipe> extends CreateRecipeCategory<R> {
    private static final IDrawable BASIC_SLOT = asDrawable(AllGuiTextures.JEI_SLOT);
    private final AnimatedMiner miner = new AnimatedMiner(RNSContent.MINER_MK1_BLOCK.get(), RNSContent.MINER_MK1_DRILL);

    public MiningRecipeCategory(Info<R> info) {
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
