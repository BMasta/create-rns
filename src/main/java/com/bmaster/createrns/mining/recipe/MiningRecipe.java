package com.bmaster.createrns.mining.recipe;

import com.bmaster.createrns.RNSRecipeTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MiningRecipe implements Recipe<SingleRecipeInput> {
    private final Block depositBlock;
    private final int tier;
    private final Yield yield;

    public MiningRecipe(Block depositBlock, int tier, List<YieldType> types) {
        this.depositBlock = depositBlock;
        this.tier = tier;
        this.yield = new Yield(types);
    }

    public Block getDepositBlock() {
        return depositBlock;
    }

    public int getTier() {
        return tier;
    }

    public Yield getYield() {
        return yield;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return new ItemStack(yield.types.getFirst().item);
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
        return new ItemStack(getYield().types.getFirst().item);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MiningRecipe.Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return RNSRecipeTypes.MINING_RECIPE_TYPE.get();
    }

    public static class Yield {
        public static final Codec<List<YieldType>> CODEC = Codec.list(YieldType.CODEC);

        public static final StreamCodec<RegistryFriendlyByteBuf, List<YieldType>> STREAM_CODEC = StreamCodec.of(
                Yield::toNetwork, Yield::fromNetwork);

        public static void toNetwork(RegistryFriendlyByteBuf buffer, List<YieldType> types) {
            ByteBufCodecs.collection(ArrayList::new, YieldType.STREAM_CODEC).encode(buffer, new ArrayList<>(types));
        }

        public static List<YieldType> fromNetwork(RegistryFriendlyByteBuf buffer) {
            return ByteBufCodecs.collection(ArrayList::new, YieldType.STREAM_CODEC).decode(buffer);
        }

        public List<YieldType> types;
        private int totalWeight = 0;

        public Yield(List<YieldType> types) {
            this.types = types;
        }

        public int getTotalWeight() {
            if (totalWeight == 0) {
                totalWeight = types.stream()
                        .map(y -> y.chanceWeight)
                        .reduce(Integer::sum)
                        .orElseThrow();
            }
            return totalWeight;
        }

        public Item roll(RandomSource rng) {
            Item result = types.getLast().item;
            float threshold = rng.nextFloat();
            float accChance = 0;
            for (var t : types) {
                accChance += (float) t.chanceWeight / getTotalWeight();
                if (accChance > threshold) {
                    result = t.item;
                    break;
                }
            }
            return result;
        }

        public Yield merge(Yield other) {
            this.types.addAll(other.types);

            // Merge yield types with the same item by combining their weights
            types = types.stream()
                    .collect(Collectors.toMap(t -> t.item, t -> t.chanceWeight, Integer::sum, Object2IntOpenHashMap::new))
                    .object2IntEntrySet()
                    .stream()
                    .map(e -> new YieldType(e.getKey(), e.getIntValue()))
                    .toList();

            return this;
        }
    }

    public record YieldType(Item item, int chanceWeight) {
        public static final Codec<YieldType> CODEC = RecordCodecBuilder.create(i -> i.group(
                        BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(YieldType::item),
                        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("chance_weight").forGetter(YieldType::chanceWeight))
                .apply(i, YieldType::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, YieldType> STREAM_CODEC = StreamCodec.of(
                YieldType::toNetwork, YieldType::fromNetwork);

        public static void toNetwork(RegistryFriendlyByteBuf buffer, YieldType type) {
            ByteBufCodecs.registry(Registries.ITEM).encode(buffer, type.item());
            ByteBufCodecs.INT.encode(buffer, type.chanceWeight);
        }

        public static YieldType fromNetwork(RegistryFriendlyByteBuf buffer) {
            return new YieldType(
                    ByteBufCodecs.registry(Registries.ITEM).decode(buffer),
                    ByteBufCodecs.INT.decode(buffer)
            );
        }
    }

    public static class Serializer implements RecipeSerializer<MiningRecipe> {
        public static MiningRecipe.Serializer INSTANCE = new MiningRecipe.Serializer();

        public static final MapCodec<MiningRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                        BuiltInRegistries.BLOCK.byNameCodec().fieldOf("deposit_block").forGetter(MiningRecipe::getDepositBlock),
                        Codec.INT.fieldOf("tier").forGetter(MiningRecipe::getTier),
                        Yield.CODEC.fieldOf("yield").forGetter((r) -> r.yield.types))
                .apply(i, MiningRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, MiningRecipe> STREAM_CODEC = StreamCodec.of(
                Serializer::toNetwork, Serializer::fromNetwork);

        public static void toNetwork(RegistryFriendlyByteBuf buffer, MiningRecipe recipe) {
            ByteBufCodecs.registry(Registries.BLOCK).encode(buffer, recipe.getDepositBlock());
            ByteBufCodecs.INT.encode(buffer, recipe.tier);
            Yield.STREAM_CODEC.encode(buffer, new ArrayList<>(recipe.yield.types));
        }

        public static MiningRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            return new MiningRecipe(
                    ByteBufCodecs.registry(Registries.BLOCK).decode(buffer),
                    ByteBufCodecs.INT.decode(buffer),
                    Yield.STREAM_CODEC.decode(buffer)
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
