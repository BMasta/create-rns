package com.bmaster.createrns.mining.recipe;

import com.bmaster.createrns.RNSRecipeTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class AdvancedMiningRecipe extends MiningRecipe {
    public AdvancedMiningRecipe(Block depositBlock, Item yield) {
        super(depositBlock, yield);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return RNSRecipeTypes.ADVANCED_MINING_TYPE.get();
    }

    public static class Serializer extends MiningRecipe.Serializer<AdvancedMiningRecipe> {
        public static AdvancedMiningRecipe.Serializer INSTANCE = new AdvancedMiningRecipe.Serializer();

        private Serializer() {
            super(AdvancedMiningRecipe::new);
        }
    }
}
