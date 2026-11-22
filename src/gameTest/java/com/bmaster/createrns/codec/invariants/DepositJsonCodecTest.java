package com.bmaster.createrns.codec.invariants;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.spec.DepositSpec;
import com.bmaster.createrns.content.deposit.worldgen.DepositStructure;
import com.bmaster.createrns.content.deposit.worldgen.PlacementStrategy;
import com.bmaster.createrns.data.deposit.DepositJson;
import com.bmaster.createrns.data.deposit.DepositResources;
import com.bmaster.createrns.util.CodecHelper;
import com.bmaster.createrns.util.codec.ResourceOrTagLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagFile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class DepositJsonCodecTest {
    @GameTest(template = "empty16x16")
    public void structuresSupportScalarAndAsymmetricHeightRanges(GameTestHelper helper) {
        var processorId = ResourceLocation.withDefaultNamespace("empty");
        var templates = List.of(new DepositResources.WeightedTemplate(DepositResources.TEMPLATE_SMALL, 1));
        var biome = ResourceOrTagLocation.parse("plains");
        var scalar = DepositJson.structure(biome, PlacementStrategy.OVERWORLD, -8, -8, templates, processorId);
        helper.assertValueEqual(scalar.get("height").getAsInt(), -8, "scalar height");
        helper.assertValueEqual(scalar.get("biomes").getAsString(), "minecraft:plains", "direct biome");
        CodecHelper.assertParses(helper, DepositStructure.CODEC.codec(), CodecHelper.registries(helper),
                scalar.toString(), "structure with scalar height and direct biome");

        var range = DepositJson.structure(ResourceOrTagLocation.parse("#minecraft:is_nether"),
                PlacementStrategy.NETHER, -17, -2, templates, processorId);
        helper.assertValueEqual(range.getAsJsonObject("height").get("min").getAsInt(), -17, "minimum height");
        helper.assertValueEqual(range.getAsJsonObject("height").get("max").getAsInt(), -2, "maximum height");
        CodecHelper.assertParses(helper, DepositStructure.CODEC.codec(), CodecHelper.registries(helper),
                range.toString(), "structure with asymmetric height range and biome tag");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void specsPreserveFallbackOrderAndScannability(GameTestHelper helper) {
        var scannerIcons = List.of(ResourceOrTagLocation.parse("#coals"), ResourceOrTagLocation.parse("coal"));
        var mapIcons = List.of(ResourceOrTagLocation.parse("compass"));
        var structureId = CreateRNS.asResource("deposit_iron");
        var json = DepositJson.spec(structureId, Level.NETHER, scannerIcons, mapIcons, false);
        helper.assertValueEqual(json.getAsJsonArray("scanner_icon_item").get(0).getAsString(),
                "#minecraft:coals", "first scanner fallback");
        helper.assertValueEqual(json.getAsJsonArray("scanner_icon_item").get(1).getAsString(),
                "minecraft:coal", "second scanner fallback");
        helper.assertValueEqual(json.get("map_icon_item").getAsString(), "minecraft:compass", "scalar map icon");
        var spec = CodecHelper.assertParses(helper, DepositSpec.CODEC, CodecHelper.registries(helper),
                json.toString(), "deposit spec");
        helper.assertFalse(spec.scannable, "Explicit false scannability must survive decoding");
        helper.assertValueEqual(spec.dimension, Level.NETHER, "spec dimension");
        var overworld = DepositJson.spec(structureId, Level.OVERWORLD, scannerIcons, mapIcons, true);
        helper.assertFalse(overworld.has("dimension"), "Default dimension should be omitted");
        helper.assertTrue(CodecHelper.assertParses(helper, DepositSpec.CODEC, CodecHelper.json(),
                overworld.toString(), "overworld spec").scannable, "Scannability should be enabled");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void processorsStructureSetsAndTagsDecode(GameTestHelper helper) {
        var processor = DepositJson.processorList(ResourceLocation.withDefaultNamespace("end_stone"),
                ResourceLocation.withDefaultNamespace("iron_block"));
        CodecHelper.assertParses(helper, StructureProcessorType.LIST_CODEC, CodecHelper.registries(helper),
                processor.toString(), "deposit processor list");

        var structures = List.of(new DepositResources.WeightedStructure(CreateRNS.asResource("deposit_iron"), 7));
        var structureSet = DepositJson.structureSet(structures, 8, 16, 12345);
        CodecHelper.assertParses(helper, StructureSet.DIRECT_CODEC, CodecHelper.registries(helper),
                structureSet.toString(), "deposit structure set");
        helper.assertValueEqual(structureSet.getAsJsonArray("structures").get(0).getAsJsonObject()
                .get("weight").getAsInt(), 7, "structure weight");
        var emptySet = DepositJson.structureSet(List.of(), 8, 16, 12345);
        helper.assertValueEqual(emptySet.getAsJsonArray("structures").size(), 0, "disabled structure set");

        var tag = DepositJson.tag(List.of(ResourceOrTagLocation.parse("plains"),
                ResourceOrTagLocation.parse("#is_forest")), false);
        var parsedTag = CodecHelper.assertParses(helper, TagFile.CODEC, CodecHelper.json(),
                tag.toString(), "mixed resource/tag entries");
        helper.assertFalse(parsedTag.replace(), "Tag should append by default");
        var disabledTag = DepositJson.tag(List.of(), true);
        var parsedDisabled = CodecHelper.assertParses(helper, TagFile.CODEC, CodecHelper.json(),
                disabledTag.toString(), "disabled tag");
        helper.assertTrue(parsedDisabled.replace(), "Disabled tag must replace previous entries");
        helper.assertTrue(parsedDisabled.entries().isEmpty(), "Disabled tag must be empty");
        helper.succeed();
    }
}
