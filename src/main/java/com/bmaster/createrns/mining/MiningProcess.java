package com.bmaster.createrns.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSTags;
import com.bmaster.createrns.mining.recipe.MiningRecipe;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MiningProcess {
    public final int tier;
    public final Set<InnerProcess> innerProcesses = new ObjectOpenHashSet<>();

    public MiningProcess(Level l, int tier, Set<BlockPos> depositBlocks, int baseProgress) {
        this.tier = tier;
        setYields(l, depositBlocks, baseProgress);
    }

    public boolean isPossible() {
        return !innerProcesses.isEmpty();
    }

    public void advance(int by) {
        if (!isPossible()) return;
        innerProcesses.forEach(p -> p.advance(by));
    }

    public Set<ItemStack> collect(RandomSource rng) {
        return innerProcesses.stream()
                .map(p -> p.collect(rng))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public void setYields(Level l, Set<BlockPos> depositBlocks, int baseProgress) {
        var depBlocks = depositBlocks.stream()
                .map(bp -> l.getBlockState(bp).getBlock())
                .filter(db -> db.defaultBlockState().is(RNSTags.Block.DEPOSIT_BLOCKS))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        innerProcesses.clear();
        for (var e : depBlocks.entrySet()) {
            var recipe = MiningRecipeLookup.find(l, tier, e.getKey());
            if (recipe == null) continue;
            var depBlockCount = e.getValue().intValue();
            innerProcesses.add(new InnerProcess(recipe, baseProgress / depBlockCount));
        }
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
            var db = BuiltInRegistries.BLOCK.getOptional(dbRL).orElse(null);
            if (db == null) {
                CreateRNS.LOGGER.error("Unknown block '{}' encountered when deserializing mining process", dbStr);
                continue;
            }
            dbToProcess.get(db).setProgressFromNBT((CompoundTag) pt);
        }
    }

    public static class InnerProcess {
        public final MiningRecipe recipe;
        public int maxProgress;
        public int progress;

        public InnerProcess(MiningRecipe recipe, int maxProgress) {
            this.recipe = recipe;
            this.maxProgress = maxProgress;
            this.progress = 0;
        }

        public void advance(int by) {
            assert by > 0;
            progress += by;
        }

        public @Nullable ItemStack collect(RandomSource rng) {
            if (progress < maxProgress) return null;
            progress = progress - maxProgress; // Keep the extra progress
            return new ItemStack(recipe.getYield().roll(rng));
        }

        public @Nullable CompoundTag getProgressAsNBT() {
            var dbRL = BuiltInRegistries.BLOCK.getKeyOrNull(recipe.getDepositBlock());
            if (dbRL == null) return null;

            CompoundTag ipTag = new CompoundTag();
            ipTag.putString("deposit_block", dbRL.toString());
            ipTag.putInt("progress", progress);
            ipTag.putInt("max_progress", maxProgress);

            return ipTag;
        }

        /// Assumes that the yield of the tag matches the yield of this instance
        public void setProgressFromNBT(CompoundTag nbt) {
            this.progress = nbt.getInt("progress");
            this.maxProgress = nbt.getInt("max_progress");
        }
    }
}
