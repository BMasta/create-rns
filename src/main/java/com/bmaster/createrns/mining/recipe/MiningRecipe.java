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
import java.lang.reflect.InvocationTargetException;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class MiningRecipe implements Recipe<SingleRecipeInput> {
    public static <T extends MiningRecipe> MapCodec<T> newCodec(Class<T> recipeClass) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                BuiltInRegistries.BLOCK.byNameCodec().fieldOf("depositBlock").forGetter(MiningRecipe::getDepositBlock),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("yield").forGetter(MiningRecipe::getYield)
        ).apply(instance, (Block d, Item y) -> {
            try {
                return recipeClass.getDeclaredConstructor(Block.class, Item.class).newInstance(d, y);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public static <T extends MiningRecipe> StreamCodec<RegistryFriendlyByteBuf, T> newStreamCodec(Class<T> recipeClass) {
        return StreamCodec.composite(
                ByteBufCodecs.registry(Registries.BLOCK), MiningRecipe::getDepositBlock,
                ByteBufCodecs.registry(Registries.ITEM), MiningRecipe::getYield,
                (d, y) -> {
                    try {
                        return recipeClass.getDeclaredConstructor(Block.class, Item.class).newInstance(d, y);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

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
}
