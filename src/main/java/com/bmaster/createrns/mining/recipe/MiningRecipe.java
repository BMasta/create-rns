package com.bmaster.createrns.mining.recipe;

import com.bmaster.createrns.RNSRecipeTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MiningRecipe implements Recipe<SingleRecipeInput> {
    private final Block depositBlock;
    private final int tier;
    private final Item yield;

    public MiningRecipe(Block depositBlock, int tier, Item yield) {
        this.depositBlock = depositBlock;
        this.tier = tier;
        this.yield = yield;
    }

    public Block getDepositBlock() {
        return depositBlock;
    }

    public int getTier() {
        return tier;
    }

    public Item getYield() {
        return yield;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return new ItemStack(yield);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, Ingredient.of(depositBlock));
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean matches(SingleRecipeInput singleRecipeInput, Level level) {
        return singleRecipeInput.item().is(getDepositBlock().asItem());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput singleRecipeInput, HolderLookup.Provider provider) {
        return new ItemStack(getYield());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MiningRecipe.Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return RNSRecipeTypes.MINING_RECIPE_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<MiningRecipe> {
        public static MiningRecipe.Serializer INSTANCE = new MiningRecipe.Serializer();

        public final MapCodec<MiningRecipe> CODEC;
        public final StreamCodec<RegistryFriendlyByteBuf, MiningRecipe> STREAM_CODEC;

        public Serializer() {
            CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("deposit_block").forGetter(MiningRecipe::getDepositBlock),
                            Codec.INT.fieldOf("tier").forGetter(MiningRecipe::getTier),
                            BuiltInRegistries.ITEM.byNameCodec().fieldOf("yield").forGetter(MiningRecipe::getYield))
                    .apply(i, MiningRecipe::new));
            STREAM_CODEC = StreamCodec.of(this::toNetwork, this::fromNetwork);
        }

        public void toNetwork(RegistryFriendlyByteBuf buffer, MiningRecipe recipe) {
            ByteBufCodecs.registry(Registries.BLOCK).encode(buffer, recipe.getDepositBlock());
            ByteBufCodecs.INT.encode(buffer, recipe.tier);
            ByteBufCodecs.registry(Registries.ITEM).encode(buffer, recipe.getYield());
        }

        public MiningRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            return new MiningRecipe(
                    ByteBufCodecs.registry(Registries.BLOCK).decode(buffer),
                    ByteBufCodecs.INT.decode(buffer),
                    ByteBufCodecs.registry(Registries.ITEM).decode(buffer)
            );
        }

        @Override
        public MapCodec<MiningRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MiningRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
