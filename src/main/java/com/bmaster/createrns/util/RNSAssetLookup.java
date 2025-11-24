package com.bmaster.createrns.util;

import com.bmaster.createrns.content.deposit.mining.multiblock.equipment.MiningEquipmentBlock;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import java.util.function.Function;

public class RNSAssetLookup {
    public static Function<BlockState, ModelFile> faceAttachedHalfRotatedModel(DataGenContext<?, ?> ctx, RegistrateBlockstateProvider prov) {
        return bs -> {
            String suffix = "";
            if (bs.getValue(FaceAttachedHorizontalDirectionalBlock.FACE) == AttachFace.WALL) {
                suffix = switch (bs.getValue(MiningEquipmentBlock.ROTATION)) {
                    case NONE, CLOCKWISE_180 -> "_rotated";
                    case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> "";
                };
            }
            return prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + suffix));
        };
    }
}
