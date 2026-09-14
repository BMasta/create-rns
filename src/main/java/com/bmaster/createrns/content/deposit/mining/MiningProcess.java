package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSTags.RNSBlockTags;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipeLookup;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MiningProcess {
    // 1 mine per block per hour at 256 points per tick
    public static final int BASE_PROGRESS = 256 * 60 * SharedConstants.TICKS_PER_MINUTE;
    public final Set<InnerProcess> innerProcesses = new ObjectOpenHashSet<>();
    public final Level level;

    public MiningProcess(
            Level l, Set<Catalyst> catalysts, Set<BlockPos> depositBlocks,
            @Nullable Object2FloatOpenHashMap<Block> savedProgress, @Nullable MiningStats stats
    ) {
        this.level = l;

        var depBlockCounts = depositBlocks.stream()
                .map(bp -> level.getBlockState(bp).getBlock())
                .filter(db -> db.defaultBlockState().is(RNSBlockTags.DEPOSIT_BLOCKS))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        var depBlockPositions = depositBlocks.stream()
                .filter(bp -> level.getBlockState(bp).getBlock().defaultBlockState().is(RNSBlockTags.DEPOSIT_BLOCKS))
                .collect(Collectors.groupingBy(bp -> level.getBlockState(bp).getBlock(), Collectors.toList()));

        if (stats != null && !l.isClientSide) {
            stats.remainingResources.clear();
            stats.catStats.clear();
        }
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
            // Clients receive stats from the server
            if (stats != null && !l.isClientSide) {
                stats.remainingResources.put(db, ip.getRemainingResources());
                stats.catStats.recompute(ip);
            }
        }
    }

    public boolean isPossible() {
        return !innerProcesses.isEmpty();
    }

    public void advance(int by) {
        if (!isPossible()) return;
        innerProcesses.forEach(p -> p.advance(by));
    }

    public Set<ItemStack> collect(MiningStats stats) {
        boolean statsStarted = stats.isStarted();
        var spoils = new HashSet<ItemStack>();

        for (var innerProcess : innerProcesses) {
            boolean infinite = stats.remainingResources.ofInnerProcess(innerProcess) == 0;
            var ipSpoils = innerProcess.collect(stats);
            if (statsStarted && !ipSpoils.isEmpty()) {
                for (var stack : ipSpoils) stats.minedItems.addTo(stack.getItem(), stack.getCount());
                stats.minedResources++;
                if (!infinite) stats.consumedResources++;
            }
            spoils.addAll(ipSpoils);
        }

        return spoils;
    }

    public Object2FloatOpenHashMap<Block> uninitialize() {
        return innerProcesses.stream().collect(Collectors.toMap(
                ip -> ip.recipe.getDepositBlock(),
                ip -> ip.progress / (float) ip.maxProgress,
                (v1, v2) -> Mth.clamp(v1 + v2, 0f, 1f),
                Object2FloatOpenHashMap::new
        ));
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
}
