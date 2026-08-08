package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.compat.kubejs.EnableDepositsKubeEvent.AvailableStructure;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class EnableDepositsKubeBuilder extends SourcedStartupKubeBuilder {

    private final ResourceLocation id;
    private final Supplier<Map<ResourceLocation, AvailableStructure>> availableStructures;
    private final Map<ResourceLocation, SelectedStructure> selectedStructures = new LinkedHashMap<>();
    private Integer separation;
    private Integer spacing;
    private Integer salt;

    public EnableDepositsKubeBuilder(
            ResourceLocation id, Map<ResourceLocation, AvailableStructure> availableStructures
    ) {
        this(id, () -> Map.copyOf(availableStructures), SourceLine.UNKNOWN);
    }

    public EnableDepositsKubeBuilder(
            ResourceLocation id, Supplier<Map<ResourceLocation, AvailableStructure>> availableStructures
    ) {
        this(id, availableStructures, SourceLine.UNKNOWN);
    }

    EnableDepositsKubeBuilder(
            ResourceLocation id,
            Supplier<Map<ResourceLocation, AvailableStructure>> availableStructures,
            SourceLine sourceLine
    ) {
        super(sourceLine, "rnsEnableDeposits", "overworld|nether");
        this.id = id;
        this.availableStructures = availableStructures;
    }

    @Info("Makes a deposit scannable and enables worldgen.")
    public EnableDepositsKubeBuilder deposit(Context cx, String structureId) {
        return depositSourced(structureId, null, true);
    }

    @HideFromJS
    public EnableDepositsKubeBuilder deposit(String structureId) {
        return deposit(structureId, null, true);
    }

    @Info("Makes a deposit scannable and enables worldgen.")
    public EnableDepositsKubeBuilder deposit(Context cx, String structureId, int weight) {
        return depositSourced(structureId, weight, true);
    }

    @HideFromJS
    public EnableDepositsKubeBuilder deposit(String structureId, int weight) {
        return deposit(structureId, weight, true);
    }

    @Info("Makes a deposit scannable and optionally enables worldgen.")
    public EnableDepositsKubeBuilder deposit(Context cx, String structureId, boolean enableWorldgen) {
        return depositSourced(structureId, null, enableWorldgen);
    }

    @HideFromJS
    public EnableDepositsKubeBuilder deposit(String structureId, boolean enableWorldgen) {
        return deposit(structureId, null, enableWorldgen);
    }

    @Info("Makes a deposit scannable and optionally enables worldgen.")
    public EnableDepositsKubeBuilder deposit(
            Context cx, String structureId, @Nullable Integer weight, boolean enableWorldgen
    ) {
        return depositSourced(structureId, weight, enableWorldgen);
    }

    @HideFromJS
    public EnableDepositsKubeBuilder deposit(String structureId, @Nullable Integer weight, boolean enableWorldgen) {
        return deposit(structureId, weight, enableWorldgen, SourceLine.UNKNOWN);
    }

    private EnableDepositsKubeBuilder deposit(
            String structureId, @Nullable Integer weight, boolean enableWorldgen, SourceLine sourceLine
    ) {
        if (weight != null && weight <= 0) {
            throw new IllegalArgumentException("Structure set weight must be positive");
        }

        var id = ResourceLocation.parse(structureId);
        var structure = availableStructures.get().get(id);
        if (structure == null) {
            throw new IllegalArgumentException("Unknown deposit structure: " + structureId);
        }
        if (!structure.valid()) return this;

        var selected = new SelectedStructure(structure, weight, enableWorldgen, sourceLine);
        if (selectedStructures.putIfAbsent(id, selected) != null) {
            throw new IllegalStateException("Deposit structure already selected in structure set " + this.id + ": " + structureId);
        }

        return this;
    }

    @Info("Sets the average distance between deposits in chunks.")
    public EnableDepositsKubeBuilder spacing(Context cx, int value) {
        return sourced(cx, "spacing", () -> spacing(value));
    }

    @HideFromJS
    public EnableDepositsKubeBuilder spacing(int value) {
        if (value <= 0) throw new IllegalArgumentException("Structure set spacing must be positive");

        spacing = value;
        validateSpacingAndSeparation();
        return this;
    }

    @Info("Sets the minimum distance between deposits in chunks.")
    public EnableDepositsKubeBuilder separation(Context cx, int value) {
        return sourced(cx, "separation", () -> separation(value));
    }

    @HideFromJS
    public EnableDepositsKubeBuilder separation(int value) {
        if (value < 0) throw new IllegalArgumentException("Structure set separation must be non-negative");

        separation = value;
        validateSpacingAndSeparation();
        return this;
    }

    @Info("Sets a seed for the random spread of deposits across the world")
    public EnableDepositsKubeBuilder salt(int value) {
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
            if (!entry.validateWeight()) continue;
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

    private EnableDepositsKubeBuilder depositSourced(
            String structureId, @Nullable Integer weight, boolean enableWorldgen
    ) {
        return sourcedAtCreation(() -> deposit(
                structureId, weight, enableWorldgen, creationSourceLine()));
    }

    static class SelectedStructure {
        private final AvailableStructure structure;
        private final Integer overriddenWeight;
        private final boolean enableWorldgen;
        private final SourceLine sourceLine;
        private boolean errorReported;

        private SelectedStructure(
                AvailableStructure structure, @Nullable Integer overriddenWeight, boolean enableWorldgen,
                SourceLine sourceLine
        ) {
            this.structure = structure;
            this.overriddenWeight = overriddenWeight;
            this.enableWorldgen = enableWorldgen;
            this.sourceLine = sourceLine;
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

        private boolean validateWeight() {
            if (overriddenWeight != null || structure.weight() != null) return true;
            if (sourceLine.isUnknown()) {
                resolvedWeight();
                return false;
            }
            if (!errorReported) {
                KubeJSStartupError.report(
                        "Structure '" + structure.id() + "' does not have an assigned weight", sourceLine);
                errorReported = true;
            }
            return false;
        }
    }
}
