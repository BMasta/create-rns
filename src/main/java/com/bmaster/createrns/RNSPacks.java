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

    private static boolean definitionsRegistered;
    private static boolean packsRegistered;

    public static void register() {
        if (packsRegistered) return;
        boolean addDefinitions = !definitionsRegistered;

        MAIN_PACK = createMainPack().buildAndRegister(addDefinitions);
        NO_DEPOSIT_PACK = createNoDepositPack().buildAndRegister(addDefinitions);

        definitionsRegistered = true;
        packsRegistered = true;
    }

    public static void registerDefinitionsOnly() {
        if (definitionsRegistered) return;

        createMainPack().registerDefinitionOnly();
        createNoDepositPack().registerDefinitionOnly();

        definitionsRegistered = true;
    }

    private static DynamicDatapack createMainPack() {
        return DynamicDatapack.createDatapack("dynamic_data")
                .title(Component.literal("Dynamic mod data for Create: Rock & Stone"))
                .addContent(DynamicDatapackContent.standardDepositBiomeTag());
    }

    private static DynamicDatapack createNoDepositPack() {
        return DynamicDatapack.createDatapack("no_deposit_worldgen")
                .title(Component.literal("Disable deposit generation"))
                .source(PackSource.FEATURE)
                .optional()
                .overwritesLoadedPacks()
                .addContent(DynamicDatapackContent.emptyDepositBiomeTag());
    }
}
