package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.util.StrictOptionalField;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CatalystRequirementSet {
    public static final Codec<CatalystRequirementSet> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name")
                    .forGetter(crs -> crs.name),
            StrictOptionalField.of("chance_multiplier", Codec.floatRange(0f, Float.MAX_VALUE), 1f)
                    .forGetter(crs -> crs.chanceMult),
            StrictOptionalField.of("optional", Codec.BOOL, false)
                    .forGetter(crs -> crs.optional),
            StrictOptionalField.of("display_priority", Codec.INT, Integer.MAX_VALUE)
                    .forGetter(crs -> crs.displayPriority),
            StrictOptionalField.of("representative_items", ForgeRegistries.ITEMS.getCodec().listOf(), List.of())
                    .forGetter(crs -> crs.representativeItems),
            StrictOptionalField.of("hide_if_present", Codec.STRING.listOf(), List.of())
                    .forGetter(crs -> crs.hideIfPresent),
            CatalystRequirement.CODEC.listOf().fieldOf("requirements")
                    .forGetter(crs -> crs.requirements)
    ).apply(i, CatalystRequirementSet::new));

    public static final ResourceKey<Registry<CatalystRequirementSet>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(CreateRNS.asResource("catalyst"));

    public final String name;
    public final float chanceMult;
    public final boolean optional;
    public final int displayPriority;
    public final List<Item> representativeItems;
    public final List<String> hideIfPresent;
    public final List<CatalystRequirement> requirements;

    public CatalystRequirementSet(
            String name, float chanceMult, boolean optional, int displayPriority, List<Item> representativeItems,
            List<String> hideIfPresent, List<CatalystRequirement> requirements
    ) {
        if (requirements.isEmpty()) throw new IllegalArgumentException("Catalyst must have at least one requirement");
        this.name = name;
        this.chanceMult = chanceMult;
        this.optional = optional;
        this.displayPriority = displayPriority;
        this.representativeItems = representativeItems;
        this.hideIfPresent = hideIfPresent;
        this.requirements = requirements;
    }

    /* Returns a list of catalysts that satisfy any requirement in this set */
    public Set<Catalyst> getRelevantCatalysts(Set<Catalyst> catalysts) {
        return catalysts.stream()
                .filter(c -> {
                    for (var cr : requirements) {
                        if (cr.relevantCatalystTypes().contains(c.getClass())) return true;
                    }
                    return false;
                }).collect(Collectors.toCollection(ObjectOpenHashSet::new));
    }

    /// True if it's possible for a yield that has this CRS to succeed (at all).
    /// This is the case if it can be feasibly satisfied with the provided catalysts or is optional.
    public boolean isSatisfiableOrOptional(Set<Catalyst> catalysts) {
        if (optional) return true;
        for (var cr : requirements) {
            if (!cr.isSatisfiedBy(catalysts)) return false;
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
        if (catalysts.isEmpty()) return false;
        boolean allSatisfied = true;
        for (var cr : requirements) {
            boolean satisifed = false;
            if (cr.useCatalysts(catalysts, simulate)) {
                for (var c : catalysts) {
                    satisifed = true;
                    break;
                }
            }
            if (!satisifed) allSatisfied = false;
        }
        return allSatisfied;
    }
}
