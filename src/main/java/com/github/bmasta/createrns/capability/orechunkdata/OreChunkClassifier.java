package com.github.bmasta.createrns.capability.orechunkdata;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;
import java.util.stream.Collectors;

public class OreChunkClassifier {
    public static final OreChunkClassifier DEFAULT = new OreChunkClassifier(
            List.of(
                    new ItemStack(Items.IRON_NUGGET),
                    new ItemStack(Items.COPPER_INGOT),
                    new ItemStack(Items.GOLD_NUGGET),
                    new ItemStack(Items.REDSTONE),
                    new ItemStack(Items.EMERALD),
                    new ItemStack(Items.LAPIS_LAZULI)
            ),
            0.0004,
            Map.ofEntries(
                    Map.entry(Items.IRON_INGOT, 1.0),
                    Map.entry(Items.COPPER_INGOT, 1.0),
                    Map.entry(Items.GOLD_INGOT, 0.2),
                    Map.entry(Items.REDSTONE, 0.2),
                    Map.entry(Items.EMERALD, 0.2),
                    Map.entry(Items.LAPIS_LAZULI, 0.2)
            ),
            Map.ofEntries(
                    Map.entry(OreChunkPurity.NONE, 0),
                    Map.entry(OreChunkPurity.IMPURE, 6),
                    Map.entry(OreChunkPurity.NORMAL, 3),
                    Map.entry(OreChunkPurity.PURE, 1)
            )
    );

    private final List<ItemStack> allowedOres;
    private final double baseProbability;
    private final Map<Item, Double> oreProbabilityMultipliers;
    private final TreeMap<OreChunkPurity, Double> purityProbabilities;

    public OreChunkData classify(LevelChunk chunk) {
        Level level = chunk.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return OreChunkData.EMPTY;

        long xz = mix64(((long) chunk.getPos().x << 32) ^ chunk.getPos().z);
        SplittableRandom rng = new SplittableRandom(mix64(serverLevel.getSeed()) ^ xz);

        List<ItemStack> acceptedOres = new ArrayList<>(allowedOres.size());

        // Roll for each allowed ore
        for (ItemStack oreStack : allowedOres) {
            double oreProbabilityMultiplier = oreProbabilityMultipliers.getOrDefault(oreStack.getItem(), 1.0);
            double oreRoll = rng.nextDouble();
            if (oreRoll < (baseProbability * oreProbabilityMultiplier)) {
                acceptedOres.add(oreStack);
            }
        }

        // No success rolls for any of the ores
        if (acceptedOres.isEmpty()) return OreChunkData.EMPTY;

        // Calculate ore stack
        ItemStack acceptedOreStack;
        if (acceptedOres.size() == 1) {
            acceptedOreStack = acceptedOres.get(0);
        } else {
            // If more than one ore got accepted, pick randomly
            int idx = rng.nextInt(0, acceptedOres.size());
            acceptedOreStack = acceptedOres.get(idx);
        }

        // Calculate purity
        OreChunkPurity acceptedPurity = purityProbabilities.lastKey();
        double probabilitySum = 0;
        double purityRoll = rng.nextDouble();
        for (Map.Entry<OreChunkPurity, Double> e : purityProbabilities.entrySet()) {
            probabilitySum += e.getValue();
            if (purityRoll < probabilitySum) {
                acceptedPurity = e.getKey();
                break;
            }
        }

        return new OreChunkData(true, acceptedOreStack, acceptedPurity);
    }

    private OreChunkClassifier(List<ItemStack> allowedOres,
                               double baseProbability,
                               Map<Item, Double> oreProbabilityMultipliers,
                               Map<OreChunkPurity, Integer> purityProbabilityWeights) {
        this.baseProbability = baseProbability;
        this.oreProbabilityMultipliers = oreProbabilityMultipliers;

        this.allowedOres = allowedOres.stream().sorted((item1, item2) ->
                        item1.getItem().getDescriptionId().compareToIgnoreCase(item2.getItem().getDescriptionId()))
                .toList();

        long purityWeightSum = purityProbabilityWeights.values().stream().mapToLong(Integer::longValue).sum();
        this.purityProbabilities = purityProbabilityWeights.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> ((double) (e.getValue()) / purityWeightSum),
                                (a, b) -> b,
                                TreeMap::new
                        )
                );
    }

    private static long mix64(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
