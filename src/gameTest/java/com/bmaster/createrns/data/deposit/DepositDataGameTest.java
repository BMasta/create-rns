package com.bmaster.createrns.data.deposit;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSRegistries;
import com.bmaster.createrns.content.deposit.spec.DepositSpecLookup;
import com.bmaster.createrns.content.deposit.worldgen.PlacementStrategy;
import com.bmaster.createrns.data.DumpSafeOverrides;
import com.bmaster.createrns.util.CodecHelper;
import com.bmaster.createrns.util.Utils;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagManager;
import net.minecraft.world.level.Level;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class DepositDataGameTest {
    @GameTest(template = "empty16x16")
    public void dumpSafeOverridesMatchRuntimeDefinitions(GameTestHelper helper) {
        CodecHelper.assertValueEqual(helper, DumpSafeOverrides.OVERWORLD, Level.OVERWORLD.location(), "Overworld id");
        CodecHelper.assertValueEqual(helper, DumpSafeOverrides.NETHER, Level.NETHER.location(), "Nether id");
        CodecHelper.assertValueEqual(helper, DumpSafeOverrides.END, Level.END.location(), "End id");
        var registries = Map.of(
                DumpSafeOverrides.BIOME, Registries.BIOME,
                DumpSafeOverrides.PROCESSOR_LIST, Registries.PROCESSOR_LIST,
                DumpSafeOverrides.STRUCTURE, Registries.STRUCTURE,
                DumpSafeOverrides.STRUCTURE_SET, Registries.STRUCTURE_SET,
                DumpSafeOverrides.DEPOSIT_SPEC, RNSRegistries.DEPOSIT_SPEC,
                DumpSafeOverrides.BLOCK, Registries.BLOCK,
                DumpSafeOverrides.ENTITY_TYPE, Registries.ENTITY_TYPE,
                DumpSafeOverrides.FLUID, Registries.FLUID,
                DumpSafeOverrides.GAME_EVENT, Registries.GAME_EVENT,
                DumpSafeOverrides.ITEM, Registries.ITEM);
        var id = ResourceLocation.parse("mymod:ores/tin");
        registries.forEach((registryId, registryKey) -> {
            CodecHelper.assertValueEqual(helper, registryId, registryKey.location(), "Registry id");
            CodecHelper.assertValueEqual(helper, Utils.tagFileLocation(registryId, id),
                    id.withPrefix(TagManager.getTagDir(registryKey) + "/"), "Tag directory for " + registryId);
        });
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void resourceFilesUseForge120Directories(GameTestHelper helper) {
        var id = ResourceLocation.parse("mymod:ores/tin");
        CodecHelper.assertValueEqual(helper, Utils.resourceFileLocation(Registries.PROCESSOR_LIST, id).toString(),
                "mymod:worldgen/processor_list/ores/tin", "Processor file location");
        CodecHelper.assertValueEqual(helper, Utils.resourceFileLocation(RNSRegistries.DEPOSIT_SPEC, id).toString(),
                "mymod:create_rns/deposit_spec/ores/tin", "Custom registry file location");
        CodecHelper.assertValueEqual(helper, Utils.tagFileLocation(Registries.BLOCK, id).toString(),
                "mymod:tags/blocks/ores/tin", "Block tags use the plural directory on 1.20");
        CodecHelper.assertValueEqual(helper, Utils.tagFileLocation(Registries.STRUCTURE, id).toString(),
                "mymod:tags/worldgen/structure/ores/tin", "Structure tag file location");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void dimensionDefaultsPreserveBuiltInNames(GameTestHelper helper) {
        CodecHelper.assertValueEqual(helper, DepositData.structureSetId(CreateRNS.ID, Level.OVERWORLD.location()).toString(),
                "create_rns:deposits", "Overworld structure set");
        CodecHelper.assertValueEqual(helper, DepositData.structureSetId(CreateRNS.ID, Level.NETHER.location()).toString(),
                "create_rns:nether_deposits", "Nether structure set");
        CodecHelper.assertValueEqual(helper, DepositData.biomeTagId(CreateRNS.ID, Level.OVERWORLD.location()).toString(),
                "create_rns:has_deposit", "Overworld biome tag");
        CodecHelper.assertValueEqual(helper, DepositData.biomeTagId(CreateRNS.ID, Level.NETHER.location()).toString(),
                "create_rns:has_deposit_nether", "Nether biome tag");
        helper.assertTrue(DepositData.defaultPlacementStrategy(Level.NETHER.location()) == PlacementStrategy.NETHER,
                "Nether placement");
        var custom = ResourceLocation.parse("mymod:custom_dimension");
        helper.assertTrue(DepositData.defaultPlacementStrategy(custom) == PlacementStrategy.OVERWORLD,
                "Other dimensions use overworld placement");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void resourceNamesPreserveNamespacesAndPaths(GameTestHelper helper) {
        CodecHelper.assertValueEqual(helper, DepositData.processorId("scripts", ResourceLocation.parse("mymod:ores/tin")).toString(),
                "scripts:replace_with_mymod_ores/tin", "Processor uses full target block id");
        CodecHelper.assertValueEqual(helper, DepositData.processorId(CreateRNS.ID, "iron").toString(),
                "create_rns:replace_with_create_rns_iron_deposit_block", "Built-in processor name");
        CodecHelper.assertValueEqual(helper, DepositData.specId(CreateRNS.ID, Level.NETHER.location(), "gold").toString(),
                "create_rns:nether_gold", "Built-in spec name");
        CodecHelper.assertValueEqual(helper, DepositData.specId(ResourceLocation.parse("mymod:deposit_tin")).toString(),
                "mymod:tin", "Strip leading deposit prefix");
        CodecHelper.assertValueEqual(helper, DepositData.specId(ResourceLocation.parse("mymod:ores/deposit_tin")).toString(),
                "mymod:ores/deposit_tin", "Preserve paths without a leading deposit prefix");
        CodecHelper.assertValueEqual(helper, DepositData.structureLangKey(ResourceLocation.parse("mymod:ores/tin")),
                "mymod.structure.ores.tin", "Structure translation key");
        var name = DepositSpecLookup.getDepositName(ResourceKey.create(Registries.STRUCTURE,
                ResourceLocation.parse("mymod:ores/tin")));
        CodecHelper.assertValueEqual(helper, ((TranslatableContents) name.getContents()).getKey(),
                "mymod.structure.ores.tin", "Runtime name lookup matches generated language keys");
        helper.succeed();
    }
}
