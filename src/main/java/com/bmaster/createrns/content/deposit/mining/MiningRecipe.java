package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.RNSRecipeTypes;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MiningRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Block depositBlock;
    private final Block replacementBlock;
    private final Durability dur;
    private final int tier;
    private final Yield yield;
    private final Byproduct byproduct;

    public MiningRecipe(ResourceLocation id, Block depositBlock, Block replacementBlock, Durability dur, int tier,
                        Yield yield, Byproduct byproduct) {
        this.id = id;
        this.depositBlock = depositBlock;
        this.replacementBlock = replacementBlock;
        this.dur = dur;
        this.tier = tier;
        this.yield = yield;
        this.byproduct = byproduct;
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

    public Byproduct getByproduct() {
        return byproduct;
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
            Block replacement;
            if (GsonHelper.isStringValue(json, "replace_when_depleted")) {
                replacement = parseBlockId(json, "replace_when_depleted");
            } else {
                replacement = Blocks.AIR;
            }
            Durability dur;
            if (GsonHelper.isObjectNode(json, "durability")) {
                var durObj = GsonHelper.getAsJsonObject(json, "durability");
                var coreDur = parseLongRange(durObj, "core", 1, Long.MAX_VALUE);
                var edgeDur = parseLongRange(durObj, "edge", 1, Long.MAX_VALUE);
                var spread = parseFloatRange(durObj, "random_spread", 0, 1);
                dur = new Durability(coreDur, edgeDur, spread);
            } else {
                dur = new Durability(0, 0, 0f);
            }
            var tier = GsonHelper.getAsInt(json, "tier");
            var yield = parseYield(GsonHelper.getAsJsonArray(json, "yield"));

            Byproduct by;
            if (GsonHelper.isObjectNode(json, "byproduct")) {
                var byObj = GsonHelper.getAsJsonObject(json, "byproduct");
                float chancePerStack = parseFloatRange(byObj, "chance_per_stack", 0, 1);
                int maxStacks;
                if (GsonHelper.isNumberValue(json, "max_stacks")) {
                    maxStacks = parseIntRange(byObj, "max_stacks", 0, Integer.MAX_VALUE);
                } else {
                    maxStacks = Integer.MAX_VALUE;
                }
                var byYield = parseYield(GsonHelper.getAsJsonArray(byObj, "yield"));
                by = new Byproduct(chancePerStack, maxStacks, byYield);
            } else {
                by = new Byproduct(0, 0, new Yield(List.of()));
            }

            return new MiningRecipe(id, depBlock, replacement, dur, tier, yield, by);
        }

        @Override
        public MiningRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            var depBlock = Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(buf.readResourceLocation()));
            var replacement = Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(buf.readResourceLocation()));
            var dur = new Durability(buf.readLong(), buf.readLong(), buf.readFloat());
            var tier = buf.readInt();
            var yieldSz = buf.readInt();
            List<YieldType> yieldTypes = new ArrayList<>(yieldSz);
            for (int i = 0; i < yieldSz; ++i) {
                yieldTypes.add(new YieldType(buf.readItem().getItem(), buf.readInt()));
            }
            float chancePerStack = buf.readFloat();
            int maxStacks = buf.readInt();
            int byYieldSz = buf.readInt();
            List<YieldType> byYieldTypes = new ArrayList<>(yieldSz);
            for (int i = 0; i < byYieldSz; ++i) {
                byYieldTypes.add(new YieldType(buf.readItem().getItem(), buf.readInt()));
            }
            Byproduct by = new Byproduct(chancePerStack, maxStacks, new Yield(byYieldTypes));
            return new MiningRecipe(id, depBlock, replacement, dur, tier, new Yield(yieldTypes), by);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, MiningRecipe r) {
            buf.writeResourceLocation(Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(r.getDepositBlock())));
            buf.writeResourceLocation(Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(r.getReplacementBlock())));
            buf.writeLong(r.dur.core);
            buf.writeLong(r.dur.edge);
            buf.writeFloat(r.dur.randomSpread);
            buf.writeInt(r.tier);
            buf.writeInt(r.yield.types.size());
            for (var t : r.yield.types) {
                buf.writeItem(new ItemStack(t.item));
                buf.writeInt(t.chanceWeight);
            }
            buf.writeFloat(r.byproduct.chancePerStack);
            buf.writeInt(r.byproduct.maxStacks);
            buf.writeInt(r.byproduct.yield.types.size());
            for (var t : r.byproduct.yield.types) {
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

        protected static Yield parseYield(JsonArray arr) {
            return new Yield(arr.asList().stream().map(e -> {
                var o = e.getAsJsonObject();
                return new YieldType(parseItemId(o, "item"),
                        GsonHelper.getAsInt(o, "chance_weight"));
            }).toList());
        }

        protected static int parseIntRange(JsonObject json, String field, int min, int maxInclusive) {
            int val = GsonHelper.getAsInt(json, field);
            if (val < min || val > maxInclusive) throw new RuntimeException("Field " + field + "=" + val +
                    " is not within the acceptable range [" + min + "," + maxInclusive + "]");
            return val;
        }

        protected static long parseLongRange(JsonObject json, String field, long min, long maxInclusive) {
            long val = GsonHelper.getAsLong(json, field);
            if (val < min || val > maxInclusive) throw new RuntimeException("Field " + field + "=" + val +
                    " is not within the acceptable range [" + min + "," + maxInclusive + "]");
            return val;
        }

        protected static float parseFloatRange(JsonObject json, String field, float min, float maxInclusive) {
            float val = GsonHelper.getAsFloat(json, field);
            if (val < min || val > maxInclusive) throw new RuntimeException("Field " + field + "=" + val +
                    " is not within the acceptable range [" + min + "," + maxInclusive + "]");
            return val;
        }
    }

    public record Byproduct(float chancePerStack, int maxStacks, Yield yield) {}

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

    public record Durability(long core, long edge, float randomSpread) {}
}
