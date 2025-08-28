package com.bmaster.createrns;

import com.bmaster.createrns.datapackgen.DepositStructure;
import com.bmaster.createrns.datapackgen.DepositStructureSet;
import com.bmaster.createrns.datapackgen.DepositStructureStart;
import com.bmaster.createrns.datapackgen.ReplaceWithProcessor;
import com.bmaster.createrns.util.GeneratedDataPackResources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import java.util.List;

public class RNSDatapacks {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final GeneratedDataPackResources depositsResources = new GeneratedDataPackResources(
            "%s:dynamic_data".formatted(CreateRNS.MOD_ID));

    static {
        depositsResources.putJson("create_rns/worldgen/processor_list/deposit/iron.json", gson.toJsonTree(
                new ReplaceWithProcessor(
                        ResourceLocation.withDefaultNamespace("end_stone"),
                        ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "iron_deposit_block"))));

        depositsResources.putJson("create_rns/worldgen/template_pool/deposit_iron/start.json", gson.toJsonTree(
                new DepositStructureStart(List.of(
                        new DepositStructureStart.WeightedElement(1,
                                new DepositStructureStart.Element())))));

        depositsResources.putJson("create_rns/worldgen/structure/deposit_iron.json", gson.toJsonTree(
                new DepositStructure()));

        depositsResources.putJson("create_rns/worldgen/structure_set/deposits.json", gson.toJsonTree(
                new DepositStructureSet()));
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
