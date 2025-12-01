package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.content.deposit.mining.recipe.Yield;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.List;
import java.util.Set;

public class CatalystHandler {
    protected final List<Yield> yields;
    protected final IntArrayList enabledYields = new IntArrayList();
    protected final Int2ObjectOpenHashMap<Set<Catalyst>> requiredCatalysts = new Int2ObjectOpenHashMap<>();
    protected final Int2ObjectOpenHashMap<Set<Catalyst>> optionalCatalysts = new Int2ObjectOpenHashMap<>();
    protected final Object2FloatOpenHashMap<Catalyst> catalystToAddedChance = new Object2FloatOpenHashMap<>();

    public CatalystHandler(MiningRecipe recipe, Set<Catalyst> catalysts) {
        this.yields = recipe.getYields();

        // For each yield, go over all available catalysts and map what chance each of them adds to it.
        // Additionally, if some non-optional requirements were not satisfied, disqualify the yield altogether.
        for (int i = 0; i < yields.size(); ++i) {
            var y = yields.get(i);
            boolean allRequiredSatisfied = true;

            // Collect the yield's requirements
            Set<CatalystRequirement> requirements = new ObjectOpenHashSet<>();
            requirements.add(y.resonanceRequirement);
            requirements.add(y.shatteringResonanceRequirement);
            requirements.add(y.stabilizingResonanceRequirement);

            // For each requirement, go over available catalysts and see if any of them match, as well as sum up the chances they add
            for (var cr : requirements) {
                boolean isCROptional = cr.isOptional();
                boolean satisfied = false;
                for (var c : catalysts) {
                    if (!cr.isSatisfiedBy(c)) continue;

                    var chance = cr.getChance(c);
                    satisfied = true;

                    if (isCROptional) {
                        optionalCatalysts.computeIfAbsent(i, ignored -> new ObjectOpenHashSet<>()).add(c);
                    } else {
                        requiredCatalysts.computeIfAbsent(i, ignored -> new ObjectOpenHashSet<>()).add(c);
                    }
                    catalystToAddedChance.put(c, chance);
                }
                // This requirement was necessary! Yield is disqualified.
                if (!isCROptional && !satisfied) allRequiredSatisfied = false;
            }

            // Yield does not provide a good way to establish equivalence, so we save its index in the recipe list
            if (allRequiredSatisfied) enabledYields.add(i);
        }
    }

    /// Returns true if at least one yield can be mined with the registered catalysts
    public boolean isMiningPossible() {
        return !enabledYields.isEmpty();
    }

    /// Returns the chance value of a mining operation succeeding
    public float useCatalysts(int index) {
        // Yield must be enabled
        if (!enabledYields.contains(index)) return 0;
        var yield = yields.get(index);
        float chance = yield.chance;

        if (requiredCatalysts.containsKey(index)) {
            // Check that all required catalysts are available for use before we commit
            boolean allRequiredSatisfied = true;
            for (var c : requiredCatalysts.get(index)) {
                if (!c.use(true)) allRequiredSatisfied = false;
            }
            if (!allRequiredSatisfied) return 0;

            for (var c : requiredCatalysts.get(index)) {
                if (c.use(false)) {
                    chance += catalystToAddedChance.getFloat(c);
                }
            }
        }

        if (optionalCatalysts.containsKey(index)) {
            for (var c : optionalCatalysts.get(index)) {
                if (c.use(false)) {
                    chance += catalystToAddedChance.getFloat(c);
                }
            }
        }

        return Math.min(chance, 1);
    }
}
