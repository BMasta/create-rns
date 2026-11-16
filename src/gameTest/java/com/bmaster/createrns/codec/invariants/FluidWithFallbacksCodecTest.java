package com.bmaster.createrns.codec.invariants;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.util.CodecHelper;
import com.bmaster.createrns.util.LogCapture;
import com.bmaster.createrns.util.codec.FluidWithFallbacks;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.List;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class FluidWithFallbacksCodecTest {
    @GameTest(template = "empty16x16")
    public void preservesScalarAndSingletonListShape(GameTestHelper helper) {
        for (var source : List.of("\"minecraft:lava\"", "[\"minecraft:lava\"]")) {
            var parsed = CodecHelper.assertParses(helper, FluidWithFallbacks.STRICT_CODEC, CodecHelper.json(),
                    source, "fluid fallback");
            CodecHelper.assertSame(helper, Fluids.EMPTY, parsed.value(), "unresolved fluid");
            helper.assertTrue(parsed.resolve(helper.getLevel().registryAccess(), false), "Lava should resolve");
            CodecHelper.assertSame(helper, Fluids.LAVA, parsed.value(), "resolved fluid");
            helper.assertValueEqual(
                    FluidWithFallbacks.STRICT_CODEC.encodeStart(CodecHelper.json(), parsed).getOrThrow(),
                    CodecHelper.parseJson(source), "original JSON shape");
        }
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void resolvesFirstAvailableFallbackIncludingTags(GameTestHelper helper) {
        var parsed = CodecHelper.assertParses(helper, FluidWithFallbacks.STRICT_CODEC, CodecHelper.json(), """
                ["create_rns:missing_fluid", "#create_rns:missing_fluid_tag", "#minecraft:water", "minecraft:lava"]
                """, "ordered fluid fallbacks");
        helper.assertTrue(parsed.resolve(helper.getLevel().registryAccess(), false), "Water tag should resolve");
        helper.assertTrue(BuiltInRegistries.FLUID.wrapAsHolder(parsed.value()).is(
                TagKey.create(Registries.FLUID, ResourceLocation.parse("minecraft:water"))),
                "Resolution should stop at the water tag before reaching lava");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void validatesStrictAndLenientFallbacks(GameTestHelper helper) {
        CodecHelper.assertFails(helper, FluidWithFallbacks.STRICT_CODEC, CodecHelper.json(), "[]",
                "No fluid resources or tags specified");
        CodecHelper.assertFails(helper, FluidWithFallbacks.STRICT_RESOLVABLE_CODEC, CodecHelper.json(),
                "\"minecraft:empty\"", "None of the fluid resources or tags resolved: [minecraft:empty]");
        CodecHelper.assertFails(helper, FluidWithFallbacks.STRICT_RESOLVABLE_CODEC, CodecHelper.json(),
                "\"create_rns:missing_fluid\"",
                "None of the fluid resources or tags resolved: [create_rns:missing_fluid]");
        CodecHelper.assertParses(helper, FluidWithFallbacks.STRICT_RESOLVABLE_CODEC, CodecHelper.json(),
                "[\"create_rns:missing_fluid\", \"minecraft:lava\"]", "registered fallback");
        CodecHelper.assertParses(helper, FluidWithFallbacks.STRICT_RESOLVABLE_CODEC, CodecHelper.json(),
                "\"#create_rns:missing_fluid_tag\"", "deferred tag validation");

        for (var source : List.of("[]", "\"create_rns:missing_fluid\"")) {
            var parsed = CodecHelper.assertParses(helper, FluidWithFallbacks.LENIENT_CODEC, CodecHelper.json(),
                    source, "lenient fluid fallback");
            helper.assertTrue(parsed.resolve(helper.getLevel().registryAccess(), true),
                    "Lenient resolution should allow missing fluid resources or tags");
            CodecHelper.assertSame(helper, Fluids.EMPTY, parsed.value(), "lenient empty fallback");
            helper.assertValueEqual(
                    FluidWithFallbacks.LENIENT_CODEC.encodeStart(CodecHelper.json(), parsed).getOrThrow(),
                    CodecHelper.parseJson(source), "lenient JSON shape");
        }
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void reportsFluidSpecificResolutionFailures(GameTestHelper helper) {
        var parsed = CodecHelper.assertParses(helper, FluidWithFallbacks.STRICT_CODEC, CodecHelper.json(), """
                ["create_rns:missing_fluid", "#create_rns:missing_fluid_tag"]
                """, "missing fluid fallbacks");
        try (var logs = LogCapture.capture(CreateRNS.LOGGER.getName())) {
            helper.assertFalse(parsed.resolve(helper.getLevel().registryAccess(), false),
                    "Strict resolution should fail without a matching resource");
            helper.assertTrue(logs.contains("Could not resolve fluid \"create_rns:missing_fluid\""),
                    "Missing resource should be identified as a fluid");
            helper.assertTrue(logs.contains("Could not resolve fluid tag \"#create_rns:missing_fluid_tag\""),
                    "Missing tag should be identified as a fluid tag");
        }
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void streamCodecPreservesOriginalWireFormat(GameTestHelper helper) {
        var source = "[\"create_rns:missing_fluid\", \"minecraft:lava\"]";
        var parsed = CodecHelper.assertParses(helper, FluidWithFallbacks.STRICT_CODEC, CodecHelper.json(),
                source, "fluid stream fallback");
        helper.assertTrue(parsed.resolve(helper.getLevel().registryAccess(), false), "Lava should resolve");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess(),
                ConnectionType.NEOFORGE);
        try {
            FluidWithFallbacks.STREAM_CODEC.encode(buffer, parsed);

            // Read the original layout independently of the new shared codec.
            CodecHelper.assertSame(helper, Fluids.LAVA, ByteBufCodecs.registry(Registries.FLUID).decode(buffer),
                    "resolved fluid on wire");
            helper.assertValueEqual(buffer.readVarInt(), 2, "fallback count on wire");
            for (var entry : List.of("create_rns:missing_fluid", "minecraft:lava")) {
                helper.assertValueEqual(ResourceLocation.STREAM_CODEC.decode(buffer), ResourceLocation.parse(entry),
                        "fallback id on wire");
                helper.assertFalse(buffer.readBoolean(), "Direct fallback should not have the tag flag");
            }
            helper.assertTrue(buffer.readBoolean(), "List shape flag should be preserved");
            helper.assertFalse(buffer.isReadable(), "No extra fields should be written");

            buffer.readerIndex(0);
            var restored = FluidWithFallbacks.STREAM_CODEC.decode(buffer);
            CodecHelper.assertSame(helper, Fluids.LAVA, restored.value(), "restored resolved fluid");
            helper.assertValueEqual(
                    FluidWithFallbacks.STRICT_CODEC.encodeStart(CodecHelper.json(), restored).getOrThrow(),
                    CodecHelper.parseJson(source), "restored JSON fallbacks");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }
}
