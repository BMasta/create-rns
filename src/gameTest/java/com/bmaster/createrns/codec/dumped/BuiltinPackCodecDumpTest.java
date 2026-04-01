package com.bmaster.createrns.codec.dumped;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.content.deposit.spec.DepositSpec;
import com.bmaster.createrns.content.deposit.worldgen.DepositStructure;
import com.bmaster.createrns.util.CodecHelper;
import com.bmaster.createrns.util.DumpedCodecHelper;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagFile;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Set;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class BuiltinPackCodecDumpTest {
    private static final Set<ResourceLocation> ALLOWED_UNRESOLVED_RECIPE_TAG_CANDIDATES =
            Set.of(ResourceLocation.parse("forge:gems/certus_quartz"));

    @GameTest(template = "empty16x16")
    public void defaultDumpedMiningRecipesRoundTrip(GameTestHelper helper) {
        var files = DumpedCodecHelper.findJsonFiles(helper,
                "default/create_rns_dynamic_data/data/create_rns/recipes");
        DumpedCodecHelper.assertRoundTrips(helper, files, MiningRecipe.CODEC.codec(),
                CodecHelper.registries(helper), "dumped mining recipe", json -> {
                    DumpedCodecHelper.stripRootField(json, "type");
                    return DumpedCodecHelper.stripEmptyFields(json, "item_candidates", "tag_candidates");
                });
        DumpedCodecHelper.assertItemAndTagCandidatesResolve(helper, files, "item_candidates", "tag_candidates",
                "mining recipe", Set.of(), ALLOWED_UNRESOLVED_RECIPE_TAG_CANDIDATES);
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void defaultDumpedDepositSpecsRoundTrip(GameTestHelper helper) {
        var files = DumpedCodecHelper.findJsonFiles(helper,
                "default/create_rns_dynamic_data/data/create_rns/create_rns/deposit_spec");
        DumpedCodecHelper.assertRoundTrips(helper, files, DepositSpec.CODEC,
                CodecHelper.registries(helper), "dumped deposit spec",
                DumpedCodecHelper::identity);
        DumpedCodecHelper.assertItemAndTagCandidatesResolve(helper, files, "scanner_icon_item_candidates",
                "scanner_icon_tag_candidates", "deposit spec");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void defaultDumpedDepositStructuresRoundTrip(GameTestHelper helper) {
        var files = DumpedCodecHelper.findJsonFiles(helper,
                "default/create_rns_dynamic_data/data/create_rns/worldgen/structure");
        DumpedCodecHelper.assertRoundTrips(helper, files, DepositStructure.CODEC,
                CodecHelper.registries(helper), "dumped deposit structure", json -> {
                    DumpedCodecHelper.stripRootField(json, "type");
                    return json;
                });
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void defaultDumpedStructureSetsRoundTrip(GameTestHelper helper) {
        var files = DumpedCodecHelper.findJsonFiles(helper, "default").stream()
                .filter(path -> path.toString().replace('\\', '/').contains("/worldgen/structure_set/"))
                .toList();
        DumpedCodecHelper.assertRoundTrips(helper, files, StructureSet.DIRECT_CODEC,
                CodecHelper.registries(helper), "dumped structure set", DumpedCodecHelper::identity);
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void defaultDumpedProcessorListsRoundTrip(GameTestHelper helper) {
        var files = DumpedCodecHelper.findJsonFiles(helper,
                "default/create_rns_dynamic_data/data/create_rns/worldgen/processor_list");
        DumpedCodecHelper.assertRoundTrips(helper, files, StructureProcessorType.LIST_CODEC,
                CodecHelper.registries(helper), "dumped processor list", DumpedCodecHelper::identity);
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void defaultDumpedWorldgenTagsRoundTrip(GameTestHelper helper) {
        var files = DumpedCodecHelper.findJsonFiles(helper, "default").stream()
                .filter(path -> path.toString().replace('\\', '/').contains("/tags/worldgen/"))
                .toList();
        DumpedCodecHelper.assertRoundTrips(helper, files, TagFile.CODEC, CodecHelper.json(),
                "dumped worldgen tag", DumpedCodecHelper::identity);
        helper.succeed();
    }
}
