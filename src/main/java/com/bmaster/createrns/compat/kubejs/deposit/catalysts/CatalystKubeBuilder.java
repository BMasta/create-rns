package com.bmaster.createrns.compat.kubejs.deposit.catalysts;

import com.bmaster.createrns.compat.kubejs.util.JSMethod;
import com.bmaster.createrns.compat.kubejs.util.StartupKubeBuilder;
import com.bmaster.createrns.data.deposit.DepositResources;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Predicate;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CatalystKubeBuilder extends StartupKubeBuilder {
    private static final int DEFAULT_ATTACHMENT_COUNT = 1;

    private final ResourceLocation id;
    private final Predicate<ResourceLocation> isUnknownCatalyst;
    private final LinkedHashSet<ResourceLocation> attachmentBlocks = new LinkedHashSet<>();
    private final List<ResourceLocation> representativeItems = new ArrayList<>();
    private final List<ResourceLocation> hideIfPresent = new ArrayList<>();
    private final List<DepositResources.CatalystRequirement> requirements = new ArrayList<>();

    private float chanceMultiplier = 1;
    private double countMultiplier = 1;
    private boolean optional;
    private Integer displayPriority;
    private ResourceLocation playWhenActive;
    private String displayName;
    private String description;

    public CatalystKubeBuilder(ResourceLocation id, Predicate<ResourceLocation> isUnknownCatalyst) {
        super("rnsCatalysts", "create");
        this.id = id;
        this.isUnknownCatalyst = isUnknownCatalyst;
    }

    @Info("""
            Generates the catalyst name lang entry used in JEI/EMI and miner tooltips.
            If omitted, make sure to supply the matching `<namespace>.catalyst.<path>.name` lang entry yourself.
            """)
    @JSMethod
    public CatalystKubeBuilder displayName(String name) {
        displayName = name;
        return this;
    }

    @Info("""
            Generates the lang entry displayed on the catalyst info page.
            If omitted, make sure to supply the matching `<namespace>.catalyst.<path>.description` lang entry yourself.
            """)
    @JSMethod
    public CatalystKubeBuilder description(String text) {
        description = text;
        return this;
    }

    @Info("Sets the chance multiplier applied while this catalyst is active.")
    @JSMethod
    public CatalystKubeBuilder chanceMultiplier(float value) {
        if (value < 0) {
            error("Catalyst chance multiplier must be non-negative", true);
            return this;
        }

        chanceMultiplier = value;
        return this;
    }

    @Info("Multiplies the number of items produced by a successful yield while this catalyst is active.")
    @JSMethod
    public CatalystKubeBuilder countMultiplier(double value) {
        if (!Double.isFinite(value) || value < 0) {
            error("Catalyst count multiplier must be finite and non-negative", true);
            return this;
        }
        countMultiplier = value;
        return this;
    }

    @Info("Allows the yield to roll even when this catalyst is inactive.")
    @JSMethod
    public CatalystKubeBuilder optional() {
        optional = true;
        return this;
    }

    @Info("""
            Defines the order in which the catalysts are shown in the miner bearing tooltip and JEI/EMI.
            Highest priority catalysts are shown at the bottom. The order will not be stable unless each catalyst has a unique priority.
            """)
    @JSMethod
    public CatalystKubeBuilder displayPriority(int value) {
        displayPriority = value;
        return this;
    }

    @Info("""
            Attaches a JEI/EMI info page to the specified item with the catalyst description.
            Call multiple times to attach the same description to more than one item.
            """)
    @JSMethod
    public CatalystKubeBuilder representativeItem(String itemId) {
        var parsed = registeredId(itemId, BuiltInRegistries.ITEM, "item");
        if (parsed == null) return this;
        representativeItems.add(parsed);
        return this;
    }

    @Info("""
            Hides this catalyst from the miner bearing tooltip while the specified catalyst is active.
            Useful when you have multiple tiers of catalysts. Can be called multiple times to add more catalysts.
            """)
    @JSMethod
    public CatalystKubeBuilder hideIfPresent(String catalystId) {
        if (catalystId.isBlank()) {
            error("Catalyst id cannot be blank", true);
            return this;
        }

        var parsedId = ResourceLocation.tryParse(catalystId);
        if (parsedId == null) {
            error("Invalid catalyst id: " + catalystId, true);
            return this;
        }

        hideIfPresent.add(parsedId);
        return this;
    }

    @Info("The specified sound event will be continuously played while the catalyst is active.")
    @JSMethod
    public CatalystKubeBuilder playWhenActive(String soundId) {
        var parsed = registeredId(soundId, BuiltInRegistries.SOUND_EVENT, "sound event");
        if (parsed == null) return this;
        playWhenActive = parsed;
        return this;
    }

    @Info("""
            Adds an attachment requirement.
            When a list is used, any matching attachment block can contribute towards the required count.
            Can be called multiple times to add more independent requirements.
            """)
    @JSMethod
    public CatalystKubeBuilder attachment(Object blockOrTagIdOrBlockIds) {
        return attachment(blockOrTagIdOrBlockIds, DEFAULT_ATTACHMENT_COUNT);
    }

    @Info("""
            Adds an attachment requirement.
            When a list is used, any matching attachment block can contribute towards the required count.
            Can be called multiple times to add more independent requirements.
            """)
    @JSMethod
    public CatalystKubeBuilder attachment(Object blockOrTagIdOrBlockIds, int count) {
        if (count <= 0) {
            error("Catalyst attachment count must be positive", true);
            return this;
        }
        var inputs = switch (blockOrTagIdOrBlockIds) {
            case String blockId -> List.of(blockId);
            case Collection<?> ids -> ids;
            case Object[] ids -> Arrays.asList(ids);
            //noinspection DataFlowIssue (null is not a valid input, but it's better to handle it with a proper error regardless)
            case null, default -> null;
        };
        if (inputs == null) {
            error("Attachment must be a block id or list of block ids", true);
            return this;
        }
        if (inputs.isEmpty()) {
            error("Attachment block id list cannot be empty", true);
            return this;
        }

        var attachment = new ArrayList<ResourceLocation>(inputs.size());
        for (var input : inputs) {
            if (!(input instanceof String blockId)) {
                error("Attachment block id lists must contain only strings", true);
                return this;
            }
            if (blockId.isBlank()) {
                error("Attachment block id cannot be blank", true);
                return this;
            }
            if (blockId.startsWith("#")) {
                error("Tags are not supported: " + blockId, true);
                return this;
            }
            var parsed = registeredId(blockId, BuiltInRegistries.BLOCK, "block");
            if (parsed == null) return this;
            attachment.add(parsed);
        }
        requirements.add(new DepositResources.AttachmentRequirement(attachment, count));
        attachmentBlocks.addAll(attachment);
        return this;
    }

    @Info("Adds a fluid consumption requirement. Can be called multiple times to require multiple fluids.")
    @JSMethod
    public CatalystKubeBuilder fluid(String fluidId, int amount) {
        if (amount <= 0) {
            error("Catalyst fluid amount must be positive", true);
            return this;
        }
        var parsed = registeredId(fluidId, BuiltInRegistries.FLUID, "fluid");
        if (parsed == null) return this;
        requirements.add(new DepositResources.FluidRequirement(parsed, amount));
        return this;
    }

    public void gatherResources(DepositResources resources) {
        if (!isValid()) return;
        resources.catalysts.add(new DepositResources.Catalyst(id, chanceMultiplier, countMultiplier, optional, displayPriority,
                representativeItems, hideIfPresent, playWhenActive, requirements));
        if (displayName != null) {
            resources.langEntries.add(new DepositResources.LangEntry(
                    id.getNamespace(), DepositResources.catalystLangKey(id, "name"), displayName));
        }
        if (description != null) {
            resources.langEntries.add(new DepositResources.LangEntry(
                    id.getNamespace(), DepositResources.catalystLangKey(id, "description"), description));
        }
    }

    public List<ResourceLocation> attachmentBlocks() {
        return List.copyOf(attachmentBlocks);
    }

    public ResourceLocation id() {
        return id;
    }

    @Override
    protected void assertValid() {
        for (var reference : hideIfPresent) {
            if (isUnknownCatalyst.test(reference)) {
                error("Unknown catalyst in hideIfPresent: " + reference, true);
            }
        }
        if (requirements.isEmpty()) {
            error("Catalyst " + id + " must define at least one requirement", true);
        }
    }

    @Nullable
    private ResourceLocation registeredId(String id, Registry<?> registry, String type) {
        if (id.isBlank()) {
            error("Id cannot be blank", true);
            return null;
        }
        var resourceId = ResourceLocation.tryParse(id);
        if (resourceId == null) {
            error("Invalid id: " + id, true);
            return null;
        }
        if (!registry.containsKey(resourceId)) {
            error("Unknown " + type + ": " + resourceId, true);
            return null;
        }
        return resourceId;
    }
}
