package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance.ResonanceCatalystRequirement;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance.ShatteringResonanceCatalystRequirement;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance.StabilizingResonanceCatalystRequirement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CatalystRequirementSet {
    public static final Codec<CatalystRequirementSet> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name")
                    .forGetter(crs -> crs.name),
            Codec.floatRange(0f, Float.MAX_VALUE).fieldOf("chance_multiplier")
                    .orElse(1f)
                    .forGetter(crs -> crs.chanceMult),
            Codec.BOOL.fieldOf("optional")
                    .orElse(false)
                    .forGetter(crs -> crs.optional),
            Codec.INT.fieldOf("display_priority")
                    .orElse(Integer.MAX_VALUE)
                    .forGetter(crs -> crs.displayPriority),
            Codec.STRING.listOf().fieldOf("hide_if_present")
                    .orElse(List.of())
                    .forGetter(crs -> crs.hideIfPresent),
            FluidCatalystRequirement.CODEC.optionalFieldOf("fluid")
                    .forGetter(crs -> crs.fluidCR),
            ResonanceCatalystRequirement.CODEC.optionalFieldOf("resonance")
                    .forGetter(crs -> crs.resCR),
            ShatteringResonanceCatalystRequirement.CODEC.optionalFieldOf("shattering_resonance")
                    .forGetter(crs -> crs.shResCR),
            StabilizingResonanceCatalystRequirement.CODEC.optionalFieldOf("stabilizing_resonance")
                    .forGetter(crs -> crs.stResCR)
    ).apply(i, CatalystRequirementSet::new));

    public static final ResourceKey<Registry<CatalystRequirementSet>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(CreateRNS.asResource("catalyst"));

    public final String name;
    public final float chanceMult;
    public final boolean optional;
    public final int displayPriority;
    public List<String> hideIfPresent;
    public final List<CatalystRequirement> requirements = new ArrayList<>();
    protected final Optional<FluidCatalystRequirement> fluidCR;
    protected final Optional<ResonanceCatalystRequirement> resCR;
    protected final Optional<ShatteringResonanceCatalystRequirement> shResCR;
    protected final Optional<StabilizingResonanceCatalystRequirement> stResCR;

    public CatalystRequirementSet(
            String name, float chanceMult, boolean optional, int displayPriority, List<String> hideIfPresent,
            Optional<FluidCatalystRequirement> fluidCR, Optional<ResonanceCatalystRequirement> resCR,
            Optional<ShatteringResonanceCatalystRequirement> shResCR, Optional<StabilizingResonanceCatalystRequirement> stResCR
    ) {
        this.name = name;
        this.chanceMult = chanceMult;
        this.optional = optional;
        this.displayPriority = displayPriority;
        this.hideIfPresent = hideIfPresent;
        this.fluidCR = fluidCR;
        fluidCR.ifPresent(requirements::add);
        this.resCR = resCR;
        resCR.ifPresent(requirements::add);
        this.shResCR = shResCR;
        shResCR.ifPresent(requirements::add);
        this.stResCR = stResCR;
        stResCR.ifPresent(requirements::add);
    }

    /* Returns a list of catalysts that satisfy any requirement in this set */
    public Set<Catalyst> getRelevantCatalysts(Set<Catalyst> catalysts) {
        return catalysts.stream()
                .filter(c -> {
                    for (var cr : requirements) {
                        if (cr.isSatisfiedBy(c)) return true;
                    }
                    return false;
                }).collect(Collectors.toCollection(ObjectOpenHashSet::new));
    }

    /// True if it's possible for a yield that has this CRS to succeed (at all).
    /// This is the case if it can be feasibly satisfied with the provided catalysts or is optional.
    public boolean isSatisfiableOrOptional(Set<Catalyst> catalysts) {
        if (optional) return true;
        for (var cr : requirements) {
            boolean satisfied = false;
            for (var c : catalysts) {
                if (cr.isSatisfiedBy(c)) {
                    satisfied = true;
                    break;
                }
            }
            if (!satisfied) return false;
        }
        return true;
    }

    public boolean useCatalysts(List<Catalyst> catalysts, boolean simulate) {
        if (!useCatalystsNonAtomic(catalysts, true)) return false;
        if (!simulate) useCatalystsNonAtomic(catalysts, false);
        return true;
    }

    public MutableComponent getNameComponent() {
        return CreateRNS.translatable("catalyst." + name + ".name");
    }

    public @Nullable MutableComponent getNameComponent(Collection<CatalystRequirementSet> activeCRSes) {
        for (var crs : activeCRSes) {
            if (hideIfPresent.contains(crs.name)) return null;
        }
        return getNameComponent();
    }

    protected boolean useCatalystsNonAtomic(List<Catalyst> catalysts, boolean simulate) {
        boolean allSatisfied = true;
        for (var cr : requirements) {
            boolean satisifed = false;
            for (var c : catalysts) {
                if (cr.useCatalyst(c, simulate)) {
                    satisifed = true;
                    break;
                }
            }
            if (!satisifed) allSatisfied = false;
        }
        return allSatisfied;
    }
}
