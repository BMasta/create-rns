// Tweak deposit structure parameters.
StartupEvents.rnsDepositStructures(event => {
  event.tweak('create_rns:deposit_iron')
    .scannerIcon("minecraft:iron_ore")
    // common - iron, copper
    // uncommon - lead, nickel
    // rare - redstone, zinc
    .preset('overworld_rare')
//    .preset('overworld_uncommon')
//    .preset('overworld_common')
//    .preset('nether_rare')
//    .preset('nether_uncommon')
//    .preset('nether_common')
})

// Select which deposits should be enabled and/or generated, as well as how frequently
StartupEvents.rnsEnableDeposits(event => {
  // IMPORTANT! Simply calling overworld() will override the default config and disable all deposits in the overworld.
  event.overworld()
    // Omitted deposits will no longer be scannable or generated.
    .deposit('create_rns:deposit_iron', true)
    .deposit('create_rns:deposit_copper', true)
    // Setting second argument to false will disable worldgen, but keep the deposit enabled.
    .deposit('create_rns:deposit_gold', false)
    .deposit('create_rns:deposit_nickel', true)
    // Average distance between deposits in chunks
    .spacing(32)
    // Minimum distance between deposits in chunks
    .separation(6)

  event.nether()
    .deposit('create_rns:deposit_nether_gold', true)
    .deposit('create_rns:deposit_nether_quartz', false)
    .spacing(10)
    .separation(3)
})
