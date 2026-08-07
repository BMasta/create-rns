package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.data.pack.DepositSpecBuilder;
import com.bmaster.createrns.data.pack.DepositStructureBuilder;
import dev.latvian.mods.kubejs.event.StartupEventJS;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositStructuresKubeEvent extends StartupEventJS {
    private final Map<ResourceLocation, DepositStructureKubeBuilder> createdById = new LinkedHashMap<>();
    private final Map<ResourceLocation, DepositStructureKubeBuilder> tweakedById = new LinkedHashMap<>();
    private final List<DepositStructureKubeBuilder> created = new ArrayList<>();
    private final List<DepositStructureKubeBuilder> tweaked = new ArrayList<>();

    @Info("""
            Creates a Create: Rock & Stone deposit structure definition.
            The id is the final structure id used in generated structure, deposit spec, and structure-set files.
            """)
    public DepositStructureKubeBuilder create(String structureId) {
        var id = ResourceLocation.parse(structureId);
        if (findBuiltInStructure(id) != null) {
            throw new IllegalArgumentException(
                    "Built-in deposit structure " + id + " must be modified with tweak(...)");
        }
        if (createdById.containsKey(id)) {
            throw new IllegalStateException("Duplicate KubeJS deposit structure id: " + structureId);
        }

        var builder = new DepositStructureKubeBuilder(id);
        createdById.put(id, builder);
        created.add(builder);
        return builder;
    }

    @Info("""
            Tweaks an existing built-in Create: Rock & Stone deposit structure.
            Single-value methods replace the built-in value. The first scannerIcon, mapIcon, or nbt call clears the
            corresponding built-in list, and subsequent calls append replacement fallback entries in order.
            """)
    public DepositStructureKubeBuilder tweak(String structureId) {
        var id = ResourceLocation.parse(structureId);
        var structure = findBuiltInStructure(id);
        if (structure == null) {
            throw new IllegalArgumentException("Unknown built-in deposit structure: " + id);
        }
        if (tweakedById.containsKey(id)) {
            throw new IllegalStateException("Built-in deposit structure already tweaked: " + id);
        }

        var spec = DepositSpecBuilder.getSpecs().stream()
                .filter(entry -> entry.spec().structureId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing deposit spec for built-in structure " + id));
        var builder = new DepositStructureKubeBuilder(structure, spec);
        tweakedById.put(id, builder);
        tweaked.add(builder);
        return builder;
    }

    List<DepositStructureKubeBuilder> created() {
        return List.copyOf(created);
    }

    List<DepositStructureKubeBuilder> tweaked() {
        return List.copyOf(tweaked);
    }

    private static @Nullable DepositStructureBuilder.ConfiguredEntry findBuiltInStructure(
            ResourceLocation structureId
    ) {
        return DepositStructureBuilder.getDeposits().stream()
                .filter(entry -> DepositStructureBuilder.structureId(entry).equals(structureId))
                .findFirst()
                .orElse(null);
    }
}
