package com.bmaster.createrns.mining;

import com.bmaster.createrns.RNSRecipes;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

public class MiningRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Block depositBlock;
    private final Item yield;

    public MiningRecipe(ResourceLocation id, Block depositBlock, Item yield) {
        this.id = id;
        this.depositBlock = depositBlock;
        this.yield = yield;
    }

    public Block getDepositBlock() {
        return depositBlock;
    }

    public Item getYield() {
        return yield;
    }

    @ParametersAreNonnullByDefault
    @Override
    public boolean matches(Container c, Level l) {
        return false;
    }

    @ParametersAreNonnullByDefault
    @Override
    public @NotNull ItemStack assemble(Container c, RegistryAccess ra) {
        return new ItemStack(yield);
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return false;
    }

    @ParametersAreNonnullByDefault
    @Override
    public @NotNull ItemStack getResultItem(RegistryAccess ra) {
        return new ItemStack(yield);
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return RNSRecipes.MINING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return RNSRecipes.MINING_TYPE.get();
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, Ingredient.of(depositBlock));
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<MiningRecipe> {
        @ParametersAreNonnullByDefault
        @Override
        public @NotNull MiningRecipe fromJson(ResourceLocation id, JsonObject json) {
            var depBlock = parseBlockId(json, "deposit_block");
            var yield = parseItemId(json, "yield");
            return new MiningRecipe(id, depBlock, yield);
        }

        @ParametersAreNonnullByDefault
        @Override
        public MiningRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            var depBlock = Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(buf.readResourceLocation()));
            var yield = buf.readItem();
            return new MiningRecipe(id, depBlock, yield.getItem());
        }

        @ParametersAreNonnullByDefault
        @Override
        public void toNetwork(FriendlyByteBuf buf, MiningRecipe r) {
            buf.writeResourceLocation(Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(r.depositBlock)));
            buf.writeItem(new ItemStack(r.yield));
        }

        private static Block parseBlockId(JsonObject json, String field) {
            String raw = GsonHelper.getAsString(json, field);
            ResourceLocation rl = ResourceLocation.tryParse(raw);
            if (rl == null) {
                throw new JsonSyntaxException("Invalid resource location for '%s': %s".formatted(field, raw));
            }
            Block block = ForgeRegistries.BLOCKS.getValue(rl);
            if (block == null || block == Blocks.AIR) {
                throw new JsonSyntaxException("Unknown block for '%s': %s".formatted(field, raw));
            }
            return block;
        }

        private static Item parseItemId(JsonObject json, String field) {
            String raw = GsonHelper.getAsString(json, field);
            ResourceLocation rl = ResourceLocation.tryParse(raw);
            if (rl == null) {
                throw new JsonSyntaxException("Invalid resource location for '%s': %s".formatted(field, raw));
            }
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item == null || item == Items.AIR) {
                throw new JsonSyntaxException("Unknown item for '%s': %s".formatted(field, raw));
            }
            return item;
        }
    }
}
