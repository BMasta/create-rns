package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSetLookup;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.latvian.mods.kubejs.client.LangEventJS;
import dev.latvian.mods.kubejs.generator.DataJsonGenerator;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CatalystKubeBuilder {
    private static final float DEFAULT_CHANCE_MULTIPLIER = 1f;
    private static final int DEFAULT_ATTACHMENT_COUNT = 1;

    private final ResourceLocation id;
    private final LinkedHashSet<String> attachmentBlocks = new LinkedHashSet<>();
    private final List<String> representativeItems = new ArrayList<>();
    private final List<String> hideIfPresent = new ArrayList<>();
    private final List<JsonObject> requirements = new ArrayList<>();

    private float chanceMultiplier = DEFAULT_CHANCE_MULTIPLIER;
    private boolean optional;
    private Integer displayPriority;
    private ResourceLocation playWhenActive;
    private String displayName;
    private String description;

    public CatalystKubeBuilder(ResourceLocation id) {
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
    public CatalystKubeBuilder representativeItem(String itemId) {
        representativeItems.add(normalizeId(itemId));
        return this;
    }

    @Info("""
            Hides this catalyst from the miner bearing tooltip while the specified catalyst is active.
            Useful when having multiple tiers of catalysts. Can be called multiple times to add more catalysts.
            """)
    public CatalystKubeBuilder hideIfPresent(String catalystId) {
        if (catalystId.isBlank()) throw new IllegalArgumentException("Catalyst id cannot be blank");

        hideIfPresent.add(CatalystRequirementSetLookup.parseId(catalystId)
                .result()
                .orElseThrow(() -> new IllegalArgumentException("Invalid catalyst id: " + catalystId))
                .toString());
        return this;
    }

    @Info("The specified sound event will be continuously played while the catalyst is active.")
    public CatalystKubeBuilder playWhenActive(String soundId) {
        playWhenActive = ResourceLocation.parse(normalizeId(soundId));
        return this;
    }

    @Info("""
            Adds an attachment requirement.
            When a list is used, any matching attachment block can contribute towards the required count.
            Can be called multiple times to add more independent requirements.
            """)
    public CatalystKubeBuilder attachment(Object blockOrTagIdOrBlockIds) {
        return attachment(blockOrTagIdOrBlockIds, DEFAULT_ATTACHMENT_COUNT);
    }

    @Info("""
            Adds an attachment requirement.
            When a list is used, any matching attachment block can contribute towards the required count.
            Can be called multiple times to add more independent requirements.
            """)
    public CatalystKubeBuilder attachment(Object blockOrTagIdOrBlockIds, int count) {
        validateAttachmentCount(count);
        var attachment = normalizeAttachmentInput(blockOrTagIdOrBlockIds);
        requirements.add(attachmentRequirement(attachment.value(), count));
        attachmentBlocks.addAll(attachment.blocks());
        return this;
    }

    @Info("Adds a fluid consumption requirement. Can be called multiple times to require multiple fluids.")
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

    void generateData(DataJsonGenerator generator) {
        validate();
        generator.json(dataPath(), json());
    }

    void generateLang(LangEventJS lang) {
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

    private void validate() {
        if (requirements.isEmpty()) {
            throw new IllegalStateException("Catalyst " + id + " must define at least one requirement");
        }
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
            root.add("hide_if_present", stringArray(hideIfPresent));
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
        if (values.size() == 1) return new JsonPrimitive(values.get(0));

        return stringArray(values);
    }

    private static AttachmentInput normalizeAttachmentInput(Object blockOrTagIdOrBlockIds) {
        if (blockOrTagIdOrBlockIds instanceof String blockOrTagId) {
            var normalizedBlockId = normalizeAttachmentBlockId(blockOrTagId);
            return new AttachmentInput(new JsonPrimitive(normalizedBlockId), List.of(normalizedBlockId));
        }
        if (blockOrTagIdOrBlockIds instanceof Collection<?> blockIds) {
            return normalizeAttachmentBlockList(blockIds);
        }
        if (blockOrTagIdOrBlockIds instanceof Object[] blockIds) {
            return normalizeAttachmentBlockList(Arrays.asList(blockIds));
        }

        throw new IllegalArgumentException("Attachment must be a block id or list of block ids");
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
        return id.contains(":") ? id : "minecraft:" + id;
    }

    private static void validateAttachmentCount(int count) {
        if (count <= 0) throw new IllegalArgumentException("Catalyst attachment count must be positive");
    }

    private record AttachmentInput(JsonElement value, List<String> blocks) {
    }
}
