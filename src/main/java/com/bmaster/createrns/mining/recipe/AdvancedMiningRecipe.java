package com.bmaster.createrns.mining.recipe;

import com.bmaster.createrns.RNSRecipeTypes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class AdvancedMiningRecipe extends MiningRecipe {
    public static final MapCodec<AdvancedMiningRecipe> CODEC = MiningRecipe.newCodec(AdvancedMiningRecipe.class);
    public static final StreamCodec<RegistryFriendlyByteBuf, AdvancedMiningRecipe> STREAM_CODEC =
            MiningRecipe.newStreamCodec(AdvancedMiningRecipe.class);

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

    @SuppressWarnings("SameParameterValue")
    @MethodsReturnNonnullByDefault
    @ParametersAreNonnullByDefault
    public enum Serializer implements RecipeSerializer<AdvancedMiningRecipe> {
        INSTANCE;

        @Override
        public MapCodec<AdvancedMiningRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AdvancedMiningRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
