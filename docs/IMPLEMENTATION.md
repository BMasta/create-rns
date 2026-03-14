# Implementation Details
## Mine Head Multiblock
* Player perspective: placing a mine head adjacent to the center block of a valid 3x3 iron-block pattern immediately forms the large mine head variant.
* Player perspective: the large mine head orientation is derived from where the small mine head was placed relative to the iron pattern, and ignores the
  small head's initially placed orientation.
* Edge behavior: if no valid 3x3 iron pattern is found (or placement is not adjacent to the pattern center), the mine head remains small.
* Core behavior: large mine heads are represented by one controller block plus invisible part blocks that reserve all occupied positions.
* Core behavior: formation consumes/replaces the iron pattern blocks and converts the originally placed mine head position into part occupancy when needed.
* Core behavior: if multiple candidate iron patterns are simultaneously valid around the placed mine head, formation is rejected to avoid ambiguous orientation.
* Core behavior: breaking any block in a large mine head immediately reconstructs the whole structure into its prerequisite layout
  (3x3 iron base + small mine head tip).
* Edge behavior: the first break on a large mine head only performs reconstruction; it does not also break any reconstructed block.
* Edge behavior: after reconstruction, subsequent breaks behave as normal block breaks on the resulting iron blocks and small mine head.
* System interaction: part blocks continue to proxy interaction/destruction to the controller, and mine head multiblock occupancy remains compatible with
  contraption assembly/movement rules.
* Gameplay outcome: mine head size affects mining footprint (small = no radius bonus, large = +1 radius), so pattern-based formation directly impacts miner range.
* Data and assets: formation logic is code-defined (not datapack-driven); large-head visuals and culling behavior remain model/renderer-driven.
* Maintenance invariant: controller/part occupancy and facing derivation from placement position must stay consistent so assembly, rendering, and drops remain stable.
* Known limitation: command/worldedit-style direct edits are allowed to partially damage mine head multiblocks; this is an accepted non-gameplay edge case.

## Miner Resonance Attachments (Contraption Composition and Mining Footprint)
* Player perspective: resonance attachments are added by placing resonators and resonance buffers on the miner contraption.
* Player perspective: resonators are face-attached components that can be placed on floor, wall, or ceiling surfaces.
* Player perspective: resonance buffers are support components that increase resonator capacity while reducing mining footprint.
* Gameplay outcome: attachment layout controls both catalyst availability and effective mining area, so contraption design
  directly affects throughput and coverage tradeoffs.
* Core behavior: contraption assembly requires exactly one mine head; resonance attachments are counted during assembly validation.
* Core behavior: resonator cap is `BASE_RESONATOR_LIMIT + buffer_count`; assembly fails when resonator count exceeds this cap.
* Core behavior: buffer count has an independent assembly cap; assembly fails when that cap is exceeded.
* Core behavior: mining radius starts from server config, then applies mine head size bonus and a single buffer penalty when at least
  one buffer is present.
* Edge behavior: additional buffers increase resonator capacity but do not stack further mining-radius penalties.
* Edge behavior: changing mine head size or resonance attachment composition causes the miner spec to refresh so claim area and mining
  behavior stay aligned with current contraption state.
* System interaction: resonance, shattering resonance, and stabilizing resonance catalysts are detected from contraption block composition.
* System interaction: fluid overclock catalyst handling composes with resonance catalysts on the same contraption.
* Data and assets: attachment behavior is code-defined; visuals are driven by models, textures, and partial models.
* Maintenance invariant: miner assembly and mining-area calculations must treat resonators and resonance buffers as the authoritative
  resonance attachment set.
* Maintenance invariant: mine head size bonus and buffer penalty rules define expected mining footprint scaling and should stay aligned
  with player-facing tooltip/guide text.

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
* System interaction: scanner structure-search region radius is inferred from the structure set spacing value computed on server startup from the loaded deposit structure-set placement.
* Data and assets: target types are data-driven via `deposit_spec` datapack entries (`scanner_icon_item` -> `structure`), not hardcoded.
* Data and assets: scanner visuals/sounds are client assets; authoritative target selection and found-state mutation are server-side.
* Maintenance invariant: tracking depends on a prior discovery cache entry and should remain cheap (no structure search per ping).
* Known limitation: request handling assumes selected scanner icon maps to a valid deposit spec; invalid icon payloads are not defensively handled.

## Deposit World Generation (Structure-Based Deposits)
* Player perspective: deposits appear underground as worldgen structures in supported biomes and are intended to be discovered, then mined in place.
* Player perspective: different deposit types do not appear equally often, and each type can appear in multiple template variants, so world distribution feels varied.
* Gameplay outcome: world seed and exploration route meaningfully affect which deposit types players find first, influencing mining progression and factory planning.
* Core behavior: deposit generation is driven by structure-based worldgen with a shared placement policy plus per-type relative weighting.
* Core behavior: the shared placement policy (including spread cadence controls) is configured through default registration definitions and emitted into the built-in pack output.
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
* Known limitation: default spawn tuning is authored in code-backed built-in-pack definitions, so balancing changes currently require updating code inputs or overriding via datapack.
