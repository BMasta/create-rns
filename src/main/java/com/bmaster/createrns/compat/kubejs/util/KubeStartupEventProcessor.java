package com.bmaster.createrns.compat.kubejs.util;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSRegistries;
import com.bmaster.createrns.compat.kubejs.RNSStartupKubeEvents;
import com.bmaster.createrns.compat.kubejs.deposit.catalysts.CatalystsKubeEvent;
import com.bmaster.createrns.compat.kubejs.deposit.enable.EnableDepositsKubeEvent;
import com.bmaster.createrns.compat.kubejs.deposit.structures.DepositStructuresKubeEvent;
import com.bmaster.createrns.compat.kubejs.util.KubeStartupError.SourcedError;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.bmaster.createrns.data.deposit.DepositJson;
import com.bmaster.createrns.data.deposit.DepositResources;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import com.bmaster.createrns.util.Utils;
import dev.latvian.mods.kubejs.client.LangKubeEvent;
import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class KubeStartupEventProcessor {
    public static final KubeStartupEventProcessor INSTANCE = new KubeStartupEventProcessor(KubeStartupEventProcessor::postEvents);

    private static StartupEvents postEvents() {
        var catalysts = new CatalystsKubeEvent();
        if (RNSStartupKubeEvents.RNS_CATALYSTS.hasListeners()) {
            RNSStartupKubeEvents.RNS_CATALYSTS.post(catalysts);
        }
        var structures = new DepositStructuresKubeEvent();
        if (RNSStartupKubeEvents.RNS_DEPOSIT_STRUCTURES.hasListeners()) {
            RNSStartupKubeEvents.RNS_DEPOSIT_STRUCTURES.post(structures);
        }
        var enable = new EnableDepositsKubeEvent(structures);
        if (RNSStartupKubeEvents.RNS_ENABLE_DEPOSITS.hasListeners()) {
            RNSStartupKubeEvents.RNS_ENABLE_DEPOSITS.post(enable);
        }
        return new StartupEvents(catalysts, structures, enable);
    }

    private static Map<ResourceKey<Level>, Set<ResourceLocation>> selectedBlocks(
            DepositResources resources, Map<ResourceKey<Level>, Set<ResourceLocation>> selectedStructures
    ) {
        if (selectedStructures.isEmpty()) return Map.of();

        var blocks = new HashMap<ResourceKey<Level>, Set<ResourceLocation>>();
        var structureToBlock = new HashMap<ResourceLocation, ResourceLocation>();
        var builtin = DynamicDatapackContent.RESOURCES;
        for (var structure : builtin.structures) {
            var block = builtin.getProcessor(structure.processorId()).targetBlock();
            structureToBlock.put(structure.id(), block);
            if (!selectedStructures.containsKey(structure.dimension()) && builtin.getSpec(structure.id()).isScannable().get()) {
                blocks.computeIfAbsent(structure.dimension(), dim -> new HashSet<>()).add(block);
            }
        }
        for (var structure : resources.structures) {
            structureToBlock.put(structure.id(), resources.getProcessor(structure.processorId()).targetBlock());
        }
        for (var entry : selectedStructures.entrySet()) {
            var inDimension = blocks.computeIfAbsent(entry.getKey(), dim -> new HashSet<>());
            for (var id : entry.getValue()) inDimension.add(structureToBlock.get(id));
        }
        // The depleted block has an Overworld recipe but no selectable structure.
        blocks.computeIfAbsent(Level.OVERWORLD, dim -> new HashSet<>())
                .add(DepositResources.blockId(CreateRNS.ID, "depleted"));
        blocks.replaceAll((dim, ids) -> Set.copyOf(ids));
        return Map.copyOf(blocks);
    }

    private static void reportErrors(List<SourcedError> errors) {
        for (var error : errors) {
            ConsoleJS.SERVER.error(error.message(), error.source(), error.error(), null);
        }
    }

    private final Supplier<StartupEvents> eventSupplier;
    private volatile @Nullable ProcessedEvents processed;

    public KubeStartupEventProcessor(CatalystsKubeEvent catalysts, DepositStructuresKubeEvent structures,
                                 EnableDepositsKubeEvent enable) {
        this(() -> new StartupEvents(catalysts, structures, enable));
    }

    private KubeStartupEventProcessor(Supplier<StartupEvents> eventSupplier) {
        this.eventSupplier = eventSupplier;
    }

    public ProcessedEvents getProcessedEvents() {
        var result = processed;
        if (result != null) return result;
        return processEvents(new ArrayList<>());
    }

    /// This method may be called by both server and client.
    /// Whoever calls it first, posts the events, processes them, and
    /// returns the processed events snapshot and encountered errors.
    private synchronized ProcessedEvents processEvents(List<SourcedError> errors) {
        // Another caller may have finished processing while this caller waited.
        var result = processed;
        if (result != null) return result;

        result = KubeStartupError.capture(errors, () -> {
            var startup = eventSupplier.get();
            startup.catalysts.assertValid();
            startup.structures.assertValid();
            startup.enable.assertValid();

            var resources = new DepositResources();
            var selected = startup.enable.selectedStructuresByDimension();
            startup.catalysts.gatherResources(resources);
            startup.structures.gatherResources(resources, selected);
            startup.enable.gatherResources(resources, selected);
            return new ProcessedEvents(resources, selectedBlocks(resources, selected), errors);
        });
        processed = result;
        return result;
    }

    public void generateData(KubeDataGenerator generator) {
        var snapshot = processed;
        List<SourcedError> errors = new ArrayList<>();
        try {
            if (snapshot == null) snapshot = processEvents(errors);
            errors = snapshot.errors();
        } finally {
            reportErrors(errors);
        }

        var resources = snapshot.resources();
        for (var catalyst : resources.catalysts) {
            generator.json(Utils.resourceFileLocation(CatalystRequirementSet.REGISTRY_KEY, catalyst.id()),
                    DepositJson.catalyst(catalyst));
        }
        for (var processor : resources.processors) {
            generator.json(Utils.resourceFileLocation(Registries.PROCESSOR_LIST, processor.id()),
                    DepositJson.processorList(DepositResources.PROCESSOR_INPUT_BLOCK, processor.targetBlock()));
        }
        for (var structure : resources.structures) {
            generator.json(Utils.resourceFileLocation(Registries.STRUCTURE, structure.id()),
                    DepositJson.structure(structure.biomeSelector(), structure.placementStrategy(),
                            -structure.depth() - structure.depthDeviation(), -structure.depth() + structure.depthDeviation(),
                            structure.templates(), structure.processorId()));
        }
        for (var spec : resources.specs) {
            generator.json(Utils.resourceFileLocation(RNSRegistries.DEPOSIT_SPEC, spec.id()),
                    DepositJson.spec(spec.structureId(), spec.dimension(), spec.scannerIconCandidates(),
                            spec.mapIconCandidates(), spec.isScannable().get()));
        }
        for (var set : resources.structureSets) {
            generator.json(Utils.resourceFileLocation(Registries.STRUCTURE_SET, set.id()),
                    DepositJson.structureSet(set.structureIds(), set.separation(), set.spacing(), set.salt()));
        }
        for (var tag : resources.blockTags) {
            generator.json(Utils.tagFileLocation(Registries.BLOCK, tag.id()), DepositJson.tag(tag.blockIds(), tag.replace()));
        }
        for (var tag : resources.structureTags) {
            generator.json(Utils.tagFileLocation(Registries.STRUCTURE, tag.id()),
                    DepositJson.tag(tag.structureIds(), tag.replace()));
        }
        for (var tag : resources.biomeSelectorTags) {
            generator.json(Utils.tagFileLocation(Registries.BIOME, tag.id()), DepositJson.tag(tag.biomeIds(), tag.replace()));
        }
    }

    public void generateLang(LangKubeEvent event) {
        var resources = getProcessedEvents().resources();
        for (var entry : resources.langEntries) event.add(entry.assetNamespace(), entry.key(), entry.value());
    }

    /// Shared between server and client. Consumers must treat the gathered resources as read-only.
    public record ProcessedEvents(
            DepositResources resources, Map<ResourceKey<Level>, Set<ResourceLocation>> selectedDepositBlocks,
            List<SourcedError> errors
    ) {
        public ProcessedEvents {
            errors = List.copyOf(errors);
        }
    }

    private record StartupEvents(CatalystsKubeEvent catalysts, DepositStructuresKubeEvent structures,
                                 EnableDepositsKubeEvent enable) {}
}
