package com.bmaster.createrns;

import com.bmaster.createrns.content.deposit.scanning.DepositScannerItem;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.data.AssetLookup;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;

public class RNSItems {
    public static final ItemEntry<DepositScannerItem> DEPOSIT_SCANNER_ITEM = CreateRNS.REGISTRATE.item(
                    "deposit_scanner", DepositScannerItem::new)
            .properties(p -> p.stacksTo(1))
            .model(AssetLookup.itemModelWithPartials())
            .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
                    .define('E', AllItems.ELECTRON_TUBE)
                    .define('W', AllBlocks.COGWHEEL)
                    .define('C', AllBlocks.ANDESITE_CASING)
                    .define('T', AllItems.TRANSMITTER)
                    .pattern(" E ")
                    .pattern("TWT")
                    .pattern(" C ")
                    .unlockedBy("has_electron_tube", RegistrateRecipeProvider.has(AllItems.ELECTRON_TUBE))
                    .save(p))
            .register();

    public static final ItemEntry<Item> RESONANT_AMETHYST = CreateRNS.REGISTRATE.item(
                    "resonant_amethyst", Item::new)
            .register();
    public static final ItemEntry<Item> IMPURE_IRON_ORE = CreateRNS.REGISTRATE.item(
            "impure_iron_ore", Item::new).tag(RNSTags.Item.IMPURE_ORES).register();

    public static final ItemEntry<Item> IMPURE_COPPER_ORE = CreateRNS.REGISTRATE.item(
            "impure_copper_ore", Item::new).tag(RNSTags.Item.IMPURE_ORES).register();

    public static final ItemEntry<Item> IMPURE_ZINC_ORE = CreateRNS.REGISTRATE.item(
            "impure_zinc_ore", Item::new).tag(RNSTags.Item.IMPURE_ORES).register();

    public static final ItemEntry<Item> IMPURE_GOLD_ORE = CreateRNS.REGISTRATE.item(
            "impure_gold_ore", Item::new).tag(RNSTags.Item.IMPURE_ORES).register();

    public static final ItemEntry<Item> IMPURE_REDSTONE_DUST = CreateRNS.REGISTRATE.item(
            "impure_redstone_dust", Item::new).tag(RNSTags.Item.IMPURE_ORES).register();

    // Yoinked from tech reborn
    public static final ItemEntry<Item> REDSTONE_SMALL_DUST = CreateRNS.REGISTRATE.item(
            "redstone_small_dust", Item::new).register();

    public static void register() {
    }
}
