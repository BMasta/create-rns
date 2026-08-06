// You can assign common yields to a variable and use them in multiple places.
const debrisYield = y => y
    .chance(0.002)
    .item('minecraft:ancient_debris')
    .catalyst('create_rns:resonance')
    .catalyst('create_rns:overclock')
    .jeiSlotColor("#8572BF")

ServerEvents.recipes(event => {
  event.recipes.create_rns.mining()
    // Deposit block to which this recipe applies. Must be a single block.
    .block('mymod:netherite_deposit')
    // Recipe only works in overworld if omitted.
    .nether()
    .replaceWhenDepleted('create_rns:depleted_deposit_block')
    // Applicable only if finite deposits are enabled in server config.
    // Goes from 200000 +-20% uses in the horizontal center of a vein and down to 75000 +-20% on the edges.
    .durability(200000, 75000, 0.2)
    // Each yield is independent of other yields. This will produce 1 netherrack on each cycle.
    .yield(y => y
        .item('minecraft:netherrack'))
    // Drops soul sand with a 50% chance, soul soil with a 40% chance, and magma block with a 10% chance on each cycle.
    .yield(y => y
        .item('minecraft:soul_sand', 5)
        .item('minecraft:soul_soil', 4)
        .item('minecraft:magma_block'))
    // 0.05% chance to get a resonant amethyst.
    .yield(y => y
        .chance(5.0e-4)
        .item('create_rns:resonant_amethyst')
        .catalyst('create_rns:overclock'))
    // 1% to get netherite scrap when both catalysts are active.
    .yield(y => y
        .chance(0.01)
        .item('minecraft:netherite_scrap')
        .catalyst('create_rns:faint_resonance')
        .catalyst('create_rns:overclock')
        .jeiSlotColor("#968CB3"))
    // Empty yields are ignored
    .yield()
    // Compat items are allowed to reference non-existent item ids without invalidating the recipe.
    .yield(y => y
        .chance(5.0e-4)
        .compatItem('minecraft:glooperblooper')
        .item('create_rns:resonant_amethyst')
        .catalyst('create_rns:resonance')
        .catalyst('create_rns:overclock')
        .jeiSlotColor("#8572BF"))
    // Using a variable defined at the top.
    .yield(debrisYield)
    .yield(y => y
        .item('minecraft:blackstone')
        .item('minecraft:basalt')
        .item('minecraft:smooth_basalt')
        .catalyst('create_rns:faint_shattering_resonance')
        .jeiSlotColor("#B28F8E"))
    .yield(y => y
        .chance(0.2)
        .item('create:crimsite')
        .item('create:veridium')
        .item('create:asurine')
        .item('create:ochrum')
        .catalyst('create_rns:shattering_resonance')
        .catalyst('create_rns:overclock')
        .jeiSlotColor("#BF7672"))
    .yield(y => y
        .chance(0.1)
        .item('minecraft:quartz')
        .item('minecraft:glowstone_dust')
        .item('minecraft:amethyst_shard')
        .catalyst('create_rns:faint_stabilizing_resonance')
        .catalyst('create_rns:overclock')
        .jeiSlotColor("#8EA9B2"))
    .yield(y => y
        .chance(0.02)
        .item('minecraft:netherite_scrap', 5)
        .item('minecraft:ancient_debris')
        .catalyst('create_rns:stabilizing_resonance')
        .catalyst('create_rns:overclock')
        .jeiSlotColor("#72ACBF"))
    .id('mymod:netherite_deposit')
})
