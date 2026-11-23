package com.bmaster.createrns.compat.kubejs.deposit.structures;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSPacks;
import com.bmaster.createrns.compat.kubejs.deposit.enable.EnableDepositsKubeEvent.DimensionSelection;
import com.bmaster.createrns.compat.kubejs.deposit.enable.EnableDepositsKubeEvent.AvailableStructure;
import com.bmaster.createrns.compat.kubejs.util.JSMethod;
import com.bmaster.createrns.compat.kubejs.util.StartupJSObject;
import com.bmaster.createrns.compat.kubejs.util.StartupKubeBuilder;
import com.bmaster.createrns.data.deposit.DepositResources;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import dev.latvian.mods.kubejs.event.KubeStartupEvent;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositStructuresKubeEvent extends StartupJSObject implements KubeStartupEvent {
    private final Map<ResourceLocation, CreateStructureKubeBuilder> createdById = new LinkedHashMap<>();
    private final Map<ResourceLocation, TweakStructureKubeBuilder> tweakedById = new LinkedHashMap<>();
    private final List<CreateStructureKubeBuilder> created = new ArrayList<>();
    private final List<TweakStructureKubeBuilder> tweaked = new ArrayList<>();

    public DepositStructuresKubeEvent() {
        super("rnsDepositStructures", "create|tweak");
    }

    @Info("Creates a new deposit structure.")
    @JSMethod
    public CreateStructureKubeBuilder create(String structureId) {
        var id = ResourceLocation.parse(structureId);
        if (findBuiltInStructure(id) != null) {
            throw new IllegalArgumentException(
                    "Built-in deposit structure " + id + " must be modified with tweak(...)");
        }
        if (createdById.containsKey(id)) {
            throw new IllegalStateException("Duplicate KubeJS deposit structure id: " + structureId);
        }

        var builder = new CreateStructureKubeBuilder(id);
        createdById.put(id, builder);
        created.add(builder);
        return builder;
    }

    @Info("Tweaks an existing built-in deposit structure.")
    @JSMethod
    public TweakStructureKubeBuilder tweak(String structureId) {
        var id = ResourceLocation.parse(structureId);
        var structure = findBuiltInStructure(id);
        if (structure == null) {
            throw new IllegalArgumentException("Unknown built-in deposit structure: " + id);
        }
        if (tweakedById.containsKey(id)) {
            throw new IllegalStateException("Built-in deposit structure already tweaked: " + id);
        }

        var builder = new TweakStructureKubeBuilder(structure);
        tweakedById.put(id, builder);
        tweaked.add(builder);
        return builder;
    }

    public List<CreateStructureKubeBuilder> created() {
        return List.copyOf(created);
    }

    public List<TweakStructureKubeBuilder> tweaked() {
        return List.copyOf(tweaked);
    }

    public void assertValid() {
        created.forEach(StartupKubeBuilder::isValid);
        tweaked.forEach(StartupKubeBuilder::isValid);
    }

    public Map<ResourceLocation, AvailableStructure> availableStructures() {
        var structures = new LinkedHashMap<ResourceLocation, AvailableStructure>();
        for (var entry : DynamicDatapackContent.RESOURCES.structures) {
            var id = entry.id();
            var tweak = tweakedById.get(id);
            var weight = tweak != null && tweak.isValid() ? tweak.structureSetWeight() : entry.weight();
            structures.put(id, new AvailableStructure(weight, entry.dimension(), true, true));
        }
        for (var builder : created) {
            structures.put(builder.id(), new AvailableStructure(builder.structureSetWeight(), null,
                    builder.hasBiome(), builder.isValid()));
        }
        return structures;
    }

    /** Gathers validated definitions and overrides using selections from the enable-deposits event. */
    public void gatherResources(
            DepositResources resources,
            Map<ResourceKey<Level>, DimensionSelection> selectedStructures
    ) {
        for (var entry : selectedStructures.entrySet()) {
            for (var id : entry.getValue().structureIds()) {
                var structure = createdById.get(id);
                if (structure == null) continue;
                structure.setDimension(entry.getKey(), entry.getValue().strategy());
                structure.gatherResources(resources, true);
            }
        }

        var validTweakIds = new LinkedHashSet<ResourceLocation>();
        for (var structure : tweakedById.values()) {
            if (!structure.isValid()) continue;
            validTweakIds.add(structure.id());
            var selection = selectedStructures.get(structure.dimension());
            boolean scannable = selection == null ? structure.defaultScannable() : selection.structureIds().contains(structure.id());
            structure.gatherResources(resources, scannable);
        }

        for (var entry : DynamicDatapackContent.RESOURCES.structures) {
            var selection = selectedStructures.get(entry.dimension());
            if (selection == null || validTweakIds.contains(entry.id())) continue;

            var spec = DynamicDatapackContent.RESOURCES.getSpec(entry.id());
            boolean scannable = selection.structureIds().contains(entry.id());
            resources.specs.add(new DepositResources.Spec(spec.id(), spec.structureId(), spec.dimension(),
                    () -> scannable, spec.scannerIconCandidates(), spec.mapIconCandidates()));
        }
        gatherDefaultStructureSetWeightOverrides(resources, selectedStructures.keySet());
    }

    private void gatherDefaultStructureSetWeightOverrides(
            DepositResources resources, Set<ResourceKey<Level>> configuredDimensions
    ) {
        var dimensions = tweaked.stream().filter(tweak -> tweak.weightChanged() && tweak.isValid())
                .map(TweakStructureKubeBuilder::dimension).distinct().toList();
        for (var dimension : dimensions) {
            if (configuredDimensions.contains(dimension)) continue;

            // Default overrides use validated definitions without creating another script builder.
            var structures = new ArrayList<DepositResources.WeightedStructure>();
            for (var entry : DynamicDatapackContent.RESOURCES.getScannableStructures(dimension)) {
                var tweak = tweakedById.get(entry.id());
                var weight = tweak != null && tweak.isValid() ? tweak.structureSetWeight() : entry.weight();
                structures.add(new DepositResources.WeightedStructure(entry.id(), Objects.requireNonNull(
                        weight, () -> "Missing weight for built-in structure " + entry.id())));
            }
            boolean nether = dimension.equals(Level.NETHER);
            resources.structureSets.add(new DepositResources.StructureSet(
                    DepositResources.structureSetId(CreateRNS.ID, dimension), structures,
                    nether ? RNSPacks.DEFAULT_NETHER_SPACING : RNSPacks.DEFAULT_SPACING,
                    nether ? RNSPacks.DEFAULT_NETHER_SEPARATION : RNSPacks.DEFAULT_SEPARATION,
                    RNSPacks.defaultSalt(dimension)));
        }
    }

    private static @Nullable DepositResources.Structure findBuiltInStructure(
            ResourceLocation structureId
    ) {
        return DynamicDatapackContent.RESOURCES.structures.stream()
                .filter(entry -> entry.id().equals(structureId))
                .findFirst()
                .orElse(null);
    }
}
