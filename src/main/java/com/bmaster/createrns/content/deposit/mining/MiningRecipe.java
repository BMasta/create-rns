package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.RNSRecipeTypes;
import com.bmaster.createrns.util.Utils;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MiningRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Block depositBlock;
    private final Block replacementBlock;
    private final Durability dur;
    private final int tier;
    private final Yield yield;
    private final Byproduct byproduct;

    public MiningRecipe(ResourceLocation id, Block depositBlock, Block replacementBlock, Durability dur, int tier,
                        List<YieldType> types, Byproduct byproduct) {
        this.id = id;
        this.depositBlock = depositBlock;
        this.replacementBlock = replacementBlock;
        this.dur = dur;
        this.tier = tier;
        this.yield = new Yield(types);
        this.byproduct = byproduct;
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

    public Byproduct getByproduct() {
        return byproduct;
    }

    public Block getReplacementBlock() {
        return replacementBlock;
    }

    public Durability getDurability() {
        return dur;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess ra) {
        return new ItemStack(yield.types.get(0).item());
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
    public boolean matches(Container c, Level l) {
        return c.getContainerSize() > 0 && c.getItem(0).is(getDepositBlock().asItem());
    }

    @Override
    public ItemStack assemble(Container c, RegistryAccess ra) {
        return new ItemStack(getYield().types.get(0).item());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MiningRecipe.Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return RNSRecipeTypes.MINING_RECIPE_TYPE.get();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    public static class Byproduct {
        public final float chancePerStack;
        public final int maxStacks;
        public final Yield yield;

        public Byproduct(float chancePerStack, int maxStacks, List<YieldType> types) {
            this.chancePerStack = chancePerStack;
            this.maxStacks = maxStacks;
            this.yield = new Yield(types);
        }

        public static final Codec<Byproduct> CODEC = RecordCodecBuilder.create(i -> i.group(
                        Codec.floatRange(0, 1).fieldOf("chance_per_stack").forGetter(bp -> bp.chancePerStack),
                        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("max_stacks").orElse(Integer.MAX_VALUE).forGetter(bp -> bp.maxStacks),
                        Yield.CODEC.fieldOf("yield").forGetter(bp -> bp.yield.types))
                .apply(i, Byproduct::new));
    }

    public static class Yield {
        public static final Codec<List<YieldType>> CODEC = Codec.list(YieldType.CODEC);

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
            Item result = types.get(types.size() - 1).item();
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
    }

    public record YieldType(Item item, int chanceWeight) {
        public static final Codec<YieldType> CODEC = RecordCodecBuilder.create(i -> i.group(
                        ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(YieldType::item),
                        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("chance_weight").forGetter(YieldType::chanceWeight))
                .apply(i, YieldType::new));
    }

    public record Durability(long core, long edge, float randomSpread) {
        public static final MapCodec<Durability> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Utils.longRangeCodec(1, Long.MAX_VALUE).fieldOf("core").forGetter(Durability::core),
                Utils.longRangeCodec(1, Long.MAX_VALUE).fieldOf("edge").forGetter(Durability::edge),
                Codec.floatRange(0f, 1f).fieldOf("random_spread").forGetter(Durability::randomSpread)
        ).apply(i, Durability::new));
    }

    public static class Serializer implements RecipeSerializer<MiningRecipe> {
        public static MiningRecipe.Serializer INSTANCE = new MiningRecipe.Serializer();

        public static final MapCodec<MiningRecipeHelper.SerializedRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                        ForgeRegistries.BLOCKS.getCodec().fieldOf("deposit_block").forGetter(MiningRecipeHelper.SerializedRecipe::depositBlock),
                        ForgeRegistries.BLOCKS.getCodec().fieldOf("replace_when_depleted").orElse(Blocks.AIR).forGetter(MiningRecipeHelper.SerializedRecipe::replacementBlock),
                        Durability.CODEC.fieldOf("durability").orElse(new Durability(0, 0, 0)).forGetter(MiningRecipeHelper.SerializedRecipe::durability),
                        Codec.INT.fieldOf("tier").forGetter(MiningRecipeHelper.SerializedRecipe::tier),
                        Yield.CODEC.fieldOf("yield").forGetter(MiningRecipeHelper.SerializedRecipe::yield),
                        Byproduct.CODEC.fieldOf("byproduct").orElse(new Byproduct(0, 0, List.of())).forGetter(MiningRecipeHelper.SerializedRecipe::byproduct))
                .apply(i, MiningRecipeHelper.SerializedRecipe::new));

        @Override
        public MiningRecipe fromJson(ResourceLocation id, JsonObject json) {
            return MiningRecipeHelper.fromJson(CODEC, id, json);
        }

        @Override
        public MiningRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return MiningRecipeHelper.fromNetwork(CODEC, id, buf);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, MiningRecipe recipe) {
            MiningRecipeHelper.toNetwork(CODEC, buf, recipe);
        }
    }
}
