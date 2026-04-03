package com.bmaster.createrns.content.deposit.mining.recipe;

import com.bmaster.createrns.RNSRecipeTypes;
import com.bmaster.createrns.util.StrictOptionalField;
import com.google.gson.JsonObject;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MiningRecipe implements Recipe<Container> {
    private static final DepositDurability DEFAULT_DURABILITY = new DepositDurability(0, 0, 0);

    public static final MapCodec<MiningRecipeHelper.SerializedRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    ForgeRegistries.BLOCKS.getCodec().fieldOf("deposit_block")
                            .forGetter(MiningRecipeHelper.SerializedRecipe::depositBlock),
                    StrictOptionalField.of("dimension", ResourceKey.codec(Registries.DIMENSION), Level.OVERWORLD)
                            .forGetter(MiningRecipeHelper.SerializedRecipe::dimension),
                    StrictOptionalField.of("replace_when_depleted", ForgeRegistries.BLOCKS.getCodec(), Blocks.AIR)
                            .forGetter(MiningRecipeHelper.SerializedRecipe::replacementBlock),
                    StrictOptionalField.of("durability", DepositDurability.CODEC, DEFAULT_DURABILITY)
                            .forGetter(MiningRecipeHelper.SerializedRecipe::dur),
                    Yield.CODEC.listOf().fieldOf("yields")
                            .forGetter(MiningRecipeHelper.SerializedRecipe::yields))
            .apply(i, MiningRecipeHelper.SerializedRecipe::new));

    public static final MapCodec<MiningRecipeHelper.SerializedRecipe> STREAM_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    ForgeRegistries.BLOCKS.getCodec().fieldOf("deposit_block")
                            .forGetter(MiningRecipeHelper.SerializedRecipe::depositBlock),
                    StrictOptionalField.of("dimension", ResourceKey.codec(Registries.DIMENSION), Level.OVERWORLD)
                            .forGetter(MiningRecipeHelper.SerializedRecipe::dimension),
                    StrictOptionalField.of("replace_when_depleted", ForgeRegistries.BLOCKS.getCodec(), Blocks.AIR)
                            .forGetter(MiningRecipeHelper.SerializedRecipe::replacementBlock),
                    StrictOptionalField.of("durability", DepositDurability.STREAM_CODEC, DEFAULT_DURABILITY)
                            .forGetter(MiningRecipeHelper.SerializedRecipe::dur),
                    Yield.STREAM_CODEC.listOf().fieldOf("yields")
                            .forGetter(MiningRecipeHelper.SerializedRecipe::yields))
            .apply(i, MiningRecipeHelper.SerializedRecipe::new));

    private final ResourceLocation id;
    private final Block depositBlock;
    private final ResourceKey<Level> dimension;
    private final Block replacementBlock;
    private final DepositDurability dur;
    private List<Yield> yields;
    private boolean isInitialized = false;

    public MiningRecipe(
            ResourceLocation id, Block depositBlock, ResourceKey<Level> dimension,
            Block replacementBlock, DepositDurability dur, List<Yield> yields
    ) {
        this.id = id;
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
    public ItemStack getResultItem(RegistryAccess ra) {
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
    public boolean matches(Container c, Level l) {
        return c.getContainerSize() > 0 && c.getItem(0).is(getDepositBlock().asItem());
    }

    @Override
    public ItemStack assemble(Container c, RegistryAccess ra) {
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

    @Override
    public ResourceLocation getId() {
        return id;
    }

    public static class Serializer implements RecipeSerializer<MiningRecipe> {
        public static MiningRecipe.Serializer INSTANCE = new MiningRecipe.Serializer();

        @Override
        public MiningRecipe fromJson(ResourceLocation id, JsonObject json) {
            return MiningRecipeHelper.fromJson(CODEC, id, json);
        }

        @Override
        public MiningRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return MiningRecipeHelper.fromNetwork(STREAM_CODEC, id, buf);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, MiningRecipe recipe) {
            MiningRecipeHelper.toNetwork(STREAM_CODEC, buf, recipe);
        }
    }
}
