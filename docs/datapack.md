# Creating a Datapack

This document covers what parts of Create: Rock & Stone are datapack-driven and can be overridden or extended.

## What You Can Override
* Mining recipes (mining rewards, resource uses for finite deposits, what to replace with when depleted, how recipe is displayed in JEI).
* Catalysts (custom requirements for mining certain kinds of items from a deposit).
* Deposit world generation (spawn rate, structure nbt, deposit scanner icon, map overlay icon)

## Default Configuration

* Most resources are located in `src/main/resources` and `src/generated/resources`.
* Default mining recipes, deposit specs, and worldgen resources come from the mod's main built-in (in-memory) datapack.
* Built-in datapacks are created dynamically based on which compatible mods are loaded.
* Dumps of datapacks created when no compatible mods are loaded can be found in `src/generated/builtin_packs/default`.
* Dumps of datapacks created when all compatible mods are loaded can be found in `src/generated/builtin_packs/with_compat`.
* Code-registered compat mining recipes follow the same dump split as other dynamic content: `default` excludes them and `with_compat` includes them.

## Custom Deposit Spawn Rate
Deposit spawn rate is influenced by these parameters:
* Spacing (configured in structure set) - the average distance in chunks between two structures.
* Separation (configured in structure set) - the minimum distance in chunks between two structures.
* Frequency (configured in structure set) - the probability to spawn a structure if other requirements are met.
* Biomes (configured in a structure or by modifying the `#create_rns:has_deposit` biome tag) - some biomes may be excluded, which will result in less frequent spawns.

See https://minecraft.fandom.com/wiki/Custom_structure for more details.

Easiest solution that works in most cases - copy the default structure set from
`src/generated/builtin_packs/with_compat/create_rns_dynamic_data/data/create_rns/worldgen/structure_set/deposits.json`
and tweak the separation and spacing.

## Mining Recipe

Path: `data/your_pack/recipe/tin_deposit_block.json`

See `src/generated/builtin_packs/with_compat/create_rns_dynamic_data/data/create_rns/recipe` for examples.

Default mining recipes shipped by the mod are authored in code and emitted into the mod's main built-in datapack.
The runtime format is still the same JSON shown below, and datapacks can override the recipe by providing a file with the same recipe id.

```json5
{
  "type": "create_rns:mining",
  // Deposit blocks must be tagged with #create_rns:deposit_blocks to work properly.
  // Only one recipe per block is allowed.
  "deposit_block": "your_pack:tin_deposit_block",
  // Optional (default minecraft:air).
  // When finite deposits are enabled and the deposit block runs out of resources, it will be
  // replaced with this block. This block does not have to be tagged as a deposit block.
  "replace_when_depleted": "create_rns:depleted_deposit_block",
  // Optional (default infinite).
  // When finite deposits are enabled, how many times can a miner mine this block
  // before it gets depleted.
  "durability": {
    // How many resources will be assigned to center blocks on average.
    "core": 120000,
    // How many resources will be assigned to outer blocks on average.
    "edge": 45000,
    // How much can the value deviate from the average. 0.2 is 20%.
    "random_spread": 0.2
  },
  // Each yield has a chance of being successful every time a miner finishes mining.
  // When a yield is successful, a random item in its pool is selected as a reward.
  "yields": [
    // Yield cobblestone on every mine.
    {
      "items": [
        { "item": "minecraft:cobblestone" }
      ]
    },
    {
      // Optional (default 100%).
      "chance": 0.35,
      "items": [
        {
          "item": "your_pack:raw_tin",
          // Optional (default 1)
          // Will be selected 6/(6+2+2)*100% = 60% of the time.
          "weight": 6
        },
        {
          // When tag is specified, the first item in the tag in selected.
          "item": "#forge:raw_materials/raw_tin",
          "weight": 2
        },
        {
          // Optional (default false).
          // Whether to suppress errors if item fails to resolve.
          // Useful for compat items that may or may not be present.
          "compat":  true,
          // Can also be a list of items and item tags. The item is resolved in the order the entries are specified.
          "item": ["your_pack:raw_tin", "#forge:raw_rich_tin", "minecraft:raw_iron"],
          "weight": 2
        },
      ],
      // Optional (default none).
      // Catalysts are specific requirements needed to mine this yield.
      // In some cases, each mine of a yield will consume a resource like lava.
      // Catalysts are defined separately (see example later in the doc) and can be referenced by name.
      "catalysts": ["overclock", "faint_resonance"],
      // Optional (default is standard background).
      // All items that belong to this yield will have this background color in JEI
      // The number can be obtained by converting an ARGB hex color into a decimal number.
      // Even though the expected value is ARGB, the alpha channel will be ignored.
      "jei_slot_color": -6910797
    }
  ]
}
```

## Catalyst

Path: `data/your_pack/create_rns/catalyst/ultimate_resonance.json`

See `src/main/resources/data/create_rns/create_rns/catalyst` for examples.

```json5
{
  // Used to reference this overclock in mining recipes.
  "name": "i_am_rich",
  // Optional (default 100% aka no change).
  // When catalyst is active, the chance to mine a yield will be multiplied by this value.
  "chance_multiplier": 2.5,
  // Optional (default false).
  "optional": true,
  // Optional (will be shown last by default).
  // Used to sort catalysts. Lower values appear first.
  "display_priority": 1004,
  // Optional (default none).
  // Bind specified items to this catalyst on the catalyst info JEI tab.
  "representative_items": ["minecraft:netherite_block", "minecraft:lava_bucket"],
  // If any catalyst specified here is active, this catalyst will be hidden
  // in the goggle tooltip of the miner bearing.
  "hide_if_present": ["i_am_ultra_rich"],
  // List of requirements that must be met to activate this catalyst.
  // Must contain at least one entry.
  "requirements": [
    {
      "type": "fluid",
      "consume": {
        "FluidName": "minecraft:lava",
        // Will be consumed on every mine operation.
        "Amount": 40
      }
    },
    {
      "type": "attachment",
      // Can be a block, a list of blocks, or a block tag.
      // Each block must  be tagged with create_rns:miner_attachments to work properly.
      "attachment": "minecraft:netherite_block",
      // How many attachment blocks activate this catalyst.
      "count": 16
    }
  ]
}
```

## Adding a New Deposit Structure

### 1. Deposit Spec

Path: `data/your_pack/create_rns/deposit_spec/tin.json`

See `src/generated/builtin_packs/with_compat/create_rns_dynamic_data/data/create_rns/create_rns/deposit_spec` for examples.

```json5
{
  // Binds the spec to a structure id used by scanner discovery and display naming.
  // Lang entry your_pack.structure.deposit_tin is used as the structure name.
  "structure": "your_pack:deposit_tin",
  // Item/block to render on a map for found deposits (if a map mod is installed).
  "map_icon_item": "your_pack:tin_deposit_block",
  // Either field is optional, but at least one item or tag must be specified.
  // Item/Block to render when this deposit structure is selected in deposit scanner.
  // Item candidates are tried in listed order until an existing item is found.
  // Tag candidates are tried after items, in listed order. The first item in a tag becomes the scanner icon.
  "scanner_icon_item_candidates": ["your_pack:raw_tin"],
  "scanner_icon_tag_candidates": ["forge:raw_materials/tin", "forge:ores/tin"],
}
```

### 2. Structure

Path: `data/your_pack/worldgen/structure/deposit_tin.json`.

See `src/generated/builtin_packs/with_compat/create_rns_dynamic_data/data/create_rns/worldgen/structure` for examples.

```json5
{
  "type": "create_rns:deposit",
  // This mod ships two biome tags: "has_deposit" and "has_deposit_nether", but any biome or biome tag is accepted.
  "biomes": "#create_rns:has_deposit",
  // Should be either "overworld" or "nether". This affects how deposits are placed in a target chunk.
  "placement_strategy": "overworld",
  // Y offset from the ground level to the top of the deposit.
  "height": -8,
  // or..
  "height": {
    "max": -8,
    "min": -12,
  },
  // List of structure NBTs.
  "structures": [
    {
      "id": "create_rns:ore_deposit_medium",
      "weight": 70,
      // Deposit NBTs shipped with this mod are generic.
      // Processors allow you to replace the placeholder block to your deposit block of choice.
      // If your custom deposit already uses the desired blocks, the processor can be skipped.
      "processor": "your_pack:replace_with_your_pack_tin_deposit_block"
    },
    {
      "id": "create_rns:ore_deposit_large",
      "weight": 30,
      "processor": "create_rns:replace_with_your_pack_tin_deposit_block"
    }
  ]
}
```

### 3. Processor
The deposit nbt's contain deposits made of end stone.
To convert it to your deposit blocks of choice, a custom processor is needed.

Path: `data/your_pack/worldgen/processor_list/replace_with_your_pack_tin_deposit_block.json`

See `src/generated/builtin_packs/with_compat/create_rns_dynamic_data/data/create_rns/worldgen/processor_list` for examples.


### 4. Structure Set

Path: `data/your_pack/worldgen/structure_set/deposits.json`.

The `deposits` structure set contains all deposit structures so they share the same distribution logic.
`structure_set` files do not support partial appends (`replace: false` style behavior).

There are 2 ways to define structure sets for custom deposits:

1. Override `create_rns:deposits` (recommended for modpacks).
Copy the default file and add all extra deposit structures from your modpack to its `structures` array.

2. Create your own structure set (recommended when pack priority is uncertain).
If other datapacks may also override `create_rns:deposits`, define a separate structure set in your own namespace/path and put your custom deposits there.

See `src/generated/builtin_packs/with_compat/create_rns_dynamic_data/data/create_rns/worldgen/structure_set/deposits.json`
for the default set.

### 5. Structure Tag

Path: `data/create_rns/tags/worldgen/structure/deposits.json`

See `src/generated/builtin_packs/with_compat/create_rns_dynamic_data/data/create_rns/tags/worldgen/structure/deposits.json` for the default tag.

```json5
{
  "replace": false,
  "values": [
    // Name of your structure
    "your_pack:deposit_tin"
  ]
}
```
