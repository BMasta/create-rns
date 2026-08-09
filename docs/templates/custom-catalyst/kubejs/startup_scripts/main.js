StartupEvents.rnsCatalysts(event => {
  event.create('mymod:superheated_overclock')
    .displayName('Superheated Overclock')
    // A JEI tab with your description will be generated for the representative item.
    .representativeItem('minecraft:lava_bucket')
    .description('Achieved by attaching 6 netherite blocks and supplying the mining contraption with lava.')
    // Multiply the chance of rolling a successful yield by 1.5.
    .chanceMultiplier(1.5)
    // An optional catalyst will only influence the chance of getting a yield.
    .optional()
    // Controls the order in which catalysts are displayed in the miner bearing tooltip and in JEI.
    // See the priorities for the default catalysts here:
    // src/main/resources/data/create_rns/create_rns/catalyst
    .displayPriority(1100)
    // Hides this catalyst in the miner bearing tooltip if the specified catalyst is also active.
    // Useful when having different tiers of catalysts.
//  .hideIfPresent('mymod:super_mega_heated_overclock')
    // Continuously plays the specified sound event when the catalyst is active.
    .playWhenActive('create_rns:mining_overclocked_accent')
    // Consumes 40 mB of lava on every cycle.
    // Can be called multiple times to add more requirements.
    .fluid('minecraft:lava', 40)
    // Becomes active if 6 netherite blocks are attached to the mining contraption.
    // Can be called multiple times to add more requirements.
    // Referenced blocks are automatically added to #create_rns:miner_attachments.
    .attachment('minecraft:netherite_block', 6)
})
