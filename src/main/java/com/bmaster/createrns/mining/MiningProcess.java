package com.bmaster.createrns.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSTags;
import com.bmaster.createrns.deposit.capability.IDepositIndex;
import com.bmaster.createrns.mining.recipe.MiningRecipe;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MiningProcess {
    // 1 mine per block per hour at 256 points per tick
    public static final int BASE_PROGRESS = 256 * 60 * SharedConstants.TICKS_PER_MINUTE;
    public final int tier;
    public final Set<InnerProcess> innerProcesses = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public final Level level;

    public MiningProcess(Level l, int tier, Set<BlockPos> depositBlocks) {
        this.tier = tier;
        this.level = l;

        var depBlockCounts = depositBlocks.stream()
                .map(bp -> level.getBlockState(bp).getBlock())
                .filter(db -> db.defaultBlockState().is(RNSTags.Block.DEPOSIT_BLOCKS))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        var depBlockPositions = depositBlocks.stream()
                .filter(bp -> level.getBlockState(bp).getBlock().defaultBlockState().is(RNSTags.Block.DEPOSIT_BLOCKS))
                .collect(Collectors.groupingBy(bp -> level.getBlockState(bp).getBlock(), Collectors.toList()));

        for (var e : depBlockCounts.entrySet()) {
            var db = e.getKey();
            var recipe = MiningRecipeLookup.find(level, db);
            if (recipe == null || tier < recipe.getTier()) continue;
            var depBlockCount = e.getValue().intValue();
            innerProcesses.add(new InnerProcess(level, depBlockPositions.get(db), recipe, BASE_PROGRESS / depBlockCount));
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

    public Object2FloatOpenHashMap<Item> getEstimatedRates(int progressPerTick) {
        var rates = new Object2FloatOpenHashMap<Item>();
        var progressPerHour = 60 * SharedConstants.TICKS_PER_MINUTE * progressPerTick;

        for (var p : innerProcesses) {
            var minesPerHour = (float) progressPerHour / p.maxProgress;
            var y = p.recipe.getYield();
            for (var t : y.types) {
                rates.addTo(t.item(), minesPerHour * t.chanceWeight() / y.getTotalWeight());
            }
        }

        return rates;
    }

    public CompoundTag getProgressAsNBT() {
        CompoundTag root = new CompoundTag();

        ListTag progressTags = new ListTag();
        innerProcesses.stream()
                .map(InnerProcess::getProgressAsNBT)
                .filter(Objects::nonNull)
                .forEach(progressTags::add);
        root.put("per_deposit_progress", progressTags);

        return root;
    }

    public void setProgressFromNBT(CompoundTag nbt) {
        Object2ObjectOpenHashMap<Block, InnerProcess> dbToProcess = innerProcesses.stream()
                .collect(Collectors.toMap(p -> p.recipe.getDepositBlock(), p -> p,
                        (o, n) -> n, Object2ObjectOpenHashMap::new));

        ListTag progressTags = nbt.getList("per_deposit_progress", Tag.TAG_COMPOUND);
        for (var pt : progressTags) {
            var dbStr = ((CompoundTag) pt).getString("deposit_block");
            var dbRL = ResourceLocation.tryParse(dbStr);
            if (dbRL == null) {
                CreateRNS.LOGGER.error("Could not parse resource location '{}' when deserializing mining process", dbStr);
                continue;
            }
            var db = ForgeRegistries.BLOCKS.getValue(dbRL);
            if (db == null) {
                CreateRNS.LOGGER.error("Unknown block '{}' encountered when deserializing mining process", dbStr);
                continue;
            }
            var ip = dbToProcess.get(db);
            if (ip != null) ip.setProgressFromNBT((CompoundTag) pt);
        }
    }

    public static class InnerProcess {
        public final Level level;
        public final List<BlockPos> depositPositions;
        public final MiningRecipe recipe;
        public int maxProgress;
        public int progress;
        public long remainingUses;

        public InnerProcess(Level level, List<BlockPos> depositPositions, MiningRecipe recipe, int maxProgress) {
            this.level = level;
            this.depositPositions = depositPositions;
            this.recipe = recipe;
            this.maxProgress = maxProgress;
            this.progress = 0;
            if (!level.isClientSide) computeRemainingUses(); // Server computes/syncs, client uses
        }

        public void advance(int by) {
            assert by > 0;
            progress += by;
        }

        public @Nullable ItemStack collect() {
            if (progress < maxProgress) return null;
            if (!(level instanceof ServerLevel sl)) return null;
            var depIdx = IDepositIndex.fromLevel(sl);
            if (depIdx == null) return null;

            progress = progress - maxProgress; // Keep the extra progress

            // Use a random deposit block
            var roll = level.random.nextIntBetweenInclusive(0, depositPositions.size() - 1);
            depIdx.useDepositBlock(depositPositions.get(roll), recipe.getReplacementBlock().defaultBlockState());

            return new ItemStack(recipe.getYield().roll(level.random));
        }

        public @Nullable CompoundTag getProgressAsNBT() {
            var dbRL = ForgeRegistries.BLOCKS.getKey(recipe.getDepositBlock());
            if (dbRL == null) return null;

            CompoundTag ipTag = new CompoundTag();
            ipTag.putString("deposit_block", dbRL.toString());
            ipTag.putInt("progress", progress);
            computeRemainingUses();
            ipTag.putLong("remaining_uses", remainingUses);

            return ipTag;
        }

        /// Assumes that the yield of the tag matches the yield of this instance
        public void setProgressFromNBT(CompoundTag nbt) {
            this.progress = nbt.getInt("progress");
            this.remainingUses = nbt.getLong("remaining_uses");
        }

        /// Returns 0 if deposit is infinite. Only callable on server side
        private void computeRemainingUses() {
            if (!(level instanceof ServerLevel sl))
                throw new IllegalStateException("Clients may not call this function");
            var depIdx = IDepositIndex.fromLevel(sl);
            if (depIdx == null) return;
            AtomicBoolean infinite = new AtomicBoolean(false);
            long totalDur = depositPositions.stream()
                    .map(bp -> {
                        var dur = depIdx.getDepositBlockDurability(bp);
                        if (dur == 0) infinite.set(true);
                        return dur;
                    })
                    .reduce(Long::sum).orElse(-1L);
            remainingUses = infinite.get() ? 0 : totalDur;
        }
    }
}
