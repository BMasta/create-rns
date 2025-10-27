package com.bmaster.createrns.compat.jei;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.RNSRecipeTypes;
import com.bmaster.createrns.mining.MiningLevel;
import com.bmaster.createrns.mining.recipe.MiningRecipe;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.compat.jei.ItemIcon;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.library.gui.elements.DrawableBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MiningRecipeCategory extends CreateRecipeCategory<MiningRecipe> {
    public static final RecipeType<RecipeHolder<MiningRecipe>> JEI_RECIPE_TYPE = RecipeType.createRecipeHolderType(
            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "mining"));
    private static final Info<MiningRecipe> INFO = new Info<>(
            JEI_RECIPE_TYPE, Component.translatable(CreateRNS.MOD_ID + ".recipe.mining"),
            new EmptyBackground(177, 90),
            new ItemIcon(() -> new ItemStack(RNSContent.MINER_MK1_BLOCK)),
            (() -> {
                var level = Minecraft.getInstance().level;
                if (level == null) return List.of();
                return level.getRecipeManager().getAllRecipesFor(RNSRecipeTypes.MINING_RECIPE_TYPE.get());
            }),
            List.of(() -> new ItemStack(RNSContent.MINER_MK1_BLOCK.get().asItem()))
    );

    private static final IDrawable BASIC_SLOT = asDrawable(AllGuiTextures.JEI_SLOT);
    private static final AnimatedMiner MINER = new AnimatedMiner(RNSContent.MINER_MK1_BLOCK.get(), RNSContent.MINER_MK1_DRILL);
    private static IDrawable TIER_ICON = null;

    public MiningRecipeCategory(IGuiHelper gui) {
        super(INFO);
        TIER_ICON = new DrawableBuilder(
                ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "textures/gui/tier_icon.png"),
                0, 0, 16, 16
        ).setTextureSize(16, 16).build();
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
    protected List<Component> getTooltipStrings(MiningRecipe recipe, IRecipeSlotsView recipeSlotsView,
                                                double mouseX, double mouseY) {
        if (32 < mouseX && mouseX < 53 && 0 <= mouseY && mouseY < 14) {
            return List.of(Component.literal("Required mining tier"));
        }
        return List.of();
    }

    @Override
    public void draw(MiningRecipe r, IRecipeSlotsView rsv, GuiGraphics gui, double mX, double mY) {
        MINER.draw(gui, 25, 45);
        AllGuiTextures.JEI_SHADOW.render(gui, 15, 67);
        AllGuiTextures.JEI_DOWN_ARROW.render(gui, 85, 23);

        gui.pose().pushPose();
        gui.pose().translate(35, 2.7, 0);
        gui.pose().scale(0.6f, 0.6f, 1f);
        int ic = Objects.requireNonNull(ChatFormatting.GRAY.getColor());
        gui.setColor(((ic >>> 16) & 0xff) / 255f, ((ic >>> 8) & 0xff) / 255f, (ic & 0xff) / 255f, 1);
        TIER_ICON.draw(gui);
        gui.setColor(1, 1, 1, 1);
        gui.pose().popPose();

        gui.drawString(Minecraft.getInstance().font, Integer.toString(r.getTier()), 45, 4,
                Objects.requireNonNull(ChatFormatting.GRAY.getColor()), false);
    }
}
