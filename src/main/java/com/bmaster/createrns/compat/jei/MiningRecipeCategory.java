package com.bmaster.createrns.compat.jei;

import com.bmaster.createrns.*;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.util.FlexibleLayoutHelper;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.compat.jei.ItemIcon;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.createmod.catnip.layout.LayoutHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MiningRecipeCategory extends CreateRecipeCategory<MiningRecipe> {
    public static final RecipeType<MiningRecipe> JEI_RECIPE_TYPE = RecipeType.create(
            CreateRNS.MOD_ID, "mining", MiningRecipe.class);
    private static final Info<MiningRecipe> INFO = new Info<>(
            JEI_RECIPE_TYPE, Component.translatable(CreateRNS.MOD_ID + ".recipe.mining"),
            new EmptyBackground(177, 90),
            new ItemIcon(() -> new ItemStack(RNSBlocks.MINER_MK1_BLOCK)),
            (() -> {
                var level = Minecraft.getInstance().level;
                if (level == null) return List.of();
                return level.getRecipeManager().getAllRecipesFor(RNSRecipeTypes.MINING_RECIPE_TYPE.get());
            }),
            List.of(() -> new ItemStack(RNSBlocks.MINER_MK1_BLOCK.get().asItem()))
    );

    private static final IDrawable BASIC_SLOT = asDrawable(AllGuiTextures.JEI_SLOT);
    private static final IDrawable CHANCE_SLOT = asDrawable(AllGuiTextures.JEI_CHANCE_SLOT);
    private static final AnimatedMiner MINER = new AnimatedMiner(RNSBlocks.MINER_MK1_BLOCK.get(), RNSPartialModels.MINER_MK1_DRILL);

    private static final int SLOTS_PER_YIELD_ROW = 5;
    private static final int MAX_YIELD_ROWS = 3;
    private static final int MAX_YIELD_SLOTS = SLOTS_PER_YIELD_ROW * MAX_YIELD_ROWS;

    public MiningRecipeCategory(IGuiHelper gui) {
        super(INFO);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MiningRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 34, 60)
                .addItemStack(new ItemStack(recipe.getDepositBlock().asItem()));

        var slots = layoutOutput(recipe);
        var bg = (slots.size() == 1) ? BASIC_SLOT : CHANCE_SLOT;
        if (slots.size() > MAX_YIELD_SLOTS) slots = slots.subList(0, MAX_YIELD_SLOTS);
        for (var slot : slots) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 120 + slot.x, 52 + slot.y)
                    .setBackground(bg, -1, -1)
                    .addRichTooltipCallback(CreateRecipeCategory.addStochasticTooltip(slot.output))
                    .addItemStack(slot.output.getStack());
        }
    }

    @Override
    public void draw(MiningRecipe r, IRecipeSlotsView rsv, GuiGraphics gui, double mX, double mY) {
        var yields = r.getYields().stream().flatMap(y -> y.items.stream()).toList();
        if (yields.size() > MAX_YIELD_SLOTS) yields = yields.subList(0, MAX_YIELD_SLOTS);
        var rows = Mth.clamp((yields.size() - 1) / SLOTS_PER_YIELD_ROW + 1, 1, MAX_YIELD_ROWS);

        MINER.draw(gui, 25, 45);
        AllGuiTextures.JEI_SHADOW.render(gui, 15, 67);
        AllGuiTextures.JEI_DOWN_ARROW.render(gui, 106, 5 + 9 * (MAX_YIELD_ROWS - rows));

        gui.pose().pushPose();
        gui.pose().translate(35, 2.7, 0);
        gui.pose().scale(0.6f, 0.6f, 1f);
        int ic = Objects.requireNonNull(ChatFormatting.GRAY.getColor());
        gui.setColor(((ic >>> 16) & 0xff) / 255f, ((ic >>> 8) & 0xff) / 255f, (ic & 0xff) / 255f, 1);
        gui.setColor(1, 1, 1, 1);
        gui.pose().popPose();
    }

    private List<LayoutEntry> layoutOutput(MiningRecipe recipe) {
        var yields = recipe.getYields().stream().flatMap(y -> y.items.stream()).toList();
        if (yields.size() > MAX_YIELD_SLOTS) yields = yields.subList(0, MAX_YIELD_SLOTS);
        var size = yields.size();
        var rows = Mth.clamp((size - 1) / SLOTS_PER_YIELD_ROW + 1, 1, MAX_YIELD_ROWS);

        LayoutHelper layout = new FlexibleLayoutHelper(size, rows, 18, 18, 1, true, true);
        List<LayoutEntry> positions = new ArrayList<>(size);
        for (var y : yields) {
            var output = new ProcessingOutput(new ItemStack(y.item()), 1f);
            positions.add(new LayoutEntry(output, layout.getX(), layout.getY()));
            layout.next();
        }

        return positions;
    }

    private record LayoutEntry(
            ProcessingOutput output,
            int x,
            int y
    ) {}
}
