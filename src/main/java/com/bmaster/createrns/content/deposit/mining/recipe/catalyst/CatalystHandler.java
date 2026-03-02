package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.content.deposit.mining.recipe.Yield;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.RegistryAccess;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CatalystHandler {
    protected final RegistryAccess access;
    protected final List<Yield> yields;
    protected final IntArrayList enabledYields;
    protected final Object2ObjectOpenHashMap<String, List<Catalyst>> crsToCatalysts;

    public CatalystHandler(RegistryAccess access, MiningRecipe recipe, Set<Catalyst> catalysts) {
        this.access = access;
        this.yields = recipe.getYields();

        // Map each unique CRS to all catalysts that can satisfy it
        var crsToCatalystsAll = yields.stream()
                .map(y -> y.crsNames)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(
                        n -> n,
                        n -> {
                            var crs = CatalystRequirementSetLookup.get(access, n);
                            return crs.getRelevantCatalysts(catalysts);
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
            boolean satisfiable = y.crsNames.stream()
                    .allMatch(crsName -> CatalystRequirementSetLookup.get(access, crsName)
                            .isSatisfiedBy(crsToCatalystsAll.get(crsName)));
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

    /// Returns true if at least one yield can be mined with the registered catalysts
    public boolean isMiningPossible() {
        return !enabledYields.isEmpty();
    }

    /// Uses necessary catalysts and returns the chance value of a mining operation succeeding for each yield
    public Int2FloatOpenHashMap useCatalysts() {
        var satisfiedYields = new Int2FloatOpenHashMap();
        var tickedCRSes = new Object2FloatOpenHashMap<String>();

        // Simulate catalyst usage and determine:
        //   1. Which yields can satisfy all requirements
        //   2. Which CRS'es need to be ticked
        for (int i : enabledYields) {
            var y = yields.get(i);

            boolean satisfied = true;
            for (var crsName : y.crsNames) {
                satisfied &= (CatalystRequirementSetLookup.get(access, crsName)
                        .useCatalysts(crsToCatalysts.get(crsName), true) >= 0f);
            }
            if (satisfied) {
                satisfiedYields.put(i, 0);
                for (var crsName : y.crsNames) {
                    tickedCRSes.put(crsName, 0);
                }
            }
        }

        // Use catalysts and calculate chance multipliers of all ticked CRS'es
        for (var crsName : tickedCRSes.keySet()) {
            float chanceMult = CatalystRequirementSetLookup.get(access, crsName)
                    .useCatalysts(crsToCatalysts.get(crsName), false);
            tickedCRSes.put(crsName, chanceMult);
        }

        // Calculate chance for each satisfied yield
        satisfiedYields.replaceAll((i, u) -> {
            var y = yields.get(i);
            var chance = y.chance;
            for (var crsName : y.crsNames) {
                chance *= tickedCRSes.getFloat(crsName);
            }
            return chance;
        });

        return satisfiedYields;
    }
}
