package com.bmaster.createrns.infrastructure;

import com.bmaster.createrns.CreateRNS;
import net.minecraft.SharedConstants;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = CreateRNS.MOD_ID)
public class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ------------------------------------------------ Config values ----------------------------------------------- //
    private static final ModConfigSpec.ConfigValue<Float> MINER_MK1_SPEED_CV = BUILDER
            .comment("""
                     How many mining operations a miner mk1 can complete in one hour
                     at 256 RPM, with one deposit block claimed, and no deposit multipliers.
                     Set to 0 to use the value defined in miner spec.\
                    """)
            .define("minerMk1Speed", 45f);

    private static final ModConfigSpec.ConfigValue<Float> MINER_MK2_SPEED_CV = BUILDER
            .comment("""
                     How many mining operations a miner mk2 can complete in one hour
                     at 256 RPM, with one deposit block claimed, and no deposit multipliers
                     Set to 0 to use the value defined in miner spec.\
                    """)
            .define("minerMk2Speed", 45f);

    private static final ModConfigSpec.ConfigValue<Boolean> INFINITE_DEPOSITS_CV = BUILDER
            .define("infiniteDeposits", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    // ------------------------------------------------ Baked values ------------------------------------------------ //
    public static float minerMk1Speed = 0;
    public static float minerMk2Speed = 0;
    public static boolean infiniteDeposits = true;

    // -------------------------------------------------------------------------------------------------------------- //
    @SubscribeEvent
    static void onLoadReload(ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Unloading) return;
        if (event.getConfig().getSpec() != ServerConfig.SPEC) return;
        minerMk1Speed = MINER_MK1_SPEED_CV.get();
        minerMk2Speed = MINER_MK2_SPEED_CV.get();
        infiniteDeposits = INFINITE_DEPOSITS_CV.get();
    }
}
