package com.bmaster.createrns.content.deposit.mining.recipe;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSetLookup;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class Yield {
    public static final Codec<Yield> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.floatRange(0, 1).fieldOf("chance")
                            .orElse(1f)
                            .forGetter(y -> y.chance),
                    WeightedItem.CODEC.listOf().fieldOf("items")
                            .forGetter(y -> y.items),
                    Codec.STRING.listOf().optionalFieldOf("catalysts")
                            .forGetter(y -> (!y.crsNames.isEmpty()) ? Optional.of(y.crsNames) : Optional.empty()),
                    Codec.INT.fieldOf("jei_slot_color")
                            .orElse(0)
                            .forGetter(y -> y.slotColor))
            .apply(i, Yield::new));

    public static final Codec<Yield> STREAM_CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.FLOAT.fieldOf("chance")
                            .orElse(1f)
                            .forGetter(y -> y.chance),
                    WeightedItem.CODEC.listOf().fieldOf("items")
                            .forGetter(y -> y.items),
                    Codec.STRING.listOf().optionalFieldOf("catalysts")
                            .forGetter(y -> (!y.crsNames.isEmpty()) ? Optional.of(y.crsNames) : Optional.empty()),
                    Codec.INT.fieldOf("jei_slot_color")
                            .orElse(0)
                            .forGetter(y -> y.slotColor))
            .apply(i, Yield::new));

    public final float chance;
    public final List<WeightedItem> items;
    public final List<String> crsNames;
    public final int slotColor;

    private int totalWeight = 0;

    public List<CatalystRequirementSet> getCatalystRequirements(RegistryAccess access) {
        return crsNames.stream().map(crsName -> CatalystRequirementSetLookup.get(access, crsName)).toList();
    }

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
        Item result = items.get(items.size() - 1).item;
        float threshold = rng.nextFloat();
        float accChance = 0;
        for (var t : items) {
            accChance += (float) t.weight / getTotalWeight();
            if (accChance > threshold) {
                result = t.item;
                break;
            }
        }
        return result;
    }

    public record WeightedItem(Item item, int weight) {
        public static final Codec<WeightedItem> CODEC = RecordCodecBuilder.create(i -> i.group(
                        ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(WeightedItem::item),
                        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("weight").orElse(1).forGetter(WeightedItem::weight))
                .apply(i, WeightedItem::new));
        public static final Codec<WeightedItem> STREAM_CODEC = RecordCodecBuilder.create(i -> i.group(
                        ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(WeightedItem::item),
                        Codec.INT.fieldOf("weight").orElse(1).forGetter(WeightedItem::weight))
                .apply(i, WeightedItem::new));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected Yield(float chance, List<WeightedItem> items, Optional<List<String>> crsNames, int slotColor) {
        this.chance = chance;
        this.items = items;
        this.crsNames = crsNames.orElse(new ArrayList<>());
        this.slotColor = slotColor;

    }
}
