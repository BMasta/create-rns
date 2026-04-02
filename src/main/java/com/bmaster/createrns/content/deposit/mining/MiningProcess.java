package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSMisc;
import com.bmaster.createrns.RNSTags.RNSBlockTags;
import com.bmaster.createrns.content.deposit.info.DepositDurabilityManager;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipeLookup;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystHandler;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystUsageStats;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.bmaster.createrns.RNSMisc.LEVEL_DEPOSIT_DATA;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MiningProcess {
    public enum RateEstimationStatus {
        NONE, SOME, ALL
    }

    // 1 mine per block per hour at 256 points per tick
    public static final int BASE_PROGRESS = 256 * 60 * SharedConstants.TICKS_PER_MINUTE;
    public final Set<InnerProcess> innerProcesses = new ObjectOpenHashSet<>();
    public final Level level;

    public MiningProcess(Level l, Set<Catalyst> catalysts, Set<BlockPos> depositBlocks) {
        this.level = l;

        var depBlockCounts = depositBlocks.stream()
                .map(bp -> level.getBlockState(bp).getBlock())
                .filter(db -> db.defaultBlockState().is(RNSBlockTags.DEPOSIT_BLOCKS))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        var depBlockPositions = depositBlocks.stream()
                .filter(bp -> level.getBlockState(bp).getBlock().defaultBlockState().is(RNSBlockTags.DEPOSIT_BLOCKS))
                .collect(Collectors.groupingBy(bp -> level.getBlockState(bp).getBlock(), Collectors.toList()));

        for (var e : depBlockCounts.entrySet()) {
            var db = e.getKey();
            var recipe = MiningRecipeLookup.find(level, db);
            if (recipe == null) continue;
            var depBlockCount = e.getValue().intValue();
            innerProcesses.add(new InnerProcess(level, depBlockPositions.get(db), recipe,
                    BASE_PROGRESS / depBlockCount, catalysts));
        }
    }

    public boolean isPossible() {
        return !innerProcesses.isEmpty();
    }

    public void advance(int by) {
        if (!isPossible()) return;
        innerProcesses.forEach(p -> p.advance(by));
    }

    public Set<ItemStack> collect() {
        return innerProcesses.stream()
                .map(InnerProcess::collect)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public RateEstimationStatus getRateEstimationStatus() {
        boolean anyEstimated = false;
        boolean allEstimated = true;
        for (var p : innerProcesses) {
            if (!p.catStats.isChancesComputed()) allEstimated = false;
            else anyEstimated = true;
            if (anyEstimated && !allEstimated) return RateEstimationStatus.SOME;
        }
        return (allEstimated) ? RateEstimationStatus.ALL : RateEstimationStatus.NONE;
    }

    public Object2FloatOpenHashMap<Item> getEstimatedRates(int progressPerTick) {
        var rates = new Object2FloatOpenHashMap<Item>();
        var progressPerHour = 60 * SharedConstants.TICKS_PER_MINUTE * progressPerTick;

        for (var p : innerProcesses) {
            if (!p.catStats.isChancesComputed()) continue;
            var ys = p.recipe.getYields();
            float minesPerHour = (float) progressPerHour / p.maxProgress;
            for (int i = 0; i < ys.size(); ++i) {
                float chance = p.catStats.getLastComputedChance(i);
                if (chance <= 0f) continue;
                var y = ys.get(i);
                for (var wi : ys.get(i).items) {
                    rates.addTo(wi.item, minesPerHour * wi.weight / y.getTotalWeight() * chance);
                }
            }
        }

        return rates;
    }

    public void uninitialize() {
        for (var p : innerProcesses) {
            p.catStats.clear();
        }
    }

    public @Nullable CompoundTag write(Provider provider, boolean clientPacket) {

        ListTag progressTags = new ListTag();
        innerProcesses.stream()
                .map(p -> p.write(provider, clientPacket))
                .filter(Objects::nonNull)
                .forEach(progressTags::add);

        if (progressTags.isEmpty()) return null;
        CompoundTag root = new CompoundTag();
        root.put("inner_processes", progressTags);
        return root;
    }

    public void read(CompoundTag nbt, Provider provider, boolean clientPacket) {
        Object2ObjectOpenHashMap<Block, InnerProcess> dbToProcess = innerProcesses.stream()
                .collect(Collectors.toMap(p -> p.recipe.getDepositBlock(), p -> p,
                        (o, n) -> n, Object2ObjectOpenHashMap::new));

        ListTag progressTags = nbt.getList("inner_processes", Tag.TAG_COMPOUND);
        for (var pt : progressTags) {
            var dbStr = ((CompoundTag) pt).getString("deposit_block");
            var dbRL = ResourceLocation.tryParse(dbStr);
            if (dbRL == null) {
                CreateRNS.LOGGER.error("Could not parse resource location '{}' when deserializing mining process", dbStr);
                continue;
            }
            var db = BuiltInRegistries.BLOCK.getOptional(dbRL).orElse(null);
            if (db == null) {
                CreateRNS.LOGGER.error("Unknown block '{}' encountered when deserializing mining process", dbStr);
                continue;
            }
            var ip = dbToProcess.get(db);
            if (ip != null) ip.read((CompoundTag) pt, provider, clientPacket);
        }
    }

    public static class InnerProcess {
        public final Level level;
        public final List<BlockPos> depositPositions;
        public final MiningRecipe recipe;
        public int maxProgress;
        public int progress;
        public final CatalystUsageStats catStats;
        public final CatalystHandler catalystHandler;
        public long remainingUses;

        protected Queue<ItemStack> uncollectedItems = new ArrayDeque<>();

        public InnerProcess(
                Level level, List<BlockPos> depositPositions, MiningRecipe recipe, int maxProgress, Set<Catalyst> catalysts
        ) {
            this.level = level;
            this.depositPositions = depositPositions;
            this.recipe = recipe;
            this.maxProgress = maxProgress;
            this.progress = 0;
            if (!level.isClientSide) computeRemainingUses(); // Server computes/syncs, client uses
            this.catStats = new CatalystUsageStats();
            this.catalystHandler = new CatalystHandler(level.registryAccess(), recipe, catalysts, catStats);
        }

        public void advance(int by) {
            assert by > 0;
            progress += by;
        }

        public @Nullable ItemStack collect() {
            if (!uncollectedItems.isEmpty()) {
                return uncollectedItems.poll();
            }

            if (progress < maxProgress) return null;
            progress = progress - maxProgress; // Keep the extra progress

            var depData = level.getData(LEVEL_DEPOSIT_DATA.get());

            // Use a random deposit block
            var rollDep = level.random.nextIntBetweenInclusive(0, depositPositions.size() - 1);
            DepositDurabilityManager.useDepositBlock((ServerLevel) level, depositPositions.get(rollDep),
                    recipe.getReplacementBlock().defaultBlockState());

            // For each yield: use all of its catalysts, then roll for success and add to collection queue if successful
            var yields = recipe.getYields();
            var chances = catalystHandler.useCatalysts(false);
            for (var e : chances.int2FloatEntrySet()) {
                int yieldIdx = e.getIntKey();
                float chance = e.getFloatValue();
                if (chance > 0) {
                    var chanceRoll = (chance < 1f) ? level.random.nextFloat() : 0;
                    if (chance > chanceRoll) {
                        var myPrecious = new ItemStack(yields.get(yieldIdx).roll(level.random));
                        uncollectedItems.offer(myPrecious);
                        if (chance != 1) {
                            CreateRNS.LOGGER.trace("Successfully rolled for {} ({}% chance)", myPrecious, (int) (chance * 100));
                        }
                    }
                }
            }

            return uncollectedItems.isEmpty() ? null : uncollectedItems.poll();
        }

        public @Nullable CompoundTag write(Provider provider, boolean clientPacket) {
            CompoundTag root = new CompoundTag();
            var dbRL = BuiltInRegistries.BLOCK.getKey(recipe.getDepositBlock());
            root.putString("deposit_block", dbRL.toString());

            if (!level.isClientSide) {
                computeRemainingUses();
                root.putLong("remaining_uses", remainingUses);
            }

            if (clientPacket) {
                if (!catStats.isChancesComputed()) {
                    // Simulate catalyst usage to collect initial stats
                    catalystHandler.useCatalysts(true);
                }
                root.put("catalyst_stats", catStats.serializeNBT(provider));
            }
            if (!clientPacket) root.putInt("progress", progress);

            return root;

        }

        public void read(CompoundTag nbt, Provider provider, boolean clientPacket) {
            if (nbt.contains("remaining_uses")) {
                this.remainingUses = nbt.getLong("remaining_uses");
            }

            if (clientPacket && nbt.contains("catalyst_stats")) {
                this.catStats.deserializeNBT(provider, nbt.getCompound("catalyst_stats"));
            }
            if (!clientPacket) this.progress = nbt.getInt("progress");

        }

        /// Returns 0 if deposit is infinite. Only callable on server side
        private void computeRemainingUses() {
            var depData = level.getData(RNSMisc.LEVEL_DEPOSIT_DATA.get());
            AtomicBoolean infinite = new AtomicBoolean(false);
            long totalDur = depositPositions.stream()
                    .map(bp -> {
                        var dur = DepositDurabilityManager.getDepositBlockDurability((ServerLevel) level, bp);
                        if (dur == 0) infinite.set(true);
                        return dur;
                    })
                    .reduce(Long::sum).orElse(-1L);
            remainingUses = infinite.get() ? 0 : totalDur;
        }
    }
}
