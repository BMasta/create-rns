# TO DO
- [ ] Implement a create mod bearing for a multiblock contraption that acts as a drill.
  - [ ] Where does the yield go? If it goes into the contraption inventory, the item extraction becomes awkward and unnatural.
  - [ ] How would the drill look? What shape? Which blocks would it be built from?
- [ ] Integration with map mods.
  - [ ] Record visited deposits per-player into persistent storage.
  - [ ] Show scanned deposits as icons on the map.
- [ ] Add deposits for metals/gems from other mods.
- [ ] Store deposits known to player on client side. Sync with server for accurate scanning.
- [ ] Port to Fabric.
- [ ] Allow veins to be finite.
  - [ ] Add server config options to toggle between finite/infinite deposits as well as setting specific durabilities.
  - [ ] Show cumulative deposit durabilities in miner tooltip.
  - [ ] Jade integration to show individual deposit durabilities.
- [ ] Add config option to disable miners' ability to eject items. If miners are made data-driven,
      add this knob to the spec (with config option being a blanket override to disable ejecting for all miners).
      
