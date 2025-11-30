package com.bmaster.createrns.util;

import com.bmaster.createrns.content.deposit.mining.multiblock.attachment.MiningEquipmentBlock;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
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

    public static <I extends BlockItem> ItemModelBuilder namedItemModel(
            DataGenContext<Item, I> ctx,
            RegistrateItemModelProvider prov,
            String name
    ) {
        return prov.blockItem(() -> ctx.getEntry().getBlock(), "/" + name);
    }

    /// Serialized name of the property value of the blockstate will be used as a model name.
    public static Function<BlockState, ModelFile> stateControlledPartialBaseModel(
            DataGenContext<?, ?> ctx,
            RegistrateBlockstateProvider prov,
            EnumProperty<?> property
    ) {
        return bs -> {
            var val = bs.getValue(property);
            String location = "block/" + ctx.getName() + "/" + val.getSerializedName();
            return prov.models().getExistingFile(prov.modLoc(location));
        };
    }
}
