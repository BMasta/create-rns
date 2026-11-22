package com.bmaster.createrns.compat.kubejs.deposit.enable;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.compat.kubejs.deposit.structures.DepositStructuresKubeEvent;
import com.bmaster.createrns.compat.kubejs.util.JSMethod;
import com.bmaster.createrns.compat.kubejs.util.StartupJSObject;
import com.bmaster.createrns.data.deposit.DepositResources;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
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
    private @Nullable EnableDepositsKubeBuilder overworld;
    private @Nullable EnableDepositsKubeBuilder nether;

    public EnableDepositsKubeEvent(DepositStructuresKubeEvent structures) {
        super("rnsEnableDeposits", "overworld|nether");
        this.structures = structures;
    }

    @Info("Returns the overworld deposit structure set builder.")
    @JSMethod
    public EnableDepositsKubeBuilder overworld() {
        if (overworld == null) {
            overworld = new EnableDepositsKubeBuilder(
                    DepositResources.structureSetId(CreateRNS.ID, Level.OVERWORLD), structures::availableStructures);
        }
        return overworld;
    }

    @Info("Returns the nether deposit structure set builder.")
    @JSMethod
    public EnableDepositsKubeBuilder nether() {
        if (nether == null) {
            nether = new EnableDepositsKubeBuilder(
                    DepositResources.structureSetId(CreateRNS.ID, Level.NETHER), structures::availableStructures);
        }
        return nether;
    }

    public boolean hasConfiguredDimensions() {
        return (overworld != null && overworld.isValid()) || (nether != null && nether.isValid());
    }

    public void assertValid() {
        var configured = configuredStructureSets();
        var overworld = configured.get(Level.OVERWORLD);
        var nether = configured.get(Level.NETHER);
        if (overworld == null || nether == null) return;

        var overworldIds = overworld.selectedStructures().stream().map(EnableDepositsKubeBuilder.SelectedStructure::id)
                .collect(Collectors.toSet());
        for (var selected : nether.selectedStructures()) {
            if (!overworldIds.contains(selected.id())) continue;
            var message = "Custom deposit structure " + selected.id()
                    + " cannot belong to both overworld and nether KubeJS structure sets";
            overworld.error(message, true);
            nether.error(message, true);
        }
    }

    public void gatherResources(DepositResources resources, Map<ResourceKey<Level>, Set<ResourceLocation>> selected) {
        for (var structureSet : configuredStructureSets().values()) structureSet.gatherResources(resources);
        if (selected.isEmpty()) return;

        var tagged = new TreeSet<ResourceLocation>(Comparator.comparing(ResourceLocation::toString));
        selected.values().forEach(tagged::addAll);
        for (var structure : DynamicDatapackContent.RESOURCES.structures) {
            if (!selected.containsKey(structure.dimension())) tagged.add(structure.id());
        }
        var entries = tagged.stream().map(id -> new ResourceOrTagLocation(id, false)).toList();
        resources.structureTags.add(new DepositResources.StructureTag(DepositResources.structureTagId(), entries, true));
    }

    public Map<ResourceKey<Level>, Set<ResourceLocation>> selectedStructuresByDimension() {
        var selected = new LinkedHashMap<ResourceKey<Level>, Set<ResourceLocation>>();
        for (var entry : configuredStructureSets().entrySet()) {
            var ids = entry.getValue().selectedStructures().stream().map(EnableDepositsKubeBuilder.SelectedStructure::id)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            // An explicit empty selection still overrides defaults in this dimension.
            selected.put(entry.getKey(), ids);
        }
        return selected;
    }

    private Map<ResourceKey<Level>, EnableDepositsKubeBuilder> configuredStructureSets() {
        var configured = new LinkedHashMap<ResourceKey<Level>, EnableDepositsKubeBuilder>();
        if (overworld != null && overworld.isValid()) configured.put(Level.OVERWORLD, overworld);
        if (nether != null && nether.isValid()) configured.put(Level.NETHER, nether);
        return configured;
    }

    public record AvailableStructure(
            ResourceLocation id, @Nullable Integer weight, @Nullable ResourceKey<Level> builtInDimension, boolean valid
    ) {}
}
