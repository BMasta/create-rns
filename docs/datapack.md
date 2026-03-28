# Creating a Datapack

This document covers what parts of Create: Rock & Stone are datapack-driven and can be overridden or extended.

## What You Can Override
* Mining recipes (mining rewards, resource uses for finite deposits, what to replace with when depleted, how recipe is displayed in JEI).
* Catalysts (custom requirements for mining certain kinds of items from a deposit).
* Deposit world generation (spawn rate, structure nbt, deposit scanner icon)

## Default Configuration

- Non-worldgen files are located in `src/main/resources`.
- Worldgen files come from built-in (in-memory) datapacks that don't have any associated files, but a dump of what they would contribute can be found in `src/generated/builtin_packs`.

## Custom Deposit Spawn Rate
Deposit spawn rate is influenced by these parameters:
* Spacing (configured in structure set) - the average distance in chunks between two structures.
* Separation (configured in structure set) - the minimum distance in chunks between two structures.
* Frequency (configured in structure set) - the probability to spawn a structure if other requirements are met.
* Biomes (configured in a structure or by modifying the `#create_rns:has_deposit` biome tag) - some biomes may be excluded, which will result in less frequent spawns.

See https://minecraft.fandom.com/wiki/Custom_structure for more details.

Easiest solution that works in most cases - copy the default structure set from
`src/generated/builtin_packs/create_rns_dynamic_data/data/create_rns/worldgen/structure_set/deposits.json`
and tweak the separation and spacing.

## Mining Recipe

Path: `data/your_pack/recipe/tin_deposit_block.json`

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
        // Will be selected 80% of the time.
        {
          "item": "your_pack:raw_tin",
          // Optional (default 1)
          "weight": 4
        },
        // Will be selected 20% of the time.
        {
          "item": "your_pack:raw_rich_tin",
          "weight": 1
        }
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

```json5
{
  // Used to reference this overclock in mining recipes.
  "name": "ultimate_resonance",
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
  "representative_items": ["create_rns:resonator", "minecraft:lava_bucket"],
  // If any catalyst specified here is active, this catalyst will be hidden
  // in the goggle tooltip of the miner bearing.
  "hide_if_present": ["super_mega_ultimate_resonance"],
  // Optional.
  "fluid": {
    "consume": {
      "id": "minecraft:lava",
      // Will be consumed on every mine operation.
      "amount": 40
    },
  },
  // Optional.
  "resonance": {
    // How many resonators of any type activate this catalyst.
    "min_resonators": 4
  },
  // Optional.
  "shattering_resonance": {
    "min_resonators": 4
  },
  // Optional.
  "stabilizing_resonance": {
    "min_resonators": 4
  },
}
```

## Adding a New Deposit Structure

### 1. Deposit Spec

Path: `data/your_pack/create_rns/deposit_spec/tin.json`

```json5
{
  // Binds the specified structure to an item/block icon.
  // Used when selecting a Deposit Scanner target.
  // Lang entry your_pack.structure.deposit_tin is used as a structure name.
  "scanner_icon_item": "your_pack:raw_tin",
  "structure": "your_pack:deposit_tin"
}
```

### 2. Structure

Path: `data/your_pack/worldgen/structure/deposit_tin.json`.

See `src/generated/create_rns_dynamic_data/data/create_rns/worldgen/structure` for examples.

### 3. Template Pool Start

Path: `data/your_pack/worldgen/template_pool/deposit_tin/start.json`.

See `src/generated/create_rns_dynamic_data/data/create_rns/worldgen/template_pool` for examples.

### 4. Processor
The deposit nbt's contain deposits made of end stone.
To convert it to your deposit blocks of choice, a custom processor is needed.

Path: `data/your_pack/worldgen/processor_list/replace_with_your_pack_tin_deposit_block.json`

See `src/generated/create_rns_dynamic_data/data/create_rns/worldgen/processor_list` for examples.


### 5. Structure Set

Path: `data/your_pack/worldgen/structure_set/deposits.json`.

The `deposits` structure set contains all deposit structures so they share the same distribution logic.
`structure_set` files do not support partial appends (`replace: false` style behavior).

There are 2 ways to define structure sets for custom deposits:

1. Override `create_rns:deposits` (recommended for modpacks).
Copy the default file and add all extra deposit structures from your modpack to its `structures` array.

2. Create your own structure set (recommended when pack priority is uncertain).
If other datapacks may also override `create_rns:deposits`, define a separate structure set in your own namespace/path and put your custom deposits there.

See `src/generated/builtin_packs/create_rns_dynamic_data/data/create_rns/worldgen/structure_set/deposits.json`
for the default set.

### 6. Structure Tag

Path: `data/create_rns/tags/worldgen/structure/deposits.json`

See `src/generated/builtin_packs/create_rns_dynamic_data/data/create_rns/tags/worldgen/structure/deposits.json` for the default tag.

```json5
{
  "replace": false,
  "values": [
    // Name of your structure
    "your_pack:deposit_tin"
  ]
}
```
