package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.content.deposit.mining.recipe.Yield;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CatalystHandler {
    /// Returns true if at least one yield can be mined with the provided catalysts
    public static boolean isMiningPossible(RegistryAccess access, MiningRecipe recipe, Set<Catalyst> catalysts) {
        for (var y : recipe.getYields()) {
            boolean satisfied = true;
            for (var crs : y.getCRSes()) {
                if (!crs.value().isSatisfiableOrOptional(catalysts)) {
                    satisfied = false;
                }
            }
            if (satisfied) return true;
        }
        return false;
    }

    protected final CatalystUsageStats stats;
    protected final RegistryAccess access;
    protected final List<Yield> yields;
    protected final IntArrayList enabledYields;
    protected final Object2ObjectOpenHashMap<ResourceLocation, CatalystMapping> crsIdToMapping;

    public CatalystHandler(
            RegistryAccess access, MiningRecipe recipe, Set<Catalyst> catalysts, CatalystUsageStats stats
    ) {
        this.access = access;
        this.stats = stats;
        this.stats.setRegistryAccess(access);
        this.yields = recipe.getYields();

        // Map each unique CRS to all catalysts that can satisfy it
        var crsIdToMappingUnfiltered = new Object2ObjectOpenHashMap<ResourceLocation, CatalystMapping>();
        for (var yield : yields) {
            for (var crsHolder : yield.getCRSes()) {
                var crs = crsHolder.value();
                crsIdToMappingUnfiltered.computeIfAbsent(CatalystRequirementSet.id(crsHolder), unused ->
                        new CatalystMapping(crs, crs.getRelevantCatalysts(catalysts).stream().toList()));
            }
        }

        // Go through all yields and enable only those that are satisfiable
        enabledYields = new IntArrayList();
        for (int i = 0; i < yields.size(); ++i) {
            var y = yields.get(i);
            boolean satisfiable = y.getCRSes().stream().allMatch(crs ->
                    crs.value().isSatisfiableOrOptional(catalysts));
            if (satisfiable) enabledYields.add(i);
        }

        // Filter only CRS'es that are referenced by enabled yields
        crsIdToMapping = crsIdToMappingUnfiltered.entrySet().stream()
                .filter(e -> enabledYields.intStream()
                        .mapToObj(yields::get)
                        .anyMatch(y -> y.getCRSIds().contains(e.getKey())))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (o, n) -> o,
                        Object2ObjectOpenHashMap::new));
    }

    /// Uses necessary catalysts and returns the chance value of a mining operation succeeding for each yield
    public Int2FloatOpenHashMap useCatalysts(boolean simulate) {
        if (stats.lastChances == null) stats.lastChances = new Int2FloatOpenHashMap();
        else stats.lastChances.clear();

        if (stats.lastTickedCRSes == null) stats.lastTickedCRSes = new ObjectOpenHashSet<>();
        else stats.lastTickedCRSes.clear();

        var yieldChances = stats.lastChances;
        var tickedCRSes = stats.lastTickedCRSes;

        // Simulate catalyst usage and determine:
        //   1. Which yields can satisfy all requirements
        //   2. Which CRS'es need to be ticked
        for (int i : enabledYields) {
            var y = yields.get(i);
            var unsatisfiedOptional = new ObjectOpenHashSet<ResourceLocation>();
            boolean satisfiable = true;
            for (var crsId : y.getCRSIds()) {
                var mapping = crsIdToMapping.get(crsId);
                if (!mapping.crs().useCatalysts(mapping.catalysts(), true)) {
                    // Required catalysts make yield unsatisfiable.
                    // Optionals don't, but should still be excluded from ticking.
                    if (mapping.crs().optional) unsatisfiedOptional.add(crsId);
                    else satisfiable = false;
                }
            }
            if (satisfiable) {
                yieldChances.put(i, 0);
            }
            for (var crsId : y.getCRSIds()) {
                if (unsatisfiedOptional.contains(crsId)) continue;
                tickedCRSes.add(crsId);
            }
        }

        // Use catalysts and calculate chance multipliers of all ticked CRS'es
        for (var crsId : tickedCRSes) {
            var mapping = crsIdToMapping.get(crsId);
            if (!simulate) mapping.crs().useCatalysts(mapping.catalysts(), false);
        }

        // Calculate chance for each satisfied yield
        yieldChances.replaceAll((i, u) -> {
            var y = yields.get(i);
            var chance = y.chance;
            for (var crs : y.getCRSes()) {
                if (tickedCRSes.contains(CatalystRequirementSet.id(crs))) {
                    chance *= crs.value().chanceMult;
                }
            }
            return chance;
        });

        return yieldChances;
    }

    protected record CatalystMapping(CatalystRequirementSet crs, List<Catalyst> catalysts) {
    }
}
