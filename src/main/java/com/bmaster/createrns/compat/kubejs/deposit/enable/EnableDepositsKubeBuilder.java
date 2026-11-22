package com.bmaster.createrns.compat.kubejs.deposit.enable;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSPacks;
import com.bmaster.createrns.compat.kubejs.util.StartupKubeBuilder;
import com.bmaster.createrns.compat.kubejs.deposit.enable.EnableDepositsKubeEvent.AvailableStructure;
import com.bmaster.createrns.compat.kubejs.util.JSMethod;
import com.bmaster.createrns.data.deposit.DepositResources;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class EnableDepositsKubeBuilder extends StartupKubeBuilder {
    private static final int MAX_PLACEMENT_DISTANCE = 4096;

    private final ResourceLocation id;
    private final Supplier<Map<ResourceLocation, AvailableStructure>> availableStructures;
    private final Map<ResourceLocation, SelectedStructure> selectedStructures = new LinkedHashMap<>();
    private Integer separation;
    private Integer spacing;
    private Integer salt;

    public EnableDepositsKubeBuilder(
            ResourceLocation id, Map<ResourceLocation, AvailableStructure> availableStructures
    ) {
        this(id, () -> Map.copyOf(availableStructures));
    }

    public EnableDepositsKubeBuilder(
            ResourceLocation id, Supplier<Map<ResourceLocation, AvailableStructure>> availableStructures
    ) {
        super("rnsEnableDeposits", "overworld|nether");
        this.id = id;
        this.availableStructures = availableStructures;
    }

    @Info("""
            Makes a deposit scannable and enables worldgen. If a built-in deposit is not selected
            with this method, its deposit block will also be disabled.
            """)
    @JSMethod
    public EnableDepositsKubeBuilder deposit(String structureId) {
        return deposit(structureId, null, true);
    }

    @Info("""
            Makes a deposit scannable and enables worldgen. If a built-in deposit is not selected
            with this method, its deposit block will also be disabled.
            """)
    @JSMethod
    public EnableDepositsKubeBuilder deposit(String structureId, int weight) {
        return deposit(structureId, weight, true);
    }

    @Info("""
            Makes a deposit scannable and enables worldgen. If a built-in deposit is not selected
            with this method, its deposit block will also be disabled.
            """)
    @JSMethod
    public EnableDepositsKubeBuilder deposit(String structureId, boolean enableWorldgen) {
        return deposit(structureId, null, enableWorldgen);
    }

    @Info("""
            Makes a deposit scannable and enables worldgen. If a built-in deposit is not selected
            with this method, its deposit block will also be disabled.
            """)
    @JSMethod
    public EnableDepositsKubeBuilder deposit(String structureId, @Nullable Integer weight, boolean enableWorldgen) {
        if (weight != null && weight <= 0) {
            error("Structure set weight must be positive", true);
            return this;
        }

        var id = ResourceLocation.parse(structureId);
        if (selectedStructures.containsKey(id)) {
            error("Deposit structure already selected in structure set " + this.id + ": " + structureId, true);
            return this;
        }
        selectedStructures.put(id, new SelectedStructure(id, weight, enableWorldgen));
        return this;
    }

    @Info("Sets the average distance between deposits in chunks.")
    @JSMethod
    public EnableDepositsKubeBuilder spacing(int value) {
        if (value <= 0 || value > MAX_PLACEMENT_DISTANCE) {
            error("Structure set spacing must be between 1 and " + MAX_PLACEMENT_DISTANCE, true);
            return this;
        }

        spacing = value;
        return this;
    }

    @Info("Sets the minimum distance between deposits in chunks.")
    @JSMethod
    public EnableDepositsKubeBuilder separation(int value) {
        if (value < 0 || value > MAX_PLACEMENT_DISTANCE) {
            error("Structure set separation must be between 0 and " + MAX_PLACEMENT_DISTANCE, true);
            return this;
        }

        separation = value;
        return this;
    }

    @Info("Sets a seed for the random spread of deposits across the world")
    @JSMethod
    public EnableDepositsKubeBuilder salt(int value) {
        if (value < 0) {
            error("Structure set salt must be non-negative", true);
            return this;
        }
        salt = value;
        return this;
    }

    public ResourceLocation id() {
        return id;
    }

    public void gatherResources(DepositResources resources) {
        if (!isValid()) return;
        boolean nether = id.equals(DepositResources.structureSetId(CreateRNS.ID, Level.NETHER));
        var structures = worldgenStructures().stream()
                .map(entry -> new DepositResources.WeightedStructure(entry.id(), entry.resolvedWeight())).toList();
        resources.structureSets.add(new DepositResources.StructureSet(id, structures,
                resolvedSpacing(nether ? RNSPacks.DEFAULT_NETHER_SPACING : RNSPacks.DEFAULT_SPACING),
                resolvedSeparation(nether ? RNSPacks.DEFAULT_NETHER_SEPARATION : RNSPacks.DEFAULT_SEPARATION),
                resolvedSalt(nether ? RNSPacks.NETHER_SALT : RNSPacks.SALT)));
    }

    public List<SelectedStructure> selectedStructures() {
        return selectedStructures.values().stream().filter(entry -> entry.valid).toList();
    }

    public List<SelectedStructure> worldgenStructures() {
        var structures = new ArrayList<SelectedStructure>();

        for (var entry : selectedStructures.values()) {
            if (!entry.valid || !entry.enableWorldgen() || !entry.validWeight) continue;
            structures.add(entry);
        }

        return structures;
    }

    public int resolvedSeparation(int defaultValue) {
        return separation != null ? separation : defaultValue;
    }

    public int resolvedSpacing(int defaultValue) {
        return spacing != null ? spacing : defaultValue;
    }

    public int resolvedSalt(int defaultValue) {
        return salt != null ? salt : defaultValue;
    }

    @Override
    protected void assertValid() {
        boolean nether = id.equals(DepositResources.structureSetId(CreateRNS.ID, Level.NETHER));
        int effectiveSpacing = resolvedSpacing(nether ? RNSPacks.DEFAULT_NETHER_SPACING : RNSPacks.DEFAULT_SPACING);
        int effectiveSeparation = resolvedSeparation(nether ? RNSPacks.DEFAULT_NETHER_SEPARATION : RNSPacks.DEFAULT_SEPARATION);
        if (effectiveSpacing <= effectiveSeparation) {
            error("Structure set spacing must be greater than separation", true);
        }
        var available = availableStructures.get();
        for (var selected : selectedStructures.values()) {
            var structure = available.get(selected.id);
            if (structure == null) {
                error("Unknown deposit structure: " + selected.id, true);
                selected.valid = false;
                continue;
            }
            var dimension = structure.builtInDimension();
            if (dimension != null && !id.equals(DepositResources.structureSetId(CreateRNS.ID, dimension))) {
                error("Built-in deposit structure " + selected.id + " belongs to " + dimension.location()
                        + " and cannot be selected in structure set " + id, true);
                selected.valid = false;
                continue;
            }
            selected.valid = structure.valid();
            selected.defaultWeight = structure.weight();
            selected.validWeight = selected.overriddenWeight != null || selected.defaultWeight != null;
            if (selected.valid && selected.enableWorldgen && !selected.validWeight) {
                error("Structure '" + selected.id + "' does not have an assigned weight", false);
            }
        }
    }

    public static class SelectedStructure {
        private final ResourceLocation id;
        private final Integer overriddenWeight;
        private final boolean enableWorldgen;
        private Integer defaultWeight;
        private boolean valid = true;
        private boolean validWeight;

        private SelectedStructure(
                ResourceLocation id, @Nullable Integer overriddenWeight, boolean enableWorldgen
        ) {
            this.id = id;
            this.overriddenWeight = overriddenWeight;
            this.enableWorldgen = enableWorldgen;
        }

        public ResourceLocation id() {
            return id;
        }

        public boolean enableWorldgen() {
            return enableWorldgen;
        }

        public int resolvedWeight() {
            if (overriddenWeight == null && defaultWeight == null) {
                throw new IllegalStateException("Structure '" + id + "' does not have an assigned weight");
            }

            return overriddenWeight != null ? overriddenWeight : defaultWeight;
        }
    }
}
