//package com.bmaster.createrns.mining.recipe;
//
//import com.google.gson.JsonObject;
//import com.google.gson.JsonSyntaxException;
//import net.minecraft.core.NonNullList;
//import net.minecraft.core.RegistryAccess;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.util.GsonHelper;
//import net.minecraft.world.Container;
//import net.minecraft.world.item.Item;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.item.Items;
//import net.minecraft.world.item.crafting.*;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.Block;
//import net.minecraft.world.level.block.Blocks;
//import net.minecraftforge.registries.ForgeRegistries;
//import org.jetbrains.annotations.NotNull;
//
//import javax.annotation.ParametersAreNonnullByDefault;
//
//public abstract class MiningRecipe implements Recipe<Container> {
//    private final ResourceLocation id;
//    private final Block depositBlock;
//    private final Item yield;
//
//    public MiningRecipe(ResourceLocation id, Block depositBlock, Item yield) {
//        this.id = id;
//        this.depositBlock = depositBlock;
//        this.yield = yield;
//    }
//
//    @Override
//    public abstract @NotNull RecipeSerializer<?> getSerializer();
//
//    @Override
//    public abstract @NotNull RecipeType<?> getType();
//
//    public Block getDepositBlock() {
//        return depositBlock;
//    }
//
//    public Item getYield() {
//        return yield;
//    }
//
//    @ParametersAreNonnullByDefault
//    @Override
//    public boolean matches(Container c, Level l) {
//        return false;
//    }
//
//    @ParametersAreNonnullByDefault
//    @Override
//    public @NotNull ItemStack assemble(Container c, RegistryAccess ra) {
//        return new ItemStack(yield);
//    }
//
//    @Override
//    public boolean canCraftInDimensions(int w, int h) {
//        return false;
//    }
//
//    @ParametersAreNonnullByDefault
//    @Override
//    public @NotNull ItemStack getResultItem(RegistryAccess ra) {
//        return new ItemStack(yield);
//    }
//
//    @Override
//    public @NotNull ResourceLocation getId() {
//        return id;
//    }
//
//    @Override
//    public @NotNull NonNullList<Ingredient> getIngredients() {
//        return NonNullList.of(Ingredient.EMPTY, Ingredient.of(depositBlock));
//    }
//
//    @Override
//    public boolean isSpecial() {
//        return true;
//    }
//
//    @SuppressWarnings("SameParameterValue")
//    @ParametersAreNonnullByDefault
//    public static abstract class Serializer<MR extends MiningRecipe> implements RecipeSerializer<MR> {
//        protected static Block parseBlockId(JsonObject json, String field) {
//            String raw = GsonHelper.getAsString(json, field);
//            ResourceLocation rl = ResourceLocation.tryParse(raw);
//            if (rl == null) {
//                throw new JsonSyntaxException("Invalid resource location for '%s': %s".formatted(field, raw));
//            }
//            Block block = ForgeRegistries.BLOCKS.getValue(rl);
//            if (block == null || block == Blocks.AIR) {
//                throw new JsonSyntaxException("Unknown block for '%s': %s".formatted(field, raw));
//            }
//            return block;
//        }
//
//        protected static Item parseItemId(JsonObject json, String field) {
//            String raw = GsonHelper.getAsString(json, field);
//            ResourceLocation rl = ResourceLocation.tryParse(raw);
//            if (rl == null) {
//                throw new JsonSyntaxException("Invalid resource location for '%s': %s".formatted(field, raw));
//            }
//            Item item = ForgeRegistries.ITEMS.getValue(rl);
//            if (item == null || item == Items.AIR) {
//                throw new JsonSyntaxException("Unknown item for '%s': %s".formatted(field, raw));
//            }
//            return item;
//        }
//    }
//}
