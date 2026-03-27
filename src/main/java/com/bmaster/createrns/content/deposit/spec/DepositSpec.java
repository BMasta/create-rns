package com.bmaster.createrns.content.deposit.spec;

import com.bmaster.createrns.CreateRNS;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositSpec {
    private static final Codec<List<ResourceLocation>> ITEM_CANDIDATES_CODEC = Codec.STRING.listOf().xmap(
            itemCandidates -> itemCandidates.stream().map(ResourceLocation::parse).toList(),
            itemCandidates -> itemCandidates.stream().map(ResourceLocation::toString).toList()
    );
    private static final Codec<List<TagKey<Item>>> TAG_CANDIDATES_CODEC = Codec.STRING.listOf().xmap(
            tagCandidates -> tagCandidates.stream()
                    .map(ResourceLocation::parse)
                    .map(id -> TagKey.create(Registries.ITEM, id))
                    .toList(),
            tagCandidates -> tagCandidates.stream()
                    .map(TagKey::location)
                    .map(ResourceLocation::toString)
                    .toList()
    );

    public static final Codec<DepositSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            ITEM_CANDIDATES_CODEC.optionalFieldOf("scanner_icon_item_candidates", List.of())
                    .forGetter(ds -> ds.scannerIconItemRls),
            TAG_CANDIDATES_CODEC.optionalFieldOf("scanner_icon_tag_candidates", List.of())
                    .forGetter(ds -> ds.scannerIconTags),
            ForgeRegistries.ITEMS.getCodec().fieldOf("map_icon_item")
                    .forGetter(ds -> ds.mapIcon.getItem()),
            ResourceLocation.CODEC.fieldOf("structure")
                    .forGetter(ds -> ds.structure)
    ).apply(i, DepositSpec::new));

    public static final ResourceKey<Registry<DepositSpec>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(CreateRNS.asResource("deposit_spec"));

    public final ResourceLocation structure;
    protected final List<ResourceLocation> scannerIconItemRls;
    protected final List<TagKey<Item>> scannerIconTags;
    protected final ItemStack mapIcon;
    protected Item scannerIcon = null;

    public DepositSpec(
            List<ResourceLocation> scannerIconItemRls, List<TagKey<Item>> scannerIconTags, Item mapIconItem,
            ResourceLocation structure
    ) {
        this.scannerIconItemRls = scannerIconItemRls;
        this.scannerIconTags = scannerIconTags;
        this.structure = structure;
        this.mapIcon = new ItemStack(mapIconItem);
    }

    public ResourceKey<Structure> structureKey() {
        return ResourceKey.create(Registries.STRUCTURE, structure);
    }

    public boolean initialize(RegistryAccess access) {
        if (scannerIconItemRls.isEmpty() && scannerIconTags.isEmpty()) {
            throw new IllegalArgumentException("Deposit spec must define at least one scanner icon item or a tag");
        }

        for (var rl : scannerIconItemRls) {
            scannerIcon = ForgeRegistries.ITEMS.getValue(rl);
            // What the actual fuck forge, why would you return air with a nullable annotated return value??
            if (scannerIcon == Items.AIR) scannerIcon = null;
            if (scannerIcon != null) return true;
        }

        for (var tag : scannerIconTags) {
            // Pick the first item from the tag. "First" is determined by the order in which the items were tagged.
            var hs = access.lookupOrThrow(Registries.ITEM).get(tag).orElse(null);
            scannerIcon = (hs != null) ? hs.stream().map(Holder::value).findFirst().orElse(null) : null;
            if (scannerIcon == Items.AIR) scannerIcon = null;
            if (scannerIcon != null) return true;
        }

        return false;
    }

    public @Nullable Item getIcon() {
        return scannerIcon;
    }

    public ItemStack getMapIcon() {
        return mapIcon;
    }
}
