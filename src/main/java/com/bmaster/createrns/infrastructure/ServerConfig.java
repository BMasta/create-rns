package com.bmaster.createrns.infrastructure;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerServerHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import javax.annotation.ParametersAreNonnullByDefault;

@EventBusSubscriber(modid = CreateRNS.ID)
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ------------------------------------------------ Config values ----------------------------------------------- //
    private static final ModConfigSpec.DoubleValue MINING_SPEED_CV = BUILDER
            .comment("""
                     How many mining operations a miner with no attachments can complete in one hour
                     at 256 RPM, with one deposit block claimed, and no deposit multipliers.\
                    """)
            .defineInRange("miningSpeed", 45.0, 0.0, Double.MAX_VALUE);

    private static final ModConfigSpec.IntValue MINING_RADIUS_CV = BUILDER
            .comment("""
                     Radius in which a miner can claim deposit blocks for mining. Radius of 2 results
                     in a 5x5 square mining area with the drill head in the middle.\
                    """)
            .defineInRange("miningRadius", 2, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue MINING_DEPTH_CV = BUILDER
            .comment("""
                     How many blocks deep can a miner claim deposit blocks.\
                    """)
            .defineInRange("miningDepth", 10, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.BooleanValue INFINITE_DEPOSITS_CV = BUILDER
            .define("infiniteDeposits", true);

    private static final ModConfigSpec.IntValue MAX_SCAN_DISTANCE_CV = BUILDER
            .comment("""
                     Maximum scanning distance of the Deposit Scanner in chunks.\
                    """)
            .defineInRange("maxScanDistance", DepositScannerServerHandler.DEFAULT_SEARCH_RADIUS_CHUNKS,
                    0, Integer.MAX_VALUE);

    public static final ModConfigSpec SPEC = BUILDER.build();

    // ------------------------------------------------ Baked values ------------------------------------------------ //
    public static int miningRadius = 0;
    public static int miningDepth = 0;
    public static double miningSpeed = 0;
    public static boolean infiniteDeposits = true;
    public static int maxScanDistance = 0;

    // -------------------------------------------------------------------------------------------------------------- //
    @SubscribeEvent
    static void onLoadReload(ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Unloading) return;
        if (event.getConfig().getSpec() != ServerConfig.SPEC) return;
        miningSpeed = MINING_SPEED_CV.get();
        miningRadius = MINING_RADIUS_CV.get();
        miningDepth = MINING_DEPTH_CV.get();
        infiniteDeposits = INFINITE_DEPOSITS_CV.get();
        maxScanDistance = MAX_SCAN_DISTANCE_CV.get();
    }
}
