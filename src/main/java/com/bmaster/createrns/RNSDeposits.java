package com.bmaster.createrns;

import com.bmaster.createrns.content.deposit.DepositBlock;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipeBuilder;
import com.bmaster.createrns.content.deposit.mining.recipe.YieldBuilder;
import com.bmaster.createrns.data.pack.DynamicDatapackDepositEntry;
import com.simibubi.create.Create;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

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

    public static final String ID_CREATE_NUCLEAR = "createnuclear";

    public static final BlockEntry<DepositBlock> IRON_DEPOSIT = DynamicDatapackDepositEntry
            .create("iron")
            .depth(8)
            .weight(10)
            .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 70)
            .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 30)
            .block("iron_deposit_block")
            .transform(depositBlock(MapColor.RAW_IRON))
            .recipe(id -> MiningRecipeBuilder.create(id)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYieldsCommon("iron_nugget", "raw_iron", "raw_iron_block"))
                    .transform(sharedResonanceYields())
                    .save())
            .registerOrThrow();

    public static final BlockEntry<DepositBlock> COPPER_DEPOSIT = DynamicDatapackDepositEntry
            .create("copper")
            .depth(8)
            .weight(5)
            .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 70)
            .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 30)
            .block("copper_deposit_block")
            .transform(depositBlock(MapColor.COLOR_ORANGE))
            .recipe(id -> MiningRecipeBuilder.create(id)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYieldsCommon(Create.ID + ":copper_nugget", "raw_copper", "raw_copper_block"))
                    .transform(sharedResonanceYields())
                    .save())
            .registerOrThrow();

    public static final BlockEntry<DepositBlock> ZINC_DEPOSIT = DynamicDatapackDepositEntry
            .create("zinc")
            .depth(8)
            .weight(2)
            .nbt(DynamicDatapackDepositEntry.DEP_SMALL, 70)
            .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 28)
            .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 2)
            .block("zinc_deposit_block")
            .transform(depositBlock(MapColor.GLOW_LICHEN))
            .recipe(id -> MiningRecipeBuilder.create(id)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYieldsRare(Create.ID + ":zinc_nugget", Create.ID + ":raw_zinc",
                            Create.ID + ":raw_zinc_block"))
                    .transform(sharedResonanceYields())
                    .save())
            .registerOrThrow();

    public static final BlockEntry<DepositBlock> GOLD_DEPOSIT = DynamicDatapackDepositEntry
            .create("gold")
            .depth(12)
            .weight(2)
            .nbt(DynamicDatapackDepositEntry.DEP_SMALL, 70)
            .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 28)
            .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 2)
            .block("gold_deposit_block")
            .transform(depositBlock(MapColor.GOLD))
            .recipe(id -> MiningRecipeBuilder.create(id)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYieldsRare("gold_nugget", "raw_gold", "raw_gold_block"))
                    .transform(sharedResonanceYields())
                    .save())
            .registerOrThrow();

    public static final BlockEntry<DepositBlock> REDSTONE_DEPOSIT = DynamicDatapackDepositEntry
            .create("redstone")
            .depth(12)
            .weight(2)
            .nbt(DynamicDatapackDepositEntry.DEP_SMALL, 70)
            .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 28)
            .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 2)
            .block("redstone_deposit_block")
            .transform(depositBlock(MapColor.FIRE))
            .recipe(id -> MiningRecipeBuilder.create(id)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYieldsRare(
                            CreateRNS.ID + ":redstone_small_dust",
                            "redstone",
                            "redstone_block"))
                    .transform(sharedResonanceYields())
                    .save())
            .registerOrThrow();

    public static final BlockEntry<DepositBlock> DEPLETED_DEPOSIT = DynamicDatapackDepositEntry
            .blockOnly("depleted_deposit_block")
            .transform(depositBlock(MapColor.COLOR_BLACK))
            .recipe(id -> MiningRecipeBuilder.create(id)
                    .yield(y -> y.item("cobblestone"))
                    .save())
            .registerOrThrow();

    public static final @Nullable BlockEntry<DepositBlock> URANIUM_DEPOSIT = DynamicDatapackDepositEntry
            .create("uranium")
            .depth(12)
            .weight(2)
            .nbt(DynamicDatapackDepositEntry.DEP_SMALL, 70)
            .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 28)
            .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 2)
            .requireMod(ID_CREATE_NUCLEAR)
            .block("uranium_deposit_block")
            .transform(depositBlock(MapColor.COLOR_GREEN))
            .recipe(ctx -> MiningRecipeBuilder.create(ctx)
                    .replaceWhenDepleted(CreateRNS.ID + ":depleted_deposit_block")
                    .durability(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_SPREAD)
                    .transform(baseYieldsRare(
                            ID_CREATE_NUCLEAR + ":uranium_powder",
                            ID_CREATE_NUCLEAR + ":raw_uranium",
                            ID_CREATE_NUCLEAR + ":raw_uranium_block"))
                    .transform(sharedResonanceYields())
                    .save())
            .registerOrNull();

    public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> depositBlock(MapColor mapColor) {
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

    private static Consumer<YieldBuilder> faintShatteringResonanceYield() {
        return y -> y
                .item("tuff")
                .item("calcite")
                .item(Create.ID + ":limestone")
                .catalyst(CATA_FAINT_SHATTERING_RESONANCE)
                .jeiSlotColor(COLOR_FAINT_SHATTERING_RESONANCE);
    }

    private static Consumer<YieldBuilder> shatteringResonanceYield() {
        return y -> y
                .chance(CHANCE_SHATTERING_RESONANCE)
                .item(Create.ID + ":crimsite")
                .item(Create.ID + ":veridium")
                .item(Create.ID + ":asurine")
                .item(Create.ID + ":ochrum")
                .catalyst(CATA_SHATTERING_RESONANCE)
                .catalyst(CATA_OVERCLOCK)
                .jeiSlotColor(COLOR_SHATTERING_RESONANCE);
    }

    private static Consumer<YieldBuilder> faintStabilizingResonanceYield() {
        return y -> y
                .chance(CHANCE_FAINT_STABILIZING_RESONANCE)
                .item("lapis_lazuli")
                .item("amethyst_shard")
                .item("emerald")
                .catalyst(CATA_FAINT_STABILIZING_RESONANCE)
                .catalyst(CATA_OVERCLOCK)
                .jeiSlotColor(COLOR_FAINT_STABILIZING_RESONANCE);
    }

    private static Consumer<YieldBuilder> stabilizingResonanceYield() {
        return y -> y
                .chance(CHANCE_STABILIZING_RESONANCE)
                .item("redstone", 5)
                .item("diamond")
                .catalyst(CATA_STABILIZING_RESONANCE)
                .catalyst(CATA_OVERCLOCK)
                .jeiSlotColor(COLOR_STABILIZING_RESONANCE);
    }

    private static UnaryOperator<MiningRecipeBuilder> baseYieldsCommon(String nugget, String rawOre, String rawOreBlock) {
        return b -> b
                .yield(y -> y.item("cobblestone"))
                .yield(y -> y
                        .chance(CHANCE_EXTREMELY_LOW)
                        .item(CreateRNS.ID + ":resonant_amethyst")
                        .catalyst(CATA_OVERCLOCK))
                .yield(y -> y
                        .chance(CHANCE_NORMAL)
                        .item(nugget)
                        .catalyst(CATA_OVERCLOCK))
                .yield(y -> y
                        .chance(CHANCE_LOW)
                        .item(rawOre)
                        .catalyst(CATA_FAINT_RESONANCE)
                        .catalyst(CATA_OVERCLOCK)
                        .jeiSlotColor(COLOR_FAINT_RESONANCE))
                .yield(y -> y
                        .chance(CHANCE_VERY_LOW)
                        .item(rawOreBlock)
                        .catalyst(CATA_RESONANCE)
                        .catalyst(CATA_OVERCLOCK)
                        .jeiSlotColor(COLOR_RESONANCE));
    }

    private static UnaryOperator<MiningRecipeBuilder> baseYieldsRare(String nugget, String rawOre, String rawOreBlock) {
        return b -> b
                .yield(y -> y.item("cobblestone"))
                .yield(y -> y
                        .chance(CHANCE_EXTREMELY_LOW)
                        .item(CreateRNS.ID + ":resonant_amethyst")
                        .catalyst(CATA_OVERCLOCK))
                .yield(y -> y
                        .chance(CHANCE_NORMAL)
                        .item(nugget)
                        .catalyst(CATA_FAINT_RESONANCE)
                        .catalyst(CATA_OVERCLOCK)
                        .jeiSlotColor(COLOR_FAINT_RESONANCE))
                .yield(y -> y
                        .chance(CHANCE_LOW)
                        .item(rawOre)
                        .catalyst(CATA_RESONANCE)
                        .catalyst(CATA_OVERCLOCK)
                        .jeiSlotColor(COLOR_RESONANCE))
                .yield(y -> y
                        .chance(CHANCE_VERY_LOW)
                        .item(rawOreBlock)
                        .catalyst(CATA_RESONANCE)
                        .catalyst(CATA_OVERCLOCK)
                        .jeiSlotColor(COLOR_RESONANCE))
                .yield(y -> y
                        .chance(CHANCE_EXTREMELY_LOW)
                        .item(CreateRNS.ID + ":resonant_amethyst")
                        .catalyst(CATA_RESONANCE)
                        .catalyst(CATA_OVERCLOCK)
                        .jeiSlotColor(COLOR_RESONANCE));
    }

    private static UnaryOperator<MiningRecipeBuilder> sharedResonanceYields() {
        return b -> b
                .yield(faintShatteringResonanceYield())
                .yield(shatteringResonanceYield())
                .yield(faintStabilizingResonanceYield())
                .yield(stabilizingResonanceYield());
    }

    public static void register() {
    }
}
