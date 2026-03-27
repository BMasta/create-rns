package com.bmaster.createrns;

import com.bmaster.createrns.compat.Mods;
import com.bmaster.createrns.content.deposit.DepositBlock;
import com.bmaster.createrns.data.pack.DepositStructureBuilder;
import com.bmaster.createrns.data.pack.DynamicDatapackContent.Dimension;
import com.bmaster.createrns.data.pack.MiningRecipeBuilder;
import com.bmaster.createrns.data.pack.YieldBuilder;
import com.simibubi.create.Create;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;
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

    public static final BlockEntry<DepositBlock> IRON_DEPOSIT = DepositStructureBuilder
            .create("iron")
            .transform(bulkDepositStructure(2))
            .block("iron_deposit_block")
            .transform(depositBlockProperties(MapColor.RAW_IRON))
            .recipe(id -> MiningRecipeBuilder.create(id)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            List.of(itemAndTagCandidates(CHANCE_NORMAL,
                                    List.of("iron_nugget"),
                                    List.of("c:nuggets/iron"))),
                            List.of(itemAndTagCandidates(CHANCE_LOW,
                                    List.of("raw_iron"),
                                    List.of("c:raw_materials/iron"))),
                            List.of(itemAndTagCandidates(CHANCE_VERY_LOW,
                                    List.of("raw_iron_block"),
                                    List.of("c:storage_blocks/raw_iron")))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> COPPER_DEPOSIT = DepositStructureBuilder
            .create("copper")
            .transform(bulkDepositStructure(2))
            .block("copper_deposit_block")
            .transform(depositBlockProperties(MapColor.COLOR_ORANGE))
            .recipe(id -> MiningRecipeBuilder.create(id)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            List.of(itemAndTagCandidates(CHANCE_NORMAL,
                                    List.of(Create.ID + ":copper_nugget"),
                                    List.of("c:nuggets/copper"))),
                            List.of(itemAndTagCandidates(CHANCE_LOW,
                                    List.of("raw_copper"),
                                    List.of("c:raw_materials/copper"))),
                            List.of(itemAndTagCandidates(CHANCE_VERY_LOW,
                                    List.of("raw_copper_block"),
                                    List.of("c:storage_blocks/raw_copper")))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> ZINC_DEPOSIT = DepositStructureBuilder
            .create("zinc")
            .transform(preciousDepositStructure(2))
            .block("zinc_deposit_block")
            .transform(depositBlockProperties(MapColor.GLOW_LICHEN))
            .recipe(id -> MiningRecipeBuilder.create(id)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            List.of(),
                            List.of(itemAndTagCandidates(CHANCE_NORMAL,
                                    List.of(Create.ID + ":zinc_nugget"),
                                    List.of("c:nuggets/zinc"))),
                            List.of(itemAndTagCandidates(CHANCE_LOW,
                                    List.of(Create.ID + ":raw_zinc"),
                                    List.of("c:raw_materials/zinc")))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> GOLD_DEPOSIT = DepositStructureBuilder
            .create("gold")
            .transform(preciousDepositStructure(2))
            .block("gold_deposit_block")
            .transform(depositBlockProperties(MapColor.GOLD))
            .recipe(id -> MiningRecipeBuilder.create(id)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            List.of(),
                            List.of(itemAndTagCandidates(CHANCE_NORMAL,
                                    List.of("gold_nugget"),
                                    List.of("c:nuggets/gold"))),
                            List.of(itemAndTagCandidates(CHANCE_LOW,
                                    List.of("raw_gold"),
                                    List.of("c:raw_materials/gold")))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> REDSTONE_DEPOSIT = DepositStructureBuilder
            .create("redstone")
            .transform(preciousDepositStructure(2))
            .block("redstone_deposit_block")
            .transform(depositBlockProperties(MapColor.FIRE))
            .recipe(id -> MiningRecipeBuilder.create(id)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            List.of(),
                            List.of(itemCandidates(CHANCE_NORMAL,
                                    List.of(CreateRNS.ID + ":redstone_small_dust"))),
                            List.of(itemAndTagCandidates(CHANCE_LOW,
                                    List.of("redstone"),
                                    List.of("c:dusts/redstone")))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    //============================================== Compat | Overworld ==============================================//

    public static final BlockEntry<DepositBlock> TIN_DEPOSIT = DepositStructureBuilder
            .create("tin")
            .enableWhenBlockPresent("tin_ore")
            .enableWhenBlockPresent("deepslate_tin_ore")
            .transform(bulkDepositStructure(1))
            .block("tin_deposit_block")
            .transform(depositBlockProperties(MapColor.COLOR_BLUE))
            .recipe(ctx -> MiningRecipeBuilder.create(ctx)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            List.of(tagCandidates(CHANCE_NORMAL,
                                    List.of("c:nuggets/tin"))),
                            List.of(tagCandidates(CHANCE_LOW,
                                    List.of("c:raw_materials/tin"))),
                            List.of(tagCandidates(CHANCE_VERY_LOW,
                                    List.of("c:storage_blocks/raw_tin")))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> LEAD_DEPOSIT = DepositStructureBuilder
            .create("lead")
            .enableWhenBlockPresent("lead_ore")
            .enableWhenBlockPresent("deepslate_lead_ore")
            .transform(semiPreciousDepositStructure(1))
            .block("lead_deposit_block")
            .transform(depositBlockProperties(MapColor.COLOR_BLUE))
            .recipe(ctx -> MiningRecipeBuilder.create(ctx)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            List.of(tagCandidates(CHANCE_NORMAL,
                                    List.of("c:nuggets/lead"))),
                            List.of(tagCandidates(CHANCE_LOW,
                                    List.of("c:raw_materials/lead"))),
                            List.of(tagCandidates(CHANCE_VERY_LOW,
                                    List.of("c:storage_blocks/raw_lead")))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> NICKEL_DEPOSIT = DepositStructureBuilder
            .create("nickel")
            .enableWhenBlockPresent("nickel_ore")
            .enableWhenBlockPresent("deepslate_nickel_ore")
            .transform(semiPreciousDepositStructure(1))
            .block("nickel_deposit_block")
            .transform(depositBlockProperties(MapColor.SAND))
            .recipe(ctx -> MiningRecipeBuilder.create(ctx)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            List.of(),
                            List.of(tagCandidates(CHANCE_NORMAL,
                                    List.of("c:nuggets/nickel"))),
                            List.of(tagCandidates(CHANCE_LOW,
                                    List.of("c:raw_materials/nickel")))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> SILVER_DEPOSIT = DepositStructureBuilder
            .create("silver")
            .enableWhenBlockPresent("silver_ore")
            .enableWhenBlockPresent("deepslate_silver_ore")
            .transform(preciousDepositStructure(1))
            .block("silver_deposit_block")
            .transform(depositBlockProperties(MapColor.SNOW))
            .recipe(ctx -> MiningRecipeBuilder.create(ctx)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            List.of(),
                            List.of(tagCandidates(CHANCE_NORMAL,
                                    List.of("c:nuggets/silver"))),
                            List.of(tagCandidates(CHANCE_LOW,
                                    List.of("c:raw_materials/silver")))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> URANIUM_DEPOSIT = DepositStructureBuilder
            .create("uranium")
            .enableWhenBlockPresent("uranium_ore")
            .enableWhenBlockPresent("deepslate_uranium_ore")
            .transform(preciousDepositStructure(1))
            .block("uranium_deposit_block")
            .transform(depositBlockProperties(MapColor.COLOR_GREEN))
            .recipe(ctx -> MiningRecipeBuilder.create(ctx)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            List.of(),
                            List.of(itemAndTagCandidates(CHANCE_NORMAL,
                                    List.of(Mods.NUCLEAR.ID + ":uranium_powder"),
                                    List.of("c:nuggets/uranium"))),
                            List.of(tagCandidates(CHANCE_LOW,
                                    List.of("c:raw_materials/uranium")))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    public static final BlockEntry<DepositBlock> THORIUM_DEPOSIT = DepositStructureBuilder
            .create("thorium")
            .enableWhenBlockPresent("thorium_ore")
            .enableWhenBlockPresent("deepslate_thorium_ore")
            .transform(preciousDepositStructure(1))
            .block("thorium_deposit_block")
            .transform(depositBlockProperties(MapColor.COLOR_ORANGE))
            .recipe(ctx -> MiningRecipeBuilder.create(ctx)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            List.of(),
                            List.of(),
                            List.of(itemAndTagCandidates(CHANCE_LOW,
                                    List.of(Mods.NEW_AGE.ID + ":thorium"),
                                    List.of("c:raw_materials/thorium")))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();


    //=========================================== Vanilla + Create | Nether ==========================================//

    public static final BlockEntry<DepositBlock> QUARTZ_DEPOSIT = DepositStructureBuilder
            .create("quartz")
            .transform(bulkNetherDepositStructure(1))
            .block("quartz_deposit_block")
            .transform(depositBlockProperties(MapColor.QUARTZ))
            .recipe(id -> MiningRecipeBuilder.create(id)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            List.of(itemCandidates(CHANCE_NORMAL,
                                    List.of("quartz"))),
                            List.of(itemCandidates(CHANCE_NORMAL,
                                    List.of("quartz"))),
                            List.of(tagCandidates(CHANCE_NORMAL,
                                    List.of("c:gems/certus_quartz", "c:gems/quartz")))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    //================================================ Compat | Nether ===============================================//

    public static final @Nullable BlockEntry<DepositBlock> COBALT_DEPOSIT = DepositStructureBuilder
            .create("cobalt")
            .enableWhenBlockPresent("cobalt_ore")
            .transform(preciousNetherDepositStructure(1))
            .block("cobalt_deposit_block")
            .transform(depositBlockProperties(MapColor.LAPIS))
            .recipe(ctx -> MiningRecipeBuilder.create(ctx)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYields(
                            List.of(),
                            List.of(tagCandidates(CHANCE_NORMAL,
                                    List.of("c:nuggets/cobalt"))),
                            List.of(tagCandidates(CHANCE_LOW,
                                    List.of("c:raw_materials/cobalt")))))
                    .transform(sharedResonanceYields())
                    .save())
            .register();

    //================================================================================================================//

    public static final BlockEntry<DepositBlock> DEPLETED_DEPOSIT = DepositStructureBuilder
            .blockOnly("depleted_deposit_block")
            .transform(depositBlockProperties(MapColor.COLOR_BLACK))
            .recipe(id -> MiningRecipeBuilder.create(id)
                    .yield(y -> y.item(List.of("cobblestone")))
                    .save())
            .register();

    public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> depositBlockProperties(MapColor mapColor) {
        return b -> b
                .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
                .properties(p -> p
                        .mapColor(mapColor)
                        .strength(50.0F, 1200f)
                        .noLootTable())
                .transform(pickaxeOnly())
                .tag(BlockTags.NEEDS_DIAMOND_TOOL)
                .tag(RNSTags.RNSBlockTags.DEPOSIT_BLOCKS)
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
                .depth(67)
                .depthDeviation(33)
                .weight(50 * weightMultiplier)
                .nbt(DepositStructureBuilder.DEP_MEDIUM, 70)
                .nbt(DepositStructureBuilder.DEP_LARGE, 30);
    }

    public static UnaryOperator<DepositStructureBuilder> preciousNetherDepositStructure(int weightMultiplier) {
        return b -> b
                .dimension(Dimension.NETHER)
                .depth(67)
                .depthDeviation(33)
                .weight(20 * weightMultiplier)
                .nbt(DepositStructureBuilder.DEP_SMALL, 70)
                .nbt(DepositStructureBuilder.DEP_MEDIUM, 28)
                .nbt(DepositStructureBuilder.DEP_LARGE, 2);
    }

    private static UnaryOperator<MiningRecipeBuilder> addYield(
            List<ItemAndTagCandidates> items, UnaryOperator<YieldBuilder> andThenApply
    ) {
        if (items.isEmpty()) return y -> y;
        return b -> b.yield(y -> {
           var yld = y;
           for (var it : items) {
               yld = yld.itemAndTag(it.items(), it.tags()).chance(it.chance());
           }
           andThenApply.apply(yld);
        });
    }

    private static UnaryOperator<MiningRecipeBuilder> baseYields(
            List<ItemAndTagCandidates> t0s, List<ItemAndTagCandidates> t1s, List<ItemAndTagCandidates> t2s
    ) {
        return b -> b
                .yield(y -> y.item(List.of("cobblestone")))
                .yield(y -> y
                        .item(List.of(CreateRNS.ID + ":resonant_amethyst"))
                        .chance(CHANCE_EXTREMELY_LOW)
                        .catalyst(CATA_OVERCLOCK))
                // T0
                .transform(addYield(t0s, y -> y
                        .compat()
                        .catalyst(CATA_OVERCLOCK)))
                // T1
                .transform(addYield(t1s, y -> y
                        .compat()
                        .catalyst(CATA_FAINT_RESONANCE)
                        .catalyst(CATA_OVERCLOCK)
                        .jeiSlotColor(COLOR_FAINT_RESONANCE)))

                .yield(y -> y
                        .item(List.of(CreateRNS.ID + ":resonant_amethyst"))
                        .chance(CHANCE_EXTREMELY_LOW)
                        .catalyst(CATA_RESONANCE)
                        .catalyst(CATA_OVERCLOCK)
                        .jeiSlotColor(COLOR_RESONANCE))

                // T2
                .transform(addYield(t2s, y -> y
                        .compat()
                        .catalyst(CATA_RESONANCE)
                        .catalyst(CATA_OVERCLOCK)
                        .jeiSlotColor(COLOR_RESONANCE)));
    }

    private static UnaryOperator<MiningRecipeBuilder> baseNetherYields(
            List<ItemAndTagCandidates> t0s, List<ItemAndTagCandidates> t1s, List<ItemAndTagCandidates> t2s
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
                .transform(addYield(t0s, y -> y
                        .compat()
                        .catalyst(CATA_OVERCLOCK)))
                // T1
                .transform(addYield(t1s, y -> y
                        .compat()
                        .catalyst(CATA_FAINT_RESONANCE)
                        .catalyst(CATA_OVERCLOCK)
                        .jeiSlotColor(COLOR_FAINT_RESONANCE)))

                .yield(y -> y
                        .item(List.of(CreateRNS.ID + ":resonant_amethyst"))
                        .chance(CHANCE_EXTREMELY_LOW)
                        .catalyst(CATA_RESONANCE)
                        .catalyst(CATA_OVERCLOCK)
                        .jeiSlotColor(COLOR_RESONANCE))

                // T2
                .transform(addYield(t2s, y -> y
                        .compat()
                        .catalyst(CATA_RESONANCE)
                        .catalyst(CATA_OVERCLOCK)
                        .jeiSlotColor(COLOR_RESONANCE)));
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

    private static ItemAndTagCandidates itemCandidates(float chance, List<String> items) {
        return new ItemAndTagCandidates(items, List.of(), chance);
    }

    private static ItemAndTagCandidates tagCandidates(float chance, List<String> tags) {
        return new ItemAndTagCandidates(List.of(), tags, chance);
    }

    private static ItemAndTagCandidates itemAndTagCandidates(float chance, List<String> items, List<String> tags) {
        return new ItemAndTagCandidates(items, tags, chance);
    }

    private record ItemAndTagCandidates(List<String> items, List<String> tags, float chance) {
        private ItemAndTagCandidates {
            if (items.isEmpty() && tags.isEmpty()) {
                throw new IllegalArgumentException("ItemAndTag must define at least one of item or tag");
            }
        }
    }

    public static void register() {
    }
}
