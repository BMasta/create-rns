package com.bmaster.createrns.content.deposit.mining.recipe;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.bmaster.createrns.util.codec.ItemWithFallbacks;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class Yield {
    public static final Codec<Yield> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.floatRange(0, 1).optionalFieldOf("chance", 1f)
                            .forGetter(y -> y.chance),
                    WeightedItemStack.CODEC.listOf().fieldOf("items")
                            .forGetter(y -> y.items),
                    RegistryFixedCodec.create(CatalystRequirementSet.REGISTRY_KEY).listOf()
                            .optionalFieldOf("catalysts", List.of())
                            .forGetter(y -> y.crsList),
                    ExtraCodecs.ARGB_COLOR_CODEC.optionalFieldOf("jei_slot_color", 0)
                            .forGetter(y -> y.slotColor))
            .apply(i, Yield::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Yield> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, y -> y.chance,
            ByteBufCodecs.collection(ArrayList::new, WeightedItemStack.STREAM_CODEC), y -> new ArrayList<>(y.items),
            ByteBufCodecs.collection(ArrayList::new,
                    ByteBufCodecs.holderRegistry(CatalystRequirementSet.REGISTRY_KEY)),
            y -> new ArrayList<>(y.crsList),
            ByteBufCodecs.INT, y -> y.slotColor,
            Yield::new
    );

    public final float chance;
    public List<WeightedItemStack> items;
    public final int slotColor;

    private final List<ResourceLocation> crsIds;
    private final List<Holder<CatalystRequirementSet>> crsList;
    private int totalWeight = 0;

    public int getTotalWeight() {
        if (totalWeight == 0) {
            totalWeight = items.stream()
                    .map(y -> y.weight)
                    .reduce(Integer::sum)
                    .orElseThrow();
        }
        return totalWeight;
    }

    public ItemStack roll(RandomSource rng) {
        var result = items.getLast();
        float threshold = rng.nextFloat();
        float accChance = 0;
        for (var t : items) {
            accChance += (float) t.weight / getTotalWeight();
            if (accChance > threshold) {
                result = t;
                if (result.item != Items.AIR) break;
            }
        }
        return result.itemStack();
    }

    public boolean initialize(RegistryAccess access) {
        items = items.stream()
                .filter(wis -> wis.initialize(access))
                .toList();
        return !items.isEmpty();
    }

    public List<ResourceLocation> getCRSIds() {
        return crsIds;
    }

    public List<Holder<CatalystRequirementSet>> getCRSes() {
        return crsList;
    }

    protected Yield(
            float chance, List<WeightedItemStack> items, List<Holder<CatalystRequirementSet>> crsList,
            int slotColor
    ) {
        this.chance = chance;
        this.items = items;
        this.crsList = List.copyOf(crsList);
        this.crsIds = this.crsList.stream()
                .map(CatalystRequirementSet::id)
                .toList();
        this.slotColor = slotColor;
    }

    public static class WeightedItemStack {
        public static final int DEFAULT_WEIGHT = 1;
        private static final MapCodec<ItemWithFallbacks> STRICT_ITEM_FIELD = ItemWithFallbacks.STRICT_RESOLVABLE_CODEC.fieldOf("item");
        private static final MapCodec<ItemWithFallbacks> LENIENT_ITEM_FIELD = ItemWithFallbacks.LENIENT_CODEC.fieldOf("item");

        public final int count;
        public final int weight;
        public final boolean compat;
        public Item item;
        protected final ItemWithFallbacks itemData;

        public static final Codec<WeightedItemStack> CODEC = RecordCodecBuilder.<WeightedItemStack>mapCodec(i -> i.group(
                                Codec.intRange(1, Item.ABSOLUTE_MAX_STACK_SIZE).optionalFieldOf("count", 1)
                                        .forGetter((WeightedItemStack wis) -> wis.count),
                                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("weight", DEFAULT_WEIGHT)
                                        .forGetter((WeightedItemStack wis) -> wis.weight),
                                Codec.BOOL.optionalFieldOf("compat", false)
                                        .forGetter((WeightedItemStack wis) -> wis.compat))
                        .apply(i, (count, weight, compat) -> new WeightedItemStack(ItemWithFallbacks.EMPTY, count, weight, compat)))
                .dependent(
                        LENIENT_ITEM_FIELD,
                        wis -> Pair.of(wis.itemData, wis.compat ? LENIENT_ITEM_FIELD : STRICT_ITEM_FIELD),
                        (wis, itemData) -> new WeightedItemStack(itemData, wis.count, wis.weight, wis.compat)
                )
                .codec();

        public static final StreamCodec<RegistryFriendlyByteBuf, WeightedItemStack> STREAM_CODEC =
                StreamCodec.composite(
                        ItemWithFallbacks.STREAM_CODEC, wis -> wis.itemData,
                        ByteBufCodecs.INT, wis -> wis.count,
                        ByteBufCodecs.INT, wis -> wis.weight,
                        ByteBufCodecs.BOOL, wis -> wis.compat,
                        WeightedItemStack::new
                );

        public WeightedItemStack(ItemWithFallbacks itemData, int count, int weight, boolean compat) {
            this.itemData = itemData;
            this.item = itemData.value();
            this.count = count;
            this.weight = weight;
            this.compat = compat;
        }

        public boolean initialize(RegistryAccess access) {
            if (!itemData.resolve(access, compat)) return false;
            item = itemData.value();
            return item != Items.AIR;
        }

        public ItemStack itemStack() {
            if (item == Items.AIR) return ItemStack.EMPTY;
            return new ItemStack(item, count);
        }
    }
}
