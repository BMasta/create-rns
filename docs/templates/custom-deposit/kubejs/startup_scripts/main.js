// Deposit blocks go here.
StartupEvents.registry('block', event => {
  event.create('mymod:netherite_deposit', 'create_rns:deposit')
    .displayName('Netherite Deposit')
})

// Deposit structures go here. Multiple structures can use the same block.
StartupEvents.rnsDepositStructures(event => {
  event.create('mymod:deposit_netherite')
    .block('mymod:netherite_deposit')
    .displayName('Netherite Deposit')
    .preset('nether_rare')
//    .preset('nether_uncommon')
//    .preset('nether_common')
//    .preset('overworld_rare')
//    .preset('overworld_uncommon')
//    .preset('overworld_common')
    .scannerIcon('minecraft:netherite_scrap')
    .mapIcon('minecraft:ancient_debris')
})

StartupEvents.rnsEnableDeposits(event => {
  // Configure overworld deposits
  event.overworld()
    // Enables scanning and worldgen
    .deposit('create_rns:deposit_iron')
    // Also configures the weight (if previously set in the deposit itself, overrides that value)
    .deposit('create_rns:deposit_copper', 20)
    // Enables scanning, but not worldgen
    .deposit('create_rns:deposit_zinc', false)
    .deposit('create_rns:deposit_gold', 10, false)
    // Omitted deposits will not be scannable and will not generate in the world
    // ...
    // Average distance between deposits in chunks
    .spacing(24)
    // Minimum distance between deposits in chunks
    .separation(4)

  event.nether()
    .deposit('create_rns:deposit_nether_gold', true)
    .deposit('create_rns:deposit_nether_quartz', false)
    .deposit('mymod:deposit_netherite', true)
    .spacing(8)
    .separation(2)
})
