package com.bmaster.createrns;

import com.bmaster.createrns.data.pack.DynamicDatapack;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSPacks {
    public static Pack MAIN_PACK;
    public static Pack NO_DEPOSIT_PACK;

    private static DynamicDatapack createMainPack() {
        return DynamicDatapack.createDatapack("dynamic_data")
                // Create a new biome tag that includes all biomes in which deposits should spawn
                .addContent(DynamicDatapackContent.depositBiomeTag(false))
                // Create a structure tag for deposits. All deposits must be tagged with it.
                .addContent(DynamicDatapackContent.depositStructureTag())
                // Create deposit worldgen files
                .addContent(DynamicDatapackContent.depositProcessorLists())
                .addContent(DynamicDatapackContent.depositTemplatePools())
                .addContent(DynamicDatapackContent.depositStructures())
                .addContent(DynamicDatapackContent.depositStructureSet());
    }

    private static DynamicDatapack createNoDepositPack() {
        return DynamicDatapack.createDatapack("no_deposit_worldgen")
                .title(Component.literal("Disable Deposit Generation"))
                .source(PackSource.FEATURE)
                .optional()
                .overwritesLoadedPacks()
                // Generate an alternative version of the deposit biome tag that includes no biomes
                .addContent(DynamicDatapackContent.depositBiomeTag(true));
    }

    public static void register() {
        MAIN_PACK = createMainPack().buildAndRegister();
        NO_DEPOSIT_PACK = createNoDepositPack().buildAndRegister();
    }

    /// Used to dump pack contents for inspection
    public static void registerSnapshots() {
        createMainPack().registerSnapshots();
        createNoDepositPack().registerSnapshots();
    }
}
