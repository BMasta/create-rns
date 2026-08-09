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
  event.overworld()
    // Keep the deposits you want. Setting the second argument to false will disable worldgen, but keep the deposit enabled.
    .deposit('create_rns:deposit_iron', true)
    .deposit('create_rns:deposit_copper', true)
    .deposit('create_rns:deposit_zinc', true)
    .deposit('create_rns:deposit_gold', true)
    .deposit('create_rns:deposit_redstone', true)
    .deposit('create_rns:deposit_tin', true)
    .deposit('create_rns:deposit_lead', true)
    .deposit('create_rns:deposit_nickel', true)
    .deposit('create_rns:deposit_silver', true)
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
    .deposit('mymod:deposit_netherite', true)
    .spacing(8)
    .separation(2)
})
