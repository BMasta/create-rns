package com.bmaster.createrns.codec.invariants;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.spec.DepositSpec;
import com.bmaster.createrns.util.CodecHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class DepositSpecCodecTest {
    private static final ResourceLocation IRON_STRUCTURE_ID = CreateRNS.asResource("deposit_iron");
    private static final TagKey<Item> PLANKS_TAG =
            TagKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:planks"));

    @GameTest(template = "empty16x16")
    public void parsesDepositSpecAndInitializesDirectIcons(GameTestHelper helper) {
        var spec = CodecHelper.assertParses(helper, DepositSpec.CODEC, CodecHelper.json(), """
                        {
                          "scanner_icon_item": "minecraft:diamond",
                          "map_icon_item": "minecraft:compass",
                          "structure": "create_rns:deposit_iron"
                        }
        """, "deposit spec");

        helper.assertTrue(spec.getScannerIcon() == null,
                "Scanner icon should stay unresolved until deposit spec initialization");
        CodecHelper.assertValueEqual(helper, spec.structure, IRON_STRUCTURE_ID, "deposit structure id");
        CodecHelper.assertValueEqual(helper, spec.structureKey().location(), IRON_STRUCTURE_ID,
                "deposit structure key id");
        helper.assertTrue(spec.initialize(helper.getLevel().registryAccess()),
                "Deposit spec initialization should resolve both icon items");
        CodecHelper.assertSame(helper, Items.DIAMOND, spec.getScannerIcon(), "resolved scanner icon");
        CodecHelper.assertSame(helper, Items.COMPASS, spec.getMapIcon().getItem(), "resolved map icon");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void initializesScannerIconFromFirstResolvableFallback(GameTestHelper helper) {
        var spec = CodecHelper.assertParses(helper, DepositSpec.CODEC, CodecHelper.json(), """
                        {
                          "scanner_icon_item": [
                            "create_rns:definitely_missing_item",
                            "#minecraft:planks",
                            "minecraft:diamond"
                          ],
                          "map_icon_item": "minecraft:compass",
                          "structure": "create_rns:deposit_iron"
                        }
                        """, "fallback-backed deposit spec");

        helper.assertTrue(spec.initialize(helper.getLevel().registryAccess()),
                "Deposit spec initialization should keep the first fallback candidate that resolves");
        var scannerIcon = spec.getScannerIcon();
        helper.assertTrue(scannerIcon != null && BuiltInRegistries.ITEM.wrapAsHolder(scannerIcon).is(PLANKS_TAG),
                "Scanner icon should resolve from the first live fallback candidate instead of later entries");
        CodecHelper.assertSame(helper, Items.COMPASS, spec.getMapIcon().getItem(), "fallback spec map icon");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void rejectsEmptyScannerIconDefinition(GameTestHelper helper) {
        CodecHelper.assertFails(helper, DepositSpec.CODEC, CodecHelper.json(), """
                        {
                          "scanner_icon_item": [],
                          "map_icon_item": "minecraft:compass",
                          "structure": "create_rns:deposit_iron"
                        }
                        """, "No items or item tags specified");
        helper.succeed();
    }
}
