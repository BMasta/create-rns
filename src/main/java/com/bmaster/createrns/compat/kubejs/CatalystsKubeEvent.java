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
public class CatalystsKubeEvent implements KubeStartupEvent {
    private final Map<ResourceLocation, CatalystKubeBuilder> createdById = new LinkedHashMap<>();
    private final List<CatalystKubeBuilder> created = new ArrayList<>();

    @Info("""
            Creates a Create: Rock & Stone catalyst definition.
            The id becomes the datapack registry id referenced by mining recipes and hide-if-present rules.
            """)
    public CatalystKubeBuilder create(String catalystId) {
        var id = ResourceLocation.parse(catalystId);
        if (createdById.containsKey(id)) {
            throw new IllegalStateException("Duplicate KubeJS catalyst id: " + catalystId);
        }

        var builder = new CatalystKubeBuilder(id);
        createdById.put(id, builder);
        created.add(builder);
        return builder;
    }

    List<CatalystKubeBuilder> created() {
        return List.copyOf(created);
    }
}
