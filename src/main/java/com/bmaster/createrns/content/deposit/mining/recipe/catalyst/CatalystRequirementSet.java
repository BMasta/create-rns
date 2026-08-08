package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.bmaster.createrns.CreateRNS;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CatalystRequirementSet {
    public static final ResourceKey<Registry<CatalystRequirementSet>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(CreateRNS.asResource("catalyst"));

    public static final Codec<CatalystRequirementSet> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.floatRange(0f, Float.MAX_VALUE).optionalFieldOf("chance_multiplier", 1f)
                    .forGetter(crs -> crs.chanceMult),
            Codec.BOOL.optionalFieldOf("optional", false)
                    .forGetter(crs -> crs.optional),
            Codec.INT.optionalFieldOf("display_priority", Integer.MAX_VALUE)
                    .forGetter(crs -> crs.displayPriority),
            BuiltInRegistries.ITEM.byNameCodec().listOf().optionalFieldOf("representative_items", List.of())
                    .forGetter(crs -> crs.representativeItems),
            RegistryFixedCodec.create(REGISTRY_KEY).listOf().optionalFieldOf("hide_if_present", List.of())
                    .forGetter(crs -> crs.hideIfPresent),
            SoundEvent.CODEC.optionalFieldOf("play_when_active")
                    .forGetter(c -> c.soundHolder),
            CatalystRequirement.CODEC.listOf().fieldOf("requirements")
                    .forGetter(crs -> crs.requirements)
    ).apply(i, CatalystRequirementSet::new));

    public static ResourceLocation id(Holder<CatalystRequirementSet> holder) {
        return holder.unwrapKey()
                .orElseThrow(() -> new IllegalStateException("Unbound catalyst requirement set holder"))
                .location();
    }

    public static Holder<CatalystRequirementSet> get(RegistryAccess access, ResourceLocation id) {
        var registry = access.registryOrThrow(REGISTRY_KEY);
        return registry.getHolder(id)
                .orElseThrow(() -> new RuntimeException("Catalyst \"" + id + "\" does not exist"));
    }

    public final float chanceMult;
    public final boolean optional;
    public final int displayPriority;
    public final List<Item> representativeItems;
    public final List<ResourceLocation> hideIfPresentIds;
    public final List<Holder<CatalystRequirementSet>> hideIfPresent;
    public final @Nullable SoundEvent sound;
    public final List<CatalystRequirement> requirements;

    protected final Optional<Holder<SoundEvent>> soundHolder;

    public CatalystRequirementSet(
            float chanceMult, boolean optional, int displayPriority, List<Item> representativeItems,
            List<Holder<CatalystRequirementSet>> hideIfPresent, Optional<Holder<SoundEvent>> soundHolder,
            List<CatalystRequirement> requirements
    ) {
        if (requirements.isEmpty()) throw new IllegalArgumentException("Catalyst must have at least one requirement");
        this.chanceMult = chanceMult;
        this.optional = optional;
        this.displayPriority = displayPriority;
        this.representativeItems = representativeItems;
        this.hideIfPresent = hideIfPresent;
        this.hideIfPresentIds = hideIfPresent.stream().map(CatalystRequirementSet::id).toList();
        this.requirements = requirements;
        this.soundHolder = soundHolder;
        this.sound = soundHolder.map(h -> h.isBound() ? h.value() : null).orElse(null);
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

    public static MutableComponent getNameComponent(Holder<CatalystRequirementSet> holder) {
        return Component.translatable(langKey(id(holder), "name"));
    }

    /// Returns null if at least one of CRSes configured in hideIfPresent is active
    public static @Nullable MutableComponent getNameComponent(
            Collection<Holder<CatalystRequirementSet>> activeCRSes, Holder<CatalystRequirementSet> self
    ) {
        for (var crs : activeCRSes) {
            if (self.value().hideIfPresentIds.contains(id(crs))) return null;
        }
        return getNameComponent(self);
    }

    public static MutableComponent getDescriptionComponent(Holder<CatalystRequirementSet> holder) {
        return Component.translatable(langKey(id(holder), "description"));
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

    private static String langKey(ResourceLocation id, String suffix) {
        return id.getNamespace() + ".catalyst." + id.getPath().replace('/', '.') + "." + suffix;
    }
}
