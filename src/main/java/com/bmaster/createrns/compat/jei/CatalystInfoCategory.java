package com.bmaster.createrns.compat.jei;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSBlocks;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.simibubi.create.compat.jei.ItemIcon;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CatalystInfoCategory implements IRecipeCategory<CatalystRequirementSet> {
    public static final RecipeType<CatalystRequirementSet> JEI_RECIPE_TYPE =
            new RecipeType<>(CreateRNS.asResource("catalyst_info"), CatalystRequirementSet.class);
    public static final List<Supplier<? extends ItemStack>> CATALYSTS = List.of(
            () -> new ItemStack(RNSBlocks.RESONATOR_BLOCK.get()),
            () -> new ItemStack(RNSBlocks.SHATTERING_RESONATOR_BLOCK.get()),
            () -> new ItemStack(RNSBlocks.STABILIZING_RESONATOR_BLOCK.get())
    );

    private static final int WIDTH = 160;
    private static final int HEIGHT = 100;

    private final IDrawable icon;

    public CatalystInfoCategory() {
        this.icon = new ItemIcon(() -> new ItemStack(RNSBlocks.RESONATOR_BLOCK));
    }

    @Override
    public RecipeType<CatalystRequirementSet> getRecipeType() {
        return JEI_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return CreateRNS.translatable("recipe.catalyst_info");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CatalystRequirementSet recipe, IFocusGroup focuses) {
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, CatalystRequirementSet recipe, IFocusGroup focuses) {
        builder.addText(recipe.getNameComponent(), WIDTH, 12)
                .setPosition(0, 0);

        Component description = CreateRNS.translatable("catalyst." + recipe.name + ".description");
        builder.addText(description, WIDTH, HEIGHT - 16)
                .setPosition(0, 16)
                .setColor(Objects.requireNonNull(ChatFormatting.DARK_GRAY.getColor()));
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(CatalystRequirementSet recipe) {
        return CreateRNS.asResource("catalyst/" + recipe.name);
    }
}
