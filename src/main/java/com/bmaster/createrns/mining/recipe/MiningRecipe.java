package com.bmaster.createrns.mining.recipe;

import com.bmaster.createrns.RNSRecipeTypes;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.util.RandomSource;
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
import java.util.ArrayList;
import java.util.List;

public class MiningRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Block depositBlock;
    private final int tier;
    private final Yield yield;

    public MiningRecipe(ResourceLocation id, Block depositBlock, int tier, Yield yield) {
        this.id = id;
        this.depositBlock = depositBlock;
        this.tier = tier;
        this.yield = yield;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return RNSRecipeTypes.MINING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return RNSRecipeTypes.MINING_RECIPE_TYPE.get();
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

    @ParametersAreNonnullByDefault
    @Override
    public boolean matches(Container c, Level l) {
        return false;
    }

    @Deprecated
    @ParametersAreNonnullByDefault
    @Override
    public @NotNull ItemStack assemble(Container c, RegistryAccess ra) {
        return new ItemStack(yield.types.get(0).item);
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return false;
    }

    @Deprecated
    @ParametersAreNonnullByDefault
    @Override
    public @NotNull ItemStack getResultItem(RegistryAccess ra) {
        return new ItemStack(yield.types.get(0).item);
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, Ingredient.of(depositBlock));
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @SuppressWarnings("SameParameterValue")
    @ParametersAreNonnullByDefault
    public static class Serializer implements RecipeSerializer<MiningRecipe> {
        @Override
        public @NotNull MiningRecipe fromJson(ResourceLocation id, JsonObject json) {
            var depBlock = parseBlockId(json, "deposit_block");
            var tier = GsonHelper.getAsInt(json, "tier");
            var yieldList = GsonHelper.getAsJsonArray(json, "yield").asList().stream().map(e -> {
                var o = e.getAsJsonObject();
                return new YieldType(parseItemId(o, "item"),
                        GsonHelper.getAsInt(o, "chance_weight"));
            }).toList();

            return new MiningRecipe(id, depBlock, tier, new Yield(yieldList));
        }

        @Override
        public MiningRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            var depBlock = Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(buf.readResourceLocation()));
            var tier = buf.readInt();
            var sz = buf.readInt();
            List<YieldType> types = new ArrayList<>(sz);
            for (int i = 0; i < sz; ++i) {
                types.add(new YieldType(buf.readItem().getItem(), buf.readInt()));
            }
            return new MiningRecipe(id, depBlock, tier, new Yield(types));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, MiningRecipe r) {
            buf.writeResourceLocation(Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(r.getDepositBlock())));
            buf.writeInt(r.tier);
            buf.writeInt(r.yield.types.size());
            for (var t : r.yield.types) {
                buf.writeItem(new ItemStack(t.item));
                buf.writeInt(t.chanceWeight);
            }
        }

        protected static Block parseBlockId(JsonObject json, String field) {
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

        protected static Item parseItemId(JsonObject json, String field) {
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

    public static class Yield {
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
            Item result = types.get(types.size() - 1).item;
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

    public record YieldType(Item item, int chanceWeight) {}
}
