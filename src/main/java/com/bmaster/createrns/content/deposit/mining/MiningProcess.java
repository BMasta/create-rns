package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSTags.RNSBlockTags;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipeLookup;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MiningProcess {
    public enum RateEstimationStatus {
        NONE, SOME, ALL
    }

    // 1 mine per block per hour at 256 points per tick
    public static final int BASE_PROGRESS = 256 * 60 * SharedConstants.TICKS_PER_MINUTE;
    public final Set<InnerProcess> innerProcesses = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public final Level level;

    public MiningProcess(
            Level l, Set<Catalyst> catalysts, Set<BlockPos> depositBlocks,
            @Nullable Object2FloatOpenHashMap<Block> savedProgress
    ) {
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
            var ip = new InnerProcess(level, depBlockPositions.get(db), recipe,
                    BASE_PROGRESS / depBlockCount, catalysts);
            // Restore progress from the saved percentage
            if (savedProgress != null && savedProgress.containsKey(db)) {
                ip.progress = (int) (savedProgress.getFloat(db) * ip.maxProgress);
            }
            innerProcesses.add(ip);
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
                .flatMap(ip -> ip.collect().stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public List<Holder<CatalystRequirementSet>> getLastSatisfiedCRSes() {
        return innerProcesses.stream()
                .flatMap(p ->
                        (p.catStats.lastTickedCRSes != null) ? p.catStats.lastTickedCRSes.stream() : Stream.of())
                .distinct()
                .map(crsId -> CatalystRequirementSet.get(level.registryAccess(), crsId))
                .sorted(Comparator.comparingInt(crs -> crs.value().displayPriority))
                .toList();
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

    public Object2FloatOpenHashMap<Block> uninitialize() {
        for (var ip : innerProcesses) {
            ip.catStats.clear();
        }
        return innerProcesses.stream().collect(Collectors.toMap(
                ip -> ip.recipe.getDepositBlock(),
                ip -> ip.progress / (float) ip.maxProgress,
                (v1, v2) -> Mth.clamp(v1 + v2, 0f, 1f),
                Object2FloatOpenHashMap::new
        ));
    }

    public @Nullable CompoundTag write(boolean clientPacket) {

        ListTag progressTags = new ListTag();
        innerProcesses.stream()
                .map(p -> p.write(clientPacket))
                .filter(Objects::nonNull)
                .forEach(progressTags::add);

        if (progressTags.isEmpty()) return null;
        CompoundTag root = new CompoundTag();
        root.put("inner_processes", progressTags);
        return root;
    }

    public void read(CompoundTag nbt, boolean clientPacket) {
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
            var db = ForgeRegistries.BLOCKS.getValue(dbRL);
            if (db == null) {
                CreateRNS.LOGGER.error("Unknown block '{}' encountered when deserializing mining process", dbStr);
                continue;
            }
            var ip = dbToProcess.get(db);
            if (ip != null) ip.read((CompoundTag) pt, clientPacket);
        }
    }
}
