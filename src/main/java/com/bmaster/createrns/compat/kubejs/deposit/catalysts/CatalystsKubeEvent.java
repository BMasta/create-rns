package com.bmaster.createrns.compat.kubejs.deposit.catalysts;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSDeposits;
import com.bmaster.createrns.RNSTags.RNSBlockTags;
import com.bmaster.createrns.compat.kubejs.util.JSMethod;
import com.bmaster.createrns.compat.kubejs.util.StartupJSObject;
import com.bmaster.createrns.compat.kubejs.util.StartupKubeBuilder;
import com.bmaster.createrns.data.deposit.DepositResources;
import com.bmaster.createrns.util.codec.ResourceOrTagLocation;
import dev.latvian.mods.kubejs.event.KubeStartupEvent;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CatalystsKubeEvent extends StartupJSObject implements KubeStartupEvent {
    private final Map<ResourceLocation, CatalystKubeBuilder> createdById = new LinkedHashMap<>();
    private final List<CatalystKubeBuilder> created = new ArrayList<>();

    public CatalystsKubeEvent() {
        super("rnsCatalysts", "create");
    }

    @Info("""
            Creates a new catalyst (like overclocked or resonance).
            """)
    @JSMethod
    public CatalystKubeBuilder create(String catalystId) {
        var id = ResourceLocation.parse(catalystId);
        if (createdById.containsKey(id)) {
            throw new IllegalStateException("Duplicate KubeJS catalyst id: " + catalystId);
        }

        var builder = new CatalystKubeBuilder(id, this::isUnknownCatalyst);
        createdById.put(id, builder);
        created.add(builder);
        return builder;
    }

    public List<CatalystKubeBuilder> created() {
        return List.copyOf(created);
    }

    public void assertValid() {
        created.forEach(StartupKubeBuilder::isValid);
    }

    public void gatherResources(DepositResources resources) {
        var attachmentBlocks = new LinkedHashSet<ResourceLocation>();
        for (var catalyst : created) {
            if (!catalyst.isValid()) continue;
            catalyst.gatherResources(resources);
            attachmentBlocks.addAll(catalyst.attachmentBlocks());
        }
        if (attachmentBlocks.isEmpty()) return;

        var entries = attachmentBlocks.stream().sorted(java.util.Comparator.comparing(ResourceLocation::toString))
                .map(id -> new ResourceOrTagLocation(id, false)).toList();
        resources.blockTags.add(new DepositResources.BlockTag(RNSBlockTags.MINER_ATTACHMENTS.location(), entries, false));
    }

    private boolean isUnknownCatalyst(ResourceLocation id) {
        if (RNSDeposits.CATALYSTS.contains(id) || createdById.containsKey(id)) return false;

        return id.getNamespace().equals(CreateRNS.ID) || createdById.keySet().stream()
                .anyMatch(createdId -> createdId.getNamespace().equals(id.getNamespace()));
    }
}
