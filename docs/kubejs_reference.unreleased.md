# KubeJS Reference - UNRELEASED
## THIS DOCUMENT IS FOR AN UNRELEASED VERSION OF THE MOD
## GO TO THE MAIN PAGE AND SELECT THE RIGHT VERSION

## Notes
* This page only covers the custom KubeJS API added by Create: Rock & Stone.
* Standard KubeJS methods such as `displayName(...)` on normal block builders are not re-documented here.
* Standard KubeJS recipe methods such as `.id(...)` are not re-documented here.
* Working examples can be found [here](templates).

## Adding mining recipes

Create: Rock & Stone adds a custom mining recipe type.

Usage:

```js
ServerEvents.recipes(event => {
  event.recipes.create_rns.mining()
    .id('mymod:tin_deposit')
    .block('mymod:tin_deposit_block')
    .replaceWhenDepleted('create_rns:depleted_deposit_block')
    .durability(2500, 1500, 0.15)
    .yield(y => y
      .item('minecraft:stone'))
    .yield(y => y
      .chance(0.25)
      .item('#c:raw_materials/tin')
      .item('mymod:raw_tin')
      .catalyst('create_rns:overclock'))
})
```

### `event.recipes.create_rns.mining()`
Creates a mining recipe builder.

### `block(blockId)`
Sets the deposit block mined by this recipe.

* `blockId`: id of an existing block tagged with `#create_rns:deposit_blocks`.

### `replaceWhenDepleted(blockId)` (optional)
Sets the block that should replace the deposit once it is depleted. The replacement block does not have to be a deposit block.

* `blockId`: block id.

Default: air

### `durability(core, edge, randomSpread)` (optional)
Sets the deposit durability values used by the mining recipe.

* `core`: positive integer
* `edge`: positive integer
* `randomSpread`: float between `0` and `1`

Default: infinite (even with finite deposits enabled in server config).

### `dimension(id)` (optional)
Sets the preferred mining dimension and JEI category for this recipe.
Define a separate recipe for each dimension that needs different yields.
If a block has no recipe for the current dimension, the miner can fall back to an existing recipe for any dimension.

* `id`: dimension id such as `mymod:moon`

### `overworld()` (optional)
Shortcut for `.dimension('minecraft:overworld')`

### `nether()` (optional)
Shortcut for `.dimension('minecraft:the_nether')`

### `end()` (optional)
Shortcut for `.dimension('minecraft:the_end')`.

## Adding mining recipes (yield builder)

Builds a yield to be used in a mining recipe.

Usage:

```js
.yield(y => y
  .chance(0.25)
  .item('#c:raw_materials/tin')
  .item('mymod:raw_tin')
  .catalyst('create_rns:overclock'))
```

### `yield(callback)`
Adds one yield entry to the recipe. Can be called multiple times to add more yields.
Each yield is independent of other yields.

* `callback`: function that receives a yield builder

### `chance(value)` (optional)
Sets the chance for this yield to roll.

* `value`: decimal between `0` and `1`.

Default: `1`

### `item(id)` / `item(id, weight)`
### `itemStack(id, count)` / `itemStack(id, count, weight)`
Adds an item to this yield. If a tag is provided, picks the first available item with this tag.
When multiple items are added to the same yield, one of them is rolled by weight (items with higher weight are picked more often).

***IMPORTANT!*** Item tags do not have the same level of validation as items.
If you specify a non-existent or empty tag, KubeJS will not report any errors, but the recipe will still be invalidated.

* `id`: item id or item tag such as `minecraft:raw_iron` or `#c:raw_materials/iron`
* `idList`: ordered list of item ids and item tags used as fallbacks; elements are tried one by one until at least one item is resolved
* `count`: integer from `1` to `99` inclusive (default: `1`)
* `weight`: positive integer (default: `1`)

### `catalyst(id)` (optional)
Attaches a catalyst to this yield.
Depending on the catalyst configuration, the effects of a catalyst can be:
1. Disables the yield when not active
2. Multiplies the chance of getting a yield when active
3. Multiplies the number of produced items when active

* `id`: id of an existing catalyst such as `create_rns:overclock`.

### `jeiSlotColor('#rrggbb')` / `jeiSlotColor(argb)` (optional)
Sets the background color shown for this yield in JEI/EMI.

* `'#rrggbb'`: RGB string
* `argb`: 32-bit ARGB integer (the alpha channel is ignored, but must be specified regardless)

Default: regular slot background.

## Adding custom catalysts

Create: Rock & Stone adds a custom catalyst-building event `StartupEvents.rnsCatalysts`.

Usage:

```js
StartupEvents.rnsCatalysts(event => {
  event.create('mymod:superheated_overclock')
    .displayName('§6Superheated Overclock')
    .description('Achieved by attaching six or more resonators and a fluid container filled with lava to a miner.')
    .chanceMultiplier(1.5)
    .countMultiplier(2)
    .optional()
    .displayPriority(1100)
    .representativeItem('minecraft:lava_bucket')
    .hideIfPresent('create_rns:overclock')
    .playWhenActive('create_rns:mining_overclocked_accent')
    .fluid('minecraft:lava', 40)
    .attachment([
      'create_rns:resonator',
      'create_rns:stabilizing_resonator',
      'create_rns:shattering_resonator'
    ], 6)
})
```

### `create(catalystId)`
Creates a catalyst definition.

* `catalystId`: catalyst id such as `mymod:superheated_overclock`

### `fluid(fluidId, amount)`
Adds a fluid consumption requirement.
Can be called multiple times to require multiple fluids.

* `fluidId`: fluid id such as `minecraft:lava`
* `amount`: positive integer amount consumed per mining operation

### `attachment(blockId, count)` / `attachment(blockId)` / `attachment(blockIdList, count)` / `attachment(blockIdList)`
Adds an attachment requirement.
When a list is used, any matching attachment block can contribute towards the required count.
Can be called multiple times to add more independent requirements.

* `blockId`: single block id such as `create_rns:resonator`
* `blockIdList`: list of block ids such as `['create_rns:resonator', 'create_rns:shattering_resonator']`
* `count`: positive integer (default: `1`)

### `displayName(name)` (optional)
Generates the catalyst name lang entry used in JEI/EMI and miner tooltips.
If omitted, make sure to supply the matching `<namespace>.catalyst.<path>.name` lang entry yourself.

* `name`: text shown to the player

### `chanceMultiplier(value)` (optional)
Multiplies the chance of producing a yield while the catalyst is active. The chance is capped at 100%.

* `value`: decimal greater than or equal to `0`

Default: `1`.

### `countMultiplier(value)` (optional)
Multiplies the number of produced items while the catalyst is active.
The number of produced items of the same type is capped at 99.

* `value`: finite decimal greater than or equal to `0` (default `1`)

### `optional()` (optional)
Marks the catalyst as optional. Optional catalysts don't disable a yield when inactive.

Default: required.

### `displayPriority(value)` (optional)
Defines the order in which the catalysts are shown in the miner bearing tooltip and JEI/EMI.
Highest priority catalysts are shown at the bottom. The order will not be stable unless each catalyst has a unique priority.

* `value`: integer

Default: shown at the very bottom.

### `representativeItem(itemId)` (optional)
Attaches a JEI/EMI info page to the specified item with the catalyst description (see [description](#descriptiontext-optional)).
Call multiple times to attach the same description to more than one item.

* `itemId`: item id such as `minecraft:lava_bucket`

### `description(text)` (optional)
Generates the lang entry displayed on the catalyst info page (see [representativeItem](#representativeitemitemid-optional)).
If omitted, make sure to supply the matching `<namespace>.catalyst.<path>.description` lang entry yourself.

* `text`: description text shown to the player

### `hideIfPresent(catalystId)` (optional)
Hides this catalyst from the miner bearing tooltip while the specified catalyst is active.
Useful when having multiple tiers of catalysts. Can be called multiple times to add more catalysts.

* `catalystId`: catalyst id such as `create_rns:resonance`.

### `playWhenActive(soundId)` (optional)
The specified sound event will be continuously played while the catalyst is active.

* `soundId`: sound event id

## Adding new deposit blocks

Create: Rock & Stone adds a custom block builder type: `create_rns:deposit`.

Usage (same as a default block builder):

```js
StartupEvents.registry('block', event => {
  event.create('mymod:tin_deposit_block', 'create_rns:deposit')
    .displayName('Tin Deposit')
})
```

What this builder does:
* Supplies the default block properties used by deposit blocks.
* Automatically tags the block with:
  * `#create_rns:deposit_blocks`
  * `#minecraft:mineable/pickaxe`
  * `#minecraft:needs_diamond_tool`

When using an existing block as deposit block, you must tag it with `#create_rns:deposit_blocks`.

## Adding new deposit structures

Create: Rock & Stone adds a custom structure-building event `StartupEvents.rnsDepositStructures`.

Usage:

```js
StartupEvents.rnsDepositStructures(event => {
  event.create('mymod:deposit_tin')
    .block('mymod:tin_deposit_block')
    .displayName('Tin Deposit')
    .preset('overworld_common')
    .scannerIconMetal('tin')

  event.tweak('create_rns:deposit_iron')
    .height(-16)
    .weight(30)
    .scannerIcon('mymod:refined_iron')
    .scannerIcon('minecraft:raw_iron')
    .mapIcon('minecraft:iron_ingot')
    .biome('#minecraft:is_forest')
    .nbt('create_rns:ore_deposit_small', 70)
    .nbt('create_rns:ore_deposit_medium', 30)
})
```

### `create(structureId)`
Creates a deposit structure.

* `structureId`: string value used as an id for this structure.

### `tweak(structureId)`
Tweaks a built-in deposit structure while inheriting every value that is not changed.
Available built-in structures can be found [here](../src/generated/builtin_packs/with_compat/create_rns_dynamic_data/data/create_rns/worldgen/structure).

* `structureId`: full id of an existing built-in structure, such as `create_rns:deposit_iron` or
  `create_rns:deposit_nether_gold`.

### `block(blockId)`
Sets the deposit block for a new structure. This method is available only on the builder returned by `event.create(...)`.

* `blockId`: id of an existing deposit block to use for this structure.

### `weight(value)`
When generating a deposit, the mod first picks a spot where it should be generated, and then decides which deposit type should be placed.
Deposit structures with a higher weight are selected more often. More formally, assuming 3 total structures with weight `w1`, `w2`, `w3`,
the chance of selecting structure 1 is `w1/(w1+w2+w3)`.

* `value`: positive integer

### `scannerIcon(itemOrTagId)`
Sets an item or an item tag to be rendered in the scanner nixie tube.
Can be called multiple times to add more items as fallbacks in case the original item does not exist.
On a tweaked built-in deposit, the first call will clear the default values.

* `itemOrTagId`: item id or item tag such as `yourmod:raw_tin` or `#c:raw_materials/tin`

### `scannerIconMetal(material)`
Shortcut that expands to:
```js
.scannerIcon("#c:raw_materials/<material>")
.scannerIcon("#c:ores/<material>")
.scannerIcon("#c:ingots/<material>")
.scannerIcon("#c:nuggets/<material>")
```

### `scannerIconGem(material)`
Shortcut that expands to:
```js
.scannerIcon("#c:gems/<material>")
```

### `scannerIconDust(material)`
Shortcut that expands to:
```js
.scannerIcon("#c:dusts/<material>")
```

### `nbt(templateId, weight)`
Adds a structure NBT template to the generated deposit.
Can be called multiple times to specify more templates.
When multiple templates are specified, the chance of picking one is determined by the weight.
On a tweaked built-in structure, the first call will clear the default values.

Default available NBT templates are `create_rns:ore_deposit_small`, `create_rns:ore_deposit_medium`, `create_rns:ore_deposit_large`.

NBT templates with a higher weight are selected more often. More formally, assuming 3 total templates with weight `w1`, `w2`, `w3`,
the chance of selecting template 1 is `w1/(w1+w2+w3)`.

* `templateId`: id of an existing nbt template
* `weight`: positive integer

### `displayName(name)` (optional)
Sets the structure name shown by the deposit scanner.
If omitted, make sure to supply the matching `<namespace>.structure.<path>` lang entry yourself.

* `name`: text that shows up on screen when the deposit is selected in the scanner.

Default: none

### `height(value)` (optional)
Sets the Y offset from the world surface for the deposit structure.
Set to a negative value to have deposits generate underground.

* `value`: height offset as an integer

Default: `-4`.

### `biome(biomeOrTagId)` (optional)
Sets the biome or biome tag in which the deposit structure may generate.
Only one biome or biome tag can be specified. Unless you need a structure-specific biome selector,
prefer using biome() from rnsEnableDeposits, which allows multiple biomes or biome tags.

***IMPORTANT!*** Choose biomes carefully because a structure can generate in any dimension that has them.
Make sure to select ONLY the biomes that correspond to the dimension the deposit is configured for.

***IMPORTANT!*** The existence of modded biomes cannot be properly validated before the datapack is loaded. If you specify
a non-existent modded biome, you will get a non-descriptive datapack load error.

* `biomeOrTagId`: biome id or biome tag such as `minecraft:badlands` or `#minecraft:is_badlands`

By default, structures use the dimension's deposit biome tag. The built-in tags are `#create_rns:has_deposit`,
`#create_rns:has_deposit_nether`, and `#create_rns:has_deposit_end`. When configuring biomes in rnsEnableDeposits,
a similar biome tag is automatically created for you.

### `mapIcon(itemOrTagId)` (optional)
Sets an item or an item tag to be rendered on the map as a marker for found deposits.
Can be called multiple times to add more items as fallbacks in case the original item does not exist.
On a tweaked built-in structure, the first call will clear the default values.

* `itemOrTagId`: item id or item tag such as `yourmod:raw_tin` or `#c:raw_materials/tin`.

Default: the deposit block item attached to this structure.

### `preset(presetId)` (optional)
Shortcut for configuring multiple parameters at once.
The exact values depend on the selected preset. For reference:
* common - iron, copper
* uncommon - lead, nickel
* rare - redstone, zinc

***IMPORTANT!*** The preset name identifies the intended dimension defaults, but does not assign the structure to that dimension.

#### overworld_common/end_common
Expands to:
```js
.weight(50)
.height(-4)
.nbt('create_rns:ore_deposit_medium', 70)
.nbt('create_rns:ore_deposit_large', 30)
```

#### overworld_uncommon/end_uncommon
Expands to:
```js
.weight(35)
.height(-6)
.nbt('create_rns:ore_deposit_small', 30)
.nbt('create_rns:ore_deposit_medium', 60)
.nbt('create_rns:ore_deposit_large', 10)
```

#### overworld_rare/end_rare
Expands to:
```js
.weight(20)
.height(-8)
.nbt('create_rns:ore_deposit_small', 70)
.nbt('create_rns:ore_deposit_medium', 28)
.nbt('create_rns:ore_deposit_large', 2)
```

#### nether_common
Expands to:
```js
.weight(50)
.height(-4)
.nbt('create_rns:ore_deposit_medium', 70)
.nbt('create_rns:ore_deposit_large', 30)
```

#### nether_uncommon
Expands to:
```js
.weight(35)
.height(-4)
.nbt('create_rns:ore_deposit_small', 30)
.nbt('create_rns:ore_deposit_medium', 60)
.nbt('create_rns:ore_deposit_large', 10)
```

#### nether_rare
Expands to:
```js
.weight(20)
.height(-4)
.nbt('create_rns:ore_deposit_small', 70)
.nbt('create_rns:ore_deposit_medium', 28)
.nbt('create_rns:ore_deposit_large', 2)
```

## Configure deposit generation and visibility
Create: Rock & Stone adds a custom deposit-enablement event `StartupEvents.rnsEnableDeposits`.

***IMPORTANT!*** Configuring deposits in a dimension overrides the defaults.
Each unconfigured dimension keeps its defaults.
Disabling built-in deposits also hides the deposit block from the creative menu.

Built-in deposits (including compat) can be found [here](../src/generated/builtin_packs/with_compat/create_rns_dynamic_data/data/create_rns/worldgen/structure).

Usage:

```js
StartupEvents.rnsEnableDeposits(event => {
  event.overworld()
    .deposit('create_rns:deposit_iron', true)
    .deposit('create_rns:deposit_copper', 20)
    .deposit('yourmod:deposit_tin', false)

  event.nether()
    .deposit('create_rns:deposit_nether_gold', 20, true)
})
```

### `dimension(id)` / `dimension(id, placementStrategy)`
Configure deposit selection for the respective dimension.

```js
event.dimension('mymod:moon', 'default')
  .biome('mymod:lunar_plains')
  .biome('#mymod:lunar_highlands')
  .deposit('mymod:deposit_lunar_iron')
  .jeiMiningTitle('Lunar Mining')
```

* `id`: dimension ID for which to configure the deposit selection
* `placementStrategy` controls placement for newly created structures. The Nether uses `nether`, all other dimensions use `default`.

***IMPORTANT!*** When called, replaces the default selection.
Calling it without adding deposits with `deposit()` essentially removes all deposits from the respective dimension.

### `overworld()`
Shortcut for `dimension('minecraft:overworld')`

### `nether()`
Shortcut for `dimension('minecraft:the_nether')`

### `end()`
Shortcut for `dimension('minecraft:the_end')`. End structures use `#create_rns:has_deposit_end`
(containing `#minecraft:is_end`) by default. The generated structure set is `create_rns:end_deposits`.

### `biome(biomeOrTagId)`
Adds a biome or biome tag in which a deposit may generate. Applies to all deposits selected in this dimension.
Can be called multiple times to specify more biomes. Structure-specific biome selectors take precedence if configured.

* `biomeOrTagId`: biome id or biome tag such as `minecraft:badlands` or `#minecraft:is_badlands`

***IMPORTANT!*** Choose biomes carefully because a structure can generate in any dimension that has them.
Make sure to select the biomes that exist ONLY in this dimension.

***IMPORTANT!*** The existence of modded biomes cannot be properly validated before the datapack is loaded. If you specify
a non-existent modded biome, you will get a non-descriptive datapack load error.

Creates a biome tag `#create_rns:has_deposit_<dimension namespace>_<dimension path>`.
Unless configured otherwise, all structures selected in this dimension will automatically use it.

### `deposit(structureId, weight)` / `deposit(structureId, enableWorldgen)` / `deposit(structureId, weight, enableWorldgen)`
Makes deposit scannable in the current dimension. Unless `enableWorldgen` is set to false, also makes it generate in the world.

* `structureId`: ID of a default or custom deposit structure
* `weight`: Overrides the deposit weight (if a worldgen-enabled deposit does not configure its weight, this parameter is required)
* `enableWorldgen`: makes the deposit generate in the world (default: true)

### `spacing(value)` (optional)
Average distance between two deposits in chunks.

* `value`: integer from `1` through `4096`, greater than `separation`

Default: `8` for the Nether and `24` for other dimensions.

### `separation(value)` (optional)
Minimum distance between two deposits in chunks. Must be smaller than `spacing`.

* `value`: integer from `0` up to `spacing - 1`

Default: `2` for the Nether and `4` for other dimensions.

### `jeiMiningTitle(title)` (optional)
Sets the JEI mining category title for this dimension.
The category contains all mining recipes for that dimension.
Creates a `create_rns.jei.title.mining_<dimension namespace>_<dimension_path>` lang entry.

### `salt(value)` (optional)
A "seed" used for calculating the random spread of deposits across the world.

* `value`: non-negative integer

The default salts are `591646342` for the Overworld, `781087034` for the Nether, and `196423253` for the End.
Custom dimensions derive their salt from the dimension ID.
