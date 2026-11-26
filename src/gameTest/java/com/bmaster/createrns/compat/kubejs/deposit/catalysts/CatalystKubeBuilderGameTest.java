package com.bmaster.createrns.compat.kubejs.deposit.catalysts;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.compat.kubejs.RNSKubeJSBuilderTest;
import com.bmaster.createrns.compat.kubejs.RNSKubeJSBuilderTestContext;
import com.bmaster.createrns.compat.kubejs.RNSKubeJSBuilderTestHelper;
import com.bmaster.createrns.compat.kubejs.RNSKubeJSBuilderTestRunner;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.FluidCatalystRequirement;
import com.bmaster.createrns.util.CodecHelper;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.bmaster.createrns.compat.kubejs.RNSKubeJSBuilderTestAssertions.*;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class CatalystKubeBuilderGameTest {
    @GameTest(template = "empty16x16")
    public void catalystBuilderScenarios(GameTestHelper helper) {
        RNSKubeJSBuilderTestRunner.runAll(helper, CatalystKubeBuilderGameTest.class);
    }

    @RNSKubeJSBuilderTest
    private static void fluidOnlyCatalystsOmitOptionalFieldsAndAttachmentTag(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var catalystId = CreateRNS.asResource("test_fluid_only");

        context.catalysts().create(catalystId.toString())
                .fluid("lava", 1000);

        var generated = context.assembleData();
        var json = generated.jsonObject(catalystPath(catalystId));
        var lang = context.assembleLang();

        helper.assertFalse(json.has("chance_multiplier"), "Default chance multiplier should stay omitted");
        helper.assertFalse(json.has("count_multiplier"), "Default count multiplier should stay omitted");
        helper.assertFalse(json.has("optional"), "Optional flag should stay omitted by default");
        helper.assertFalse(json.has("display_priority"), "Display priority should stay omitted by default");
        helper.assertFalse(json.has("representative_items"), "Representative items should stay omitted by default");
        helper.assertFalse(json.has("hide_if_present"), "Hide-if-present should stay omitted by default");
        helper.assertFalse(json.has("play_when_active"), "Play-when-active should stay omitted by default");

        var requirements = json.getAsJsonArray("requirements");
        helper.assertValueEqual(requirements.size(), 1, "requirement count");
        assertFluidRequirement(helper, requirements.get(0).getAsJsonObject(), "minecraft:lava", 1000);

        CodecHelper.assertParses(helper.raw(), CatalystRequirementSet.CODEC, CodecHelper.registries(helper.raw()),
                json.toString(), "generated fluid-only catalyst");

        helper.assertFalse(generated.hasJson(CreateRNS.asResource("tags/block/miner_attachments")),
                "Fluid-only catalysts should not emit the miner attachment tag");
        helper.assertTrue(lang.map().isEmpty(), "Expected no lang entries without displayName()/description()");
    }

    @RNSKubeJSBuilderTest
    private static void metadataLangAndRequirementOrderingSerializeCorrectly(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var catalystId = CreateRNS.asResource("nested/test_catalyst");

        context.catalysts().create(catalystId.toString())
                .displayName("Nested Catalyst")
                .description("Nested description")
                .chanceMultiplier(0f)
                .countMultiplier(2.5)
                .optional()
                .displayPriority(4)
                .displayPriority(9)
                .representativeItem("stone")
                .representativeItem("minecraft:diamond")
                .hideIfPresent("create_rns:overclock")
                .hideIfPresent("create_rns:resonance")
                .playWhenActive("create_rns:mining")
                .attachment("stone")
                .fluid("lava", 250);

        var generated = context.assembleData();
        var json = generated.jsonObject(catalystPath(catalystId));
        var lang = context.assembleLang();

        CodecHelper.assertFloat(helper.raw(), json.get("chance_multiplier").getAsFloat(), 0f, "chance multiplier");
        helper.assertValueEqual(json.get("count_multiplier").getAsDouble(), 2.5, "count multiplier");
        helper.assertTrue(json.get("optional").getAsBoolean(), "Expected explicit optional flag");
        helper.assertValueEqual(json.get("display_priority").getAsInt(), 9, "last display priority wins");
        assertArrayStrings(helper, json.get("representative_items"), "representative items",
                "minecraft:stone",
                "minecraft:diamond");
        assertArrayStrings(helper, json.get("hide_if_present"), "hide-if-present entries",
                "create_rns:overclock",
                "create_rns:resonance");
        helper.assertValueEqual(json.getAsJsonObject("play_when_active").get("sound_id").getAsString(),
                "create_rns:mining", "play-when-active sound id");

        var requirements = json.getAsJsonArray("requirements");
        helper.assertValueEqual(requirements.size(), 2, "requirement count");
        assertAttachmentRequirement(helper, requirements.get(0).getAsJsonObject(), "minecraft:stone", null);
        assertFluidRequirement(helper, requirements.get(1).getAsJsonObject(), "minecraft:lava", 250);

        CodecHelper.assertParses(helper.raw(), CatalystRequirementSet.CODEC, CodecHelper.registries(helper.raw()),
                json.toString(), "generated catalyst with metadata and mixed requirements");

        assertLangValue(helper, lang, "create_rns", "en_us",
                "create_rns.catalyst.nested.test_catalyst.name", "Nested Catalyst");
        assertLangValue(helper, lang, "create_rns", "en_us",
                "create_rns.catalyst.nested.test_catalyst.description", "Nested description");
    }

    @RNSKubeJSBuilderTest
    private static void attachmentInputsMergeIntoSortedDedupedTagValues(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var firstId = CreateRNS.asResource("test_attachment_inputs");
        var secondId = CreateRNS.asResource("test_attachment_inputs_second");

        context.catalysts().create(firstId.toString())
                .attachment(List.of("stone", "cobblestone"), 2)
                .attachment(new Object[]{"andesite"}, 3)
                .fluid("water", 1);
        context.catalysts().create(secondId.toString())
                .attachment(List.of("granite", "stone"))
                .attachment("diorite");

        var generated = context.assembleData();
        var firstJson = generated.jsonObject(catalystPath(firstId));
        var secondJson = generated.jsonObject(catalystPath(secondId));
        var firstRequirements = firstJson.getAsJsonArray("requirements");
        var secondRequirements = secondJson.getAsJsonArray("requirements");

        helper.assertValueEqual(firstRequirements.size(), 3, "first catalyst requirement count");
        assertAttachmentRequirement(helper, firstRequirements.get(0).getAsJsonObject(),
                List.of("minecraft:stone", "minecraft:cobblestone"), 2);
        assertAttachmentRequirement(helper, firstRequirements.get(1).getAsJsonObject(), "minecraft:andesite", 3);
        assertFluidRequirement(helper, firstRequirements.get(2).getAsJsonObject(), "minecraft:water", 1);

        helper.assertValueEqual(secondRequirements.size(), 2, "second catalyst requirement count");
        assertAttachmentRequirement(helper, secondRequirements.get(0).getAsJsonObject(),
                List.of("minecraft:granite", "minecraft:stone"), null);
        assertAttachmentRequirement(helper, secondRequirements.get(1).getAsJsonObject(), "minecraft:diorite", null);

        var tagJson = generated.jsonObject(CreateRNS.asResource("tags/block/miner_attachments"));
        helper.assertFalse(tagJson.has("replace") && tagJson.get("replace").getAsBoolean(),
                "Attachment tag should append instead of replace");
        assertArrayStrings(helper, tagJson.get("values"), "attachment tag values",
                "minecraft:andesite",
                "minecraft:cobblestone",
                "minecraft:diorite",
                "minecraft:granite",
                "minecraft:stone");
    }

    @RNSKubeJSBuilderTest
    private static void assemblyRejectsCatalystsWithoutRequirements(RNSKubeJSBuilderTestContext context) {
        context.catalysts().create(CreateRNS.asResource("test_missing_requirements").toString())
                .displayName("Missing Requirements");

        assertValidationError(context, "must define at least one requirement");
        context.helper().assertValueEqual(context.assembleData().size(), 0, "Invalid catalysts emit no data");
    }

    @RNSKubeJSBuilderTest
    private static void hideIfPresentResolvesForwardReferencesAndPermitsExternalDatapacks(
            RNSKubeJSBuilderTestContext context
    ) {
        context.catalysts().create("testmod:test_forward_reference")
                .hideIfPresent("testmod:test_later_catalyst")
                .hideIfPresent("create_rns:overclock")
                .hideIfPresent("external_datapack:unknown_to_kubejs")
                .fluid("water", 1);
        context.catalysts().create("testmod:test_later_catalyst").fluid("water", 1);
        context.helper().assertTrue(context.validationErrors().isEmpty(),
                "Forward references and external datapack catalysts are accepted");
        context.helper().assertValueEqual(context.assembleData().size(), 2, "Both valid catalysts generate");
    }

    @RNSKubeJSBuilderTest
    private static void unknownOwnedCatalystReferenceInvalidatesOnlyItsBuilder(RNSKubeJSBuilderTestContext context) {
        var invalid = context.catalysts().create("create_rns:test_bad_reference")
                .hideIfPresent("create_rns:unknown_catalyst")
                .fluid("water", 1);
        var valid = context.catalysts().create("create_rns:test_good_reference").fluid("water", 1);
        assertValidationError(context, "Unknown catalyst in hideIfPresent");
        context.helper().assertFalse(invalid.isValid(), "Unknown owned reference invalidates its builder");
        context.helper().assertTrue(valid.isValid(), "Unrelated builder remains valid");
        context.helper().assertValueEqual(context.assembleData().size(), 1, "Only valid catalyst generates");
    }

    @RNSKubeJSBuilderTest
    private static void catalystNamespaceOwnershipIncludesLaterDeclarations(RNSKubeJSBuilderTestContext context) {
        var invalid = context.catalysts().create("create_rns:test_later_namespace")
                .hideIfPresent("testmod:missing")
                .fluid("water", 1);
        context.catalysts().create("testmod:declared_later").fluid("water", 1);

        assertValidationError(context, "Unknown catalyst in hideIfPresent: testmod:missing");
        context.helper().assertFalse(invalid.isValid(), "A later declaration makes its namespace owned");
        context.helper().assertValueEqual(context.assembleData().size(), 1, "Valid later declaration still generates");
    }

    @RNSKubeJSBuilderTest
    private static void chanceMultiplierRejectsNegativeValues(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builder = context.catalysts().create(CreateRNS.asResource("test_invalid_chance").toString());

        assertFatalBuilderError(helper, builder, "must be non-negative",
                () -> builder.chanceMultiplier(-0.01f));
    }

    @RNSKubeJSBuilderTest
    private static void idBasedHelpersRejectBlankAndInvalidInputs(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builder = context.catalysts().create(CreateRNS.asResource("test_invalid_ids").toString());

        assertFatalBuilderError(helper, builder, "Id cannot be blank",
                () -> builder.representativeItem(" "));
        assertFatalBuilderError(helper, builder, "Id cannot be blank",
                () -> builder.playWhenActive(" "));
        assertFatalBuilderError(helper, builder, "Id cannot be blank",
                () -> builder.fluid(" ", 1));
        assertFatalBuilderError(helper, builder, "Catalyst id cannot be blank",
                () -> builder.hideIfPresent(" "));
        assertFatalBuilderError(helper, builder, "Invalid catalyst id: create_rns:bad id",
                () -> builder.hideIfPresent("create_rns:bad id"));
    }

    @RNSKubeJSBuilderTest
    private static void attachmentInputsRejectUnsupportedValues(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builder = context.catalysts().create(CreateRNS.asResource("test_invalid_attachments").toString());

        assertFatalBuilderError(helper, builder, "must be positive",
                () -> builder.attachment("stone", 0));
        assertFatalBuilderError(helper, builder, "must be positive",
                () -> builder.attachment("stone", -1));
        assertFatalBuilderError(helper, builder, "cannot be blank",
                () -> builder.attachment(" "));
        assertFatalBuilderError(helper, builder, "Tags are not supported",
                () -> builder.attachment("#c:stones"));
        assertFatalBuilderError(helper, builder, "cannot be empty",
                () -> builder.attachment(List.of()));
        assertFatalBuilderError(helper, builder, "must contain only strings",
                () -> builder.attachment(List.of("stone", Integer.valueOf(1))));
        helper.assertTrue(builder.attachmentBlocks().isEmpty(), "A rejected list must not add any attachment blocks");
        assertFatalBuilderError(helper, builder, "must be a block id or list of block ids",
                () -> builder.attachment(Integer.valueOf(1)));
    }

    @RNSKubeJSBuilderTest
    private static void fluidRejectsNonPositiveAmounts(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builder = context.catalysts().create(CreateRNS.asResource("test_invalid_fluid_amount").toString());

        assertFatalBuilderError(helper, builder, "must be positive",
                () -> builder.fluid("lava", 0));
        assertFatalBuilderError(helper, builder, "must be positive",
                () -> builder.fluid("lava", -1));
    }

    private static void assertAttachmentRequirement(
            RNSKubeJSBuilderTestHelper helper, JsonObject requirement, String expectedBlockId, Integer expectedCount
    ) {
        helper.assertValueEqual(requirement.get("type").getAsString(), "attachment", "attachment requirement type");
        helper.assertValueEqual(requirement.get("attachment").getAsString(), expectedBlockId, "attachment requirement");
        assertAttachmentCount(helper, requirement, expectedCount);
    }

    private static void assertAttachmentRequirement(
            RNSKubeJSBuilderTestHelper helper, JsonObject requirement, List<String> expectedBlockIds, Integer expectedCount
    ) {
        helper.assertValueEqual(requirement.get("type").getAsString(), "attachment", "attachment requirement type");
        assertArrayStrings(helper, requirement.get("attachment"), "attachment requirement candidates",
                expectedBlockIds.toArray(String[]::new));
        assertAttachmentCount(helper, requirement, expectedCount);
    }

    private static void assertAttachmentCount(
            RNSKubeJSBuilderTestHelper helper, JsonObject requirement, Integer expectedCount
    ) {
        helper.assertValueEqual(requirement.has("count"), expectedCount != null, "attachment count presence");
        if (expectedCount != null) {
            helper.assertValueEqual(requirement.get("count").getAsInt(), expectedCount, "attachment count");
        }
    }

    private static void assertFluidRequirement(
            RNSKubeJSBuilderTestHelper helper, JsonObject requirement, String expectedFluidId, int expectedAmount
    ) {
        helper.assertValueEqual(requirement.get("type").getAsString(), "fluid", "fluid requirement type");
        var consume = requirement.getAsJsonObject("consume");
        helper.assertValueEqual(consume.get("id").getAsString(), expectedFluidId, "fluid id");
        helper.assertValueEqual(consume.get("amount").getAsInt(), expectedAmount, "fluid amount");

        var decoded = CodecHelper.assertParses(helper.raw(), FluidCatalystRequirement.MAP_CODEC.codec(),
                CodecHelper.registries(helper.raw()), requirement.toString(), "generated fluid requirement");
        helper.assertValueEqual(BuiltInRegistries.FLUID.getKey(decoded.fluidStack.getFluid()),
                ResourceLocation.parse(expectedFluidId), "decoded fluid id");
        helper.assertValueEqual(decoded.fluidStack.getAmount(), expectedAmount, "decoded fluid amount");
    }

    private static ResourceLocation catalystPath(ResourceLocation catalystId) {
        return ResourceLocation.fromNamespaceAndPath(catalystId.getNamespace(),
                "create_rns/catalyst/" + catalystId.getPath());
    }
}
