package com.bmaster.createrns.capability.orechunkdata;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.util.Hasher;
import com.simibubi.create.Create;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;
import java.util.stream.Collectors;

public class OreChunkClassifier {
    public static final OreChunkClassifier INSTANCE = new OreChunkClassifier(
            List.of(
                    new ItemStack(Items.IRON_NUGGET),
                    new ItemStack(Items.COPPER_INGOT),
                    new ItemStack(Items.GOLD_NUGGET),
                    new ItemStack(Items.REDSTONE),
                    new ItemStack(Items.EMERALD),
                    new ItemStack(Items.LAPIS_LAZULI)
            ),
            0.0004f,
            Map.ofEntries(
                    Map.entry(Items.IRON_INGOT, 1.0f),
                    Map.entry(Items.COPPER_INGOT, 1.0f),
                    Map.entry(Items.GOLD_INGOT, 0.2f),
                    Map.entry(Items.REDSTONE, 0.2f),
                    Map.entry(Items.EMERALD, 0.2f),
                    Map.entry(Items.LAPIS_LAZULI, 0.2f)
            ),
            Map.ofEntries(
                    Map.entry(OreChunkPurity.NONE, 0),
                    Map.entry(OreChunkPurity.IMPURE, 6),
                    Map.entry(OreChunkPurity.NORMAL, 3),
                    Map.entry(OreChunkPurity.PURE, 1)
            )
    );

    private final List<ItemStack> allowedOres;
    private final float baseProbability;
    private final Map<Item, Float> oreProbabilityMultipliers;
    private final TreeMap<OreChunkPurity, Float> purityProbabilities;

    public OreChunkData classify(LevelChunk chunk) {
        Level level = chunk.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return OreChunkData.EMPTY;
        Hasher hasher = new Hasher(serverLevel.getSeed(), chunk.getPos().toLong());

        ItemStack oreStack = getOre(hasher);
        OreChunkPurity purity = getPurity(hasher);

        if (oreStack.isEmpty()) return OreChunkData.EMPTY;
        return new OreChunkData(true, oreStack, purity);
    }

    public Optional<ChunkPos> getNearestOreChunk(ChunkPos pos, long seed, Item ore, int maxChessboardDistance) {
        ChunkPos oreChunkPos = null;
        Queue<Tuple<ChunkPos, Direction>> q = new ArrayDeque<>();
        Hasher hasher = new Hasher(seed, pos.toLong());
        LongOpenHashSet visited = new LongOpenHashSet();
        int chunksProcessed = 0;

        q.offer(new Tuple<>(pos, Direction.UP)); // Up/down == all 4 sides
        while(!q.isEmpty()) {
            Tuple<ChunkPos, Direction> e = q.poll();
            ChunkPos curPos = e.getA();
            Direction dir = e.getB();
            hasher.reset(seed, curPos.toLong());

            if (pos.getChessboardDistance(curPos) > maxChessboardDistance) continue;
            if (visited.contains(curPos.toLong())) continue;
            visited.add(curPos.toLong());

            if (getOre(hasher).getItem() == ore) {
                oreChunkPos = curPos;
                break;
            }

            if (dir != Direction.SOUTH) {
                    q.offer(new Tuple<>(new ChunkPos(curPos.x, curPos.z - 1), Direction.NORTH));
            }
            if (dir != Direction.NORTH) {
                    q.offer(new Tuple<>(new ChunkPos(curPos.x, curPos.z + 1), Direction.SOUTH));
            }
            if (dir != Direction.EAST) {
                    q.offer(new Tuple<>(new ChunkPos(curPos.x - 1, curPos.z), Direction.WEST));
            }
            if (dir != Direction.WEST) {
                    q.offer(new Tuple<>(new ChunkPos(curPos.x + 1, curPos.z), Direction.EAST));
            }
            chunksProcessed++;
        }
        if (oreChunkPos != null) {
            int x1 = oreChunkPos.getBlockX(8);
            int z1 = oreChunkPos.getBlockZ(8);
            int x2 = pos.getBlockX(8);
            int z2 = pos.getBlockZ(8);
            int dist = Math.round((float) Math.hypot(x2 - x1, z2 - z1));
            CreateRNS.LOGGER.info("Processed {} chunks to find {} at {},{}, {} blocks away", chunksProcessed, ore,
                    oreChunkPos.getBlockX(8), oreChunkPos.getBlockZ(8), dist);
        } else {
            CreateRNS.LOGGER.info("Processed {} chunks, but found no ore chunks of type {}",
                    chunksProcessed, ore);
        }

        return Optional.ofNullable(oreChunkPos);
    }

    private ItemStack getOre(Hasher hasher) {
        List<ItemStack> acceptedOres = new ArrayList<>(allowedOres.size());
        for (ItemStack oreStack : allowedOres) {
            float oreProbabilityMultiplier = oreProbabilityMultipliers.getOrDefault(oreStack.getItem(), 1.0f);
            float roll = hasher.roll();
            if (roll < (baseProbability * oreProbabilityMultiplier)) {
                acceptedOres.add(oreStack);
            }
        }

        // No success rolls for any of the ores
        if (acceptedOres.isEmpty()) return ItemStack.EMPTY;

        // Select accepted ore
        ItemStack acceptedOreStack;
        if (acceptedOres.size() == 1) {
            acceptedOreStack = acceptedOres.get(0);
        } else {
            // If more than one ore got accepted, pick randomly
            float tiebreakRoll = hasher.roll() * (acceptedOres.size() - 1);
            int idx = Math.round(tiebreakRoll);
            acceptedOreStack = acceptedOres.get(idx);
        }

        return acceptedOreStack;
    }

    private OreChunkPurity getPurity(Hasher hasher) {
        OreChunkPurity acceptedPurity = purityProbabilities.lastKey();
        float probabilitySum = 0;
        float purityRoll = hasher.roll();
        for (Map.Entry<OreChunkPurity, Float> e : purityProbabilities.entrySet()) {
            probabilitySum += e.getValue();
            if (purityRoll < probabilitySum) {
                acceptedPurity = e.getKey();
                break;
            }
        }
        return acceptedPurity;
    }

    private OreChunkClassifier(List<ItemStack> allowedOres,
                               float baseProbability,
                               Map<Item, Float> oreProbabilityMultipliers,
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
                                e -> ((float) (e.getValue()) / purityWeightSum),
                                (a, b) -> b,
                                TreeMap::new
                        )
                );
    }
}
