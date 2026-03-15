package com.bmaster.createrns;

import com.bmaster.createrns.content.deposit.scanning.DepositScannerItem;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.data.AssetLookup;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
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
            "resonant_amethyst", Item::new).register(),

    POLISHED_RESONANT_AMETHYST = CreateRNS.REGISTRATE.item(
            "polished_resonant_amethyst", Item::new).register(),

    // Yoinked from tech reborn
    REDSTONE_SMALL_DUST = CreateRNS.REGISTRATE.item(
            "redstone_small_dust", Item::new).register();

    public static void register() {
    }
}
