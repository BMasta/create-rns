package com.bmaster.createrns.content.deposit.mining.recipe;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance.ResonanceCatalystRequirement;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance.ShatteringResonanceCatalystRequirement;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance.StabilizingResonanceCatalystRequirement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

public class Yield {
    public static final Codec<Yield> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.floatRange(0, 1).fieldOf("chance").orElse(0f).forGetter(y -> y.chance),
                    WeightedItem.CODEC.listOf().fieldOf("items").forGetter(y -> y.items),
                    ResonanceCatalystRequirement.CODEC.fieldOf("resonance")
                            .orElse(new ResonanceCatalystRequirement(true, 0, 0, 0))
                            .forGetter(y -> y.resonanceRequirement),
                    ShatteringResonanceCatalystRequirement.CODEC.fieldOf("shattering_resonance")
                            .orElse(new ShatteringResonanceCatalystRequirement(true, 0, 0, 0))
                            .forGetter(y -> y.shatteringResonanceRequirement),
                    StabilizingResonanceCatalystRequirement.CODEC.fieldOf("stabilizing_resonance")
                            .orElse(new StabilizingResonanceCatalystRequirement(true, 0, 0, 0))
                            .forGetter(y -> y.stabilizingResonanceRequirement))
            .apply(i, Yield::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Yield> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, y -> y.chance,
            ByteBufCodecs.collection(ArrayList::new, WeightedItem.STREAM_CODEC), y -> new ArrayList<>(y.items),
            ResonanceCatalystRequirement.STREAM_CODEC, y -> y.resonanceRequirement,
            ShatteringResonanceCatalystRequirement.STREAM_CODEC, y -> y.shatteringResonanceRequirement,
            StabilizingResonanceCatalystRequirement.STREAM_CODEC, y -> y.stabilizingResonanceRequirement,
            Yield::new
    );

    public final float chance;
    public final List<WeightedItem> items;
    public final ResonanceCatalystRequirement resonanceRequirement;
    public final ShatteringResonanceCatalystRequirement shatteringResonanceRequirement;
    public final StabilizingResonanceCatalystRequirement stabilizingResonanceRequirement;
    private int totalWeight = 0;

    public Yield(float chance, List<WeightedItem> items,
                 ResonanceCatalystRequirement resonanceRequirement,
                 ShatteringResonanceCatalystRequirement shatteringResonanceRequirement,
                 StabilizingResonanceCatalystRequirement stabilizingResonanceRequirement) {
        this.chance = chance;
        this.items = items;
        this.resonanceRequirement = resonanceRequirement;
        this.shatteringResonanceRequirement = shatteringResonanceRequirement;
        this.stabilizingResonanceRequirement = stabilizingResonanceRequirement;
    }

    public int getTotalWeight() {
        if (totalWeight == 0) {
            totalWeight = items.stream()
                    .map(y -> y.chanceWeight)
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
            accChance += (float) t.chanceWeight / getTotalWeight();
            if (accChance > threshold) {
                result = t.item;
                break;
            }
        }
        return result;
    }

    public record WeightedItem(Item item, int chanceWeight) {
        public static final Codec<WeightedItem> CODEC = RecordCodecBuilder.create(i -> i.group(
                        BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(WeightedItem::item),
                        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("weight").orElse(1).forGetter(WeightedItem::chanceWeight))
                .apply(i, WeightedItem::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, WeightedItem> STREAM_CODEC = StreamCodec.of(
                WeightedItem::toNetwork, WeightedItem::fromNetwork);

        public static void toNetwork(RegistryFriendlyByteBuf buffer, WeightedItem type) {
            ByteBufCodecs.registry(Registries.ITEM).encode(buffer, type.item());
            ByteBufCodecs.INT.encode(buffer, type.chanceWeight);
        }

        public static WeightedItem fromNetwork(RegistryFriendlyByteBuf buffer) {
            return new WeightedItem(
                    ByteBufCodecs.registry(Registries.ITEM).decode(buffer),
                    ByteBufCodecs.INT.decode(buffer)
            );
        }
    }
}
