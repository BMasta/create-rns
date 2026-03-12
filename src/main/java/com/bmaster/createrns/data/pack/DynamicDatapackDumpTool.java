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

        SharedConstants.tryDetectVersion();

        // Registers in-memory pack definitions without requiring full NeoForge runtime bootstrap.
        RNSPacks.registerDefinitionsOnly();

        DynamicDatapack.dumpRegisteredPacks(outputDir);

        System.out.println("Dumped in-memory packs to " + outputDir.toAbsolutePath());
        for (var def : DynamicDatapack.getRegisteredDefinitions()) {
            System.out.println("- " + def.id() + " [" + def.type() + "] " + def.files().size() + " file(s)");
        }
    }
}
