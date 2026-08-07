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
                    WeightedItem.CODEC.listOf().fieldOf("items")
                            .forGetter(y -> y.items),
                    RegistryFixedCodec.create(CatalystRequirementSet.REGISTRY_KEY).listOf()
                            .optionalFieldOf("catalysts", List.of())
                            .forGetter(y -> y.crsList),
                    ExtraCodecs.ARGB_COLOR_CODEC.optionalFieldOf("jei_slot_color", 0)
                            .forGetter(y -> y.slotColor))
            .apply(i, Yield::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Yield> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, y -> y.chance,
            ByteBufCodecs.collection(ArrayList::new, WeightedItem.STREAM_CODEC), y -> new ArrayList<>(y.items),
            ByteBufCodecs.collection(ArrayList::new,
                    ByteBufCodecs.holderRegistry(CatalystRequirementSet.REGISTRY_KEY)),
            y -> new ArrayList<>(y.crsList),
            ByteBufCodecs.INT, y -> y.slotColor,
            Yield::new
    );

    public final float chance;
    public List<WeightedItem> items;
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

    public Item roll(RandomSource rng) {
        Item result = items.getLast().item;
        float threshold = rng.nextFloat();
        float accChance = 0;
        for (var t : items) {
            accChance += (float) t.weight / getTotalWeight();
            if (accChance > threshold) {
                result = t.item;
                if (result != Items.AIR) break;
            }
        }
        return result;
    }

    public boolean initialize(RegistryAccess access) {
        items = items.stream()
                .filter(wi -> wi.initialize(access))
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
            float chance, List<WeightedItem> items, List<Holder<CatalystRequirementSet>> crsList,
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

    public static class WeightedItem {
        public static final int DEFAULT_WEIGHT = 1;
        private static final MapCodec<ItemWithFallbacks> STRICT_ITEM_FIELD = ItemWithFallbacks.STRICT_RESOLVABLE_CODEC.fieldOf("item");
        private static final MapCodec<ItemWithFallbacks> LENIENT_ITEM_FIELD = ItemWithFallbacks.LENIENT_CODEC.fieldOf("item");

        public final int weight;
        public final boolean compat;
        public Item item;
        protected final ItemWithFallbacks itemData;

        public static final Codec<WeightedItem> CODEC = RecordCodecBuilder.<WeightedItem>mapCodec(i -> i.group(
                                Codec.BOOL.optionalFieldOf("compat", false)
                                        .forGetter((WeightedItem wi) -> wi.compat),
                                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("weight", DEFAULT_WEIGHT)
                                        .forGetter((WeightedItem wi) -> wi.weight))
                        .apply(i, (compat, weight) -> new WeightedItem(ItemWithFallbacks.EMPTY, compat, weight)))
                .dependent(
                        LENIENT_ITEM_FIELD,
                        wi -> Pair.of(wi.itemData, wi.compat ? LENIENT_ITEM_FIELD : STRICT_ITEM_FIELD),
                        (wi, itemData) -> new WeightedItem(itemData, wi.compat, wi.weight)
                )
                .codec();

        public static final StreamCodec<RegistryFriendlyByteBuf, WeightedItem> STREAM_CODEC =
                StreamCodec.composite(
                        ItemWithFallbacks.STREAM_CODEC, wi -> wi.itemData,
                        ByteBufCodecs.BOOL, wi -> wi.compat,
                        ByteBufCodecs.INT, wi -> wi.weight,
                        WeightedItem::new
                );

        public WeightedItem(ItemWithFallbacks itemData, boolean compat, int weight) {
            this.itemData = itemData;
            this.compat = compat;
            this.item = itemData.item;
            this.weight = weight;
        }

        public boolean initialize(RegistryAccess access) {
            if (!itemData.resolve(access, compat)) return false;
            item = itemData.item;
            return item != Items.AIR;
        }
    }
}
