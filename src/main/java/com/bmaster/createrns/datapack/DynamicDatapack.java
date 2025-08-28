package com.bmaster.createrns.datapack;

import com.bmaster.createrns.CreateRNS;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DynamicDatapack {
    private static final String PROCESSOR_PATH = "%s/worldgen/processor_list/deposit/%s.json";
    private static final String STRUCT_START_PATH = "%s/worldgen/template_pool/deposit_%s/start.json";
    private static final String STRUCT_PATH = "%s/worldgen/structure/deposit_%s.json";
    private static final String STRUCT_SET_PATH = "%s/worldgen/structure_set/deposits.json";

    private static final String PROCESSOR_RL = "%s:deposit/%s";
    private static final String STRUCT_START_RL = "%s:deposit_%s/start";
    private static final String STRUCT_RL = "%s:deposit_%s";

    private static final String NOOP_PROCESSOR = "minecraft:empty";
    private static final String PLACEHOLDER_BLOCK = "minecraft:end_stone";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DynamicDatapackResources depositsResources = new DynamicDatapackResources(
            "%s:dynamic_data".formatted(CreateRNS.MOD_ID));

    public static void add(DepositSet dSet) {
        dSet.addToDatapack();
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

        private void addToDatapack() {
            if (added) return;

            List<DepositStructureSet.WeightedStructure> wsList = new ArrayList<>();
            for (var d : deposits) {
                d.addToDatapack();
                var sRL = STRUCT_RL.formatted(CreateRNS.MOD_ID, d.name);
                wsList.add(new DepositStructureSet.WeightedStructure(sRL, d.weight));
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
        public final ResourceLocation nbt;
        public final int depth;
        public final int weight;

        private boolean added = false;

        public Deposit(String name, @Nullable Block replacePlaceholderWith, ResourceLocation nbt, int depth, int weight) {
            this.name = name;
            this.replacePlaceholderWith = replacePlaceholderWith;
            this.nbt = nbt;
            this.depth = depth;
            this.weight = weight;
        }

        private void addToDatapack() {
            if (added) return;

            if (depth < 0) {
                throw new IllegalArgumentException("Could not create deposit '%s': Depth cannot be negative".formatted(name));
            }
            if (weight < 0) {
                throw new IllegalArgumentException("Could not create deposit '%s': Weight cannot be negative".formatted(name));
            }

            String processor = NOOP_PROCESSOR;

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

                processor = PROCESSOR_RL.formatted(CreateRNS.MOD_ID, name);
            }

            // Create structure start
            var sStartPath = STRUCT_START_PATH.formatted(CreateRNS.MOD_ID, name);
            depositsResources.putJson(sStartPath, gson.toJsonTree(new DepositStructureStart(List.of(
                    new DepositStructureStart.WeightedElement(1, nbt.toString(), processor)))));

            // Create structure
            var sPath = STRUCT_PATH.formatted(CreateRNS.MOD_ID, name);
            var sStartRl = STRUCT_START_RL.formatted(CreateRNS.MOD_ID, name);
            depositsResources.putJson(sPath, gson.toJsonTree(new DepositStructure(sStartRl, -depth)));

            added = true;
        }
    }
}
