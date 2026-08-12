# Miner Sublevel Compatibility Plan

NOTE:
I'm currently in the middle of implementing miner_sublevel_compatibility_plan.md (stages 1-5 are implemented), but I decided to change up the design. I previously wanted miners that would mine from a different sublevel to not claim deposits at all and mine independently without forcing any deposit exclusivity.

I now changed my mind to the following design:
* Miners in the same sublevel as their claimed deposits mine like normal.
* Miners that mine from a different sublevel also claim deposits, but in a different way. Instead of having a confined area, they claim the deposit vein starting from the source block (where the mine head tip touches a deposit block), and do a breadth-first traversal across the vein, counting up the blocks that haven't been claimed. Instead of the area restriction, they are instead restricted by the number of blocks they can claim at a time.
* Miners across all sublevels that mine a deposit vein in the same sublevel have to ensure that each deposit block is claimed exclusively by a single miner.

## Goals

Support miners whose physical mining footprint reaches deposit blocks in the main world or another Sable sublevel while
preserving the following rules:

1. A miner has exactly one claimed target space at a time. It cannot simultaneously mine deposits from its own sublevel
   and another sublevel, or from multiple remote sublevels.
2. Deposits in the miner's own sublevel are exclusively claimed using the current ownership rules.
3. Deposits in a different sublevel are exclusively claimed by block, with a configured claim-count limit replacing the
   local mining-area restriction.
4. A claimed target space remains active while it contains mineable deposits. Other local or remote spaces do not
   preempt it.
5. Starting, refreshing, stopping, or removing a remote miner refreshes only miners competing for claims in the same
   target operating space.
6. The main world is treated as one operating space. Two main-world miners are in the same space; a main-world miner and a
   sublevel deposit are not.
7. Physics assembly and disassembly preserve each moved deposit block's exact stored durability.
8. Behavior without Sable/Simulated installed remains unchanged, and optional compatibility classes must not be loaded
   when their dependency is absent.

## Current Assumptions That Must Be Generalized

The current implementation uses `claimedDepositBlocks` for both exclusive ownership and the blocks passed to
`MiningProcess`. That remains intentional because every mineable block must be claimed, but the claim must now carry a
target operating-space identity and support different local and cross-sublevel traversal limits.

The current code also assumes that the miner, its integer mining `BoundingBox`, and every deposit position share one
coordinate space:

- `IDepositBlockClaimer.getConfinedDepositVein()` starts at the block directly in front of the mine head and performs a
  local-coordinate flood fill.
- `DepositClaimerInstanceHolder` groups claimers by sided dimension, but not by Sable sublevel identity.
- Overlap and deposit-change queries compare untransformed `BlockPos` and `BoundingBox` values.
- `ContraptionMiningBehaviour.refresh()` always performs peer reclamation after rebuilding its own claim.
- `MiningProcess` assumes every stored position remains a valid deposit until an external claim refresh rebuilds it.
- Each `ServerLevel` owns one `LevelDepositData`, whose durability map is keyed by block position.
  `DepositBlock.onRemove()` treats physics movement like destruction and deletes the source entry without transferring
  it to the destination level's data and destination position.

Cross-sublevel support should not weaken these assumptions implicitly. It should introduce explicit operating-space and
target-discovery boundaries.

## Proposed Behavior Model

### Operating-space identity

Add a common-code operating-space abstraction that can answer which logical space contains a block position and whether
two positions are in the same space.

- With no physics integration loaded, the space identity is the current dimension's main space.
- With Sable loaded, a sublevel's stable UUID identifies the space; positions not contained by a sublevel use a
  main-world identity for that dimension.
- The effective mine-head tip is the authoritative source-space identity and mining origin. `MinerEquipmentManager`
  already applies the mine-head size's axial `tipOffset`, and that resulting position is the point from which the root
  deposit block and mining prism are calculated.
- The bearing position remains the stable registry position used to resolve the owning block entity. It does not define
  mining geometry or source-space identity.
- Resolve identities dynamically at claim/query boundaries so sublevel split, merge, unload, or reconstruction cannot
  leave a permanently stale holder index.

Sable-specific types must stay behind an optional adapter under `compat/sable`. Common mining and claiming classes
should depend only on the mod-owned abstraction. A vanilla adapter must preserve current behavior.

### One claimed target space

Track one authoritative claimed target group in `ClaimingBehaviour`. It atomically contains the target operating-space
identity, whether that space differs from the mine head's source space, and the claimed deposit positions. Local and
cross-sublevel targets both use exclusive ownership; there is no separate active-target or remote shared mode.

The claim's positions drive `MiningProcess`, rates, remaining uses, effects, serialization, outlines, and target
validation. They are never a union of positions from multiple spaces. Conflict filtering must compare target-space
identity so miners in different source spaces still compete when they target the same remote space.

Local claims can derive their target identity from the effective mine-head tip after load. Cross-sublevel claims must
either serialize their target identity and mode with the positions or be rediscovered before rebuilding the process;
they must never be restored as a claim in the source space. Server-to-client synchronization must carry the same
authoritative claim used by presentation and process state.

Claim choice is sticky:

1. If the current claimed space still has at least one valid mineable deposit, retain it and ignore other spaces.
2. If no claimed block remains mineable, release the claim and notify competitors in that target space.
3. When claiming again, try the mine head's own sublevel first using normal exclusive filtering.
4. If no local block can be claimed, choose one remote candidate deterministically and resolve it through exclusive
   target-space conflict filtering. Prefer the nearest seed intersection, then stable operating-space identity.

A claimed space becomes unmineable when it has no qualifying connected deposit blocks because of depletion,
replacement, movement out of the mining prism, recipe/catalyst eligibility changes, or unload. Pose-dependent remote
claims must be validated against current geometry after save/load rather than trusting stale storage positions.

### Candidate discovery

Keep the existing local flood-fill semantics for same-sublevel claims. For other spaces, add a Sable-backed scan that
returns candidates grouped by operating-space identity rather than flattening them into one set:

1. Build the miner's seed cell directly in front of the mine head in the source sublevel's coordinates.
2. Transform that cell through the source sublevel's logical pose into global coordinates. Main-world miners use an
   identity transform.
3. Query the main world and Sable sublevels intersecting the transformed seed cell.
4. Exclude the source operating space from the remote pass; it is handled by normal exclusive claiming when a new target
   space must be selected.
5. Transform the seed cell into each target space and find deposit blocks touching it.
6. Breadth-first traverse face-connected, loaded deposit blocks in that target space, skipping blocks already claimed
   by competitors and stopping when the remote claim-count limit is reached. Do not apply the local mining prism.
7. Return one candidate group per target space, including the positions needed by recipe, durability, load-state, and
   block-state lookups.

For Sable, the main world and its sublevels share one parent `Level`, while candidate positions refer to Sable's
plot/storage coordinates. The separate operating-space identity distinguishes logical sublevels within that level.

Using the seed volume preserves the current rule that a miner must contact a deposit immediately in front of its mine
head. Exact transformed-volume intersection should be used after broad integer-bounds collection so rotated sublevels
do not gain false-positive contact blocks from an enclosing AABB.

### Refresh lifecycle and CPU cost

`ClaimingBehaviour` owns the claimed-space state machine, with mining-specific local and remote claim
refresh triggers supplied by `ContraptionMiningBehaviour`.

- Assembly, disassembly, recipe reload, equipment/spec changes, and effective mine-head source-space changes invalidate
  the claimed space or force it to be revalidated.
- Same-sublevel deposit placement/removal continues to refresh local claimers through `DepositBlock` and the holder.
- Moving sublevels require the owning miner to discover when its transformed seed/mining volume changes or when the set
  of intersecting target sublevels changes.
- A lightweight pose/intersection fingerprint may run each server tick. Full block enumeration and flood filling should
  run only when that fingerprint changes, after a relevant deposit mutation, or at a bounded fallback interval needed
  to discover a target sublevel moving into range.
- While a claimed space remains valid, rescan only that space as needed. Do not scan and combine all candidate spaces
  merely because another target becomes physically reachable.
- If the claimed space becomes invalid, release it, clear its process, and choose a new space using the sticky claim
  policy.
- A remote claim change rebuilds this miner's process and client state and notifies competitors in the target space.
  Newly available local deposits must not preempt a still-valid remote claim.
- If a target sublevel unloads or cannot be resolved, treat that claimed space as unmineable and claim again rather
  than retaining its storage positions as valid remote deposits.

The fingerprint must include enough information to detect motion of both the miner's source sublevel and intersecting
target sublevels. Quantized per-target-space block bounds are preferable to refreshing for pose changes that do not
alter which blocks can intersect the mining volume.

### Physics movement durability transfer

Deposit durability is stored in the `LevelDepositData` attached to each `ServerLevel`, with entries keyed by block
position. Physics assembly currently invokes normal deposit removal, which removes the source-level entry without
restoring it in the destination level's data at the assembled position. Disassembly has the same problem in reverse.

Use Sable's `BlockSubLevelAssemblyListener` through an optional mixin applied to `DepositBlock`:

1. `beforeMove(oldLevel, newLevel, state, oldPos, newPos)` reads the existing durability without initializing an
   uninitialized deposit and captures the exact value for this move.
2. Normal block removal may delete the old entry as it does today.
3. `afterMove(...)` writes the captured durability to the destination level and position after the deposit block has
   been placed.
4. The same callback path handles disassembly by reversing the source and destination.

Infinite deposits have no finite stored value to transfer. An absent source value must stay absent instead of being
initialized or rerolled. A pre-existing destination entry is replaced by the authoritative source state and reported as
an error rather than being overwritten silently. Each block in a moved vein retains its independently rolled value.

Sable 2.0.3 catches movement failures internally and does not invoke `afterMove` or expose a failure callback. A failed
move may therefore leave one inert in-memory capture until the next `beforeMove` replaces it. The capture contains no
level references, an `afterMove` must match its complete immutable callback tuple before consuming it, and no destination
durability is written when `afterMove` is skipped. Strict immediate failure cleanup would require another mixin into
Sable's assembly internals or an upstream lifecycle callback.

Moving durability is independent of target-space claiming: it preserves world data whenever a deposit moves, whether
or not any miner currently sees that deposit.

### Stale-claim depletion safety

Exclusive resolution prevents two live claims from intentionally owning the same block, but process state can still be
stale after movement, replacement, or a competing refresh. Consumption must therefore validate ownership targets.

- Before producing a yield, `MiningProcess.InnerProcess` must verify that a selected position still contains the
  deposit block expected by that inner process.
- Change durability consumption to an authoritative `tryUse` operation that reports whether a finite or infinite
  deposit was actually consumed. Missing/replaced blocks must report failure rather than being treated like infinite
  deposits.
- Remove invalid positions from the process and request an owning-miner target refresh when no valid position remains.
- Generate items and consume catalysts only after successful deposit consumption.
- Because mining runs on the server thread, sequential `tryUse` calls are sufficient to prevent two miners from both
  receiving the final use. The second call must observe the replacement block and fail without yielding.

This self-validation prevents stale processes from yielding after the target changed while normal claim refreshes
redistribute released blocks among competitors.

`tryUse` does not provide cross-sublevel access. Candidate discovery supplies positions in the selected logical space;
`tryUse` only makes the final validation and durability update authoritative before catalysts or output are consumed.

## Required Code Changes

### Compatibility boundary

- Add `Mods.SABLE` and a guarded compatibility bootstrap/factory.
- Add a common operating-space/candidate-scan interface with a vanilla implementation.
- Add a Sable implementation using `Sable.HELPER.getContaining(...)`, `getAllIntersecting(...)`, stable sublevel IDs,
  logical poses, and inverse transforms.
- Add a Sable-conditional mixin that makes `DepositBlock` participate in `BlockSubLevelAssemblyListener` durability
  transfers without putting optional Sable interfaces on the always-loaded block class.
- Register the optional mixin in `create_rns.mixins.json` and gate its namespace through `RNSMixinPlugin` and
  `Mods.SABLE`.
- Keep Sable classes out of common method signatures and static initializers so the mod remains loadable without Sable.
- No Create or Sable mixin should be necessary for the mining path unless runtime investigation finds that sublevel
  lifecycle changes cannot be observed through the public helper API. The durability-transfer listener is a separate,
  expected mixin-based integration.

### Deposit operators and claimers

- Keep deposit geometry and confined-vein discovery in `IDepositBlockOperator`; keep exclusive ownership, conflict
  filtering, and claim serialization in its `IDepositBlockClaimer` child interface.
- Make same-space filtering an explicit invariant of `getClaimableDepositVein(...)`.
- Reclaim helpers derive their source operating-space identity from a representative position in the operating area. This
  relies on the invariant that a local operating `BoundingBox` is wholly contained in one operating space.
- Keep local and remote claims in one serialized shape, including enough target-space metadata to avoid restoring remote
  positions as local. Pose-dependent reachability must still be revalidated after load.
- Preserve the existing local seed-block flood fill and area restriction. Add a remote breadth-first traversal limited
  by claim count rather than the local operating box.

### `DepositClaimerInstanceHolder`

- Preserve the current sided-dimension lookup as the first filter, then apply same-sublevel filtering to intersection,
  point-containment, and nearby-claimer queries.
- Resolve the query space from the relevant mine-head anchor, deposit position, or explicit space key. Resolve each
  candidate claimer from its current effective mine-head anchor.
- Keep stale block-entity pruning, but reject entries whose current sublevel does not match the query sublevel. Two
  claimers in the main world or the same Sable sublevel still interact exactly as before.
- Prefer dynamic filtering over permanently nesting the registry by sublevel UUID; this avoids reindexing bugs after
  sublevel lifecycle changes. "Dynamic" means resolving the additional sublevel predicate at query time; it does not
  replace or weaken current dimension/client-side filtering.
- Area-only queries use a representative position from the bounding box to resolve their operating space. Do not scan
  every position in the box; local operating boxes are required to remain within one operating space.

### Operating and mining behaviours

- Keep one immutable claimed target group in `ClaimingBehaviour`; it owns the target identity, local/cross-sublevel mode,
  and positions. Build `MiningProcess` only from those positions, never from a union of candidates in multiple spaces.
- Route local and remote candidates through exclusive claim resolution in their target space. A remote rescan may cause
  peer reclamation only for miners competing in that same target space.
- Keep `MiningBehaviour` responsible for translating claim changes into process reconstruction, durability
  initialization, process ticking, and collection.
- Initialize durability only for deposits in the authoritative claim, not unclaimed candidates.
- Synchronize and persist enough claim metadata to restore the target space without misclassifying remote positions as
  local ownership.
- Preserve pending process progress across a target rebuild where the corresponding deposit recipe still exists.

### `ContraptionMiningBehaviour`

- Resolve source identity and mining geometry from `equipment.mineHeadPos`, which already includes the size-specific tip
  offset. Continue using the bearing position only to locate the owning block entity.
- Implement the sticky claimed-space state machine and invoke the optional remote scanner only when the current space
  needs validation or a new space must be selected.
- Track the transformed-volume fingerprint and schedule remote rescans without a full per-tick block search.
- Refactor `refresh()` into equipment/spec validation, claimed-space validation, optional reselection, claim update,
  and process rebuild stages.
- Resolve reclamation in the claim's target space, including when a remote claim changes or is released.
- On disassembly/unload, release remote claims and notify competitors in their target spaces.

### Deposit and bearing lifecycle

- `DepositBlock.updateNearbyClaimers(...)` should refresh claimers targeting the deposit's operating space, including
  miners whose source is in another sublevel.
- `MinerBearingBlock.onRemove(...)` must capture and release its target-space claim before block-entity removal, then
  refresh competitors for that target space.
- Add durability-manager operations that capture an existing value without initialization and restore it at a moved
  block's destination. Invoke them from the optional Sable assembly listener on both assembly and disassembly.
- Recipe reloads must revalidate the claimed set. Reselect only if the current space no longer contains a
  mineable block.

### Mining process, durability, and client visuals

- Keep `MiningProcess` on the miner's parent `Level`; Sable sublevels are partitions of that same level. Pass positions
  from exactly one logical space.
- Update `MiningProcess` to prune stale claimed targets and to produce output only after successful durability use.
- Update `DepositDurabilityManager` with an unambiguous success-returning consumption operation. This validates stale
  targets; it is not responsible for resolving or transforming cross-sublevel positions.
- Update `MinerEffectsGenerator` to scale from the authoritative claimed positions.
- Keep `DepositClaimerOutlineRenderer` claim-oriented and render remote claims in their target operating space.

## Delivery Order

Each stage is intended to be independently reviewable and mergeable. At every boundary, the mod must compile with and
without optional compatibility classes available, existing non-Sable behavior must remain usable, and all tests relevant
to that stage must pass. Do not leave temporary dual state, partially migrated serialization, or a runtime path that
depends on a later stage to avoid crashes or data loss.

### Stage 1: Characterize and protect current behavior

**Outcome:** no production behavior changes. Existing miner and durability semantics are pinned before refactoring.

- Add focused GameTests around local vein discovery, overlapping exclusive claims, reclaim after miner removal, deposit
  placement/removal refresh, claim serialization, process rebuild, and durability depletion.
- Add test helpers that can observe claim ownership, process target positions, and lifecycle outcomes without depending
  on Sable or adding production instrumentation.
- Record the current invariant that, in vanilla operation, the process input and exclusive claim contain the same
  positions.
- Record known gaps, such as stale positions being indistinguishable from infinite durability during void consumption,
  without encoding those gaps as desired passing behavior. Stage 6 adds the regression expectation with its fix.

**Verification gate:** production sources are unchanged; existing miner GameTests and the new characterization tests
pass. The compile-only Sable dependency is not referenced by always-loaded classes.

### Stage 2: Consolidate claims and process input

**Outcome:** behavior remains vanilla-equivalent and one authoritative claim represents both ownership and mining input.

- Retain `claimedDepositBlocks` as the exclusive ownership and process-input set.
- Make `MiningProcess`, rate calculations, remaining uses, effects, and process validation consume the claim directly.
- Remove transient active-selection state and its independent serialization.
- Extract claim recomputation, process rebuild, client notification, and process-progress restoration into explicit
  steps. Each actual claim change must invalidate the process exactly once.
- Add disk and client-packet serialization regression coverage proving that claims restore process targets without a
  second target representation.

**Verification gate:** all Stage 1 tests still pass unchanged, and additional assertions prove that claim and process
targets remain equal throughout assembly, refresh, reload, disassembly, and reconstruction after save/load.

### Stage 3: Introduce dependency-free operating-space seams

**Outcome:** common code can represent one claimed target space and its positions within the miner's parent `Level`,
but the only available implementation still behaves exactly like the current dimension-local miner.

- Add mod-owned `OperatingSublevel`, claimed-space/mode state, and a candidate group containing one space identity and
  its positions.
- Add a common operating-space adapter interface and a vanilla implementation. The vanilla implementation exposes only the
  miner's current dimension as one local space and never returns remote candidates.
- Change holder queries to carry the position relevant to the query, while area-only reclaim queries derive a
  representative position from their bounding box. Preserve `SidedDimension` as the effective vanilla filter.
- Make the effective mine-head tip the source anchor throughout claiming and process setup. The bearing position remains
  only the block-entity registry location.
- Introduce the sticky claimed-space state machine with only local exclusive mode reachable. This validates state
  transitions before optional compatibility adds another mode.
- Keep `MiningProcess` on the bearing's parent `Level`; the claimed space partitions positions within that level.

**Verification gate:** Stage 1 and Stage 2 behavior remains unchanged, Sable is not required at runtime, and a no-Sable
startup/compile check proves that no optional type leaked into common signatures or static initialization.

### Stage 4: Make local mining sublevel-aware

**Outcome:** a miner placed inside a Sable sublevel can robustly claim and mine deposits in that same sublevel. Remote
sublevels and the main world are not yet candidates for that miner.

- Add `Mods.SABLE`, a guarded compatibility factory, and the Sable operating-space identity implementation.
- Compile the integration against Sable Companion, which is embedded in the Sable distribution but absent from its
  published Modrinth POM. Expose that nested API jar only to the compile classpath; do not package it with this mod.
- Resolve the source identity from the effective mine-head tip. Treat Sable's stable sublevel UUID as the logical space
  and use a dimension-specific main-world identity outside sublevels.
- Add dynamic same-sublevel filtering to `DepositClaimerInstanceHolder` after its existing sided-dimension filter.
- Scope overlap, point-containment, nearby-claimer, deposit-change, reclaim, removal, and client-outline queries to the
  mine head/deposit's current logical space.
- Do not cache or poll a separate local source-space identity. The bearing and effective mine-head tip must occupy the
  same operating space, so resolve fresh local claims from the bearing position. Sable assembly/disassembly recreates
  the bearing block entity but loads its source NBT, so validate a local claim restored into an uninitialized owner
  once on the next tick against a representative target position. Retain same-space claims without rescanning; rebuild
  changed-space or empty claims. Dynamic query filtering must still resolve current anchors so stale holder
  registrations cannot cross space boundaries.
- Keep the remote candidate API returning no candidates. This stage must not introduce cross-sublevel mining.
- Update `implementation.md` and `AGENTS.md` with the completed same-sublevel ownership behavior and optional adapter
  boundary.

**Verification gate:** vanilla behavior remains unchanged with Sable absent; with Sable present, two miners in the same
sublevel conflict normally, miners in different sublevels do not affect one another, and a same-sublevel deposit change
refreshes only claimers in that space.

### Stage 5: Preserve durability across physics movement

**Outcome:** assembling or disassembling deposits as physics contraptions moves initialized durability with each block,
independently of miner compatibility.

- Add durability-manager operations to read an existing value without initialization and restore that exact value at a
  destination level/position.
- Add the optional `BlockSubLevelAssemblyListener` mixin for `DepositBlock`, register it, and gate it through
  `RNSMixinPlugin` and `Mods.SABLE`.
- Capture source durability before normal removal and restore it after placement. Support moves within one
  `ServerLevel` and moves between distinct `ServerLevel` data objects.
- Preserve absent/infinite state and replace a conflicting destination entry with the source state while reporting an
  error.
- Match `afterMove` against the complete captured callback tuple so unrelated callbacks cannot consume stale state. If
  Sable skips `afterMove` after an internal failure, leave the inert capture until the next `beforeMove` replaces it.
- Verify that normal destruction still removes durability rather than transferring it.
- Document the completed movement behavior in `implementation.md` and its optional mixin boundary in `AGENTS.md`.

**Verification gate:** round-trip assembly/disassembly preserves every finite value exactly, uninitialized/infinite
deposits do not gain entries, failed moves do not duplicate values or leak level references, unrelated callbacks cannot
consume stale captures, and Sable-absent startup remains unaffected.

### Stage 6: Make deposit consumption authoritative

**Outcome:** process consumption is authoritative and safe against stale claims, but miners still claim only their local
sublevel.

- Replace void durability use with `tryUse`, accepting the expected deposit identity, target `ServerLevel`, and target
  position.
- Verify the target block immediately before consumption and distinguish a missing/replaced target from an infinite
  deposit.
- Consume catalysts and create output only after `tryUse` succeeds.
- Prune stale positions and request an owning-behavior refresh when an inner process has no valid positions left.
- Add a two-consumer regression test proving only one process receives the final finite use.
- Preserve existing local mining rates, progress serialization, infinite-deposit behavior, and replacement behavior.

**Verification gate:** all earlier behavior remains functional, local mining produces identical valid output, and stale
or competing process state cannot create output from a replaced deposit.

### Stage 7: Add exclusive one-space remote mining with conservative refresh

**Outcome:** cross-sublevel mining becomes player-visible and correct. Each miner exclusively claims either its own
local space or one remote space, never a union, using a correctness-first refresh strategy.

- Implement Sable transformed seed/prism discovery and return remote candidates grouped by operating-space identity.
- Apply exact transformed-volume intersection to find the root contact, then breadth-first traverse the remote vein up
  to the configured cross-sublevel claim-count limit without applying the local mining prism.
- Activate sticky claiming: retain a valid claimed space, prefer local mining when claiming from no state, and otherwise
  choose and exclusively resolve one remote group deterministically.
- Store local and remote targets in the same authoritative claim representation and build the process solely from it.
- Ensure remote assembly, refresh, disassembly, and removal release ownership and notify competing claimers in the
  target space.
- Initially use a conservative bounded rescan interval plus immediate refresh on known lifecycle changes. This may do
  more work than the final design, but it must correctly notice source/target motion, unload, depletion, and re-entry.
- Complete server/client and disk synchronization or deterministic rediscovery so effects, tooltips, outlines, and
  processes use the same remote claim without restoring it into the wrong space.
- Update `implementation.md` with the completed one-space exclusive remote-claim rules.

**Verification gate:** local and remote positions never coexist in one process; two miners targeting the same remote
deposit receive disjoint claims; releasing one refreshes the other; moving out of range eventually clears/reclaims;
save/load and reload preserve or safely rediscover remote ownership without phantom output or stale claims.

### Stage 8: Replace conservative rescans with dirty/fingerprint refresh

**Outcome:** Stage 7 behavior is preserved while unchanged physics contraptions avoid repeated block enumeration and
flood filling.

- Add a lightweight per-tick fingerprint covering source identity/pose, claimed target identity/pose, transformed seed
  bounds, and claimed-space load state.
- Rescan the claimed space only when seed contact or relevant eligibility state changes.
- When no claimed space is valid, use the broad intersecting-space query only as needed to claim a replacement.
- Retain a low-frequency fallback scan if Sable exposes no reliable event for a previously unrelated sublevel moving
  into range.
- Add counters or test instrumentation that distinguish fingerprint checks from full scans.
- Confirm that sub-block pose motion which cannot change intersected blocks does not rebuild the process.

**Verification gate:** the Stage 7 integration matrix is unchanged, full-scan counts remain stable while poses and block
coverage are unchanged, and source/target block-boundary changes still trigger timely refresh.

### Stage 9: Lifecycle hardening and release validation

**Outcome:** all compatibility paths are covered across reload, persistence, movement, optional-mod absence, and client
presentation, with no temporary implementation scaffolding remaining.

- Exercise assembly/disassembly of miners and deposits, bearing removal, chunk/sublevel unload, sublevel reconstruction,
  recipe reload, catalyst changes, finite depletion, infinite deposits, and save/load.
- Verify sticky claiming across normal refreshes and deliberate reselection after the claimed space becomes unmineable.
- Verify claim NBT or deterministic rediscovery preserves target-space ownership, and pending process progress is
  restored only where runtime reconstruction claims targets for which the saved recipes remain applicable.
- Remove temporary compatibility diagnostics or migrate useful scan counters behind normal debug logging.
- Complete `implementation.md` and `AGENTS.md` maintenance notes, including optional classloading, claim invariants,
  durability transfer, refresh performance, and known limitations.
- Run the full compile and GameTest suites, then perform the manual Sable scenarios that cannot be constructed reliably
  in the existing GameTest harness.

**Verification gate:** the complete test matrix below passes, Sable-absent and Sable-present configurations both load,
and the codebase contains one authoritative claimed-space path rather than retained pre-compatibility branches.

## Test Matrix

- Two overlapping miners in the main world still receive disjoint exclusive claims.
- Two overlapping miners in one Sable sublevel still receive disjoint exclusive claims.
- Claim queries never compare or subtract claims belonging to another sublevel, even when local coordinates or broad
  transformed bounds overlap.
- A sublevel miner can exclusively claim and mine a main-world deposit intersecting its physical mining prism.
- A miner in sublevel A can exclusively claim and mine a deposit in sublevel B.
- Two miners in different source spaces targeting the same remote deposit receive disjoint claims; releasing either
  refreshes the other in the target space.
- When both local and remote candidates exist during initial claiming, the miner claims only the local group and its
  process contains no remote positions.
- A valid local claim is not expanded with remote candidates, and a valid remote claim is not expanded or
  preempted by newly reachable local or other remote candidates.
- After the claimed space becomes unmineable, the miner releases that target-space ownership and deterministically claims
  at most one replacement space.
- In remote mode, claim serialization or deterministic rediscovery preserves the target-space identity, and the claim
  and process contain only positions from that remote space.
- Moving or rotating either sublevel across a block-boundary threshold refreshes only affected miners and does not run a
  full deposit scan on every unchanged tick.
- A target sublevel moving out of range or unloading removes its targets without crashes, phantom yields, or peer
  reclamation.
- When two miners attempt the final durability use, exactly one receives output and consumes catalysts.
- Deposit placement/removal updates all claimers targeting that operating space, including remote miners.
- Physics assembly transfers every initialized finite durability from the old level/position to the new level/position;
  disassembly transfers it back without rerolling, duplicating, or dropping the value.
- Moving an uninitialized or infinite deposit does not create a finite durability entry. A failed move does not duplicate
  durability or let unrelated callbacks consume its inert capture, which is replaced by the next `beforeMove`.
- Save/load restores local claims and safely restores or recomputes pose-dependent remote claims without losing their
  target-space identity. `/reload` retains the in-memory claimed space when it
  remains mineable and preserves recipe progress where possible.
- With Sable absent, classloading succeeds and all existing miner GameTests retain their current behavior.

Sable integration scenarios will likely require dedicated GameTest setup or manual in-client verification. Per project
rules, `runGameTestServer` and `runClient` must be run by the project owner rather than by the coding agent.
