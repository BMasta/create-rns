package com.bmaster.createrns.content.deposit;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositBlock extends Block {
    public static final float DEFAULT_HARDNESS = 50.0F;
    public static final float DEFAULT_RESISTANCE = 1200.0F;

    public static Properties defaultProperties() {
        return Properties.ofFullCopy(Blocks.RAW_IRON_BLOCK)
                .mapColor(MapColor.COLOR_BLACK)
                .strength(DEFAULT_HARDNESS, DEFAULT_RESISTANCE)
                .sound(SoundType.DEEPSLATE)
                .requiresCorrectToolForDrops()
                .noLootTable();
    }

    public DepositBlock(Properties pProperties) {
        super(pProperties);
    }
}
