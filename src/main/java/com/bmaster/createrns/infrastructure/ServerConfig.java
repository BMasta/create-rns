package com.bmaster.createrns.infrastructure;

import com.bmaster.createrns.content.deposit.scanning.DepositScannerServerHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.common.ModConfigSpec;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MINING_SPEED = BUILDER
            .comment("""
                     How many mining operations a miner with no attachments can complete in one hour,
                     at 256 RPM, with one deposit block claimed.\
                    """)
            .defineInRange("miningSpeed", 45, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MINING_RADIUS = BUILDER
            .comment("""
                     Radius in which a miner can claim deposit blocks for mining. Radius of 2 results
                     in a 5x5 square mining area with the Mine Head in the middle.
                     Warning: active miners have to be reassembled to apply this setting.\
                    """)
            .defineInRange("miningRadius", 2, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MINING_DEPTH = BUILDER
            .comment("""
                     How many blocks deep can a miner claim deposit blocks.
                     Warning: active miners have to be reassembled to apply this setting.\
                    """)
            .defineInRange("miningDepth", 10, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue INFINITE_DEPOSITS = BUILDER
            .comment("""
                     Whether deposits never run out of resources.\
                    """)
            .define("infiniteDeposits", true);

    public static final ModConfigSpec.BooleanValue MOVABLE_DEPOSITS = BUILDER
            .comment("""
                     Whether deposits can be attached to a contraption or moved by a piston.\
                    """)
            .define("movableDeposits", false);

    public static final ModConfigSpec.IntValue MAX_SCAN_DISTANCE = BUILDER
            .comment("""
                     Maximum scanning distance of the Deposit Scanner in chunks.\
                    """)
            .defineInRange("maxScanDistance", DepositScannerServerHandler.DEFAULT_SEARCH_RADIUS_CHUNKS,
                    0, Integer.MAX_VALUE);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
