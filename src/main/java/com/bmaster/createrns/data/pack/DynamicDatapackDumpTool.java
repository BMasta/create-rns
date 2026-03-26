package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.RNSDeposits;
import com.bmaster.createrns.RNSPacks;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.SharedConstants;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.file.Path;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DynamicDatapackDumpTool {
    private static final String DEFAULT_DUMP_PATH = "src/generated/builtin_packs";
    private static final String DEFAULT_VARIANT_PATH = "default";
    private static final String WITH_COMPAT_VARIANT_PATH = "with_compat";
    private static boolean includeCompat = false;

    public static void main(String[] args) throws IOException {
        var outputRoot = (args.length > 0)
                ? Path.of(args[0])
                : Path.of(DEFAULT_DUMP_PATH);

        DepositStructureBuilder.dumpMode = true;
        try {
            // This tool runs outside normal game bootstrap. Initialize SharedConstants so pack_format can be resolved.
            SharedConstants.tryDetectVersion();
            RNSDeposits.register();

            includeCompat = false;
            dumpVariant(outputRoot.resolve(DEFAULT_VARIANT_PATH));
            includeCompat = true;
            dumpVariant(outputRoot.resolve(WITH_COMPAT_VARIANT_PATH));
        } finally {
            DepositStructureBuilder.dumpMode = false;
        }
    }

    public static boolean includeCompat() {
        return includeCompat;
    }

    private static void dumpVariant(Path outputDir) throws IOException {
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
