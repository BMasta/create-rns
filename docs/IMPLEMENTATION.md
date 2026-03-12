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
* Edge behavior: for a given deposit type, scanner targets are unique per chunk; admin-added custom targets are rejected if that chunk already
  contains a same-type structure start or custom target.
* Edge behavior: if cached target type no longer matches selection, or the player moves outside scan range, tracking stops and must be rediscovered.
* System interaction: scanner state depends on `LevelDepositData` level attachment, including generated/ungenerated/found sets and per-player cache.
* System interaction: chunk-load structure detection continuously populates scanner targets, and `/rns scanner` commands can add/remove targets or override found state.
* System interaction: scanner command structure arguments are validated against `#create_rns:deposits`; non-deposit structures/tags are rejected.
* System interaction: `/rns scanner found` resolves the nearest target of the specified type within a fixed chunk radius around
  the provided coordinates, then reads/writes that target's found state.
* Player/admin feedback: `/rns scanner found ...` responses include the resolved target coordinates so found-state checks are tied to a concrete target.
* System interaction: scanner discovery can run under a scanner-only locate context consumed by a `ChunkGenerator` mixin hook, allowing
  structure candidates to be filtered (for example, already found deposits) without changing vanilla locate traversal order.
* Data and assets: target types are data-driven via `deposit_spec` datapack entries (`scanner_icon_item` -> `structure`), not hardcoded.
* Data and assets: scanner visuals/sounds are client assets; authoritative target selection and found-state mutation are server-side.
* Maintenance invariant: scan radius and ping scaling are coupled to deposit worldgen spacing constants; changing spacing changes scanner behavior.
* Maintenance invariant: tracking depends on a prior discovery cache entry and should remain cheap (no structure search per ping).
* Known limitation: request handling assumes selected scanner icon maps to a valid deposit spec; invalid icon payloads are not defensively handled.

## Deposit World Generation (Structure-Based Deposits)
* Player perspective: deposits appear underground as worldgen structures in supported biomes and are intended to be discovered, then mined in place.
* Player perspective: different deposit types do not appear equally often, and each type can appear in multiple template variants, so world distribution feels varied.
* Gameplay outcome: world seed and exploration route meaningfully affect which deposit types players find first, influencing mining progression and factory planning.
* Core behavior: deposit generation is driven by structure-based worldgen with a shared placement policy plus per-type relative weighting.
* Core behavior: each deposit type selects from weighted structure templates and applies block replacement rules so shared templates can produce type-specific deposits.
* Core behavior: placement is constrained by biome eligibility and generation step settings, so deposits are injected into terrain generation rather than placed as ad-hoc runtime edits.
* Edge behavior: disabling eligible biomes prevents new deposits from generating while preserving already-generated terrain.
* Edge behavior: changing worldgen data affects newly generated chunks; existing chunks keep their previously generated deposits unless modified by gameplay or admin tools.
* System interaction: scanner discovery and admin locate tooling depend on deposit worldgen structure identity remaining consistent with scanner target definitions.
* System interaction: scanner search behavior assumes the worldgen placement model remains in the same general scale; major placement changes can require scanner tuning.
* System interaction: found-state filtering applies during structure lookup so discovery can ignore already-discovered deposits without changing worldgen itself.
* Data and assets: behavior is data-driven through generated worldgen JSON and structure templates, with additional built-in datapack content controlling biome eligibility.
* Data and assets: deposit type metadata for scanner selection is datapack-driven and must remain aligned with generated structure identities.
* Data and assets: default built-in-pack worldgen entries are sourced from per-deposit registration definitions;
  dump tooling uses an explicit dump-mode bootstrap path to materialize inspectable defaults outside game startup.
* Maintenance invariant: structure templates must keep their agreed placeholder convention so replacement rules can reliably convert template blocks into deposit blocks.
* Maintenance invariant: per-type worldgen entries, structure tags, and scanner target definitions must stay synchronized across code and datapack data.
* Known limitation: default spawn tuning is authored in code-generated data, so balancing changes currently require updating generation inputs or overriding via datapack.
