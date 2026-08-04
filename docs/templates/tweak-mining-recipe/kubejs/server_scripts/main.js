// You can assign common yields to a variable and reuse them in multiple places.
 const nuggetYield = y => y
  .chance(0.5)
  .item('minecraft:iron_nugget')
  .catalyst('overclock')

ServerEvents.recipes(event => {
  // Built-in recipe ids can be found in:
  // src/generated/builtin_packs/default/create_rns_dynamic_data/data/create_rns/recipe
  // Remove the shipped recipe before adding your replacement.
  event.remove({ id: 'create_rns:iron_deposit_block' })

  event.recipes.create_rns.mining()
    // Existing deposit block to which this replacement recipe applies. Must be a single block.
    .block('create_rns:iron_deposit_block')
    // Use the original recipe id
    .id('create_rns:iron_deposit_block')
    // Use for nether deposits
    // .nether()
    // Replacement block does not have to be a deposit block.
    // Applicable only if finite deposits are enabled in the server config.
    .replaceWhenDepleted('create_rns:depleted_deposit_block')
    // Goes from 200000 +-20% uses in the horizontal center of the vein and down to 75000 +-20% on the edges.
    // Applicable only if finite deposits are enabled in server config.
    .durability(200000, 75000, 0.2)
    // Each yield is independent of other yields. This will produce 1 cobblestone on each cycle.
    .yield(y => y
        .item('minecraft:cobblestone'))
    // 0.05% chance to get a resonant amethyst while overclocking.
    .yield(y => y
      .chance(5.0e-4)
      .item('create_rns:resonant_amethyst')
      .catalyst('overclock'))
    // Using the variable defined at the top.
    .yield(nuggetYield)
    // Empty yields are ignored.
    .yield()
    .yield(y => y
      .chance(0.05)
      .item('minecraft:raw_iron')
      .catalyst('faint_resonance')
      .catalyst('overclock')
      .jeiSlotColor('#968CB3'))
    // Compat items are allowed to reference non-existent item ids without invalidating the recipe.
    .yield(y => y
        .chance(5.0e-4)
        .compatItem('minecraft:glooperblooper')
        .item('create_rns:resonant_amethyst')
        .catalyst('resonance')
        .catalyst('overclock')
        .jeiSlotColor('#8572BF'))
    .yield(y => y
        .chance(0.005)
        .item('minecraft:raw_iron_block')
        .catalyst('resonance')
        .catalyst('overclock')
        .jeiSlotColor('#8572BF'))
    // Weight defaults to 1 when not specified.
    // Drops tuff with a 60% chance (3/3+1+1), calcite and limestone with a 20% chance (1/3+1+1).
    .yield(y => y
        .item('minecraft:tuff', 3)
        .item('minecraft:calcite')
        .item('create:limestone')
        .catalyst('faint_shattering_resonance')
        .jeiSlotColor('#B28F8E'))
    .yield(y => y
        .chance(0.3)
        .item('create:crimsite')
        .item('create:veridium')
        .item('create:asurine')
        .item('create:ochrum')
        .catalyst('shattering_resonance')
        .catalyst('overclock')
        .jeiSlotColor('#BF7672'))
    .yield(y => y
        .chance(0.15)
        .item('minecraft:lapis_lazuli')
        .item('minecraft:amethyst_shard')
        .item('minecraft:emerald')
        .catalyst('faint_stabilizing_resonance')
        .catalyst('overclock')
        .jeiSlotColor('#8EA9B2'))
    .yield(y => y
        .chance(0.06)
        .item('minecraft:redstone', 5)
        .item('minecraft:diamond')
        .catalyst('stabilizing_resonance')
        .catalyst('overclock')
        .jeiSlotColor('#72ACBF'))
})
