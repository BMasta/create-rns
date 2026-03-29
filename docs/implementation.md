# Implementation Details
## Recipe Viewer Compat (JEI and EMI)
* Player perspective: when JEI is installed on the client, mining recipes appear in a dedicated Mining category and catalyst descriptions appear as ingredient info pages on representative catalyst items instead of in a separate category.
* Player perspective: when EMI is installed alongside JEI, EMI mirrors the mining category through its JEI bridge and converts JEI catalyst info pages into native EMI info recipes rather than relying on JEI extra widgets.
* Player perspective: mining recipe pages show the mined deposit block, the possible outputs, and tooltip details explaining catalyst requirements plus group/item chance behavior.
* Player perspective: resonance catalyst pages are attached to the relevant resonator blocks, while the fluid overclock page is attached to lava bucket as the representative lookup item for that fluid-only catalyst.
* Edge behavior: when infinite deposits are enabled, the depleted-deposit mining recipe is hidden from both recipe viewers so the client UI matches current gameplay rules.
* Core behavior: the mod ships a JEI plugin only, and EMI support is achieved by keeping that JEI content compatible with EMI's JEI bridge.
* Core behavior: when EMI is loaded, the mining category stops relying on JEI's scroll-grid widget and instead assigns fixed slot positions so EMI's bridged slot overlay stays aligned.
* Core behavior: the bridged EMI path may also use a different JEI background height and animated miner scale than plain JEI so the recipe stays inside EMI's available panel area, and the animated miner scales around a fixed assembly pivot so size changes do not shift it unpredictably.
* Core behavior: mining output entries preserve per-yield background coloring and stochastic tooltip semantics across JEI and bridged EMI views.
* Core behavior: catalyst info pages are derived from `CatalystRequirementSet` registry entries sorted by `display_priority`, and each entry is mapped to one or more representative ingredients based on the catalyst requirement type.
* System interaction: JEI and bridged EMI both depend on the same mining recipe type, catalyst registry entries, and server config state, so display filtering and ordering stay aligned.
* System interaction: viewer categories are workbench-style discovery surfaces only; they do not change mining logic, catalyst activation, or datapack loading behavior.
* Data and assets: mining and catalyst content still comes from runtime recipe JSON and datapack registries, while JEI layout/render code remains code-defined.
* Maintenance invariant: EMI-specific behavior should stay inside the JEI compat path unless the project intentionally reintroduces a separate native EMI plugin.
* Maintenance invariant: catalyst viewer text should remain sourced from JEI/EMI info pages instead of JEI extra-text widgets, because EMI's JEI bridge does not reliably preserve those widgets for custom categories.
* Known limitation: the bridge-safe EMI fallback is a fixed `3 x 6` grid, so datapack recipes with more than 18 visible outputs would need a follow-up layout strategy.

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
* Player perspective: face-attached miner components that use the shared attachment base, including mine heads and resonators, can now be placed into water and remain present as waterlogged blocks instead of being washed away.
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
* System interaction: shared face-attached miner component placement now follows Create's standard waterlogging pattern, so thin attachment geometry no longer relies on occupying most of the voxel to survive in water.
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
* Data and assets: target types are data-driven via `deposit_spec` datapack entries (`scanner_icon_item`, optional `map_icon_item`, and `structure`), not hardcoded.
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
* Core behavior: deposit definitions may declare a required mod id; entries with unmet mod requirements are omitted from all generated deposit worldgen files, while compatible loaded mods automatically extend the shared deposit structure set instead of enabling a separate compat pack.
* Core behavior: placement is constrained by biome eligibility and generation step settings, so deposits are injected into terrain generation rather than placed as ad-hoc runtime edits.
* Edge behavior: disabling eligible biomes prevents new deposits from generating while preserving already-generated terrain.
* Edge behavior: changing worldgen data affects newly generated chunks; existing chunks keep their previously generated deposits unless modified by gameplay or admin tools.
* System interaction: scanner discovery and admin locate tooling depend on deposit worldgen structure identity remaining consistent with scanner target definitions.
* System interaction: scanner search behavior assumes the worldgen placement model remains in the same general scale; major placement changes can require scanner tuning.
* System interaction: found-state filtering applies during structure lookup so discovery can ignore already-discovered deposits without changing worldgen itself.
* Data and assets: behavior is data-driven through generated worldgen JSON and structure templates, with additional built-in datapack content controlling biome eligibility.
* Data and assets: deposit type metadata for scanner selection is datapack-driven and must remain aligned with generated structure identities; compat expansion remains code-defined through deposit registration plus required-mod gating rather than through a separate built-in datapack.
* Data and assets: default built-in-pack worldgen entries are sourced from per-deposit registration definitions;
  dump tooling uses an explicit dump-mode bootstrap path to materialize inspectable defaults outside game startup.
* Maintenance invariant: structure templates must keep their agreed placeholder convention so replacement rules can reliably convert template blocks into deposit blocks.
* Maintenance invariant: per-type worldgen entries, structure tags, and scanner target definitions must stay synchronized across code and datapack data.
* Known limitation: default spawn tuning is authored in code-backed built-in-pack definitions, so balancing changes currently require updating code inputs or overriding via datapack.

## Dynamic Mining Recipe Registration
* Player perspective: mining behavior is still defined by mining recipe JSON, but the mod's default mining recipes now come from the built-in dynamic datapack instead of handwritten resource files.
* Player perspective: when a compat recipe is defined in code for an optional dependency, it loads automatically when that mod is present and is absent otherwise; players do not toggle it separately.
* Edge behavior: compat-gated mining recipes follow the same runtime-vs-dump enable rules as compat deposit worldgen, so default built-in-pack dumps exclude them while compat-enabled dumps include them.
* Core behavior: code-defined mining recipes are registered as immutable configured entries through `MiningRecipeBuilder`, with recipe ids derived from their deposit block ids.
* Core behavior: registration is colocated with deposit definitions through `DepositBlockBuilder.recipe(...)`, so a deposit block and its dynamic mining recipe can stay authored in the same chain.
* Core behavior: the main built-in dynamic datapack serializes enabled configured mining recipes into normal `recipe/*.json` files at pack-build time, leaving recipe loading itself to vanilla recipe handling.
* System interaction: builder-defined recipe emission is independent from Registrate recipe generation and does not go through `RNSRecipes`; the shared bootstrap path is `RNSDeposits.register()` in both game runtime and dump tooling.
* System interaction: compat recipe item and block references may use registry ids directly so recipes can target optional-mod content without adding those mods as compile-time dependencies.
* System interaction: compat deposit blocks may also skip runtime block registration when their required mod is absent, so compat `BlockEntry` fields should not be assumed to exist unless the corresponding mod is loaded.
* Data and assets: recipe behavior remains datapack-driven at runtime; this feature only changes how some default JSON is authored and emitted.
* Maintenance invariant: every dynamic mining recipe id must remain derived from its deposit block id so handwritten and generated defaults cannot silently diverge.
* Maintenance invariant: compat gating for dynamic mining recipes must stay aligned with the same mod-presence and dump-mode checks used by other dynamic built-in pack entries.
* Known limitation: compat recipe ids that reference optional-mod items or blocks are not compile-time validated against those mods unless the integration dependency is added locally for testing.

## Dynamic Deposit Spec Registration
* Player perspective: scanner target icons and supported map-overlay icons are still driven by `deposit_spec` datapack entries, but the mod's default deposit specs now come from the built-in dynamic datapack instead of handwritten resource files.
* Player perspective: compat deposit types contribute their scanner/map metadata automatically when the corresponding deposit is enabled, and disappear from defaults when that compat deposit is absent.
* Core behavior: code-defined deposit specs are registered as immutable configured entries through `DepositSpecBuilder`, with spec ids derived from the deposit structure registration chain and map icons defaulting to the deposit block item unless overridden.
* Core behavior: registration is colocated with deposit definitions through `DepositBlockBuilder.spec(...)`, so worldgen structure identity, scanner icon candidates, and map icon metadata stay authored in the same chain.
* System interaction: scanner selection, discovery naming, and map overlay item rendering continue to resolve through the `DepositSpec` datapack registry and `DepositSpecLookup`; this feature changes how default JSON is authored and emitted, not how lookup works at runtime.
* System interaction: compat-gated deposit specs must stay aligned with the same enable checks used by compat deposit worldgen and mining recipes so scanner targets never outlive their corresponding structures.
* Data and assets: runtime behavior remains datapack-driven; the main built-in dynamic datapack now serializes enabled configured deposit specs into normal registry JSON files at pack-build time.
* Maintenance invariant: every dynamic deposit spec must keep its structure reference synchronized with the corresponding generated deposit structure id so scanner targets, structure tags, and map overlays continue to describe the same deposit type.
* Maintenance invariant: the default map icon fallback should remain the deposit block item unless a deposit intentionally needs a different overlay icon.

## Release Automation (Manual Version Bump, Tag, and GitHub Release)
* Maintainer perspective: releases are created by manually running `.github/workflows/release.yml` in GitHub Actions.
* Maintainer perspective: each run uses `bump_type` (`patch`, `minor`, `major`, `custom`), and `custom_version` is used only
  when `bump_type` is `custom`.
* Core behavior: the workflow reads `mod_version` from `gradle.properties` and derives the next version.
* Core behavior: semantic bumps only modify the numeric semver core before the first `-`; any suffix after the first `-` is preserved.
* Edge behavior: if current `mod_version` is not semver-bumpable, maintainers must use `custom_version`; the workflow fails fast with an error.
* Edge behavior: providing `custom_version` with a non-`custom` bump type, or selecting `custom` without `custom_version`, fails fast with an error.
* Edge behavior: resolved versions (including `custom_version`) must match `x.x.x-<digits[.digits...]>-<digits>`; invalid formats fail fast.
* Edge behavior: runs fail when the resulting tag already exists locally or on origin to prevent duplicate releases.
* System interaction: the workflow commits the version change, creates a tag, pushes both to GitHub, and then creates a GitHub Release with autogenerated notes.
* System interaction: the workflow runs `./gradlew assemble` before tagging and uploads `build/libs/create_rns-<version>.jar` as the release asset.
* System interaction: release CI provisions Temurin Java 21 explicitly before build steps to avoid runner-default JDK drift.
* System interaction: pushing relies on repository `contents: write` workflow permission and can be blocked by branch protection policies.
* Data and assets: automation is code-defined in workflow YAML, mutates `gradle.properties`, and publishes the assembled jar artifact.
* Maintenance invariant: `mod_version` must remain defined in `gradle.properties` as a single `key=value` line for automated updates to remain stable.
* Maintenance invariant: Gradle packaging must continue to produce `build/libs/create_rns-<mod_version>.jar` for release upload to succeed.

## Client Map Waypoint Compat (Deposit Discovery Markers)
* Player perspective: when scanner tracking completes discovery, supported client map mods create a waypoint at the player's discovery position.
* Player perspective: the marker name matches the discovered deposit type, so players can treat it as a field note for where they first confirmed that deposit.
* Edge behavior: the marker is placed at the discovery spot, not at the deposit structure center, so it reflects where the player stood when discovery completed.
* Core behavior: waypoint creation is driven from the client-side scanner found-state transition, after the authoritative server discovery flow has already marked the deposit as found.
* Core behavior: the shared compat entrypoint fans out to every supported installed map integration, so clients with both Xaero and JourneyMap receive markers in both systems.
* Core behavior: JourneyMap markers are stored under create-rns ownership and replace an existing marker for the same deposit type at the same position instead of stacking duplicates.
* Edge behavior: Xaero markers are written into the currently active waypoint set, so switching sets later can hide them even though they remain saved in Xaero data.
* System interaction: the compat code is optional and must only execute when the corresponding map mod is present on the client.
* System interaction: scanner discovery packets remain the only gameplay authority; map mods only mirror the local client's discovery result and are not used for sync.
* Data and assets: the feature is code-defined only; it adds no datapack fields and no generated assets.
* Maintenance invariant: compat entrypoints exposed outside the package should stay free of direct JourneyMap and Xaero types so the mod can load safely without either mod installed.
* Known limitation: markers are still client-local discovery notes, so other players do not receive them automatically and previously found deposits only appear after each client discovers them.

## Xaero World Map Overlay
* Player perspective: when Xaero World Map is installed, opening the fullscreen world map renders icons for found deposits from the synchronized client cache.
* Player perspective: each rendered deposit uses the item configured by that deposit type's `deposit_spec`, so different deposits can present different map icons without code changes.
* Player perspective: a toggle widget anchored near the lower-left edge of the map screen can temporarily disable deposit rendering without affecting the synchronized cache.
* Edge behavior: deposits only appear after the client has received the authoritative found-deposit sync from the server; opening the map earlier simply shows fewer or no markers until the cache fills.
* Core behavior: the overlay hooks the Xaero fullscreen map screen through a client-only pseudo-mixin and projects world coordinates into Gui coordinates using Xaero's current camera position and zoom state.
* Core behavior: each render pass reads the current-dimension found deposits from `FoundDepositClientCache`, resolves the deposit's configured map item through `DepositSpecLookup`, and renders that item at the synced deposit location.
* Edge behavior: the overlay currently filters by the local player's current dimension; if the map UI is browsing another dimension, markers are not yet remapped to that viewed dimension.
* System interaction: the overlay is optional and only activates when Xaero World Map is present on the client runtime.
* System interaction: the toggle widget currently reuses Create's train-map toggle textures as placeholders and flips a shared client-only overlay-enabled flag used by both fullscreen map integrations.
* System interaction: toggle placement is resolved from per-map anchor metadata plus per-map offsets at render time, so the same renderer can target different screen corners or centered alignments without hardcoding absolute coordinates.
* System interaction: on Xaero, deposit markers render in the map layer while the deposit toggle renders in a later screen-space phase so markers stay behind other late UI.
* System interaction: overlay rendering is read-only and consumes the same cache populated by snapshot/delta sync, so it stays decoupled from scanner audiovisual logic.
* Data and assets: the feature is code-defined and uses datapack-provided `deposit_spec.map_icon_item` values for marker appearance; it adds no generated resources.
* Maintenance invariant: the compat package should remain decoupled from direct Xaero classes, with Xaero-specific field access isolated to pseudo-mixin accessors.
* Known limitation: the Xaero fullscreen hook still assumes the viewed dimension matches the local player's dimension because Xaero does not expose a stable viewed-dimension accessor through the current pseudo-mixin path.

## JourneyMap Fullscreen Overlay
* Player perspective: when JourneyMap is installed, the fullscreen map renders icons for found deposits from the synchronized client cache.
* Player perspective: markers appear in the dimension JourneyMap is currently displaying, so browsing another synced dimension shows that dimension's found deposits rather than the player's current one.
* Player perspective: the same lower-left anchored toggle widget used on Xaero can temporarily disable JourneyMap deposit rendering without mutating synced state.
* Edge behavior: if the client cache has not been filled yet, JourneyMap opens normally and simply shows no deposit markers until sync arrives.
* Core behavior: the compat layer is implemented as a JourneyMap client plugin that subscribes to JourneyMap's fullscreen render event.
* Core behavior: the plugin translates JourneyMap fullscreen camera and zoom state into the shared `RNSMapRenderer` context and reuses the same cache-backed marker renderer as Xaero.
* System interaction: the JourneyMap hook is optional and only activates when JourneyMap is present on the client runtime.
* System interaction: the plugin also subscribes to JourneyMap fullscreen click events so the shared overlay toggle can consume clicks before the map handles them.
* System interaction: marker content still comes from `FoundDepositClientCache` plus `deposit_spec.map_icon_item`, so JourneyMap does not own any separate discovery or sync state.
* Data and assets: the feature is code-defined and reuses the same datapack-driven icon selection as other map overlays.
* Maintenance invariant: JourneyMap-specific API types should stay isolated inside the compat package so the mod can load safely without JourneyMap installed.
* Known limitation: this path currently targets JourneyMap fullscreen rendering only; minimap integration remains separate work.

## Found Deposit Map Sync Cache, Snapshot Bootstrap, and Deltas
* Player perspective: this phase still adds no direct new map UI yet, but reconnecting to a server now restores the local found-deposit cache automatically before any new discoveries happen in that session.
* Player perspective: maps and overlays are expected to open immediately even if the snapshot has not arrived yet; they should render from the current cache contents without waiting on network traffic.
* Player perspective: leaving a world or server clears the local found-deposit cache so stale overlay state does not leak into the next session.
* Core behavior: found-deposit map sync has dedicated payload types for snapshot request, snapshot response, and incremental add/remove deltas.
* Core behavior: the client sends one snapshot request when it logs into a world/server, and the server responds with the current authoritative found-deposit set from all server dimensions.
* Core behavior: snapshot entries carry dimension, deposit structure identity, origin chunk, and render position so clients can reconstruct `ClientDepositLocation` values without server-only classes.
* Core behavior: the client cache stores found deposits grouped by dimension and supports full replacement, incremental add, incremental remove, and full clear operations.
* Core behavior: once the cache is bootstrapped, authoritative found-state mutations immediately push one add/remove delta to all connected clients instead of waiting for reconnect.
* Edge behavior: snapshot bootstrap replaces the whole cache at once, so stale entries from a previous session or partial local state are discarded when the authoritative response arrives.
* Edge behavior: cache identity is based on deposit structure key plus origin chunk, matching authoritative found-state identity rather than transient marker position.
* System interaction: snapshot assembly reads only from `LevelDepositData` found-state attachments on each `ServerLevel`; it does not rescan world structures on demand.
* System interaction: scanner discovery success and `/rns scanner found` command mutations flow through `ServerDepositLocation.setFound`, so duplicate no-op writes do not emit delta packets.
* System interaction: `/rns scanner found forget_all` removes all found deposits in the targeted level attachment and emits a dimension-clear payload so clients drop that level's cached deposits in one step.
* System interaction: the cache is owned by deposit-info code rather than scanner or map compat code, and both Xaero and JourneyMap fullscreen overlays now consume it directly at render time.
* Data and assets: this feature is code-defined only; it adds no datapack fields and no generated assets.
* Maintenance invariant: client map rendering must read only from the cache or future compat APIs layered on top of it, not from direct scanner events or synchronous network requests.
* Known limitation: map compat redraw triggering is still deferred to a later phase, though the current Xaero overlay path naturally reflects cache changes on the next frame because it rereads the cache during render.
