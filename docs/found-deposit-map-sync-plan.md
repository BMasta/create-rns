# Found Deposit Map Sync Plan

## Goal
Move map markers for found deposits from a purely local client-side event into a server-authoritative client cache that:

* stays mostly coherent during normal play,
* does not block map opening,
* does not depend on synchronous request/response during render,
* supports overlay-based map integrations.

The intended model is:

1. Client requests a full snapshot once when it joins a world/server.
2. Server replies with the current found-deposit set for the relevant dimensions.
3. Server pushes incremental updates whenever found state changes afterward.
4. Client overlay rendering reads only from the local cache.

## Principles
* Server is authoritative for found state.
* Map UI must never wait on network traffic.
* Full snapshot is for bootstrap and recovery, not for per-open refresh.
* Incremental updates are the normal steady-state path.
* Client cache should be dimension-aware because found deposits are stored per level attachment.
* Map compat code should consume a mod-owned cache, not reach into networking or scanner state directly.

## Scope
This plan covers synchronization of found deposit positions and the client cache that powers map markers.

This plan does not yet cover:

* replay/resync after datapack changes,
* automatic migration from the current client-local marker prototype,
* sync of deposit durability or any other deposit metadata.

## Existing Touchpoints
* Found deposit authority currently lives in `LevelDepositData.foundDeposits` as `ServerDepositLocation` entries.
* Payload registration already happens in `CommonEvents.onRegisterPayloadHandlers`.
* The scanner client handler still owns audiovisual scanner feedback, but no longer mutates map state directly.
* Xaero overlay experiments already render from client-side data without blocking the map screen.
* Deposit location logic is now split intentionally:
  * `ServerDepositLocation` owns server-only search, persistence, and found-state behavior.
  * `ClientDepositLocation` is the client-safe representation for rendering and cache storage.

These are the main places to extend rather than adding parallel systems.

## Target Architecture

### 1. Client Sync Shape
Use the new server/client location split directly instead of introducing a parallel model hierarchy.

The network payload for one found deposit still needs these fields:

* `ResourceKey<Level> dimension`
* `ResourceKey<Structure> structureKey`
* `ChunkPos origin`
* `BlockPos location`

Notes:
* `location` is what rendering should use.
* `origin` is useful as a stable identity because found-state identity is keyed by structure key plus origin.
* On the server, data originates as `ServerDepositLocation`.
* On the client, packet handlers should materialize `ClientDepositLocation`.

### 2. Client Cache
Add a dedicated client-only cache manager, separate from scanner state and separate from any one map mod.

Suggested responsibilities:

* store found deposits grouped by dimension,
* support full replacement from a snapshot,
* support add/remove incremental updates,
* expose read-only queries for map compat,
* clear on logout

Suggested shape:

* `Map<ResourceKey<Level>, Set<ClientDepositLocation>>`

Identity is provided by `DepositLocation.equals/hashCode`, which is based on:

* `ResourceKey<Structure>`
* `ChunkPos origin`

This mirrors the server-side `foundDeposits` structure and avoids relying on transient client marker state or rendered positions alone as identity.

### 3. Payload Set
Add three new payloads:

* `FoundDepositsSnapshotC2SPayload`
  Purpose: client asks server for the current snapshot after joining or explicit resync.

* `FoundDepositsSnapshotS2CPayload`
  Purpose: server sends the full authoritative set for one or more dimensions.

* `FoundDepositDeltaS2CPayload`
  Purpose: server pushes one add/remove change when a deposit becomes found or not found.

`FoundDepositsSnapshotC2SPayload` can be empty.

`FoundDepositsSnapshotS2CPayload` should contain enough data to instantiate per-dimension `ClientDepositLocation` entries.

`FoundDepositDeltaS2CPayload` should contain:

* operation: `ADD` or `REMOVE`
* one entry payload convertible into `ClientDepositLocation`

### 4. Server Push Points
Whenever authoritative found state changes, send a delta to affected clients immediately.

Expected mutation points:

* deposit discovery success from scanner flow,
* commands that mark a deposit found,
* commands that mark a deposit not found,
* any future admin or gameplay system that mutates `LevelDepositData.foundDeposits`.

To keep this reliable, the actual send should not be duplicated across many call sites. Prefer a narrow helper/service that:

1. mutates found state,
2. marks level data dirty if needed,
3. emits the correct delta packet if the state actually changed.

### 5. Client Consumers
Map integrations should consume the cache rather than packet handlers directly.

Planned consumer order:

1. client cache receives snapshot/deltas,
2. cache updates internal state,
3. cache invokes a compat-layer redraw API,
4. map compat draws overlay markers from the updated cache.

This keeps rendering independent from networking timing.

## Join and Resync Flow

### Join Flow
1. Client connects and world is ready enough for client packet handling.
2. Client sends `FoundDepositsSnapshotC2SPayload`.
3. Server gathers all currently found deposits from relevant `ServerLevel` attachments.
4. Server sends `FoundDepositsSnapshotS2CPayload`.
5. Client constructs `ClientDepositLocation` entries and replaces its local cache contents with the snapshot.
6. Map compat rebuilds markers from the cache.

Important property:
The client does not block waiting for step 4. The map can open immediately and simply render whatever cache state exists at that moment.

### Resync Flow
Support an explicit resync path for recovery after packet loss or debugging.

Potential triggers:

* client command,
* debug keybind,
* automatic request after dimension join if cache is empty,
* future timeout-based stale-data recovery

This should reuse the same snapshot request/response packets.

## Snapshot Scope
Snapshots should include all dimensions.

Reasoning:
* simplest mental model,
* map can browse other dimensions without extra roundtrips,
* best fit for fullscreen world-map browsing,
* found deposits are relatively static, so bootstrap simplicity is worth more than aggressively minimizing packet size.

## Integration with Current Scanner Flow
Current behavior:
`DepositScannerClientHandler.processTrackingStateUpdate()` no longer mutates map state directly.

Target behavior:
* keep scanner audiovisual feedback client-side,
* keep map state fully outside the scanner handler,
* let the eventual server delta drive cache update and overlay rendering.

Why:
* multiplayer correctness,
* single source of truth,
* same mechanism works for commands and future non-scanner discovery paths.

## Map Integration Strategy

### Overlay integrations
Xaero overlay rendering should query the cache each frame or render from a precomputed per-dimension list.

This path is already naturally non-blocking because rendering only reads local memory.

## Proposed Implementation Phases

## Phase 1: Data and Payload Foundation
* Add the payload codecs needed to transfer dimension plus `ClientDepositLocation` construction data.
* Add the three payload classes and register them in `CommonEvents`.
* Add a client cache manager with `replaceAll`, `add`, `remove`, and `clear`.
* Clear the cache on logout in `ClientEvents.onClientLogout`.

Definition of done:
* packets compile,
* client cache can be updated manually from packet handlers,
* no map integration changes yet.

## Phase 2: Snapshot Bootstrap
* Send `FoundDepositsSnapshotC2SPayload` on client join.
* Server responds with a full snapshot built from all server levels' `LevelDepositData.foundDeposits`.
* Client converts snapshot entries into `ClientDepositLocation` values and replaces the cache.

Definition of done:
* reconnecting to a server restores the full found-deposit cache without opening the map,
* cache is populated even if the player has not discovered anything during this session.

## Phase 3: Incremental Deltas
* Introduce a narrow helper around found-state mutation.
* Route scanner success and command-based state changes through that helper.
* Convert the authoritative `ServerDepositLocation` to the client payload form at the network boundary.
* Emit `FoundDepositDeltaS2CPayload` on actual add/remove changes only.

Definition of done:
* new discoveries appear client-side without reconnecting,
* admin "not found" operations remove entries from the client cache,
* duplicate no-op updates do not send packets.

## Phase 4: Map Consumers
* Rework map compat to consume the cache instead of scanner-local events.
* Add a compat-layer API that requests overlay redraw/rebuild when cache contents change.
* Replace test overlay markers with cache-backed deposit markers.
* Filter rendered markers by the currently viewed dimension and any future visibility rules.

Definition of done:
* map markers are driven entirely by the cache,
* scanner handler no longer directly adds markers,
* cache updates trigger compat redraw requests through a dedicated API,
* overlay markers survive map opening/closing because the cache is independent from the UI lifecycle.

## Phase 5: Resync and Debugging
* Add a manual resync trigger.
* Add debug logging around snapshot size and delta application.
* Optionally surface last-sync tick/time for troubleshooting.

Definition of done:
* stale cache can be repaired without reconnecting,
* packet and cache behavior can be diagnosed in multiplayer testing.

## Server-Side Implementation Notes
* Build snapshot entries from `LevelDepositData.foundDeposits` only.
* Avoid scanning world state on each request; use the attachment as the source of truth.
* Found deposits are universal and visible to all players, so snapshot and delta recipients should be all connected clients.
* Keep the mutation helper close to deposit info logic so command and gameplay callers share the same path.
* Treat `ServerDepositLocation` as server-internal. Packet assembly is the point where server data is converted into the client-safe shape.

## Client-Side Implementation Notes
* Packet handlers should enqueue minimal work:
  * decode,
  * construct `ClientDepositLocation`,
  * update cache,
  * invoke the compat redraw API.
* Because `ClientDepositLocation` is immutable for sync purposes and equality is based on structure key plus origin, delta application can use normal set add/remove operations.
* Avoid doing expensive overlay rebuilds directly inside packet handler logic unless the operation is known to be lightweight.
* Prefer reconciling in memory and then letting the renderer consume the updated cache.

## Failure and Recovery Behavior
Expected behavior under packet delay or temporary connection issues:

* map opens normally,
* overlay renders from the last known cache,
* no UI thread blocking occurs,
* snapshot bootstrap eventually fills the cache once packets resume.

Expected behavior under lost delta packets:

* cache may become stale,
* explicit resync repairs it,
* optional future timeout-based resync can automate this.

## Settled Decisions
* Found deposits are universal and visible to all players.
* Snapshots should include all dimensions.
* Compat layers should expose an API to redraw the map overlay when the cache changes.
* The cache manager should call that compat redraw API when it applies snapshot or delta payloads.

## Recommended Next Step
Implement Phase 1 and Phase 2 first. They establish the authoritative cache and prove the non-blocking sync model before any map-specific reconciliation logic is made more complex.
