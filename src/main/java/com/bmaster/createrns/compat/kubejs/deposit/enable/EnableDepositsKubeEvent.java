package com.bmaster.createrns.compat.kubejs.deposit.enable;

import com.bmaster.createrns.content.deposit.worldgen.PlacementStrategy;
import com.bmaster.createrns.compat.kubejs.deposit.structures.DepositStructuresKubeEvent;
import com.bmaster.createrns.compat.kubejs.util.JSMethod;
import com.bmaster.createrns.compat.kubejs.util.StartupJSObject;
import com.bmaster.createrns.data.deposit.DepositResources;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import com.bmaster.createrns.util.Utils;
import com.bmaster.createrns.util.codec.ResourceOrTagLocation;
import dev.latvian.mods.kubejs.event.KubeStartupEvent;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class EnableDepositsKubeEvent extends StartupJSObject implements KubeStartupEvent {
    private final DepositStructuresKubeEvent structures;
    private final Map<ResourceKey<Level>, EnableDepositsKubeBuilder> dimensions = new LinkedHashMap<>();

    public EnableDepositsKubeEvent(DepositStructuresKubeEvent structures) {
        super("rnsEnableDeposits", "overworld|nether|end|dimension");
        this.structures = structures;
    }

    @Info("Returns the Overworld deposit selection builder.")
    @JSMethod
    public EnableDepositsKubeBuilder overworld() {
        return dimension(Level.OVERWORLD, null);
    }

    @Info("Returns the Nether deposit selection builder.")
    @JSMethod
    public EnableDepositsKubeBuilder nether() {
        return dimension(Level.NETHER, null);
    }

    @Info("Returns the End deposit selection builder.")
    @JSMethod
    public EnableDepositsKubeBuilder end() {
        return dimension(Level.END, null);
    }

    @JSMethod
    public EnableDepositsKubeBuilder dimension(String id) {
        return dimension(Utils.parseDimension(id), null);
    }

    @JSMethod
    public EnableDepositsKubeBuilder dimension(String id, String placementStrategy) {
        var strategy = switch (placementStrategy) {
            case "default" -> PlacementStrategy.DEFAULT;
            case "nether" -> PlacementStrategy.NETHER;
            default -> throw new IllegalArgumentException("Unknown placement strategy: " + placementStrategy
                    + ". Expected default or nether");
        };
        return dimension(Utils.parseDimension(id), strategy);
    }

    private EnableDepositsKubeBuilder dimension(ResourceKey<Level> dimension, @Nullable PlacementStrategy strategy) {
        var builder = dimensions.computeIfAbsent(dimension,
                key -> new EnableDepositsKubeBuilder(key, structures::availableStructures));
        if (strategy != null) builder.setPlacementStrategy(strategy);
        return builder;
    }

    public boolean hasConfiguredDimensions() {
        return dimensions.values().stream().anyMatch(EnableDepositsKubeBuilder::isValid);
    }

    public void assertValid() {
        var owners = new HashMap<ResourceLocation, EnableDepositsKubeBuilder>();
        for (var builder : configuredStructureSets().values()) {
            for (var selected : builder.selectedStructures()) {
                var first = owners.putIfAbsent(selected.id(), builder);
                if (first == null) continue;
                var message = "Custom deposit structure " + selected.id() + " cannot belong to both "
                        + first.dimension.location() + " and " + builder.dimension.location();
                first.error(message, true);
                builder.error(message, true);
            }
        }
    }

    public void gatherResources(DepositResources resources, Map<ResourceKey<Level>, DimensionSelection> selected) {
        for (var builder : configuredStructureSets().values()) builder.gatherResources(resources);
        if (selected.isEmpty()) return;

        var tagged = new TreeSet<>(Comparator.comparing(ResourceLocation::toString));
        selected.values().forEach(selection -> tagged.addAll(selection.structureIds()));
        for (var structure : DynamicDatapackContent.RESOURCES.structures) {
            if (!selected.containsKey(structure.dimension())) tagged.add(structure.id());
        }
        var entries = tagged.stream().map(id -> new ResourceOrTagLocation(id, false)).toList();
        resources.structureTags.add(new DepositResources.StructureTag(DepositResources.structureTagId(), entries, true));
    }

    public Map<ResourceKey<Level>, DimensionSelection> selectedStructuresByDimension() {
        var selected = new LinkedHashMap<ResourceKey<Level>, DimensionSelection>();
        for (var entry : configuredStructureSets().entrySet()) {
            var ids = entry.getValue().selectedStructures().stream().map(EnableDepositsKubeBuilder.SelectedStructure::id)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            // An explicit empty selection still overrides defaults in this dimension.
            selected.put(entry.getKey(), new DimensionSelection(entry.getValue().placementStrategy(), ids));
        }
        return selected;
    }

    private Map<ResourceKey<Level>, EnableDepositsKubeBuilder> configuredStructureSets() {
        var configured = new LinkedHashMap<ResourceKey<Level>, EnableDepositsKubeBuilder>();
        dimensions.forEach((dimension, builder) -> {
            if (builder.isValid()) configured.put(dimension, builder);
        });
        return configured;
    }

    public record DimensionSelection(PlacementStrategy strategy, Set<ResourceLocation> structureIds) {}

    public record AvailableStructure(
            @Nullable Integer weight, @Nullable ResourceKey<Level> builtInDimension, boolean hasBiome, boolean valid
    ) {}
}
