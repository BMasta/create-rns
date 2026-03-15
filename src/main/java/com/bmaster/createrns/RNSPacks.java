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
    public static int DEFAULT_SEPARATION = 4;
    public static int DEFAULT_SPACING = 24;

    public static Pack MAIN_PACK;
    public static Pack NO_DEPOSIT_PACK;
    public static Pack SPARSE_DEPOSIT_PACK;
    public static Pack STANDARD_DEPOSIT_PACK;
    public static Pack FREQUENT_DEPOSIT_PACK;
    public static Pack VERY_FREQUENT_DEPOSIT_PACK;

    private static final PackSource OPTIONAL = PackSource.create(PackSource.DEFAULT::decorate, false);

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
                .addContent(DynamicDatapackContent.depositStructureSet(DEFAULT_SEPARATION, DEFAULT_SPACING));
    }

    private static DynamicDatapack createNoDepositPack() {
        return DynamicDatapack.createDatapack("no_deposit_worldgen")
                .title(Component.literal("Disable Deposit Generation"))
                .source(OPTIONAL)
                .optional()
                .overwritesLoadedPacks()
                // Generate an alternative version of the deposit biome tag that includes no biomes
                .addContent(DynamicDatapackContent.depositBiomeTag(true));
    }

    private static DynamicDatapack createDepositFrequencyPack(
            String prefix, int separation, int spacing, boolean enableByDefault
    ) {
        var id = prefix + "_deposit_worldgen";
        return DynamicDatapack.createDatapack(id)
                .title(CreateRNS.translatable("datapack." + id + ".title"))
                .description(CreateRNS.translatable("datapack." + id + ".description"))
                .source(enableByDefault ? PackSource.DEFAULT : OPTIONAL)
                .optional()
                .overwritesLoadedPacks()
                // Generate an alternative version of the deposit structure set with specified distribution parameters
                .addContent(DynamicDatapackContent.depositStructureSet(separation, spacing));
    }

    public static void register() {
        MAIN_PACK = createMainPack().buildAndRegister();
        NO_DEPOSIT_PACK = createNoDepositPack().buildAndRegister();
        SPARSE_DEPOSIT_PACK = createDepositFrequencyPack(
                "rare", 8, 48, false).buildAndRegister();
        STANDARD_DEPOSIT_PACK = createDepositFrequencyPack(
                "standard", DEFAULT_SEPARATION, DEFAULT_SPACING, true).buildAndRegister();
        FREQUENT_DEPOSIT_PACK = createDepositFrequencyPack(
                "frequent", 3, 16, false).buildAndRegister();
        VERY_FREQUENT_DEPOSIT_PACK = createDepositFrequencyPack(
                "very_frequent", 2, 12, false).buildAndRegister();
    }

    /// Used to dump pack contents for inspection
    public static void registerSnapshots() {
        createMainPack().registerSnapshots();
        createNoDepositPack().registerSnapshots();
    }
}
