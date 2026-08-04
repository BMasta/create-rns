## Checklist
1. Add new deposit blocks and structures in `kubejs/startup_scripts/main.js`
2. In `StartupEvents.createRnsStructureSet(...)`, decide which deposits should be scanner-only and which should also generate in the world
3. Add/replace a mining recipe for each deposit in `kubejs/server_scripts/main.js`
4. Add a texture for each new deposit block in `assets/<your namespace>/textures/block/<your block>.png`.
For a deposit block `mymod:netherite_deposit`, the path would be `kubejs/assets/mymod/textures/block/netherite_deposit.png`
