package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.RNSDeposits;
import com.bmaster.createrns.RNSPacks;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.SharedConstants;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DynamicDatapackDumpTool {
    private static final String DEFAULT_DUMP_PATH = "src/generated/builtin_packs";
    private static final String DEFAULT_VARIANT_PATH = "default";
    private static final String WITH_COMPAT_VARIANT_PATH = "with_compat";
    public static @Nullable List<String> ENABLED_MODS = null;

    public static void main(String[] args) throws IOException {
        var outputRoot = (args.length > 0)
                ? Path.of(args[0])
                : Path.of(DEFAULT_DUMP_PATH);

        DynamicDatapackDepositEntry.dumpMode = true;
        try {
            RNSDeposits.register();
            // This tool runs outside normal game bootstrap. Initialize SharedConstants so pack_format can be resolved.
            SharedConstants.tryDetectVersion();

            dumpVariant(outputRoot.resolve(DEFAULT_VARIANT_PATH), List.of());
            dumpVariant(outputRoot.resolve(WITH_COMPAT_VARIANT_PATH), null);
        } finally {
            DynamicDatapackDepositEntry.dumpMode = false;
        }
    }

    public static @Nullable List<String> getEnabledMods() {
        return ENABLED_MODS;
    }

    private static void dumpVariant(Path outputDir, @Nullable List<String> enabledMods) throws IOException {
        ENABLED_MODS = enabledMods;
        DynamicDatapack.clearPackSnapshots();

        // Registers in-memory pack definitions without requiring full NeoForge runtime bootstrap
        RNSPacks.registerSnapshots();
        DynamicDatapack.dumpRegisteredPacks(outputDir);

        System.out.println("Dumped in-memory packs to " + outputDir.toAbsolutePath());
        for (var def : DynamicDatapack.getPackSnapshots()) {
            System.out.println("- " + def.id() + " [" + def.type() + "] " + def.files().size() + " file(s)");
        }
    }
}
