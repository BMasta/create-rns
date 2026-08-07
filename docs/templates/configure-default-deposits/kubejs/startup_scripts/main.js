// Reconfigure the built-in deposit structure sets without adding any new deposits.
StartupEvents.createRnsStructureSet(event => {
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
