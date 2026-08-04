package com.bmaster.createrns.compat.kubejs;

import dev.latvian.mods.kubejs.event.KubeStartupEvent;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositStructuresKubeEvent implements KubeStartupEvent {
    private final Map<ResourceLocation, DepositStructureKubeBuilder> createdById = new LinkedHashMap<>();
    private final List<DepositStructureKubeBuilder> created = new ArrayList<>();

    @Info("""
            Creates a Create: Rock & Stone deposit structure definition.
            The id is the final structure id used in generated structure, deposit spec, and structure-set files.
            """)
    public DepositStructureKubeBuilder create(String structureId) {
        var id = ResourceLocation.parse(structureId);
        if (createdById.containsKey(id)) {
            throw new IllegalStateException("Duplicate KubeJS deposit structure id: " + structureId);
        }

        var builder = new DepositStructureKubeBuilder(id);
        createdById.put(id, builder);
        created.add(builder);
        return builder;
    }

    List<DepositStructureKubeBuilder> created() {
        return List.copyOf(created);
    }
}
