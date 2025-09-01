package com.bmaster.createrns.infrastructure;

import com.bmaster.createrns.CreateRNS;
import net.minecraft.SharedConstants;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = CreateRNS.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ------------------------------------------------ Config values ----------------------------------------------- //
    private static final ForgeConfigSpec.ConfigValue<Float> MINER_SPEED_CV = BUILDER
            .comment(" How many mining operations a miner can complete in one hour\n" +
                    " at 256 RPM, with one deposit block claimed, and no deposit multipliers")
            .define("minerSpeed", 100f);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // ------------------------------------------------ Baked values ------------------------------------------------ //
    public static int minerBaseProgress;

    // -------------------------------------------------------------------------------------------------------------- //
    @SubscribeEvent
    static void onLoadReload(final ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Unloading) return;
        if (event.getConfig().getSpec() != ServerConfig.SPEC) return;

        var ticksPerHour = 60 * SharedConstants.TICKS_PER_MINUTE;
        minerBaseProgress = 256 * ticksPerHour / (int) MINER_SPEED_CV.get().floatValue();
    }
}
