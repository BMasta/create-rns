package com.bmaster.createrns.content.deposit.mining.recipe;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirement;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance.ResonanceCatalystRequirement;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance.ShatteringResonanceCatalystRequirement;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance.StabilizingResonanceCatalystRequirement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Yield {
    public static final Codec<Yield> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.floatRange(0, 1)
                            .fieldOf("chance")
                            .orElse(0f)
                            .forGetter(y -> y.chance),
                    WeightedItem.CODEC.listOf()
                            .fieldOf("items")
                            .forGetter(y -> y.items),
                    ResonanceCatalystRequirement.CODEC
                            .optionalFieldOf("resonance")
                            .forGetter(y -> Optional.ofNullable(y.resonanceRequirement)),
                    ShatteringResonanceCatalystRequirement.CODEC
                            .optionalFieldOf("shattering_resonance")
                            .forGetter(y -> Optional.ofNullable(y.shatteringResonanceRequirement)),
                    StabilizingResonanceCatalystRequirement.CODEC
                            .optionalFieldOf("stabilizing_resonance")
                            .forGetter(y -> Optional.ofNullable(y.stabilizingResonanceRequirement)))
            .apply(i, Yield::new));

    public static final Codec<Yield> STREAM_CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.FLOAT
                            .fieldOf("chance")
                            .orElse(0f)
                            .forGetter(y -> y.chance),
                    WeightedItem.STREAM_CODEC.listOf()
                            .fieldOf("items")
                            .forGetter(y -> y.items),
                    ResonanceCatalystRequirement.STREAM_CODEC
                            .optionalFieldOf("resonance")
                            .forGetter(y -> Optional.ofNullable(y.resonanceRequirement)),
                    ShatteringResonanceCatalystRequirement.STREAM_CODEC
                            .optionalFieldOf("shattering_resonance")
                            .forGetter(y -> Optional.ofNullable(y.shatteringResonanceRequirement)),
                    StabilizingResonanceCatalystRequirement.STREAM_CODEC
                            .optionalFieldOf("stabilizing_resonance")
                            .forGetter(y -> Optional.ofNullable(y.stabilizingResonanceRequirement)))
            .apply(i, Yield::new));

    public final float chance;
    public final List<WeightedItem> items;
    public final @Nullable ResonanceCatalystRequirement resonanceRequirement;
    public final @Nullable ShatteringResonanceCatalystRequirement shatteringResonanceRequirement;
    public final @Nullable StabilizingResonanceCatalystRequirement stabilizingResonanceRequirement;

    protected final List<CatalystRequirement> allCatalystRequirements = new ArrayList<>();
    private int totalWeight = 0;

    public List<CatalystRequirement> getCatalystRequirements() {
        return allCatalystRequirements;
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
    protected Yield(float chance, List<WeightedItem> items,
            Optional<ResonanceCatalystRequirement> resonanceRequirement,
            Optional<ShatteringResonanceCatalystRequirement> shatteringResonanceRequirement,
            Optional<StabilizingResonanceCatalystRequirement> stabilizingResonanceRequirement
    ) {
        this.chance = chance;
        this.items = items;
        this.resonanceRequirement = unpackAndAddRequirement(resonanceRequirement);
        this.shatteringResonanceRequirement = unpackAndAddRequirement(shatteringResonanceRequirement);
        this.stabilizingResonanceRequirement = unpackAndAddRequirement(stabilizingResonanceRequirement);

    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected <CR extends CatalystRequirement> CR unpackAndAddRequirement(Optional<CR> req) {
        req.ifPresent(allCatalystRequirements::add);
        return req.orElse(null);
    }
}
