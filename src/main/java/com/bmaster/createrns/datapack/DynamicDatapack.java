package com.bmaster.createrns.datapack;

import com.bmaster.createrns.CreateRNS;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import java.util.List;

public class DynamicDatapack {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final DynamicDatapackResources depositsResources = new DynamicDatapackResources(
            "%s:dynamic_data".formatted(CreateRNS.MOD_ID));

    static {
        depositsResources.putJson("create_rns/worldgen/processor_list/deposit/iron.json", gson.toJsonTree(
                new ReplaceWithProcessor("minecraft:end_stone", "create_rns:iron_deposit_block")));

        depositsResources.putJson("create_rns/worldgen/template_pool/deposit_iron/start.json", gson.toJsonTree(
                new DepositStructureStart(List.of(
                        new DepositStructureStart.WeightedElement(1, "create_rns:ore_deposit_medium",
                                "create_rns:deposit/iron")))));

        depositsResources.putJson("create_rns/worldgen/structure/deposit_iron.json", gson.toJsonTree(
                new DepositStructure("create_rns:deposit_iron/start", 30)));

        depositsResources.putJson("create_rns/worldgen/structure_set/deposits.json", gson.toJsonTree(
                new DepositStructureSet(List.of(
                        new DepositStructureSet.WeightedStructure("create_rns:deposit_iron", 1)))));
    }

    public static final Pack DEPOSITS_DATAPACK = Pack.readMetaAndCreate(
            depositsResources.packId(),
            Component.empty(),
            true,
            name -> depositsResources,
            PackType.SERVER_DATA,
            Pack.Position.BOTTOM,
            PackSource.BUILT_IN);
}
