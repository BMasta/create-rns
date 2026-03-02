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
import net.minecraft.resources.ResourceKey;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CatalystRequirementSet {
    public static final Codec<CatalystRequirementSet> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(crs -> crs.name),
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
    public final List<CatalystRequirement> requirements = new ArrayList<>();
    protected final Optional<FluidCatalystRequirement> fluidCR;
    protected final Optional<ResonanceCatalystRequirement> resCR;
    protected final Optional<ShatteringResonanceCatalystRequirement> shResCR;
    protected final Optional<StabilizingResonanceCatalystRequirement> stResCR;

    public CatalystRequirementSet(
            String name, Optional<FluidCatalystRequirement> fluidCR, Optional<ResonanceCatalystRequirement> resCR,
            Optional<ShatteringResonanceCatalystRequirement> shResCR, Optional<StabilizingResonanceCatalystRequirement> stResCR
    ) {
        this.name = name;
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

    public boolean isSatisfiedBy(Set<Catalyst> catalysts) {
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

    public float useCatalysts(List<Catalyst> catalysts, boolean simulate) {
        // Check that all requirements can be satisfied
        boolean allSatisfied = true;
        for (var cr : requirements) {
            boolean satisifed = false;
            for (var c : catalysts) {
                if (c.use(cr, simulate)) {
                    satisifed = true;
                    break;
                }
            }
            if (!satisifed) allSatisfied = false;
        }
        if (!allSatisfied) return -1f;

        // Calculate chances
        float chance = 1f;
        for (var cr : requirements) {
            for (var c : catalysts) {
                if (c.use(cr, true)) {
                    if (simulate) c.use(cr, false);
                    chance *= cr.getChanceMult(c);
                    break;
                }
            }
        }
        return chance;
    }

    private boolean useCatalystsNonAtomic(List<Catalyst> catalysts, boolean simulate) {
        var doneArr = new Boolean[requirements.size()];
        Arrays.fill(doneArr, false);
        for (int i = 0; i < requirements.size(); ++i) {
            for (var c : catalysts) {
                var cr = requirements.get(i);
                if (!doneArr[i]) doneArr[i] = (requirements.get(i) == null || c.use(cr, simulate));
            }
        }
        return Arrays.stream(doneArr).allMatch(b -> b);
    }
}
