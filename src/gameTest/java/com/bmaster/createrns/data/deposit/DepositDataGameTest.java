package com.bmaster.createrns.data.deposit;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.spec.DepositSpecLookup;
import com.bmaster.createrns.content.deposit.worldgen.PlacementStrategy;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class DepositDataGameTest {
    @GameTest(template = "empty16x16")
    public void builtInStructuresHaveDefaultWeights(GameTestHelper helper) {
        var structures = DynamicDatapackContent.RESOURCES.structures;
        helper.assertFalse(structures.isEmpty(), "Expected registered built-in structures");
        for (var structure : structures) {
            helper.assertTrue(structure.weight() != null, "Missing weight for built-in structure " + structure.id());
        }
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void dimensionDefaultsPreserveBuiltInNames(GameTestHelper helper) {
        helper.assertValueEqual(DepositResources.structureSetId(CreateRNS.ID, Level.OVERWORLD).toString(),
                "create_rns:deposits", "Overworld structure set");
        helper.assertValueEqual(DepositResources.structureSetId(CreateRNS.ID, Level.NETHER).toString(),
                "create_rns:nether_deposits", "Nether structure set");
        helper.assertValueEqual(DepositResources.biomeTagId(CreateRNS.ID, Level.OVERWORLD).toString(),
                "create_rns:has_deposit", "Overworld biome tag");
        helper.assertValueEqual(DepositResources.biomeTagId(CreateRNS.ID, Level.NETHER).toString(),
                "create_rns:has_deposit_nether", "Nether biome tag");
        helper.assertTrue(DepositResources.defaultPlacementStrategy(Level.NETHER) == PlacementStrategy.NETHER,
                "Nether placement");
        var custom = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("mymod:custom_dimension"));
        helper.assertTrue(DepositResources.defaultPlacementStrategy(custom) == PlacementStrategy.DEFAULT,
                "Custom dimensions use default placement");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void resourceNamesPreserveNamespacesAndPaths(GameTestHelper helper) {
        helper.assertValueEqual(DepositResources.processorId("scripts", ResourceLocation.parse("mymod:ores/tin")).toString(),
                "scripts:replace_with_mymod_ores/tin", "Processor uses full target block id");
        helper.assertValueEqual(DepositResources.processorId(CreateRNS.ID, "iron").toString(),
                "create_rns:replace_with_create_rns_iron_deposit_block", "Built-in processor name");
        helper.assertValueEqual(DepositResources.specId(CreateRNS.ID, Level.NETHER, "gold").toString(),
                "create_rns:nether_gold", "Built-in spec name");
        helper.assertValueEqual(DepositResources.specId(ResourceLocation.parse("mymod:deposit_tin")).toString(),
                "mymod:tin", "Strip leading deposit prefix");
        helper.assertValueEqual(DepositResources.specId(ResourceLocation.parse("mymod:ores/deposit_tin")).toString(),
                "mymod:ores/deposit_tin", "Preserve paths without a leading deposit prefix");
        helper.assertValueEqual(DepositResources.structureLangKey(ResourceLocation.parse("mymod:ores/tin")),
                "mymod.structure.ores.tin", "Structure translation key");
        var name = DepositSpecLookup.getDepositName(ResourceKey.create(Registries.STRUCTURE,
                ResourceLocation.parse("mymod:ores/tin")));
        helper.assertValueEqual(((TranslatableContents) name.getContents()).getKey(),
                "mymod.structure.ores.tin", "Runtime name lookup matches generated language keys");
        helper.succeed();
    }
}
