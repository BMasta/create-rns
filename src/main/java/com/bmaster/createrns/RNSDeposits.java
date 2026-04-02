package com.bmaster.createrns;

import com.bmaster.createrns.RNSTags.RNSBlockTags;
import com.bmaster.createrns.compat.Mods;
import com.bmaster.createrns.content.deposit.DepositBlock;
import com.bmaster.createrns.data.pack.*;
import com.bmaster.createrns.data.pack.DynamicDatapackContent.Dimension;
import com.bmaster.createrns.data.pack.YieldBuilder.ConfiguredWeightedItem;
import com.simibubi.create.Create;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RNSDeposits {
    public static final int DURABILITY_CORE = 200000;
    public static final int DURABILITY_EDGE = 75000;
    public static final float DURABILITY_SPREAD = 0.2f;

    public static final float CHANCE_EXTREMELY_LOW = 0.0005f;
    public static final float CHANCE_VERY_LOW = 0.005f;
    public static final float CHANCE_LOW = 0.05f;
    public static final float CHANCE_NORMAL = 0.5f;
    public static final float CHANCE_SHATTERING_RESONANCE = 0.3f;
    public static final float CHANCE_FAINT_STABILIZING_RESONANCE = 0.15f;
    public static final float CHANCE_STABILIZING_RESONANCE = 0.06f;

    public static final String CATA_FAINT_RESONANCE = "faint_resonance";
    public static final String CATA_RESONANCE = "resonance";
    public static final String CATA_FAINT_SHATTERING_RESONANCE = "faint_shattering_resonance";
    public static final String CATA_SHATTERING_RESONANCE = "shattering_resonance";
    public static final String CATA_FAINT_STABILIZING_RESONANCE = "faint_stabilizing_resonance";
    public static final String CATA_STABILIZING_RESONANCE = "stabilizing_resonance";
    public static final String CATA_OVERCLOCK = "overclock";

    public static final int COLOR_FAINT_RESONANCE = 0xFF968CB3;
    public static final int COLOR_RESONANCE = 0xFF8572BF;
    public static final int COLOR_FAINT_SHATTERING_RESONANCE = 0xFFB28F8E;
    public static final int COLOR_SHATTERING_RESONANCE = 0xFFBF7672;
    public static final int COLOR_FAINT_STABILIZING_RESONANCE = 0xFF8EA9B2;
    public static final int COLOR_STABILIZING_RESONANCE = 0xFF72ACBF;

    //========================================= Vanilla + Create | Overworld =========================================//

    public static final BlockEntry<DepositBlock> IRON_DEPOSIT = DepositBlockBuilder
            .create("iron")
            .transform(depositBlockProperties(MapColor.RAW_IRON))
            .attach(ctx -> DepositStructureBuilder.create(ctx)
                    .transform(bulkDepositStructure(2))
                    .save())
            .attach(id -> MiningRecipeBuilder.create(id)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            y -> y.chance(CHANCE_NORMAL).item("iron_nugget"),
                            y -> y.chance(CHANCE_LOW).item("raw_iron"),
                            y -> y.chance(CHANCE_VERY_LOW).item("raw_iron_block")))
                    .transform(sharedResonanceYields())
                    .save())
            .attach(ctx -> DepositSpecBuilder.create(ctx)
                    .scannerIconVanillaItem("raw_iron")
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> COPPER_DEPOSIT = DepositBlockBuilder
            .create("copper")
            .transform(depositBlockProperties(MapColor.COLOR_ORANGE))
            .attach(ctx -> DepositStructureBuilder.create(ctx)
                    .transform(bulkDepositStructure(2))
                    .save())
            .attach(ctx -> DepositSpecBuilder.create(ctx)
                    .scannerIconVanillaItem("raw_copper")
                    .save())
            .attach(ctx -> MiningRecipeBuilder.create(ctx)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            y -> y.chance(CHANCE_NORMAL).item(List.of(
                                    Create.ID + ":copper_nugget",
                                    nuggetTag("copper"))),
                            y -> y.chance(CHANCE_LOW).item("raw_copper"),
                            y -> y.chance(CHANCE_VERY_LOW).item("raw_copper_block")))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> ZINC_DEPOSIT = DepositBlockBuilder
            .create("zinc")
            .transform(depositBlockProperties(MapColor.GLOW_LICHEN))
            .attach(ctx -> DepositStructureBuilder.create(ctx)
                    .transform(preciousDepositStructure(2))
                    .save())
            .attach(ctx -> DepositSpecBuilder.create(ctx)
                    .scannerIconItem(Create.ID, "raw_zinc")
                    .save())
            .attach(id -> MiningRecipeBuilder.create(id)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            y -> y,
                            y -> y.chance(CHANCE_NORMAL).item(List.of(
                                    Create.ID + ":zinc_nugget",
                                    nuggetTag("zinc"))),
                            y -> y.chance(CHANCE_LOW).item(List.of(
                                    Create.ID + ":raw_zinc",
                                    rawMaterialTag("zinc")))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> GOLD_DEPOSIT = DepositBlockBuilder
            .create("gold")
            .transform(depositBlockProperties(MapColor.GOLD))
            .attach(ctx -> DepositStructureBuilder.create(ctx)
                    .transform(preciousDepositStructure(2))
                    .save())
            .attach(ctx -> DepositSpecBuilder.create(ctx)
                    .scannerIconVanillaItem("raw_gold")
                    .save())
            .attach(id -> MiningRecipeBuilder.create(id)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            y -> y,
                            y -> y.chance(CHANCE_NORMAL).item("gold_nugget"),
                            y -> y.chance(CHANCE_LOW).item("raw_gold")))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> REDSTONE_DEPOSIT = DepositBlockBuilder
            .create("redstone")
            .transform(depositBlockProperties(MapColor.FIRE))
            .attach(ctx -> DepositStructureBuilder.create(ctx)
                    .transform(preciousDepositStructure(2))
                    .save())
            .attach(ctx -> DepositSpecBuilder.create(ctx)
                    .scannerIconVanillaItem("redstone")
                    .save())
            .attach(id -> MiningRecipeBuilder.create(id)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            y -> y,
                            y -> y.chance(CHANCE_NORMAL).item(CreateRNS.ID + ":redstone_small_dust"),
                            y -> y.chance(CHANCE_LOW).item("redstone")))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    //============================================== Compat | Overworld ==============================================//

    public static final BlockEntry<DepositBlock> TIN_DEPOSIT = DepositBlockBuilder
            .create("tin")
            .transform(depositBlockProperties(MapColor.COLOR_BLUE))
            .enableWhenBlockPresent("tin_ore")
            .enableWhenBlockPresent("deepslate_tin_ore")
            .attach(ctx -> DepositStructureBuilder.create(ctx)
                    .transform(bulkDepositStructure(1))
                    .save())
            .attach(ctx -> DepositSpecBuilder.create(ctx)
                    .transform(scannerIconTagCandidates("tin"))
                    .save())
            .attach(ctx -> MiningRecipeBuilder.create(ctx)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            y -> y.chance(CHANCE_NORMAL).compatItem(nuggetTag("tin")),
                            y -> y.chance(CHANCE_LOW).compatItem(rawMaterialTag("tin")),
                            y -> y.chance(CHANCE_VERY_LOW).compatItem(rawBlockTag("tin"))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> LEAD_DEPOSIT = DepositBlockBuilder
            .create("lead")
            .transform(depositBlockProperties(MapColor.COLOR_BLUE))
            .enableWhenBlockPresent("lead_ore")
            .enableWhenBlockPresent("deepslate_lead_ore")
            .attach(ctx -> DepositStructureBuilder.create(ctx)
                    .transform(semiPreciousDepositStructure(1))
                    .save())
            .attach(ctx -> DepositSpecBuilder.create(ctx)
                    .transform(scannerIconTagCandidates("lead"))
                    .save())
            .attach(ctx -> MiningRecipeBuilder.create(ctx)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            y -> y.chance(CHANCE_NORMAL).compatItem(nuggetTag("lead")),
                            y -> y.chance(CHANCE_LOW).compatItem(rawMaterialTag("lead")),
                            y -> y.chance(CHANCE_VERY_LOW).compatItem(rawBlockTag("lead"))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> NICKEL_DEPOSIT = DepositBlockBuilder
            .create("nickel")
            .transform(depositBlockProperties(MapColor.SAND))
            .enableWhenBlockPresent("nickel_ore")
            .enableWhenBlockPresent("deepslate_nickel_ore")
            .attach(ctx -> DepositStructureBuilder.create(ctx)
                    .transform(semiPreciousDepositStructure(1))
                    .save())
            .attach(ctx -> DepositSpecBuilder.create(ctx)
                    .transform(scannerIconTagCandidates("nickel"))
                    .save())
            .attach(ctx -> MiningRecipeBuilder.create(ctx)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            y -> y,
                            y -> y.chance(CHANCE_NORMAL).compatItem(nuggetTag("nickel")),
                            y -> y.chance(CHANCE_LOW).compatItem(rawMaterialTag("nickel"))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> SILVER_DEPOSIT = DepositBlockBuilder
            .create("silver")
            .transform(depositBlockProperties(MapColor.SNOW))
            .enableWhenBlockPresent("silver_ore")
            .enableWhenBlockPresent("deepslate_silver_ore")
            .attach(ctx -> DepositStructureBuilder.create(ctx)
                    .transform(preciousDepositStructure(1))
                    .save())
            .attach(ctx -> DepositSpecBuilder.create(ctx)
                    .transform(scannerIconTagCandidates("silver"))
                    .save())
            .attach(ctx -> MiningRecipeBuilder.create(ctx)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            y -> y,
                            y -> y.chance(CHANCE_NORMAL).compatItem(nuggetTag("silver")),
                            y -> y.chance(CHANCE_LOW).compatItem(rawMaterialTag("silver"))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> URANIUM_DEPOSIT = DepositBlockBuilder
            .create("uranium")
            .transform(depositBlockProperties(MapColor.COLOR_GREEN))
            .enableWhenBlockPresent("uranium_ore")
            .enableWhenBlockPresent("deepslate_uranium_ore")
            .attach(ctx -> DepositSpecBuilder.create(ctx)
                    .transform(scannerIconTagCandidates("uranium"))
                    .save())
            .attach(ctx -> DepositStructureBuilder.create(ctx)
                    .transform(preciousDepositStructure(1))
                    .save())
            .attach(ctx -> MiningRecipeBuilder.create(ctx)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            y -> y,
                            y -> y.chance(CHANCE_NORMAL).compatItem(List.of(
                                    Mods.NUCLEAR.ID + ":uranium_powder",
                                    nuggetTag("uranium"))),
                            y -> y.chance(CHANCE_LOW).compatItem(rawMaterialTag("uranium"))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> THORIUM_DEPOSIT = DepositBlockBuilder
            .create("thorium")
            .transform(depositBlockProperties(MapColor.COLOR_ORANGE))
            .enableWhenBlockPresent("thorium_ore")
            .enableWhenBlockPresent("deepslate_thorium_ore")
            .attach(ctx -> DepositStructureBuilder.create(ctx)
                    .transform(preciousDepositStructure(1))
                    .save())
            .attach(ctx -> DepositSpecBuilder.create(ctx)
                    .scannerIconItem(Mods.NEW_AGE.ID, "thorium")
                    .transform(scannerIconTagCandidates("thorium"))
                    .save())
            .attach(ctx -> MiningRecipeBuilder.create(ctx)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            y -> y,
                            y -> y,
                            y -> y.chance(CHANCE_NORMAL).compatItem(List.of(
                                    Mods.NEW_AGE.ID + ":thorium",
                                    rawMaterialTag("thorium")))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    //=========================================== Vanilla + Create | Nether ==========================================//

    public static final BlockEntry<DepositBlock> QUARTZ_DEPOSIT = DepositBlockBuilder
            .create("quartz")
            .transform(depositBlockProperties(MapColor.QUARTZ))
            .attach(ctx -> DepositStructureBuilder.create(ctx)
                    .transform(bulkNetherDepositStructure(1))
                    .save())
            .attach(ctx -> DepositSpecBuilder.create(ctx)
                    .scannerIconVanillaItem("quartz")
                    .save())
            .attach(id -> MiningRecipeBuilder.create(id)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            y -> y.chance(CHANCE_NORMAL).item("quartz"),
                            y -> y.chance(CHANCE_NORMAL).item("quartz"),
                            y -> y.chance(CHANCE_NORMAL).item(List.of(
                                    gemTag("certus_quartz"),
                                    "quartz"))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    //================================================ Compat | Nether ===============================================//

    public static final BlockEntry<DepositBlock> COBALT_DEPOSIT = DepositBlockBuilder
            .create("cobalt")
            .transform(depositBlockProperties(MapColor.LAPIS))
            .enableWhenBlockPresent("cobalt_ore")
            .attach(ctx -> DepositStructureBuilder.create(ctx)
                    .transform(preciousNetherDepositStructure(1))
                    .save())
            .attach(ctx -> DepositSpecBuilder.create(ctx)
                    .transform(scannerIconTagCandidates("cobalt"))
                    .save())
            .attach(ctx -> MiningRecipeBuilder.create(ctx)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            y -> y,
                            y -> y.chance(CHANCE_NORMAL).compatItem(nuggetTag("cobalt")),
                            y -> y.chance(CHANCE_LOW).compatItem(rawMaterialTag("cobalt"))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    //================================================================================================================//

    public static final BlockEntry<DepositBlock> DEPLETED_DEPOSIT = DepositBlockBuilder
            .create("depleted")
            .transform(depositBlockProperties(MapColor.COLOR_BLACK))
            .attach(id -> MiningRecipeBuilder.create(id)
                    .yield(y -> y.item(List.of("cobblestone")))
                    .save())
            .register();

    public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> depositBlockProperties(MapColor mapColor) {
        return b -> b
                .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
                .properties(p -> p
                        .mapColor(mapColor)
                        .strength(50.0F, 1200f)
                        .sound(SoundType.DEEPSLATE)
                        .noLootTable())
                .transform(pickaxeOnly())
                .tag(BlockTags.NEEDS_DIAMOND_TOOL)
                .tag(RNSBlockTags.DEPOSIT_BLOCKS)
                .item()
                .build();
    }

    public static UnaryOperator<DepositStructureBuilder> bulkDepositStructure(int weightMultiplier) {
        return b -> b
                .depth(8)
                .weight(50 * weightMultiplier)
                .nbt(DepositStructureBuilder.DEP_MEDIUM, 70)
                .nbt(DepositStructureBuilder.DEP_LARGE, 30);
    }

    public static UnaryOperator<DepositStructureBuilder> semiPreciousDepositStructure(int weightMultiplier) {
        return b -> b
                .depth(10)
                .weight(35 * weightMultiplier)
                .nbt(DepositStructureBuilder.DEP_SMALL, 30)
                .nbt(DepositStructureBuilder.DEP_MEDIUM, 60)
                .nbt(DepositStructureBuilder.DEP_LARGE, 10);
    }

    public static UnaryOperator<DepositStructureBuilder> preciousDepositStructure(int weightMultiplier) {
        return b -> b
                .depth(12)
                .weight(20 * weightMultiplier)
                .nbt(DepositStructureBuilder.DEP_SMALL, 70)
                .nbt(DepositStructureBuilder.DEP_MEDIUM, 28)
                .nbt(DepositStructureBuilder.DEP_LARGE, 2);
    }

    public static UnaryOperator<DepositStructureBuilder> bulkNetherDepositStructure(int weightMultiplier) {
        return b -> b
                .dimension(Dimension.NETHER)
                .depth(4)
                .weight(50 * weightMultiplier)
                .nbt(DepositStructureBuilder.DEP_MEDIUM, 70)
                .nbt(DepositStructureBuilder.DEP_LARGE, 30);
    }

    public static UnaryOperator<DepositStructureBuilder> preciousNetherDepositStructure(int weightMultiplier) {
        return b -> b
                .dimension(Dimension.NETHER)
                .depth(4)
                .weight(20 * weightMultiplier)
                .nbt(DepositStructureBuilder.DEP_SMALL, 70)
                .nbt(DepositStructureBuilder.DEP_MEDIUM, 28)
                .nbt(DepositStructureBuilder.DEP_LARGE, 2);
    }

    private static UnaryOperator<DepositSpecBuilder> scannerIconTagCandidates(String material) {
        return b -> b
                .scannerIconCommonTag("raw_materials/" + material)
                .scannerIconCommonTag("ores/" + material)
                .scannerIconCommonTag("ingots/" + material)
                .scannerIconCommonTag("nuggets/" + material);
    }

    private static UnaryOperator<MiningRecipeBuilder> addYield(
            List<ConfiguredWeightedItem> items, UnaryOperator<YieldBuilder> andThenApply
    ) {
        if (items.isEmpty()) return y -> y;
        return b -> b.yield(y -> {
            var yld = y;
            for (var item : items) {
                if (item.compat()) {
                    yld = yld.compatItem(item.candidateIds());
                } else {
                    yld = yld.item(item.candidateIds());
                }
            }
            andThenApply.apply(yld);
        });
    }

    private static UnaryOperator<MiningRecipeBuilder> baseYields(
            UnaryOperator<YieldBuilder> t0, UnaryOperator<YieldBuilder> t1, UnaryOperator<YieldBuilder> t2
    ) {
        return b -> b
                .yield(y -> y.item(List.of("cobblestone")))
                .yield(y -> y
                        .item(CreateRNS.ID + ":resonant_amethyst")
                        .chance(CHANCE_EXTREMELY_LOW)
                        .catalyst(CATA_OVERCLOCK))
                // T0
                .yield(y -> t0.apply(y)
                        .catalyst(CATA_OVERCLOCK))
                // T1
                .yield(y -> t1.apply(y)
                        .catalyst(CATA_FAINT_RESONANCE)
                        .catalyst(CATA_OVERCLOCK)
                        .jeiSlotColor(COLOR_FAINT_RESONANCE))
                .yield(y -> y
                        .item(CreateRNS.ID + ":resonant_amethyst")
                        .chance(CHANCE_EXTREMELY_LOW)
                        .catalyst(CATA_RESONANCE)
                        .catalyst(CATA_OVERCLOCK)
                        .jeiSlotColor(COLOR_RESONANCE))
                // T2
                .yield(y -> t2.apply(y)
                        .catalyst(CATA_RESONANCE)
                        .catalyst(CATA_OVERCLOCK)
                        .jeiSlotColor(COLOR_RESONANCE));
    }

    private static UnaryOperator<MiningRecipeBuilder> baseNetherYields(
            UnaryOperator<YieldBuilder> t0, UnaryOperator<YieldBuilder> t1, UnaryOperator<YieldBuilder> t2
    ) {
        return b -> b
                .yield(y -> y.item(List.of("netherrack")))
                .yield(y -> y
                        .item(List.of("soul_sand"))
                        .item(List.of("soul_soil"))
                        .item(List.of("magma_block")))
                .yield(y -> y
                        .item(List.of(CreateRNS.ID + ":resonant_amethyst"))
                        .chance(CHANCE_EXTREMELY_LOW)
                        .catalyst(CATA_OVERCLOCK))
                // T0
                .yield(y -> t0.apply(y)
                        .catalyst(CATA_OVERCLOCK))
                // T1
                .yield(y -> t1.apply(y)
                        .catalyst(CATA_FAINT_RESONANCE)
                        .catalyst(CATA_OVERCLOCK)
                        .jeiSlotColor(COLOR_FAINT_RESONANCE))
                .yield(y -> y
                        .item(List.of(CreateRNS.ID + ":resonant_amethyst"))
                        .chance(CHANCE_EXTREMELY_LOW)
                        .catalyst(CATA_RESONANCE)
                        .catalyst(CATA_OVERCLOCK)
                        .jeiSlotColor(COLOR_RESONANCE))

                // T2
                .yield(y -> t2.apply(y)
                        .catalyst(CATA_RESONANCE)
                        .catalyst(CATA_OVERCLOCK)
                        .jeiSlotColor(COLOR_RESONANCE));
    }

    private static Consumer<YieldBuilder> faintShatteringResonanceYield() {
        return y -> y
                .item(List.of("tuff"))
                .item(List.of("calcite"))
                .item(List.of(Create.ID + ":limestone"))
                .catalyst(CATA_FAINT_SHATTERING_RESONANCE)
                .jeiSlotColor(COLOR_FAINT_SHATTERING_RESONANCE);
    }

    private static Consumer<YieldBuilder> shatteringResonanceYield() {
        return y -> y
                .chance(CHANCE_SHATTERING_RESONANCE)
                .item(List.of(Create.ID + ":crimsite"))
                .item(List.of(Create.ID + ":veridium"))
                .item(List.of(Create.ID + ":asurine"))
                .item(List.of(Create.ID + ":ochrum"))
                .catalyst(CATA_SHATTERING_RESONANCE)
                .catalyst(CATA_OVERCLOCK)
                .jeiSlotColor(COLOR_SHATTERING_RESONANCE);
    }

    private static Consumer<YieldBuilder> faintStabilizingResonanceYield() {
        return y -> y
                .chance(CHANCE_FAINT_STABILIZING_RESONANCE)
                .item(List.of("lapis_lazuli"))
                .item(List.of("amethyst_shard"))
                .item(List.of("emerald"))
                .catalyst(CATA_FAINT_STABILIZING_RESONANCE)
                .catalyst(CATA_OVERCLOCK)
                .jeiSlotColor(COLOR_FAINT_STABILIZING_RESONANCE);
    }

    private static Consumer<YieldBuilder> stabilizingResonanceYield() {
        return y -> y
                .chance(CHANCE_STABILIZING_RESONANCE)
                .item(List.of("redstone"), 5)
                .item(List.of("diamond"))
                .catalyst(CATA_STABILIZING_RESONANCE)
                .catalyst(CATA_OVERCLOCK)
                .jeiSlotColor(COLOR_STABILIZING_RESONANCE);
    }

    private static UnaryOperator<MiningRecipeBuilder> sharedResonanceYields() {
        return b -> b
                .yield(faintShatteringResonanceYield())
                .yield(shatteringResonanceYield())
                .yield(faintStabilizingResonanceYield())
                .yield(stabilizingResonanceYield());
    }

    private static String nuggetTag(String keyword) {
        return "#forge:nuggets/" + keyword;
    }

    private static String rawMaterialTag(String keyword) {
        return "#forge:raw_materials/" + keyword;
    }

    private static String rawBlockTag(String keyword) {
        return "#forge:storage_blocks/raw_" + keyword;
    }

    private static String gemTag(String keyword) {
        return "#forge:gems/" + keyword;
    }

    public static void register() {
    }
}
