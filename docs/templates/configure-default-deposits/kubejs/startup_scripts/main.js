// Reconfigure the built-in deposit structure sets without adding any new deposits.
StartupEvents.createRnsStructureSet(event => {
  event.overworld()
    // Omitted deposits will no longer be scannable or generated.
    // When compat mods are active, the compat deposits will also be omitted unless included here.
    .deposit('create_rns:deposit_iron', true)
    .deposit('create_rns:deposit_copper', true)
    .deposit('create_rns:deposit_gold', false)
    // Average distance between deposits in chunks
    .spacing(32)
    // Minimum distance between deposits in chunks
    .separation(6)

  event.nether()
    .deposit('create_rns:deposit_nether_gold', true)
    // Nethe quartz stays scannable but is no longer added to worldgen.
    .deposit('create_rns:deposit_nether_quartz', false)
    .spacing(10)
    .separation(3)
})
