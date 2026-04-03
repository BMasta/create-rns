package com.bmaster.createrns.content.deposit.mining.recipe;

import com.bmaster.createrns.RNSRecipeTypes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MiningRecipe implements Recipe<SingleRecipeInput> {
    private static final DepositDurability DEFAULT_DURABILITY = new DepositDurability(0, 0, 0);

    public static final MapCodec<MiningRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    BuiltInRegistries.BLOCK.byNameCodec().fieldOf("deposit_block")
                            .forGetter(MiningRecipe::getDepositBlock),
                    ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension", Level.OVERWORLD)
                            .forGetter(MiningRecipe::getDimension),
                    BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("replace_when_depleted", Blocks.AIR)
                            .forGetter(MiningRecipe::getReplacementBlock),
                    DepositDurability.CODEC.optionalFieldOf("durability", DEFAULT_DURABILITY)
                            .forGetter(MiningRecipe::getDurability),
                    Yield.CODEC.listOf().fieldOf("yields")
                            .forGetter(MiningRecipe::getYields))
            .apply(i, MiningRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MiningRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.BLOCK), r -> r.depositBlock,
            ResourceKey.streamCodec(Registries.DIMENSION), r -> r.dimension,
            ByteBufCodecs.registry(Registries.BLOCK), r -> r.replacementBlock,
            DepositDurability.STREAM_CODEC, r -> r.dur,
            ByteBufCodecs.collection(ArrayList::new, Yield.STREAM_CODEC), r -> new ArrayList<>(r.yields),
            MiningRecipe::new);

    private final Block depositBlock;
    private final ResourceKey<Level> dimension;
    private final Block replacementBlock;
    private final DepositDurability dur;
    private List<Yield> yields;
    private boolean isInitialized = false;

    public MiningRecipe(
            Block depositBlock, ResourceKey<Level> dimension, Block replacementBlock,
            DepositDurability dur, List<Yield> yields
    ) {
        this.depositBlock = depositBlock;
        this.dimension = dimension;
        this.replacementBlock = replacementBlock;
        this.dur = dur;
        this.yields = yields;
    }

    public Block getDepositBlock() {
        return depositBlock;
    }

    public List<Yield> getYields() {
        return yields;
    }

    public Block getReplacementBlock() {
        return replacementBlock;
    }

    public DepositDurability getDurability() {
        return dur;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public boolean initialize(RegistryAccess access) {
        if (!isInitialized) {
            yields = yields.stream()
                    .filter(y -> y.initialize(access))
                    .toList();
            isInitialized = true;
        }
        return !yields.isEmpty();
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
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
        return ItemStack.EMPTY;
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
