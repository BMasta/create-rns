package com.bmaster.createrns;

import com.bmaster.createrns.data.pack.DynamicDatapack;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

public class RNSPacks {
    // Dynamic packs
    public static Pack MAIN_PACK = DynamicDatapack.createDatapack("dynamic_data")
            .title(Component.literal("Dynamic mod data for Create: Rock & Stone"))
            .addContent(DynamicDatapackContent.standardDepositBiomeTag())
            .buildAndRegister();
    public static Pack NO_DEPOSIT_PACK = DynamicDatapack.createDatapack("no_deposit_worldgen")
            .title(Component.literal("Disable deposit generation"))
            .source(PackSource.FEATURE)
            .optional()
            .overwritesLoadedPacks()
            .addContent(DynamicDatapackContent.emptyDepositBiomeTag())
            .buildAndRegister();

    public static void register() {
    }
}
