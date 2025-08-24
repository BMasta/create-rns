package com.bmaster.createrns.capability.depositindex;

import com.bmaster.createrns.AllContent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

public interface IDepositIndex {
    @Nullable BlockPos getNearest(ResourceKey<Structure> depositKey, ServerPlayer sp, int searchRadiusChunks,
                                  boolean knownOnly);

    void add(ResourceKey<Structure> depositKey, StructureStart ss, ServerLevel sl);
    void remove(ResourceKey<Structure> depositKey, BlockPos centerPos);

    static LazyOptional<IDepositIndex> fromLevel(ServerLevel level) {
        return level.getCapability(AllContent.DEPOSIT_INDEX);
    }
}
