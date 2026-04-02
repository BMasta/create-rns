package com.bmaster.createrns.content.deposit.mining.recipe;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSetLookup;
import com.bmaster.createrns.util.codec.ItemWithFallbacks;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class Yield {
    public static final Codec<Yield> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.floatRange(0, 1).optionalFieldOf("chance", 1f)
                            .forGetter(y -> y.chance),
                    WeightedItem.CODEC.listOf().fieldOf("items")
                            .forGetter(y -> y.items),
                    Codec.STRING.listOf().optionalFieldOf("catalysts")
                            .forGetter(y -> (!y.crsNames.isEmpty()) ? Optional.of(y.crsNames) : Optional.empty()),
                    ExtraCodecs.ARGB_COLOR_CODEC.optionalFieldOf("jei_slot_color", 0)
                            .forGetter(y -> y.slotColor))
            .apply(i, Yield::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Yield> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, y -> y.chance,
            ByteBufCodecs.collection(ArrayList::new, WeightedItem.STREAM_CODEC), y -> new ArrayList<>(y.items),
            ByteBufCodecs.optional(ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8)), y ->
                    (!y.crsNames.isEmpty()) ? Optional.of(new ArrayList<>(y.crsNames)) : Optional.empty(),
            ByteBufCodecs.INT, y -> y.slotColor,
            Yield::new
    );

    public final float chance;
    public List<WeightedItem> items;
    public final List<String> crsNames;
    public final int slotColor;

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
        if (items.isEmpty()) return false;
        if (crsNames.isEmpty()) return true;

        CatalystRequirementSetLookup.build(access);
        for (var crsName : crsNames) {
            try {
                CatalystRequirementSetLookup.get(access, crsName);
            } catch (RuntimeException e) {
                CreateRNS.LOGGER.error("Yield references unknown catalyst requirement set \"{}\"", crsName);
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected Yield(float chance, List<WeightedItem> items, Optional<List<String>> crsNames,
            int slotColor
    ) {
        this.chance = chance;
        this.items = items;
        this.crsNames = crsNames.orElse(new ArrayList<>());
        this.slotColor = slotColor;

    }

    public static class WeightedItem {
        public static final int DEFAULT_WEIGHT = 1;
        private static final MapCodec<ItemWithFallbacks> STRICT_ITEM_FIELD = ItemWithFallbacks.STRICT_CODEC.fieldOf("item");
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
                .apply(i, (compat, weight) ->  new WeightedItem(ItemWithFallbacks.EMPTY, compat, weight)))
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
