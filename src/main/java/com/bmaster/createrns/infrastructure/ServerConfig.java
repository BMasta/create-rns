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
            .comment(" How many mining operations a miner mk1 can complete in one hour\n" +
                    " at 256 RPM, with one deposit block claimed, and no deposit multipliers")
            .define("minerSpeed", 45f);

    private static final ModConfigSpec.ConfigValue<Float> MINER_MK2_SPEED_CV = BUILDER
            .comment(" How many mining operations a miner mk2 can complete in one hour\n" +
                    " at 256 RPM, with one deposit block claimed, and no deposit multipliers")
            .define("minerSpeed", 45f);

    public static final ModConfigSpec SPEC = BUILDER.build();

    // ------------------------------------------------ Baked values ------------------------------------------------ //
    public static int minerMk1BaseProgress;
    public static int minerMk2BaseProgress;

    // -------------------------------------------------------------------------------------------------------------- //
    @SubscribeEvent
    static void onLoadReload(final ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Unloading) return;
        if (event.getConfig().getSpec() != ServerConfig.SPEC) return;

        var ticksPerHour = 60 * SharedConstants.TICKS_PER_MINUTE;
        minerMk1BaseProgress = 256 * ticksPerHour / (int) MINER_MK1_SPEED_CV.get().floatValue();
        minerMk2BaseProgress = 256 * ticksPerHour / (int) MINER_MK2_SPEED_CV.get().floatValue();
    }
}
