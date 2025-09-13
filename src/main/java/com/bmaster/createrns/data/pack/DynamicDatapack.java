package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.data.pack.json.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DynamicDatapack {
    public static final Function<String, ResourceLocation> PROCESSOR_RL = name ->
            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "deposit/%s".formatted(name));
    public static final Function<String, ResourceLocation> STRUCT_START_RL = name ->
            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "deposit_%s/start".formatted(name));
    public static final Function<String, ResourceLocation> STRUCT_RL = name ->
            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "deposit_%s".formatted(name));

    private static final String PROCESSOR_PATH = "%s/worldgen/processor_list/deposit/%s.json";
    private static final String STRUCT_START_PATH = "%s/worldgen/template_pool/deposit_%s/start.json";
    private static final String STRUCT_PATH = "%s/worldgen/structure/deposit_%s.json";
    private static final String STRUCT_SET_PATH = "%s/worldgen/structure_set/deposits.json";
    private static final String HAS_DEPOSIT_TAG_PATH = "%s/tags/worldgen/biome/has_deposit.json";
    private static final String DEPOSIT_STRUCTURE_TAG_PATH = "%s/tags/worldgen/structure/deposits.json";

    private static final String HAS_DEPOSIT_TAG = "#%s:has_deposit";
    private static final String NOOP_PROCESSOR = "minecraft:empty";
    private static final String PLACEHOLDER_BLOCK = "minecraft:end_stone";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DynamicDatapackResources depositsResources = new DynamicDatapackResources(
            "%s:dynamic_data".formatted(CreateRNS.MOD_ID));

    private static final Set<Tuple<ResourceLocation, Integer>> bulkNBTPool = Set.of(
            new Tuple<>(ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "ore_deposit_medium"), 70),
            new Tuple<>(ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "ore_deposit_large"), 30)
    );
    private static final Set<Tuple<ResourceLocation, Integer>> preciousNBTPool = Set.of(
            new Tuple<>(ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "ore_deposit_small"), 70),
            new Tuple<>(ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "ore_deposit_medium"), 28),
            new Tuple<>(ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "ore_deposit_large"), 2)
    );

    private static final ObjectOpenHashSet<Deposit> deposits = new ObjectOpenHashSet<>();

    public static void addDepositBiomeTag() {
        var path = HAS_DEPOSIT_TAG_PATH.formatted(CreateRNS.MOD_ID);
        depositsResources.putJson(path, gson.toJsonTree(new HasDepositBiomeTag()));
    }

    public static void addVanillaDeposits() {
        deposits.add(new Deposit("iron", RNSContent.IRON_DEPOSIT_BLOCK.get(), bulkNBTPool, 8, 10));
        deposits.add(new Deposit("copper", RNSContent.COPPER_DEPOSIT_BLOCK.get(), bulkNBTPool, 8, 5));
        deposits.add(new Deposit("zinc", RNSContent.ZINC_DEPOSIT_BLOCK.get(), preciousNBTPool, 8, 2));
        deposits.add(new Deposit("gold", RNSContent.GOLD_DEPOSIT_BLOCK.get(), preciousNBTPool, 12, 2));
        deposits.add(new Deposit("redstone", RNSContent.REDSTONE_DEPOSIT_BLOCK.get(), preciousNBTPool, 12, 2));
    }

    public static void addDepositSetAndTag() {
        // Tag all added deposits and add tag to datapack
        var path = DEPOSIT_STRUCTURE_TAG_PATH.formatted(CreateRNS.MOD_ID);
        var tag = new DepositStructureTag(deposits.stream()
                .map(d -> STRUCT_RL.apply(d.name).toString())
                .toList());
        depositsResources.putJson(path, gson.toJsonTree(tag));

        // Add all deposits to a structure set and the structure set to datapack
        new DepositSet(deposits).addToResources();
    }

    public static Pack finish() {
        return Pack.readMetaAndCreate(
                depositsResources.packId(),
                Component.empty(),
                true,
                name -> depositsResources,
                PackType.SERVER_DATA,
                Pack.Position.BOTTOM,
                PackSource.BUILT_IN);
    }

    public static class DepositSet {
        public Set<Deposit> deposits;
        private boolean added = false;

        public DepositSet(Set<Deposit> deposits) {
            this.deposits = deposits;
        }

        private void addToResources() {
            if (added) return;

            List<DepositStructureSet.WeightedStructure> wsList = new ArrayList<>();
            for (var d : deposits) {
                d.addToResources();
                var sRL = STRUCT_RL.apply(d.name);
                wsList.add(new DepositStructureSet.WeightedStructure(sRL.toString(), d.weight));
            }

            // Create structure set
            var sSetPath = STRUCT_SET_PATH.formatted(CreateRNS.MOD_ID);
            depositsResources.putJson(sSetPath, gson.toJsonTree(new DepositStructureSet(wsList)));

            added = true;
        }
    }

    public static class Deposit {
        public final String name;
        public final @Nullable Block replacePlaceholderWith;
        public final Collection<Tuple<ResourceLocation, Integer>> nbts_and_weights;
        public final int depth;
        public final int weight;

        private boolean added = false;

        public Deposit(String name, @Nullable Block replacePlaceholderWith,
                       Collection<Tuple<ResourceLocation, Integer>> nbts_and_weights, int depth, int weight) {
            this.name = name;
            this.replacePlaceholderWith = replacePlaceholderWith;
            this.nbts_and_weights = nbts_and_weights;
            this.depth = depth;
            this.weight = weight;
        }

        private void addToResources() {
            if (added) return;

            if (depth < 0) {
                throw new IllegalArgumentException("Could not create deposit '%s': Depth cannot be negative".formatted(name));
            }
            if (weight < 0) {
                throw new IllegalArgumentException("Could not create deposit '%s': Weight cannot be negative".formatted(name));
            }

            String processor;

            // Create processor that replaces placeholder blocks with the specified block
            if (replacePlaceholderWith != null) {
                ResourceLocation depBlockRL = ForgeRegistries.BLOCKS.getKey(replacePlaceholderWith);
                if (depBlockRL == null) {
                    throw new IllegalArgumentException("Could not create a processor for deposit '%s': ".formatted(name) +
                            "provided deposit block does not exist");
                }
                var procPath = PROCESSOR_PATH.formatted(CreateRNS.MOD_ID, name);
                depositsResources.putJson(procPath, gson.toJsonTree(
                        new ReplaceWithProcessor(PLACEHOLDER_BLOCK, depBlockRL.toString())));

                processor = PROCESSOR_RL.apply(name).toString();
            } else {
                processor = NOOP_PROCESSOR;
            }

            // Create structure start
            var sStartPath = STRUCT_START_PATH.formatted(CreateRNS.MOD_ID, name);
            var elements = nbts_and_weights.stream().map(t ->
                            new DepositStructureStart.WeightedElement(t.getA().toString(), t.getB(), processor))
                    .collect(Collectors.toList());
            depositsResources.putJson(sStartPath, gson.toJsonTree(new DepositStructureStart(elements)));

            // Create structure
            var sPath = STRUCT_PATH.formatted(CreateRNS.MOD_ID, name);
            var sStartRl = STRUCT_START_RL.apply(name);

            String tag = HAS_DEPOSIT_TAG.formatted(CreateRNS.MOD_ID);
            depositsResources.putJson(sPath, gson.toJsonTree(
                    new DepositStructure(sStartRl.toString(), -depth, tag)));

            added = true;
        }
    }
}
