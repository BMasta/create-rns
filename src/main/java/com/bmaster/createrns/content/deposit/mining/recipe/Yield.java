package com.bmaster.createrns.content.deposit.mining.recipe;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSetLookup;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
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
        Item result = items.getLast().getItem();
        float threshold = rng.nextFloat();
        float accChance = 0;
        for (var t : items) {
            accChance += (float) t.weight / getTotalWeight();
            if (accChance > threshold) {
                result = t.getItem();
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
        public final int weight;
        protected final List<ResourceLocation> itemRls;
        protected final List<TagKey<Item>> tags;
        protected @Nullable Item item;

        public static final Codec<WeightedItem> CODEC = RecordCodecBuilder.create(i -> i.group(
                        ResourceLocation.CODEC.listOf().optionalFieldOf("item_candidates", List.of())
                                .forGetter(wi -> wi.itemRls),
                        TagKey.codec(Registries.ITEM).listOf().optionalFieldOf("tag_candidates", List.of())
                                .forGetter(wi -> wi.tags),
                        Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("weight", 1)
                                .forGetter(wi -> wi.weight))
                .apply(i, WeightedItem::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, TagKey<Item>> ITEM_TAG_STREAM_CODEC = StreamCodec.of(
                (buffer, tag) -> ResourceLocation.STREAM_CODEC.encode(buffer, tag.location()),
                buffer -> TagKey.create(Registries.ITEM, ResourceLocation.STREAM_CODEC.decode(buffer))
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, WeightedItem> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.collection(ArrayList::new, ResourceLocation.STREAM_CODEC), wi -> new ArrayList<>(wi.itemRls),
                ByteBufCodecs.collection(ArrayList::new, ITEM_TAG_STREAM_CODEC), wi -> new ArrayList<>(wi.tags),
                ByteBufCodecs.INT, wi -> wi.weight,
                WeightedItem::new
        );

        public WeightedItem(List<ResourceLocation> itemRls, List<TagKey<Item>> tags, int weight) {
            if (itemRls.isEmpty() && tags.isEmpty()) {
                throw new IllegalArgumentException("Weighted item must define at least an item or a tag");
            }
            this.itemRls = itemRls;
            this.tags = tags;
            this.weight = weight;
        }

        public boolean initialize(RegistryAccess access) {
            if (item != null) return true;

            for (var rl : itemRls) {
                item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
                if (item != null) return true;
            }

            for (var tag : tags) {
                // Pick the first item from the tag. "First" is determined by the order in which the items were tagged.
                // Defaults to AIR of tag does not exist or does not contain any items.
                var hs = access.lookupOrThrow(Registries.ITEM).get(tag).orElse(null);
                item = (hs != null) ? hs.stream().map(Holder::value).findFirst().orElse(null) : null;
                if (item != null) return true;
            }

            CreateRNS.LOGGER.error("Failed to resolve weighted item from item candidates {} and tag candidates {}",
                    itemRls, tags);
            return false;
        }

        public Item getItem() {
            return (item != null) ? item : Items.AIR;
        }
    }
}
