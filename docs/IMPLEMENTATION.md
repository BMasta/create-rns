# Implementation Details
## Drill Head Multiblock
* Drill heads can be upgraded into larger multiblock variants.
* Larger drill variants are composed of a main controller block plus invisible part blocks that provide physical occupancy.
* Upgrading is performed by applying additional drill head items and validates whether surrounding space can be reserved for the larger structure.
* Growth logic supports expanding in alternate directions when needed so the full structure can still fit.
* Part blocks act as extensions of the controller for interaction and destruction behavior.
* Breaking a drill returns resources based on how much was invested into its current size.
* Drill multiblock behavior is integrated with contraption assembly in a general way so controller and part blocks move together without requiring glue on every part.
* Oversized drill rendering is handled as a scaled/offset single model, with culling bounds expanded to prevent disappearing visuals when only part of the model is on screen.
* Drill part blocks are configured to avoid unintended light occlusion artifacts.

## Deposit Scanner (Server-Driven Discovery and Tracking)
* Player perspective: right-click starts discovery for the currently selected deposit type, and right-click again cancels tracking.
* Player perspective: sneaking + scroll cycles deposit types; selection is shown by the icon in the scanner and by a structure name toast.
* Player perspective: once discovery succeeds, scanner pings guide the player to the target deposit; faster pings mean closer range.
* Player perspective: antenna state communicates direction (left/right/aligned), and reaching close range completes discovery.
* Gameplay outcome: once a deposit is found, it is marked as discovered and excluded from normal scanner discovery results until reset by admin tooling.
* Core behavior: discovery requests perform nearest-target resolution and write a per-player cached target; tracking requests only use the cached target.
* Core behavior: server-side nearest-target computation is rate-limited; rapid repeated discovery attempts can intentionally return no target.
* Core behavior: scanner targets may initially be ungenerated structure starts and are promoted to generated structure centers once chunks load.
* Edge behavior: if cached target type no longer matches selection, or the player moves outside scan range, tracking stops and must be rediscovered.
* System interaction: scanner state depends on `LevelDepositData` level attachment, including generated/ungenerated/found sets and per-player cache.
* System interaction: chunk-load structure detection continuously populates scanner targets, and `/rns scanner` commands can add/remove targets or override found state.
* System interaction: scanner discovery can run under a scanner-only locate context consumed by a `ChunkGenerator` mixin hook, allowing
  structure candidates to be filtered (for example, already found deposits) without changing vanilla locate traversal order.
* Data and assets: target types are data-driven via `deposit_spec` datapack entries (`scanner_icon_item` -> `structure`), not hardcoded.
* Data and assets: scanner visuals/sounds are client assets; authoritative target selection and found-state mutation are server-side.
* Maintenance invariant: scan radius and ping scaling are coupled to deposit worldgen spacing constants; changing spacing changes scanner behavior.
* Maintenance invariant: tracking depends on a prior discovery cache entry and should remain cheap (no structure search per ping).
* Known limitation: request handling assumes selected scanner icon maps to a valid deposit spec; invalid icon payloads are not defensively handled.
