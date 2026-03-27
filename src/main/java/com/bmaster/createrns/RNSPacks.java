package com.bmaster.createrns;

import com.bmaster.createrns.data.pack.DynamicDatapack;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import com.bmaster.createrns.data.pack.DynamicDatapackContent.Dimension;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackSource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.UnaryOperator;

@SuppressWarnings("unused")
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSPacks {
    public static final int DEFAULT_SEPARATION = 4;
    public static final int DEFAULT_SPACING = 24;
    public static final int DEFAULT_NETHER_SEPARATION = 2;
    public static final int DEFAULT_NETHER_SPACING = 8;

    public static final int SALT = 591646342;
    public static final int NETHER_SALT = 781087034;

    private static final PackSource OPTIONAL = PackSource.create(PackSource.DEFAULT::decorate, false);

    public static DynamicDatapack MAIN_PACK = DynamicDatapack.createDatapack("dynamic_data")
            // Create a new biome tag that includes all biomes in which deposits should spawn
            .addContent(DynamicDatapackContent.depositBiomeTag(Dimension.OVERWORLD, false))
            .addContent(DynamicDatapackContent.depositBiomeTag(Dimension.NETHER, false))
            // Create a structure tag for deposits. All deposits must be tagged with it.
            .addContent(DynamicDatapackContent.depositStructureTag())
            // Create dynamically registered mining recipes.
            .addContent(DynamicDatapackContent.miningRecipes())
            // Create deposit worldgen files
            .addContent(DynamicDatapackContent.depositProcessorLists())
            .addContent(DynamicDatapackContent.depositTemplatePools())
            .addContent(DynamicDatapackContent.depositStructures())
            .addContent(DynamicDatapackContent.depositStructureSet(Dimension.OVERWORLD,
                    DEFAULT_SEPARATION, DEFAULT_SPACING, SALT))
            .addContent(DynamicDatapackContent.depositStructureSet(Dimension.NETHER,
                    DEFAULT_NETHER_SEPARATION, DEFAULT_NETHER_SPACING, NETHER_SALT))
            .register();

    public static DynamicDatapack NO_DEPOSIT_PACK = DynamicDatapack.createDatapack("no_deposit_worldgen")
            .title(Component.literal("Disable Deposit Generation"))
            .source(OPTIONAL)
            .optional()
            .overwritesLoadedPacks()
            // Generate an alternative version of the deposit biome tag that includes no biomes
            .addContent(DynamicDatapackContent.depositBiomeTag(Dimension.OVERWORLD, true))
            .addContent(DynamicDatapackContent.depositBiomeTag(Dimension.NETHER, true))
            .register();

    public static DynamicDatapack RARE_DEPOSIT_PACK = DynamicDatapack.createDatapack("rare_deposit_worldgen")
            .transform(frequencyPackCommon("rare_deposit_worldgen", false))
            // Generate an alternative version of the deposit structure set with specified distribution parameters
            .addContent(DynamicDatapackContent.depositStructureSet(Dimension.OVERWORLD, 8, 48,  SALT))
            .addContent(DynamicDatapackContent.depositStructureSet(Dimension.NETHER, 3, 16, NETHER_SALT))
            .register();

    public static DynamicDatapack STANDARD_DEPOSIT_PACK = DynamicDatapack.createDatapack("standard_deposit_worldgen")
            .transform(frequencyPackCommon("standard_deposit_worldgen", false))
            // Generate an alternative version of the deposit structure set with specified distribution parameters
            .addContent(DynamicDatapackContent.depositStructureSet(Dimension.OVERWORLD, DEFAULT_SEPARATION, DEFAULT_SPACING,  SALT))
            .addContent(DynamicDatapackContent.depositStructureSet(Dimension.NETHER, DEFAULT_NETHER_SEPARATION, DEFAULT_NETHER_SPACING, NETHER_SALT))
            .register();

    public static DynamicDatapack FREQUENT_DEPOSIT_PACK = DynamicDatapack.createDatapack("frequent_deposit_worldgen")
            .transform(frequencyPackCommon("frequent_deposit_worldgen", false))
            // Generate an alternative version of the deposit structure set with specified distribution parameters
            .addContent(DynamicDatapackContent.depositStructureSet(Dimension.OVERWORLD, 3, 16,  SALT))
            .addContent(DynamicDatapackContent.depositStructureSet(Dimension.NETHER, 2, 6, NETHER_SALT))
            .register();

    public static DynamicDatapack VERY_FREQUENT_DEPOSIT_PACK = DynamicDatapack.createDatapack("very_frequent_deposit_worldgen")
            .transform(frequencyPackCommon("very_frequent_deposit_worldgen", false))
            // Generate an alternative version of the deposit structure set with specified distribution parameters
            .addContent(DynamicDatapackContent.depositStructureSet(Dimension.OVERWORLD, 2, 12,  SALT))
            .addContent(DynamicDatapackContent.depositStructureSet(Dimension.NETHER, 1, 4, NETHER_SALT))
            .register();

    private static UnaryOperator<DynamicDatapack> frequencyPackCommon(String id, boolean enableByDefault) {
        return d -> d
                .title(Component.translatable(CreateRNS.ID + ".datapack." + id + ".title"))
                .description(Component.translatable(CreateRNS.ID + ".datapack." + id + ".description"))
                .source(enableByDefault ? PackSource.DEFAULT : OPTIONAL)
                .optional()
                .overwritesLoadedPacks();
    }

    public static void register() {
    }
}
