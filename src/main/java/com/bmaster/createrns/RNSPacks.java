package com.bmaster.createrns;

import com.bmaster.createrns.data.pack.DynamicDatapack;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackSource;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSPacks {
    public static final int DEFAULT_SEPARATION = 4;
    public static final int DEFAULT_SPACING = 24;


    public static final int SALT = 591646342;
    public static final String DEP_SET_NAME = "deposits";

    public static DynamicDatapack MAIN_PACK;
    public static DynamicDatapack NO_DEPOSIT_PACK;
    public static DynamicDatapack SPARSE_DEPOSIT_PACK;
    public static DynamicDatapack STANDARD_DEPOSIT_PACK;
    public static DynamicDatapack FREQUENT_DEPOSIT_PACK;
    public static DynamicDatapack VERY_FREQUENT_DEPOSIT_PACK;

    private static final PackSource OPTIONAL = PackSource.create(PackSource.DEFAULT::decorate, false);

    private static DynamicDatapack createMainPack() {
        return DynamicDatapack.createDatapack("dynamic_data")
                // Create a new biome tag that includes all biomes in which deposits should spawn
                .addContent(DynamicDatapackContent.depositBiomeTag(false))
                // Create a structure tag for deposits. All deposits must be tagged with it.
                .addContent(DynamicDatapackContent.depositStructureTag())
                // Create dynamically registered mining recipes.
                .addContent(DynamicDatapackContent.miningRecipes())
                // Create deposit worldgen files
                .addContent(DynamicDatapackContent.depositProcessorLists())
                .addContent(DynamicDatapackContent.depositTemplatePools())
                .addContent(DynamicDatapackContent.depositStructures())
                .addContent(DynamicDatapackContent.depositStructureSet(DEP_SET_NAME,
                        DEFAULT_SEPARATION, DEFAULT_SPACING, SALT));
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
                .title(Component.translatable(CreateRNS.ID + ".datapack." + id + ".title"))
                .description(Component.translatable(CreateRNS.ID + ".datapack." + id + ".description"))
                .source(enableByDefault ? PackSource.DEFAULT : OPTIONAL)
                .optional()
                .overwritesLoadedPacks()
                // Generate an alternative version of the deposit structure set with specified distribution parameters
                .addContent(DynamicDatapackContent.depositStructureSet(DEP_SET_NAME, separation, spacing, SALT));
    }

    public static void register() {
        MAIN_PACK = createMainPack().register();
        NO_DEPOSIT_PACK = createNoDepositPack().register();
        SPARSE_DEPOSIT_PACK = createDepositFrequencyPack(
                "rare", 8, 48, false).register();
        STANDARD_DEPOSIT_PACK = createDepositFrequencyPack(
                "standard", DEFAULT_SEPARATION, DEFAULT_SPACING, true).register();
        FREQUENT_DEPOSIT_PACK = createDepositFrequencyPack(
                "frequent", 3, 16, false).register();
        VERY_FREQUENT_DEPOSIT_PACK = createDepositFrequencyPack(
                "very_frequent", 2, 12, false).register();
    }

    /// Used to dump pack contents for inspection
    public static void registerSnapshots() {
        createMainPack().registerSnapshots();
        createNoDepositPack().registerSnapshots();
        createDepositFrequencyPack("standard", DEFAULT_SEPARATION, DEFAULT_SPACING, true).registerSnapshots();
        createDepositFrequencyPack("frequent", 3, 16, false).registerSnapshots();
        createDepositFrequencyPack("very_frequent", 2, 12, false).registerSnapshots();
    }
}
