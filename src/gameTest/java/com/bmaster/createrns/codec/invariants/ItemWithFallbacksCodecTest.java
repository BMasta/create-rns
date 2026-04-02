package com.bmaster.createrns.codec.invariants;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.util.CodecHelper;
import com.bmaster.createrns.util.LogCapture;
import com.bmaster.createrns.util.codec.ItemWithFallbacks;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class ItemWithFallbacksCodecTest {
    private static final TagKey<Item> PLANKS_TAG =
            TagKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:planks"));

    @GameTest(template = "empty16x16")
    public void parsesSingleItemAndRoundTripsAsScalar(GameTestHelper helper) {
        var codec = ItemWithFallbacks.STRICT_CODEC;
        var parsed = CodecHelper.assertParses(helper, codec, CodecHelper.json(), "\"minecraft:diamond\"",
                "item fallback scalar");

        CodecHelper.assertSame(helper, Items.AIR, parsed.item,
                "Direct-item fallback should stay unresolved until live registry resolution");
        helper.assertTrue(parsed.resolve(helper.getLevel().registryAccess(), false),
                "Direct-item fallback should resolve against live registry data");
        CodecHelper.assertSame(helper, Items.DIAMOND, parsed.item, "resolved fallback item");
        assertRoundTrips(helper, codec, parsed, "\"minecraft:diamond\"", "scalar item fallback");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void parsesSingleTagAndRoundTripsAsSingletonList(GameTestHelper helper) {
        var codec = ItemWithFallbacks.STRICT_CODEC;
        var parsed = CodecHelper.assertParses(helper, codec, CodecHelper.json(), "[\"#minecraft:planks\"]",
                "item fallback singleton list");

        CodecHelper.assertSame(helper, Items.AIR, parsed.item,
                "Tag-backed fallback should stay unresolved until live registry initialization");
        helper.assertTrue(parsed.resolve(helper.getLevel().registryAccess(), false),
                "Singleton-list tag fallback should initialize against live registry data");
        helper.assertTrue(BuiltInRegistries.ITEM.wrapAsHolder(parsed.item).is(PLANKS_TAG),
                "Singleton-list tag fallback should resolve to an item from the live planks tag");
        helper.assertTrue(parsed.item != Items.AIR,
                "Singleton-list tag fallback should not resolve to air");
        assertRoundTrips(helper, codec, parsed, "[\"#minecraft:planks\"]", "singleton-list item fallback");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void resolvesFallbackEntriesInListedOrder(GameTestHelper helper) {
        var codec = ItemWithFallbacks.STRICT_CODEC;
        var sourceJson = """
                [
                  "create_rns:definitely_missing_item",
                  "#minecraft:planks",
                  "minecraft:diamond"
                ]
                """;
        var parsed = CodecHelper.assertParses(helper, codec, CodecHelper.json(), sourceJson,
                "ordered item fallback list");

        CodecHelper.assertSame(helper, Items.AIR, parsed.item,
                "Fallbacks that depend on tags should stay unresolved until initialization");
        helper.assertTrue(parsed.resolve(helper.getLevel().registryAccess(), false),
                "Ordered item fallback list should initialize against live registry data");
        helper.assertTrue(BuiltInRegistries.ITEM.wrapAsHolder(parsed.item).is(PLANKS_TAG),
                "Fallback resolution should stop at the first entry that resolves");
        assertRoundTrips(helper, codec, parsed, sourceJson, "ordered item fallback list");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void resolveFailsWhenNoFallbackCandidateResolves(GameTestHelper helper) {
        var parsed = CodecHelper.assertParses(helper, ItemWithFallbacks.STRICT_CODEC, CodecHelper.json(), """
                        [
                          "create_rns:definitely_missing_item",
                          "#create_rns:definitely_missing_tag"
                        ]
                        """, "unresolvable item fallback list");

        try (var logs = LogCapture.capture(CreateRNS.LOGGER.getName())) {
            helper.assertFalse(parsed.resolve(helper.getLevel().registryAccess(), false),
                    "Unresolvable strict fallbacks should fail during live registry resolution");
            helper.assertTrue(logs.contains("Could not resolve item \"create_rns:definitely_missing_item\""),
                    "Unresolvable fallback errors should report missing direct item candidates");
            helper.assertTrue(logs.contains("Could not resolve item tag \"#create_rns:definitely_missing_tag\""),
                    "Unresolvable fallback errors should report missing tag candidates");
        }
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void streamCodecRoundTripsOriginalEntriesAndListShape(GameTestHelper helper) {
        var codec = ItemWithFallbacks.STRICT_CODEC;
        var parsed = CodecHelper.assertParses(helper, codec, CodecHelper.json(), """
                        [
                          "create_rns:definitely_missing_item",
                          "#minecraft:planks",
                          "minecraft:diamond"
                        ]
                        """, "stream round-trip item fallback");

        var restored = roundTrip(ItemWithFallbacks.STREAM_CODEC, parsed, helper.getLevel().registryAccess());

        CodecHelper.assertSame(helper, Items.AIR, restored.item,
                "Stream codec should preserve unresolved tag-backed fallbacks before initialization");
        helper.assertTrue(restored.resolve(helper.getLevel().registryAccess(), false),
                "Restored fallback should initialize against live registry data");
        helper.assertTrue(BuiltInRegistries.ITEM.wrapAsHolder(restored.item).is(PLANKS_TAG),
                "Stream codec should preserve the resolved main item");
        assertRoundTrips(helper, codec, restored, """
                [
                  "create_rns:definitely_missing_item",
                  "#minecraft:planks",
                  "minecraft:diamond"
                ]
                """, "stream round-trip item fallback");
        helper.succeed();
    }

    private static void assertRoundTrips(
            GameTestHelper helper, Codec<ItemWithFallbacks> codec,
            ItemWithFallbacks parsed, String sourceJson, String valueName
    ) {
        JsonElement encoded = codec.encodeStart(CodecHelper.json(), parsed).result().orElse(null);
        helper.assertTrue(encoded != null, "Expected " + valueName + " encode success");
        helper.assertValueEqual(encoded, CodecHelper.parseJson(sourceJson), valueName + " JSON");
    }

    private static <T> T roundTrip(
            StreamCodec<RegistryFriendlyByteBuf, T> codec, T value, RegistryAccess access
    ) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), access, ConnectionType.NEOFORGE);
        codec.encode(buffer, value);
        return codec.decode(buffer);
    }
}
