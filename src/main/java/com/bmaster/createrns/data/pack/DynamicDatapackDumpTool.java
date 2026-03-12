package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.RNSPacks;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.SharedConstants;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.file.Path;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DynamicDatapackDumpTool {
    private static final String DEFAULT_DUMP_PATH = "build/generated/dynamic_datapacks";

    public static void main(String[] args) throws IOException {
        var outputDir = (args.length > 0)
                ? Path.of(args[0])
                : Path.of(DEFAULT_DUMP_PATH);

        registerDumpDefaults();
        // This tool runs outside normal game bootstrap; initialize SharedConstants so pack_format can be resolved.
        SharedConstants.tryDetectVersion();

        // Registers in-memory pack definitions without requiring full NeoForge runtime bootstrap.
        RNSPacks.registerSnapshots();

        DynamicDatapack.dumpRegisteredPacks(outputDir);

        System.out.println("Dumped in-memory packs to " + outputDir.toAbsolutePath());
        for (var def : DynamicDatapack.getPackSnapshots()) {
            System.out.println("- " + def.id() + " [" + def.type() + "] " + def.files().size() + " file(s)");
        }
    }

    /// The actual definitions are registered in RNSBlocks, but calling RNSBlocks.register() outside the main
    /// bootstrap sequence leads to errors. This function mimics those definitions.
    private static void registerDumpDefaults() {
        DynamicDatapackDepositEntry
                .create("iron")
                .depth(8)
                .weight(10)
                .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 70)
                .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 30)
                .block("iron_deposit_block");

        DynamicDatapackDepositEntry
                .create("copper")
                .depth(8)
                .weight(5)
                .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 70)
                .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 30)
                .block("copper_deposit_block");

        DynamicDatapackDepositEntry
                .create("zinc")
                .depth(8)
                .weight(2)
                .nbt(DynamicDatapackDepositEntry.DEP_SMALL, 70)
                .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 28)
                .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 2)
                .block("zinc_deposit_block");

        DynamicDatapackDepositEntry
                .create("gold")
                .depth(12)
                .weight(2)
                .nbt(DynamicDatapackDepositEntry.DEP_SMALL, 70)
                .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 28)
                .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 2)
                .block("gold_deposit_block");

        DynamicDatapackDepositEntry
                .create("redstone")
                .depth(12)
                .weight(2)
                .nbt(DynamicDatapackDepositEntry.DEP_SMALL, 70)
                .nbt(DynamicDatapackDepositEntry.DEP_MEDIUM, 28)
                .nbt(DynamicDatapackDepositEntry.DEP_LARGE, 2)
                .block("redstone_deposit_block");
    }
}
