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

### `overworld()` (optional)
Marks this recipe as an overworld recipe. This is the default dimension unless specified otherwise.
A single recipe can only work in one dimension. Add two recipes if both dimensions are required.

### `nether()` (optional)
Marks this recipe as a nether recipe.
A single recipe can only work in one dimension. Add two recipes if both dimensions are required.

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
Empty yields are ignored. Each yield is independent of other yields.

* `callback`: function that receives a yield builder

### `chance(value)` (optional)
Sets the chance for this yield to roll.

* `value`: decimal between `0` and `1`.

Default: `1`

### `item(itemOrTagId, weight)` / `item(itemOrTagId)` / `item(itemOrTagIdList, weight)` / `item(itemOrTagIdList)`
Adds an item, item tag, or ordered fallback list of item ids and item tags to this yield.
When multiple items are added to the same yield, one of them is rolled by weight.

***IMPORTANT!*** Item tags do not have the same level of validation as items.
If you specify a non-existent or empty tag, KubeJS will not report any errors.

* `itemOrTagId`: item id or item tag such as `minecraft:raw_iron` or `#c:raw_materials/iron`
* `itemOrTagIdList`: ordered list of item ids and item tags used as fallbacks; elements are tried one by one until at least one item is resolved
* `weight`: positive integer (default: `1`)

### `compatItem(itemOrTagId, weight)` / `compatItem(itemOrTagId)` / `compatItem(itemOrTagIdList, weight)` / `compatItem(itemOrTagIdList)`
Adds a compat-gated item, item tag, or ordered fallback list of item ids and item tags to this yield.
If it resolves to nothing at runtime, the entry is discarded instead of making the recipe invalid.

* `itemOrTagId`: item id or item tag
* `itemOrTagIdList`: ordered list of item ids and item tags used as fallbacks; elements are tried one by one until at least one item is resolved
* `weight`: positive integer (default: `1`)

### `catalyst(id)` (optional)
Attaches a catalyst to this yield.
Depending on the catalyst, it may be required or optional, and may modify the chance of getting a yield.

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
Sets the chance multiplier applied while this catalyst is active.

* `value`: decimal greater than or equal to `0`

Default: `1`.

### `optional()` (optional)
Marks the catalyst as optional. Optional catalysts influence the chance of a yield without being strictly required for it.

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
* Uses the proper `DepositBlock` class to create the block.
* Applies the block properties used by built-in deposit blocks.
* Automatically tags the block with:
  * `#create_rns:deposit_blocks`
  * `#minecraft:mineable/pickaxe`
  * `#minecraft:needs_diamond_tool`

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

For `scannerIcon(...)`, `mapIcon(...)`, and `nbt(...)`, the first call clears the defaults.


* `structureId`: full id of an existing built-in structure, such as `create_rns:deposit_iron` or
  `create_rns:deposit_nether_gold`.

### `block(blockId)`
Sets the deposit block used by this deposit structure.

* `blockId`: id of an existing deposit block to use for this structure.

### `weight(value)`
When generating a deposit, the mod first picks a spot where it should be generated, and then decides which deposit type should be placed.
Deposit structures with a higher weight are selected more often. More formally, assuming 3 total structures with weight `w1`, `w2`, `w3`,
the chance of selecting structure 1 is `w1/(w1+w2+w3)`.

* `value`: positive integer

### `scannerIcon(itemOrTagId)`
Sets an item or an item tag to be rendered in the scanner nixie tube.
Can be called multiple times to add more items as fallbacks in case the original item does not exist.
On a tweaked built-in structure, the first call will clear the default values.

* `itemOrTagId`: item id or item tag such as `yourmod:raw_tin` or `#c:raw_materials/tin`

### `scannerIconMetal(material)`
Shortcut that expands to :
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

Default: * `-8` for overworld deposits, `-4` for nether deposits.

### `biome(biomeOrTagId)` (optional)
Sets the biome or biome tag in which the deposit structure may generate.

***IMPORTANT!*** The existence of modded biomes cannot be properly validated before the datapack is loaded. If you specify
a non-existent modded biome, you will get a non-descriptive datapack load error.

* `biomeOrTagId`: biome id or biome tag such as `minecraft:badlands` or `#minecraft:is_badlands`

Default: `#create_rns:has_deposit` for overworld deposits and `#create_rns:has_deposit_nether` for nether deposits.

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

The preset name identifies the intended dimension defaults, but does not assign the structure to that dimension.
Dimension is assigned when the structure is selected through `event.overworld()` or `event.nether()` in
`StartupEvents.rnsEnableDeposits`.

#### overworld_common
Expands to:
```js
.weight(50)
.height(-8)
.nbt('create_rns:ore_deposit_medium', 70)
.nbt('create_rns:ore_deposit_large', 30)
```

#### overworld_uncommon
Expands to:
```js
.weight(35)
.height(-10)
.nbt('create_rns:ore_deposit_small', 30)
.nbt('create_rns:ore_deposit_medium', 60)
.nbt('create_rns:ore_deposit_large', 10)
```

#### overworld_rare
Expands to:
```js
.weight(20)
.height(-12)
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

***IMPORTANT!*** Calling either `event.overworld()` or `event.nether()` puts Create: Rock & Stone into KubeJS-managed deposit mode.
Only the structures selected through these builders stay scannable, only built-in deposit blocks that correspond to them stay visible in the mod's creative tab,
and only built-in mining recipes for those deposit blocks (plus the depleted deposit block) stay enabled.
This applies to default deposits as well. Only the dimensions whose builders are called are overridden; omitted dimensions keep their built-in default deposit selection.

Default deposits (including compat) can be found [here](../src/generated/builtin_packs/with_compat/create_rns_dynamic_data/data/create_rns/worldgen/structure).

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

### `overworld()` / `nether()`
Configure the structure set for the respective dimension.

***IMPORTANT!*** When called, replaces the default structure set.
This means that once called, only the deposits specified with `deposit()` will be enabled in the world.
Calling it without adding deposits with `deposit()` essentially removes all deposits from the respective dimension.

For default deposits, not including them in any dimension will also remove the respective deposit block from the creative menu tab.
The only exception is the depleted deposit block, which is always available.

### `deposit(structureId, weight)` / `deposit(structureId, enableWorldgen)` / `deposit(structureId, weight, enableWorldgen)`
Makes deposit scannable in the current dimension. Unless `enableWorldgen` is set to false, also makes it generate in the world.

* `structureId`: id of a default or custom deposit structure
* `weight`: Overrides the deposit weight (if a deposit does not configure its weight, this parameter is required)
* `enableWorldgen`: makes the deposit generate in the world (default: true)

### `spacing(value)` (optional)
Average distance between two deposits in chunks.

* `value`: positive integer

Default: `24` for `event.overworld()`, `8` for `event.nether()`.

### `separation(value)` (optional)
Minimum distance between two deposits in chunks. Must be smaller than `spacing`.

* `value`: non-negative integer

Default: `4` for `event.overworld()`, `2` for `event.nether()`.

### `salt(value)` (optional)
A "seed" used for calculating the random spread of deposits across the world.

* `value`: integer

Default: `591646342` for `event.overworld()`, `781087034` for `event.nether()`.
