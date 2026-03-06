package com.bmaster.createrns.infrastructure;

import com.bmaster.createrns.CreateRNS;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = CreateRNS.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ------------------------------------------------ Config values ----------------------------------------------- //
    private static final ForgeConfigSpec.ConfigValue<Float> MINING_SPEED_CV = BUILDER
            .comment("""
                     How many mining operations a miner with no attachments can complete in one hour
                     at 256 RPM, with one deposit block claimed, and no deposit multipliers.\
                    """)
            .define("miningSpeed", 45f);

    private static final ForgeConfigSpec.ConfigValue<Integer> MINING_RADIUS_CV = BUILDER
            .comment("""
                     Radius in which a miner can claim deposit blocks for mining. Radius of 2 results
                     in a 5x5 square mining area with the drill head in the middle.\
                    """)
            .define("miningRadius", 2);

    private static final ForgeConfigSpec.ConfigValue<Integer> MINING_DEPTH_CV = BUILDER
            .comment("""
                     How many blocks deep can a miner claim deposit blocks.\
                    """)
            .define("miningDepth", 10);

    private static final ForgeConfigSpec.ConfigValue<Boolean> INFINITE_DEPOSITS_CV = BUILDER
            .define("infiniteDeposits", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // ------------------------------------------------ Baked values ------------------------------------------------ //
    public static int miningRadius = 0;
    public static int miningDepth = 0;
    public static float miningSpeed = 0;
    public static boolean infiniteDeposits = true;

    // -------------------------------------------------------------------------------------------------------------- //
    @SubscribeEvent
    static void onLoadReload(final ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Unloading) return;
        if (event.getConfig().getSpec() != ServerConfig.SPEC) return;
        miningSpeed = MINING_SPEED_CV.get();
        miningRadius = MINING_RADIUS_CV.get();
        miningDepth = MINING_DEPTH_CV.get();
        infiniteDeposits = INFINITE_DEPOSITS_CV.get();
    }
}
