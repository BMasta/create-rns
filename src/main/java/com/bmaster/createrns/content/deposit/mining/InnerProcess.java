package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSMisc;
import com.bmaster.createrns.content.deposit.info.DepositDurabilityManager;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystHandler;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystUsageStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class InnerProcess {
    public final Level level;
    public final List<BlockPos> depositPositions;
    public final MiningRecipe recipe;
    public int maxProgress;
    public int progress;
    public final CatalystUsageStats catStats;
    public final CatalystHandler catalystHandler;
    public long remainingUses;

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

    public Set<ItemStack> collect() {
        if (progress < maxProgress) return Set.of();
        if (!(level instanceof ServerLevel sl)) return Set.of();

        progress = progress - maxProgress; // Keep the extra progress

        // Use a random deposit block.
        // This can trigger an area reclaim for the process holder, which would invalidate the current process.
        // Regardless, this method can still finish and return the collected items before the process is destroyed.
        var rollDep = level.random.nextIntBetweenInclusive(0, depositPositions.size() - 1);
        if (!DepositDurabilityManager.useDepositBlock(sl, depositPositions.get(rollDep),
                recipe.getReplacementBlock().defaultBlockState())) return Set.of();

        // For each yield: use all of its catalysts, then roll for success and add to collection queue if successful
        var spoils = new HashSet<ItemStack>();
        var yields = recipe.getYields();
        var chances = catalystHandler.useCatalysts(false);
        for (var e : chances.int2FloatEntrySet()) {
            int yieldIdx = e.getIntKey();
            float chance = e.getFloatValue();
            if (chance > 0) {
                var chanceRoll = (chance < 1f) ? level.random.nextFloat() : 0;
                if (chance > chanceRoll) {
                    var myPrecious = new ItemStack(yields.get(yieldIdx).roll(level.random));
                    spoils.add(myPrecious);
                    if (chance < 1) {
                        CreateRNS.LOGGER.trace("Successfully rolled for {} ({}% chance)", myPrecious, (int) (chance * 100));
                    }
                }
            }
        }

        return spoils;
    }

    public @Nullable CompoundTag write(HolderLookup.Provider provider, boolean clientPacket) {
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

    public void read(CompoundTag nbt, HolderLookup.Provider provider, boolean clientPacket) {
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
                    var dur = DepositDurabilityManager.get((ServerLevel) level, bp);
                    if (dur == 0) infinite.set(true);
                    return dur;
                })
                .reduce(Long::sum).orElse(-1L);
        remainingUses = infinite.get() ? 0 : totalDur;
    }
}
