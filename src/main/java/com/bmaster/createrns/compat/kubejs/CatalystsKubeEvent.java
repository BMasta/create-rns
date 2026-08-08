package com.bmaster.createrns.compat.kubejs;

import dev.latvian.mods.kubejs.event.KubeStartupEvent;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.HideFromJS;
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
    public CatalystKubeBuilder create(Context cx, String catalystId) {
        try {
            return create(catalystId, KubeJSStartupError.builderStart(cx, "rnsCatalysts", "create"));
        } catch (RuntimeException cause) {
            throw KubeJSStartupError.exception(cx, "rnsCatalysts", "create", "create", cause);
        }
    }

    @HideFromJS
    public CatalystKubeBuilder create(String catalystId) {
        return create(catalystId, dev.latvian.mods.kubejs.script.SourceLine.UNKNOWN);
    }

    private CatalystKubeBuilder create(
            String catalystId, dev.latvian.mods.kubejs.script.SourceLine sourceLine
    ) {
        var id = ResourceLocation.parse(catalystId);
        if (createdById.containsKey(id)) {
            throw new IllegalStateException("Duplicate KubeJS catalyst id: " + catalystId);
        }

        var builder = new CatalystKubeBuilder(id, sourceLine);
        createdById.put(id, builder);
        created.add(builder);
        return builder;
    }

    List<CatalystKubeBuilder> created() {
        return created.stream().filter(builder -> !builder.isInvalid()).toList();
    }
}
