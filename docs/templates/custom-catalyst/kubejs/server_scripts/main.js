//
// For more info on mining recipe configuration, see the "tweak-mining-recipe" template.
//

ServerEvents.recipes(event => {
  // Remove the built-in recipe before adding yours.
  event.remove({ id: 'create_rns:iron_deposit_block' })

  event.recipes.create_rns.mining()
    .block('create_rns:iron_deposit_block')
    .id('create_rns:iron_deposit_block')
    .replaceWhenDepleted('create_rns:depleted_deposit_block')
    // This yield only rolls when the custom catalyst is active.
    .yield(y => y
      .item('minecraft:raw_iron_block')
      .catalyst('mymod:superheated_overclock')
      .jeiSlotColor('#BF8A5A'))
})
