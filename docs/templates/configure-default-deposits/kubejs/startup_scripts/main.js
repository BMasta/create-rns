/*
// Tweak deposit structure parameters.
StartupEvents.rnsDepositStructures(event => {
  event.tweak('create_rns:deposit_tin')
    .scannerIcon("some_mod:raw_tin")
    // common - iron, copper
    // uncommon - lead, nickel
    // rare - redstone, zinc
    .preset('overworld_rare')
    .preset('overworld_uncommon')
    .preset('overworld_common')
    .preset('nether_rare')
    .preset('nether_uncommon')
    .preset('nether_common')
    .biome("old_growth_birch_forest")
})
*/

StartupEvents.rnsEnableDeposits(event => {
  event.overworld()
    // Keep the deposits you want. Setting the second argument to false will disable worldgen, but keep the deposit enabled.
    .deposit('create_rns:deposit_coal', true)
    .deposit('create_rns:deposit_iron', true)
    .deposit('create_rns:deposit_copper', true)
    .deposit('create_rns:deposit_zinc', true)
    .deposit('create_rns:deposit_gold', true)
    .deposit('create_rns:deposit_lapis', true)
    .deposit('create_rns:deposit_redstone', true)
    .deposit('create_rns:deposit_tin', true)
    .deposit('create_rns:deposit_osmium', true)
    .deposit('create_rns:deposit_lead', true)
    .deposit('create_rns:deposit_nickel', true)
    .deposit('create_rns:deposit_silver', true)
    .deposit('create_rns:deposit_platinum', true)
    .deposit('create_rns:deposit_uranium', true)
    .deposit('create_rns:deposit_thorium', true)
    // Average distance between deposits in chunks
    .spacing(24)
    // Minimum distance between deposits in chunks
    .separation(4)

  event.nether()
    .deposit('create_rns:deposit_nether_gold', true)
    .deposit('create_rns:deposit_nether_quartz', true)
    .deposit('create_rns:deposit_nether_cobalt', true)
    .deposit('create_rns:deposit_nether_wolframite', true)
    .spacing(8)
    .separation(2)
})
