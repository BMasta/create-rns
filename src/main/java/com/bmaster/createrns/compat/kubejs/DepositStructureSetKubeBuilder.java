package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.compat.kubejs.DepositStructureSetKubeEvent.AvailableStructure;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositStructureSetKubeBuilder {

    private final ResourceLocation id;
    private final Map<ResourceLocation, AvailableStructure> availableStructures;
    private final Map<ResourceLocation, SelectedStructure> selectedStructures = new LinkedHashMap<>();
    private Integer separation;
    private Integer spacing;
    private Integer salt;

    public DepositStructureSetKubeBuilder(
            ResourceLocation id, Map<ResourceLocation, AvailableStructure> availableStructures
    ) {
        this.id = id;
        this.availableStructures = Map.copyOf(availableStructures);
    }

    @Info("Makes a deposit scannable and enables worldgen.")
    public DepositStructureSetKubeBuilder deposit(String structureId) {
        return deposit(structureId, null, true);
    }

    @Info("Makes a deposit scannable and enables worldgen.")
    public DepositStructureSetKubeBuilder deposit(String structureId, int weight) {
        return deposit(structureId, weight, true);
    }

    @Info("Makes a deposit scannable and optionally enables worldgen.")
    public DepositStructureSetKubeBuilder deposit(String structureId, boolean enableWorldgen) {
        return deposit(structureId, null, enableWorldgen);
    }

    @Info("Makes a deposit scannable and optionally enables worldgen.")
    public DepositStructureSetKubeBuilder deposit(String structureId, @Nullable Integer weight, boolean enableWorldgen) {
        if (weight != null && weight <= 0) {
            throw new IllegalArgumentException("Structure set weight must be positive");
        }

        var id = ResourceLocation.parse(structureId);
        var structure = availableStructures.get(id);
        if (structure == null) {
            throw new IllegalArgumentException("Unknown deposit structure: " + structureId);
        }

        var selected = new SelectedStructure(structure, weight, enableWorldgen);
        if (selectedStructures.putIfAbsent(id, selected) != null) {
            throw new IllegalStateException("Deposit structure already selected in structure set " + this.id + ": " + structureId);
        }

        return this;
    }

    @Info("Sets the average distance between deposits in chunks.")
    public DepositStructureSetKubeBuilder spacing(int value) {
        if (value <= 0) throw new IllegalArgumentException("Structure set spacing must be positive");

        spacing = value;
        validateSpacingAndSeparation();
        return this;
    }

    @Info("Sets the minimum distance between deposits in chunks.")
    public DepositStructureSetKubeBuilder separation(int value) {
        if (value < 0) throw new IllegalArgumentException("Structure set separation must be non-negative");

        separation = value;
        validateSpacingAndSeparation();
        return this;
    }

    @Info("Sets a seed for the random spread of deposits across the world")
    public DepositStructureSetKubeBuilder salt(int value) {
        salt = value;
        return this;
    }

    ResourceLocation id() {
        return id;
    }

    List<SelectedStructure> selectedStructures() {
        return new ArrayList<>(selectedStructures.values());
    }

    List<SelectedStructure> worldgenStructures() {
        var structures = new ArrayList<SelectedStructure>();

        for (var entry : selectedStructures.values()) {
            if (!entry.enableWorldgen()) continue;

            entry.resolvedWeight();
            structures.add(entry);
        }

        return structures;
    }

    int resolvedSeparation(int defaultValue) {
        return separation != null ? separation : defaultValue;
    }

    int resolvedSpacing(int defaultValue) {
        return spacing != null ? spacing : defaultValue;
    }

    int resolvedSalt(int defaultValue) {
        return salt != null ? salt : defaultValue;
    }

    private void validateSpacingAndSeparation() {
        if (spacing != null && separation != null && spacing <= separation) {
            throw new IllegalArgumentException("Structure set spacing must be greater than separation");
        }
    }

    static class SelectedStructure {
        private final AvailableStructure structure;
        private final Integer overriddenWeight;
        private final boolean enableWorldgen;

        private SelectedStructure(
                AvailableStructure structure, @Nullable Integer overriddenWeight, boolean enableWorldgen
        ) {
            this.structure = structure;
            this.overriddenWeight = overriddenWeight;
            this.enableWorldgen = enableWorldgen;
        }

        ResourceLocation id() {
            return structure.id();
        }

        boolean enableWorldgen() {
            return enableWorldgen;
        }

        int resolvedWeight() {
            if (overriddenWeight == null && structure.weight() == null) {
                throw new IllegalStateException("Structure '" + structure.id() + "' does not have an assigned weight");
            }

            return overriddenWeight != null ? overriddenWeight : structure.weight();
        }
    }
}
