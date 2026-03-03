package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.content.deposit.mining.recipe.Yield;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.RegistryAccess;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CatalystHandler {
    /// Returns true if at least one yield can be mined with the provided catalysts
    public static boolean isMiningPossible(RegistryAccess access, MiningRecipe recipe, Set<Catalyst> catalysts) {
        for (var y : recipe.getYields()) {
            boolean satisfied = true;
            for (var crsName : y.crsNames) {
                if (!CatalystRequirementSetLookup.get(access, crsName).isSatisfiableOrOptional(catalysts))  {
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
    protected final Object2ObjectOpenHashMap<String, List<Catalyst>> crsToCatalysts;

    public CatalystHandler(
            RegistryAccess access, MiningRecipe recipe, Set<Catalyst> catalysts, CatalystUsageStats stats
    ) {
        this.access = access;
        this.stats = stats;
        this.stats.setRegistryAccess(access);
        this.yields = recipe.getYields();

        // Map each unique CRS to all catalysts that can satisfy it
        var crsToCatalystsAll = yields.stream()
                .map(y -> y.crsNames)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(
                        crsName -> crsName,
                        crsName -> {
                            var crs = CatalystRequirementSetLookup.get(access, crsName);
                            var cats = crs.getRelevantCatalysts(catalysts);
                            // Meanwhile, also assign CRS'es to catalysts
                            for (var c : cats) {
                                c.assignCRS(crsName);
                            }
                            return cats;
                        },
                        (cats1, cats2) -> {
                            cats1.addAll(cats2);
                            return cats1;
                        },
                        Object2ObjectOpenHashMap::new
                ));

        // Go through all yields and enable only those that are satisfiable
        enabledYields = new IntArrayList();
        for (int i = 0; i < yields.size(); ++i) {
            var y = yields.get(i);
            boolean satisfiable = y.crsNames.stream().allMatch(crsName ->
                    CatalystRequirementSetLookup.get(access, crsName)
                            .isSatisfiableOrOptional(crsToCatalystsAll.get(crsName)));
            if (satisfiable) enabledYields.add(i);
        }

        // Filter only CRS'es that are referenced by enabled yields
        crsToCatalysts = crsToCatalystsAll.entrySet().stream()
                .filter(e -> enabledYields.intStream()
                        .mapToObj(yields::get)
                        .anyMatch(y -> y.crsNames.contains(e.getKey())))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().toList(),
                        (v1, v2) -> {
                            v1.addAll(v2);
                            return v1;
                        },
                        Object2ObjectOpenHashMap::new));
    }

    /// Uses necessary catalysts and returns the chance value of a mining operation succeeding for each yield
    public Int2FloatOpenHashMap useCatalysts(boolean simulate) {
        if (stats.lastChances == null) stats.lastChances = new Int2FloatOpenHashMap();
        else stats.lastChances.clear();

        if (stats.lastTickedCRSes == null) stats.lastTickedCRSes = new Object2FloatOpenHashMap<>();
        else stats.lastTickedCRSes.clear();

        var yieldChances = stats.lastChances;
        var tickedCRSes = stats.lastTickedCRSes;

        // Simulate catalyst usage and determine:
        //   1. Which yields can satisfy all requirements
        //   2. Which CRS'es need to be ticked
        for (int i : enabledYields) {
            var y = yields.get(i);
            var unsatisfiedOptional = new ObjectOpenHashSet<String>();
            boolean satisfiable = true;
            for (var crsName : y.crsNames) {
                var crs = CatalystRequirementSetLookup.get(access, crsName);
                if (crs.useCatalysts(crsToCatalysts.get(crsName), true) < 0f) {
                    // Required catalysts make yield unsatisfiable.
                    // Optionals don't, but should still be excluded from ticking.
                    if (crs.optional) unsatisfiedOptional.add(crsName);
                    else satisfiable = false;
                }
            }
            if (satisfiable) {
                yieldChances.put(i, 0);
            }
            for (var crsName : y.crsNames) {
                if (unsatisfiedOptional.contains(crsName)) continue;
                tickedCRSes.put(crsName, 0);
            }
        }

        // Use catalysts and calculate chance multipliers of all ticked CRS'es
        for (var crsName : tickedCRSes.keySet()) {
            var crs = CatalystRequirementSetLookup.get(access, crsName);
            float chanceMult = crs.useCatalysts(crsToCatalysts.get(crsName), simulate);
            if (chanceMult >= 0f) tickedCRSes.put(crsName, chanceMult);
        }

        // Calculate chance for each satisfied yield
        yieldChances.replaceAll((i, u) -> {
            var y = yields.get(i);
            var chance = y.chance;
            for (var crsName : y.crsNames) {
                if (tickedCRSes.containsKey(crsName)) chance *= tickedCRSes.getFloat(crsName);
            }
            return chance;
        });

        return yieldChances;
    }
}
