package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.content.deposit.mining.recipe.Yield;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CatalystHandler implements INBTSerializable<CompoundTag> {
    /// Returns true if at least one yield can be mined with the provided catalysts
    public static boolean isMiningPossible(MiningRecipe recipe, Set<Catalyst> catalysts) {
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

    protected @Nullable ObjectOpenHashSet<ResourceLocation> currentActiveCRSes = null;
    protected @Nullable PerYieldCatalystEffects currentEffects = null;

    protected final List<Yield> yields;
    protected final IntArrayList enabledYields;
    protected Set<Catalyst> catalysts;
    protected final Object2ObjectOpenHashMap<ResourceLocation, CatalystMapping> crsIdToMapping;

    public CatalystHandler(MiningRecipe recipe, Set<Catalyst> catalysts) {
        this.yields = recipe.getYields();
        this.catalysts = catalysts;

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

    public Set<ResourceLocation> getCurrentActiveCRSes() {
        if (currentActiveCRSes == null) reserve(new CatalystReservations());
        assert currentActiveCRSes != null;
        return Set.copyOf(currentActiveCRSes);
    }

    public CatalystEffects getCurrentCatalystEffects(int i) {
        if (currentEffects == null) reserve(new CatalystReservations());
        assert currentEffects != null;
        return currentEffects.get(i);
    }

    public PerYieldCatalystEffects reserve(CatalystReservations reservations) {
        if (currentActiveCRSes == null) currentActiveCRSes = new ObjectOpenHashSet<>();
        else currentActiveCRSes.clear();
        if (currentEffects == null) currentEffects = new PerYieldCatalystEffects();
        else currentEffects.clear();

        var res = new CatalystReservations();

        // Calculate which CRSes should be activated and the cost of doing so
        for (int i : enabledYields) {
            var y = yields.get(i);

            // Satisfy all required CRSes
            var newRes = res.copy();
            boolean satisfied = true;
            Set<ResourceLocation> yieldTickedCRSes = new HashSet<>();
            for (var crsId : y.getCRSIds()) {
                var crsMapping = crsIdToMapping.get(crsId);
                if (currentActiveCRSes.contains(crsId) || yieldTickedCRSes.contains(crsId) || crsMapping.crs().optional) {
                    continue;
                }

                if (!crsMapping.crs().reserve(crsMapping.catalysts(), newRes)) {
                    satisfied = false;
                    break;
                } else {
                    yieldTickedCRSes.add(crsId);
                }
            }
            if (!satisfied) continue;
            currentActiveCRSes.addAll(yieldTickedCRSes);
            res.update(newRes);

            // Try to satisfy as many optional CRSes as possible
            for (var crsId : y.getCRSIds()) {
                var mapping = crsIdToMapping.get(crsId);
                if (currentActiveCRSes.contains(crsId) || !mapping.crs().optional) continue;
                if (mapping.crs().reserve(mapping.catalysts(), res)) {
                    currentActiveCRSes.add(crsId);
                }
            }
        }

        // Calculate per-yield catalyst effects
        for (int i : enabledYields) {
            var y = yields.get(i);
            float chanceMult = 1;
            double countMult = 1;
            for (var crsId : y.getCRSIds()) {
                var crs = crsIdToMapping.get(crsId).crs();
                if (!currentActiveCRSes.contains(crsId)) {
                    if (!crs.optional) {
                        chanceMult = 0;
                        countMult = 0;
                        break;
                    }
                } else {
                    chanceMult *= crs.chanceMult;
                    countMult *= crs.countMult;
                }
            }
            currentEffects.set(i, new CatalystEffects(chanceMult, countMult));
        }

        reservations.update(res);
        return currentEffects;
    }

    public boolean consume(CatalystReservations reservations) {
        return reservations.consume(catalysts);
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var root = new CompoundTag();
        if (currentActiveCRSes == null || currentEffects == null) reserve(new CatalystReservations());
        assert currentActiveCRSes != null && currentEffects != null;

        var ltcTag = new ListTag();
        for (var crsId : currentActiveCRSes) {
            ltcTag.add(StringTag.valueOf(crsId.toString()));
        }
        root.put("current_active_catalysts", ltcTag);

        var lceMapTag = new CompoundTag();
        for (var e : currentEffects.entrySet()) {
            var effects = e.getValue();
            var lceTag = new CompoundTag();
            lceTag.putFloat("chance_multiplier", effects.chanceMult());
            lceTag.putDouble("count_multiplier", effects.countMult());
            lceMapTag.put(e.getIntKey() + "", lceTag);
        }
        root.put("current_catalyst_effects", lceMapTag);

        return root;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        currentActiveCRSes = null;
        currentEffects = null;
        if (!tag.contains("current_active_catalysts") || !tag.contains("current_catalyst_effects")) return;

        var eMapTag = tag.getCompound("current_catalyst_effects");
        var crsTag = tag.getList("current_active_catalysts", Tag.TAG_STRING);

        currentActiveCRSes = new ObjectOpenHashSet<>();
        for (int i = 0; i < crsTag.size(); ++i) {
            var idStr = crsTag.getString(i);
            ResourceLocation crsId;

            // Legacy fallback
            if (!idStr.contains(":")) crsId = CreateRNS.asResource(idStr);
            else crsId = ResourceLocation.tryParse(idStr);

            if (crsId == null) {
                CreateRNS.LOGGER.error("Failed to deserialize catalyst id: {}", idStr);
                continue;
            }

            currentActiveCRSes.add(crsId);
        }

        currentEffects = new PerYieldCatalystEffects();
        for (var yieldIdx : eMapTag.getAllKeys()) {
            var eTag = eMapTag.getCompound(yieldIdx);
            currentEffects.set(Integer.parseInt(yieldIdx), new CatalystEffects(
                    eTag.getFloat("chance_multiplier"),
                    eTag.getDouble("count_multiplier")));
        }
    }

    public record CatalystEffects(float chanceMult, double countMult) {
        public static final CatalystEffects DISABLE = new CatalystEffects(0, 0);
    }

    public static class PerYieldCatalystEffects {
        private final Int2ObjectOpenHashMap<CatalystEffects> yieldIdxToEffects = new Int2ObjectOpenHashMap<>();

        public void set(int yieldIdx, CatalystEffects effects) {
            yieldIdxToEffects.put(yieldIdx, effects);
        }

        public CatalystEffects get(int yieldIdx) {
            return yieldIdxToEffects.getOrDefault(yieldIdx, CatalystEffects.DISABLE);
        }

        public Int2ObjectMap.FastEntrySet<CatalystEffects> entrySet() {
            return yieldIdxToEffects.int2ObjectEntrySet();
        }

        public void clear() {
            yieldIdxToEffects.clear();
        }
    }

    protected record CatalystMapping(CatalystRequirementSet crs, List<Catalyst> catalysts) {
    }
}
