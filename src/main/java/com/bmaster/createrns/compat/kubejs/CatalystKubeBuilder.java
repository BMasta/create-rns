package com.bmaster.createrns.compat.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.latvian.mods.kubejs.client.LangKubeEvent;
import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CatalystKubeBuilder extends SourcedStartupKubeBuilder {
    private static final float DEFAULT_CHANCE_MULTIPLIER = 1f;
    private static final int DEFAULT_ATTACHMENT_COUNT = 1;

    private final ResourceLocation id;
    private final LinkedHashSet<String> attachmentBlocks = new LinkedHashSet<>();
    private final List<String> representativeItems = new ArrayList<>();
    private final List<CatalystReference> hideIfPresent = new ArrayList<>();
    private final List<JsonObject> requirements = new ArrayList<>();

    private float chanceMultiplier = DEFAULT_CHANCE_MULTIPLIER;
    private boolean optional;
    private Integer displayPriority;
    private ResourceLocation playWhenActive;
    private String displayName;
    private String description;

    public CatalystKubeBuilder(ResourceLocation id) {
        this(id, SourceLine.UNKNOWN);
    }

    CatalystKubeBuilder(ResourceLocation id, SourceLine sourceLine) {
        super(sourceLine, "rnsCatalysts", "create");
        this.id = id;
    }

    @Info("""
            Generates the catalyst name lang entry used in JEI/EMI and miner tooltips.
            If omitted, make sure to supply the matching `<namespace>.catalyst.<path>.name` lang entry yourself.
            """)
    public CatalystKubeBuilder displayName(String name) {
        displayName = name;
        return this;
    }

    @Info("""
            Generates the lang entry displayed on the catalyst info page.
            If omitted, make sure to supply the matching `<namespace>.catalyst.<path>.description` lang entry yourself.
            """)
    public CatalystKubeBuilder description(String text) {
        description = text;
        return this;
    }

    @Info("Sets the chance multiplier applied while this catalyst is active.")
    public CatalystKubeBuilder chanceMultiplier(Context cx, float value) {
        return sourced(cx, "chanceMultiplier", () -> chanceMultiplier(value));
    }

    @HideFromJS
    public CatalystKubeBuilder chanceMultiplier(float value) {
        if (value < 0) throw new IllegalArgumentException("Catalyst chance multiplier must be non-negative");

        chanceMultiplier = value;
        return this;
    }

    @Info("Marks the catalyst as optional. Optional catalysts influence the chance of a yield without being strictly required for it.")
    public CatalystKubeBuilder optional() {
        optional = true;
        return this;
    }

    @Info("""
            Defines the order in which the catalysts are shown in the miner bearing tooltip and JEI/EMI.
            Highest priority catalysts are shown at the bottom. The order will not be stable unless each catalyst has a unique priority.
            """)
    public CatalystKubeBuilder displayPriority(int value) {
        displayPriority = value;
        return this;
    }

    @Info("""
            Attaches a JEI/EMI info page to the specified item with the catalyst description.
            Call multiple times to attach the same description to more than one item.
            """)
    public CatalystKubeBuilder representativeItem(Context cx, String itemId) {
        return sourced(cx, "representativeItem", () -> {
            var result = representativeItem(itemId);
            requireRegistered(BuiltInRegistries.ITEM, normalizeId(itemId), "item");
            return result;
        });
    }

    @HideFromJS
    public CatalystKubeBuilder representativeItem(String itemId) {
        representativeItems.add(normalizeId(itemId));
        return this;
    }

    @Info("""
            Hides this catalyst from the miner bearing tooltip while the specified catalyst is active.
            Useful when having multiple tiers of catalysts. Can be called multiple times to add more catalysts.
            """)
    public CatalystKubeBuilder hideIfPresent(Context cx, String catalystId) {
        return sourced(cx, "hideIfPresent", () -> hideIfPresent(catalystId, methodSource(cx, "hideIfPresent")));
    }

    @HideFromJS
    public CatalystKubeBuilder hideIfPresent(String catalystId) {
        return hideIfPresent(catalystId, SourceLine.UNKNOWN);
    }

    private CatalystKubeBuilder hideIfPresent(String catalystId, SourceLine sourceLine) {
        if (catalystId.isBlank()) throw new IllegalArgumentException("Catalyst id cannot be blank");

        var parsedId = ResourceLocation.tryParse(catalystId);
        if (parsedId == null) throw new IllegalArgumentException("Invalid catalyst id: " + catalystId);

        hideIfPresent.add(new CatalystReference(parsedId, sourceLine));
        return this;
    }

    @Info("The specified sound event will be continuously played while the catalyst is active.")
    public CatalystKubeBuilder playWhenActive(Context cx, String soundId) {
        return sourced(cx, "playWhenActive", () -> {
            var result = playWhenActive(soundId);
            requireRegistered(BuiltInRegistries.SOUND_EVENT, normalizeId(soundId), "sound event");
            return result;
        });
    }

    @HideFromJS
    public CatalystKubeBuilder playWhenActive(String soundId) {
        playWhenActive = ResourceLocation.parse(normalizeId(soundId));
        return this;
    }

    @Info("""
            Adds an attachment requirement.
            When a list is used, any matching attachment block can contribute towards the required count.
            Can be called multiple times to add more independent requirements.
            """)
    public CatalystKubeBuilder attachment(Context cx, Object blockOrTagIdOrBlockIds) {
        return sourced(cx, "attachment", () -> {
            var result = attachment(blockOrTagIdOrBlockIds);
            validateRegisteredAttachmentBlocks(blockOrTagIdOrBlockIds);
            return result;
        });
    }

    @HideFromJS
    public CatalystKubeBuilder attachment(Object blockOrTagIdOrBlockIds) {
        return attachment(blockOrTagIdOrBlockIds, DEFAULT_ATTACHMENT_COUNT);
    }

    @Info("""
            Adds an attachment requirement.
            When a list is used, any matching attachment block can contribute towards the required count.
            Can be called multiple times to add more independent requirements.
            """)
    public CatalystKubeBuilder attachment(Context cx, Object blockOrTagIdOrBlockIds, int count) {
        return sourced(cx, "attachment", () -> {
            var result = attachment(blockOrTagIdOrBlockIds, count);
            validateRegisteredAttachmentBlocks(blockOrTagIdOrBlockIds);
            return result;
        });
    }

    @HideFromJS
    public CatalystKubeBuilder attachment(Object blockOrTagIdOrBlockIds, int count) {
        validateAttachmentCount(count);
        var attachment = normalizeAttachmentInput(blockOrTagIdOrBlockIds);
        requirements.add(attachmentRequirement(attachment.value(), count));
        attachmentBlocks.addAll(attachment.blocks());
        return this;
    }

    @Info("Adds a fluid consumption requirement. Can be called multiple times to require multiple fluids.")
    public CatalystKubeBuilder fluid(Context cx, String fluidId, int amount) {
        return sourced(cx, "fluid", () -> {
            var result = fluid(fluidId, amount);
            requireRegistered(BuiltInRegistries.FLUID, normalizeId(fluidId), "fluid");
            return result;
        });
    }

    @HideFromJS
    public CatalystKubeBuilder fluid(String fluidId, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Catalyst fluid amount must be positive");

        var consume = new JsonObject();
        consume.addProperty("id", normalizeId(fluidId));
        consume.addProperty("amount", amount);

        var requirement = new JsonObject();
        requirement.addProperty("type", "fluid");
        requirement.add("consume", consume);
        requirements.add(requirement);
        return this;
    }

    void generateData(KubeDataGenerator generator) {
        if (!validate()) return;
        generator.json(dataPath(), json());
    }

    void generateLang(LangKubeEvent lang) {
        if (isInvalid()) return;
        if (displayName != null) {
            lang.add(id.getNamespace(), langKey("name"), displayName);
        }
        if (description != null) {
            lang.add(id.getNamespace(), langKey("description"), description);
        }
    }

    List<String> attachmentBlocks() {
        return List.copyOf(attachmentBlocks);
    }

    ResourceLocation id() {
        return id;
    }

    boolean validateHideIfPresent(
            Collection<ResourceLocation> knownCatalysts, Collection<String> ownedNamespaces
    ) {
        if (isInvalid() || !hasKnownCreationSource()) return !isInvalid();

        for (var reference : hideIfPresent) {
            if (knownCatalysts.contains(reference.id())) continue;
            if (!ownedNamespaces.contains(reference.id().getNamespace())) continue;

            return reportDeferredError("Unknown catalyst in hideIfPresent: " + reference.id(),
                    reference.sourceLine());
        }
        return true;
    }

    private boolean validate() {
        if (isInvalid()) return false;
        if (requirements.isEmpty()) {
            return reportDeferredError("Catalyst " + id + " must define at least one requirement");
        }
        return true;
    }

    private ResourceLocation dataPath() {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "create_rns/catalyst/" + id.getPath());
    }

    private JsonObject json() {
        var root = new JsonObject();

        if (chanceMultiplier != DEFAULT_CHANCE_MULTIPLIER) {
            root.addProperty("chance_multiplier", chanceMultiplier);
        }
        if (optional) {
            root.addProperty("optional", true);
        }
        if (displayPriority != null) {
            root.addProperty("display_priority", displayPriority);
        }
        if (!representativeItems.isEmpty()) {
            root.add("representative_items", stringArray(representativeItems));
        }
        if (!hideIfPresent.isEmpty()) {
            root.add("hide_if_present", stringArray(hideIfPresent.stream()
                    .map(reference -> reference.id().toString())
                    .toList()));
        }
        if (playWhenActive != null) {
            var sound = new JsonObject();
            sound.addProperty("sound_id", playWhenActive.toString());
            root.add("play_when_active", sound);
        }

        var requirementArray = new JsonArray();
        for (var requirement : requirements) {
            requirementArray.add(requirement);
        }
        root.add("requirements", requirementArray);
        return root;
    }

    private String langKey(String suffix) {
        return id.getNamespace() + ".catalyst." + id.getPath().replace('/', '.') + "." + suffix;
    }

    private static JsonObject attachmentRequirement(JsonElement attachment, int count) {
        var requirement = new JsonObject();
        requirement.addProperty("type", "attachment");
        requirement.add("attachment", attachment);
        if (count != DEFAULT_ATTACHMENT_COUNT) {
            requirement.addProperty("count", count);
        }
        return requirement;
    }

    private static JsonArray stringArray(List<String> values) {
        var array = new JsonArray();
        for (var value : values) {
            array.add(value);
        }
        return array;
    }

    private static JsonElement stringOrArray(List<String> values) {
        if (values.size() == 1) return new JsonPrimitive(values.getFirst());

        return stringArray(values);
    }

    private static AttachmentInput normalizeAttachmentInput(Object blockOrTagIdOrBlockIds) {
        return switch (blockOrTagIdOrBlockIds) {
            case String blockOrTagId -> {
                var normalizedBlockId = normalizeAttachmentBlockId(blockOrTagId);
                yield new AttachmentInput(new JsonPrimitive(normalizedBlockId), List.of(normalizedBlockId));
            }
            case Collection<?> blockIds -> normalizeAttachmentBlockList(blockIds);
            case Object[] blockIds -> normalizeAttachmentBlockList(Arrays.asList(blockIds));
            default -> throw new IllegalArgumentException("Attachment must be a block id or list of block ids");
        };

    }

    private static AttachmentInput normalizeAttachmentBlockList(Collection<?> blockIds) {
        if (blockIds.isEmpty()) throw new IllegalArgumentException("Attachment block id list cannot be empty");

        var normalizedBlockIds = new ArrayList<String>(blockIds.size());
        for (var blockId : blockIds) {
            if (!(blockId instanceof String blockIdString)) {
                throw new IllegalArgumentException("Attachment block id lists must contain only strings");
            }
            normalizedBlockIds.add(normalizeAttachmentBlockId(blockIdString));
        }

        return new AttachmentInput(stringOrArray(normalizedBlockIds), List.copyOf(normalizedBlockIds));
    }

    private static String normalizeAttachmentBlockId(String blockId) {
        if (blockId.isBlank()) throw new IllegalArgumentException("Attachment block id cannot be blank");
        if (blockId.startsWith("#")) {
            throw new IllegalArgumentException("Tags are not supported: " + blockId);
        }
        return normalizeId(blockId);
    }

    private static String normalizeId(String id) {
        if (id.isBlank()) throw new IllegalArgumentException("Id cannot be blank");
        var normalized = id.contains(":") ? id : "minecraft:" + id;
        if (ResourceLocation.tryParse(normalized) == null) throw new IllegalArgumentException("Invalid id: " + id);
        return normalized;
    }

    private static void validateRegisteredAttachmentBlocks(Object blockOrTagIdOrBlockIds) {
        var attachment = normalizeAttachmentInput(blockOrTagIdOrBlockIds);
        for (var blockId : attachment.blocks()) {
            requireRegistered(BuiltInRegistries.BLOCK, blockId, "block");
        }
    }

    private static <T> void requireRegistered(
            net.minecraft.core.Registry<T> registry, String id, String type
    ) {
        var resourceId = ResourceLocation.parse(id);
        if (!registry.containsKey(resourceId)) throw new IllegalArgumentException("Unknown " + type + ": " + id);
    }

    private static void validateAttachmentCount(int count) {
        if (count <= 0) throw new IllegalArgumentException("Catalyst attachment count must be positive");
    }

    private record AttachmentInput(JsonElement value, List<String> blocks) {
    }

    private record CatalystReference(ResourceLocation id, SourceLine sourceLine) {
    }
}
