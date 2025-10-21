package com.bmaster.createrns.mining.recipe;

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
import java.util.function.BiFunction;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class MiningRecipe implements Recipe<SingleRecipeInput> {
    private final Block depositBlock;
    private final Item yield;

    public MiningRecipe(Block depositBlock, Item yield) {
        this.depositBlock = depositBlock;
        this.yield = yield;
    }

    public Block getDepositBlock() {
        return depositBlock;
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

    public static abstract class Serializer<R extends MiningRecipe> implements RecipeSerializer<R> {
        public final MapCodec<R> CODEC;
        public final StreamCodec<RegistryFriendlyByteBuf, R> STREAM_CODEC;
        private final BiFunction<Block,Item,R> recipeFactory;

        public Serializer(BiFunction<Block,Item,R> recipeFactory) {
            this.recipeFactory = recipeFactory;
            CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("deposit_block").forGetter(MiningRecipe::getDepositBlock),
                            BuiltInRegistries.ITEM.byNameCodec().fieldOf("yield").forGetter(MiningRecipe::getYield))
                    .apply(i, recipeFactory));
            STREAM_CODEC = StreamCodec.of(this::toNetwork, this::fromNetwork);
        }

        public void toNetwork(RegistryFriendlyByteBuf buffer, R recipe) {
            ByteBufCodecs.registry(Registries.BLOCK).encode(buffer, recipe.getDepositBlock());
            ByteBufCodecs.registry(Registries.ITEM).encode(buffer, recipe.getYield());
        }

        public R fromNetwork(RegistryFriendlyByteBuf buffer) {
            return recipeFactory.apply(
                    ByteBufCodecs.registry(Registries.BLOCK).decode(buffer),
                    ByteBufCodecs.registry(Registries.ITEM).decode(buffer)
            );
        }

        @Override
        public MapCodec<R> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
