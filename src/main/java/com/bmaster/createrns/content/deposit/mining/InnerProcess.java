package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.content.deposit.info.DepositDurabilityManager;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystHandler;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystReservations;
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
    public final CatalystHandler catalystHandler;
    public final int maxProgress;
    public int progress;

    public InnerProcess(
            Level level, List<BlockPos> depositPositions, MiningRecipe recipe, int maxProgress, Set<Catalyst> catalysts
    ) {
        this.level = level;
        this.depositPositions = depositPositions;
        this.recipe = recipe;
        this.maxProgress = maxProgress;
        this.progress = 0;
        this.catalystHandler = new CatalystHandler(recipe, catalysts);
    }

    public void advance(int by) {
        assert by > 0;
        progress += by;
    }

    public Set<ItemStack> collect(MiningStats stats) {
        if (progress < maxProgress) return Set.of();
        if (!(level instanceof ServerLevel sl)) return Set.of();

        progress = progress - maxProgress; // Keep the extra progress

        // For each yield, reserve required resources and roll for items
        var spoils = new HashSet<ItemStack>();
        var yields = recipe.getYields();
        var reservations = new CatalystReservations();
        var perYieldEffects = catalystHandler.reserve(reservations);
        for (var e : perYieldEffects.entrySet()) {
            int yieldIdx = e.getIntKey();
            var effects = e.getValue();
            var myPrecious = yields.get(yieldIdx).roll(level.random, effects);
            if (myPrecious.isEmpty()) continue;
            spoils.add(myPrecious);
        }

        if (spoils.isEmpty()) return spoils;

        // Use a random deposit block.
        // This can trigger an area reclaim for the process holder, which would invalidate the current process.
        // Regardless, this method can still finish and return the collected items before the process is destroyed.
        var rollDep = level.random.nextIntBetweenInclusive(0, depositPositions.size() - 1);
        if (!DepositDurabilityManager.useDepositBlock(sl, depositPositions.get(rollDep),
                recipe.getReplacementBlock().defaultBlockState())) return Set.of();
        if (stats.isStarted()) stats.remainingResources.computeIfPresent(recipe.getDepositBlock(),
                (k, v) -> v > 0 ? v - 1 : v);

        // Consume reserved resources
        if (!catalystHandler.consume(reservations)) return Set.of();

        return spoils;
    }

    public @Nullable CompoundTag write(HolderLookup.Provider provider, boolean clientPacket) {
        CompoundTag root = new CompoundTag();
        var dbRL = BuiltInRegistries.BLOCK.getKey(recipe.getDepositBlock());
        root.putString("deposit_block", dbRL.toString());
        if (!clientPacket) root.putInt("progress", progress);
        else root.put("catalyst_handler", catalystHandler.serializeNBT(provider));

        return root;

    }

    public void read(CompoundTag nbt, HolderLookup.Provider provider, boolean clientPacket) {
        if (!clientPacket) this.progress = nbt.getInt("progress");
        else catalystHandler.deserializeNBT(provider, nbt.getCompound("catalyst_handler"));
    }

    /// Returns 0 if deposit is infinite. Only callable on server side
    public long getRemainingResources() {
        var infinite = new AtomicBoolean(false);
        long total = depositPositions.stream()
                .map(bp -> {
                    var dur = DepositDurabilityManager.get((ServerLevel) level, bp);
                    if (dur == 0) infinite.set(true);
                    return dur;
                })
                .reduce(Long::sum).orElse(-1L);
        return infinite.get() ? 0 : total;
    }
}
