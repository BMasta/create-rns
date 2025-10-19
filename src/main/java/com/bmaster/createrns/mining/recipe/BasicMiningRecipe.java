package com.bmaster.createrns.mining.recipe;

import com.bmaster.createrns.RNSRecipeTypes;
import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class BasicMiningRecipe extends MiningRecipe {
//    public static final MapCodec<BasicMiningRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
//            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("depositBlock").forGetter(BasicMiningRecipe::getDepositBlock),
//            BuiltInRegistries.ITEM.byNameCodec().fieldOf("yield").forGetter(BasicMiningRecipe::getYield))
//    .apply(instance, BasicMiningRecipe::new));
//
//    public static final StreamCodec<RegistryFriendlyByteBuf, BasicMiningRecipe> STREAM_CODEC =
//            StreamCodec.composite(
//                    ByteBufCodecs.registry(Registries.BLOCK), BasicMiningRecipe::getDepositBlock,
//                    ByteBufCodecs.registry(Registries.ITEM), BasicMiningRecipe::getYield,
//                    BasicMiningRecipe::new
//            );

    public static final MapCodec<BasicMiningRecipe> CODEC = MiningRecipe.newCodec(BasicMiningRecipe.class);
    public static final StreamCodec<RegistryFriendlyByteBuf, BasicMiningRecipe> STREAM_CODEC =
            MiningRecipe.newStreamCodec(BasicMiningRecipe.class);

    public BasicMiningRecipe(Block depositBlock, Item yield) {
        super(depositBlock, yield);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return RNSRecipeTypes.BASIC_MINING_TYPE.get();
    }

    @SuppressWarnings("SameParameterValue")
    @MethodsReturnNonnullByDefault
    @ParametersAreNonnullByDefault
    public enum Serializer implements RecipeSerializer<BasicMiningRecipe> {
        INSTANCE;

        @Override
        public MapCodec<BasicMiningRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BasicMiningRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
