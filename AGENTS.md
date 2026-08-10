# Create: Rock & Stone

**************************************IMPORTANT**************************************
DO NOT TRY TO EXECUTE THE FOLLOWING GRADLE TASKS YOURSELF UNDER ANY CIRCUMSTANCES:
* runClient
* runServer
* runGameTestServer

IF ANY OF THESE ARE NEEDED FOR TESTING, ASK TO DO IT FOR YOU.
*************************************************************************************

## Project Description

The goal of this project is to develop a mod for Minecraft 1.21.1.
The mod is called "Create: Rock & Stone" and is itself an addon to Create mod.

Create: Rock & Stone adds ore deposits in the world for player to find using a Deposit Scanner.
The deposits are scattered across the world and cannot be moved.
Instead, player is encouraged to set up miners and mine resources from deposits on site.
The miners are multiblock contraptions that are created with a special type of bearing added by the mod,
the Miner Bearing. The resources that a miner can mine as well as its efficiency can be modified by adding
the so-called Catalysts.

The current catalysts are:
1. Attachment-based catalysts - achieved by attaching a sufficient number of specific miner attachment blocks to the miner contraption. The default datapack uses this for the various resonance catalysts.
2. Overclock - achieved by attaching a fluid container to the miner contraption and filling it with lava, which is then consumed as the miner is working. More catalysts may be added in the future.

## Dependencies

All versions are defined in `gradle.properties`. Java version is 21.

| Dependency         | Type | Version Property             | Role                                                                          |
|--------------------|------|------------------------------|-------------------------------------------------------------------------------|
| Minecraft          | Hard | `minecraft_version`          | Base game (1.21.1)                                                            |
| NeoForge           | Hard | `neo_version`                | Mod loader                                                                    |
| Create             | Hard | `create_version`             | Parent mod this is an addon for                                               |
| Flywheel           | Hard | `flywheel_version`           | Rendering engine (transitive via Create)                                      |
| Registrate         | Hard | `registrate_version`         | Content registration framework                                                |
| Ponder             | Hard | `ponder_version`             | In-game tutorial system                                                       |
| JEI                | Soft | `jei_version`                | Recipe viewer; mod provides a JEI plugin for mining recipes and catalyst info |
| Jade               | Soft | `jade_version`               | Tooltip overlay; mod provides a plugin showing remaining deposit resources    |
| JourneyMap API     | Soft | `journeymap_api_version`     | Optional client waypoint integration surface used for deposit markers         |
| Xaero's Minimap    | Soft | `xaero_minimap_version`      | Optional client waypoint integration surface used for deposit markers         |
| Xaero's World Map  | Soft | `xaero_worldmap_version`     | Optional client map UI that displays Xaero waypoint data                      |
| Create Aeronautics | Soft | `create_aeronautics_version` | Optional physics-contraption compatibility surface                            |
| Sable              | Soft | `sable_version`              | Required physics runtime for optional Create Aeronautics compatibility        |

## Coding Style Guidelines
* Preferred line length is 120 characters.
* Class members are ordered in the following way (earlier rules take precedence):
    * Static members come before non-static members (except classes).
    * Simple enums come first (with no methods), then properties, then methods, then inner classes, records, complex enums.
    * Final properties come before non-final properties.
    * Public members come first, then protected, then private.
    * Uninitialized properties come before initialized properties.
* Rough Order:
    1. public->protected->private simple enum.
    1. public->protected->private static final uninitialized property.
    2. public->protected->private static final initialized property.
    3. public->protected->private static non-final property.
    4. public->protected->private static method.
    5. Same as above, but non-static.
    4. public->protected->private class or record or complex enum.
* The rules above can be broken if the locality of certain class members greatly improves readability. E.g. public method overrides that delegate to a main private method can and should be placed adjacent to each other.
* Consider factoring out a code block into a helper method ONLY if at least one of these is true:
    * The method gets very large (>50 loc).
    * The code block is reused, and is either larger than 3 loc, or used more than 3 times.
* Even in cases when one of the above points holds, adding a helper method is discouraged if:
    * It requires a lot of arguments.
    * Its name cannot accurately describe its behavior.
    * The code block in question is trivial to understand and factoring it out would make it substantially less readable.
* If introducing a helper method is undesirable, use line breaks to separate the logical block of code and a comment above describing what it does. The comment should focus on the intent, instead of describing the process/implementation itself.
* Try to avoid extra indentation levels whenever possible:
    * Use negative checks to return early.
    * Use `if (bad) continue;` instead of `if (good) { ... }` inside loops.
* Single-line conditional statements can omit braces. Only one statement is allowed per condition.
* Multiline conditionals must always use braces.
* A single `if - else if - else` block cannot mix single- and multiline conditionals.
* Use `var` unless the type has to be defined explicitly or if using a basic type. When assigning variables to parametrized types, prefer adding type to the constructor call and not the variable unless you have to specify the variable type regardless (e.g. class properties).
* Hardcoded literals must be defined at the top level. No magic numbers unless all of the above are true:
    * They are easily understood from context.
    * They are unlikely to ever change.
* The hierarchy of Java code, as well as conventions for naming variables, classes, and files are inspired by the Create mod and should be kept in line with it.

## Architecture and Design
* Files that register content in Create are named `All*.java`. This mod replaces it with `RNS*.java`.
* Mod ID: `create_rns`, main class: `CreateRNS`, package root: `com.bmaster.createrns`.
* Mod-provided content is predominantly registered using Registrate (similar to Create).
* Files in src/generated are autogenerated using the datagen capabilities provided by NeoForge.
* Code is located in `src/main/java`.
* Assets and data files are located in resources.
* Resources in `src/generated/resources` are generated automatically using datagen and shouldn't be modified/added manually.
* Data-driven design: catalysts, deposit specs, and miner specs are defined as JSON datapack registry entries, not hardcoded. This makes them extensible by modpacks without code changes.
* Catalyst requirement registry entries use the registry id itself as the catalyst identity and serialize requirements as an ordered `requirements` list of tagged objects. Each entry declares a `type` discriminator (currently `fluid` or `attachment`) and the payload for that requirement type, so new requirement kinds only need a tagged codec and do not require reshaping the enclosing catalyst JSON. `hide_if_present` entries are registry-backed catalyst holders so missing targets fail decoding regardless of which datapack supplied the catalyst.
* In-memory pack outputs can be inspected via the Gradle task `dumpDynamicDatapacks`, which writes generated pack files to `src/generated/builtin_packs/default` and `src/generated/builtin_packs/with_compat`.
* `dumpDynamicDatapacks` bootstraps dynamic built-in pack content through `RNSDeposits.register()` with `DepositStructureBuilder.dumpMode` enabled; in that mode deposit definitions stay centralized, Registrate block registration is skipped, and compat-gated dynamic entries (including deposit worldgen and code-registered mining recipes) are controlled by `DynamicDatapackDumpTool.getEnabledMods()` (`null` means include all compat-gated entries, otherwise the list acts as an allowlist of enabled mod ids). The dump tool emits both a no-compat pass and an all-compat pass by rebuilding pack snapshots separately for each variant.
* Default mining recipes are registered in code and emitted into the main dynamic built-in pack. Mining recipe registration is colocated with deposit definitions in `RNSDeposits` through the shared `DepositBlockBuilder.attach(...)` builder chain plus `MiningRecipeBuilder`, and compat-gated recipe emission follows the same mod-presence rules as compat deposit worldgen.
* Default deposit specs are registered in code and emitted into the main dynamic built-in pack. Deposit spec registration is colocated with deposit definitions in `RNSDeposits` through the shared `DepositBlockBuilder.attach(...)` builder chain plus `DepositSpecBuilder`, and compat-gated spec emission follows the same mod-presence rules as compat deposit worldgen and mining recipes.
* Deposit worldgen structure JSON uses the mod-owned `create_rns:deposit` structure type with a deposit-specific schema. The codec hardcodes the shared deposit defaults (underground-ores step, no terrain adaptation, rigid single-piece placement, standard liquid settings) and exposes deposit-authored placement through `placement_strategy`, `height`, and a weighted `structures` list (`id`, `weight`, `processor`).
* Compat deposit blocks may skip runtime block/item registration entirely when their required mod is absent. `DepositBlockBuilder.registerOrNull()` is used for those cases, while `registerOrThrow()` is used for non-compat entries and for code paths that require the compat dependency to be present. Compat `BlockEntry` fields that use `registerOrNull()` must be treated as nullable.
* Targeted vanilla integration points may be implemented via Mixins declared in `${mod_id}.mixins.json` when no stable mod API hook exists.
* Compat plugins (JEI, Jade, optional Xaero and JourneyMap map integrations) live in a `compat/` package and are conditionally loaded when the respective mod is present.
* KubeJS integration is exposed through a KubeJS plugin discovered from `kubejs.plugins.txt`; the current custom builder type is `create_rns:deposit`, which instantiates the real `DepositBlock` and applies deposit tags when KubeJS is installed. Shared default deposit block properties are owned by `DepositBlock` itself so Registrate and KubeJS deposits stay aligned.
* KubeJS integration is split by lifecycle: `event.create('<name>', 'create_rns:deposit')` registers only the deposit block itself during startup, `ServerEvents.recipes(...)` exposes a custom `create_rns:mining` recipe schema with callback-style yield builders for reloadable mining recipe authoring, `StartupEvents.rnsCatalysts(...)` emits catalyst registry entries plus optional catalyst name/description lang keys and auto-generated `#create_rns:miner_attachments` tag entries for explicitly referenced attachment blocks, and `StartupEvents.rnsDepositStructures(...)` either defines deposit worldgen/spec entries against explicit structure ids via `.create(...)` or overlays registered built-in entries via `.tweak(...)`. Deposit structures may override their dimension-derived default biome tag with one biome or biome tag. Built-in tweaks inherit unmodified structure/spec values, replace inherited scanner-icon, map-icon, and NBT lists on the first corresponding call, and share preset/template/icon defaults with the code-side registration builders.
* Final KubeJS-generated resource and lang assembly is centralized in `RNSKubeJSAssembler`, with `RNSKubeJSPlugin` acting as the thin KubeJS-facing adapter that registers the integration surfaces and delegates assembly. Startup event callbacks are captured once per KubeJS cache lifecycle and shared by resource, language, and deposit-selection assembly; builder validation reports sourced KubeJS errors and excludes invalid output rather than surfacing as a datapack load failure.
* KubeJS startup scripts may optionally define `StartupEvents.rnsEnableDeposits(...)` to configure deposit visibility and worldgen through `event.overworld()` and `event.nether()`, then add deposits with overloaded `.deposit(...)` calls and configure placement knobs with `.spacing(...)`, `.separation(...)`, and `.salt(...)`. Every `deposit(...)` call makes that deposit scannable in the selected dimension, while the overload's `enableWorldgen` flag controls whether it is also emitted into the generated structure set. When that event has listeners, only the dimensions whose builders are explicitly requested emit replacement `create_rns:deposits` / `create_rns:nether_deposits` structure-set files, but the shared scanner-selection data and built-in `deposit_spec` overrides are still regenerated from the final selected structure list so omitted deposits cannot fall back to the mod-shipped scanner defaults. Built-in deposit weights default to their registered or `.tweak(...).weight(...)` value unless overridden through the `deposit(..., weight, ...)` overload, while KubeJS-authored custom structure placement metadata is inferred from whether the structure is selected into the built-in overworld or nether structure set.
* KubeJS builder emission tests live in the `gameTest` source set and use the `RNSKubeJSBuilderTest` harness to create KubeJS event objects plus an in-memory `RNSKubeJSAssembler`, so builder/resource assembly can be verified without booting the full KubeJS script runtime.
* The same harness also instantiates `MiningRecipeKubeRecipe` directly through a dedicated test helper, allowing method-level JSON emission, default omission, and validation-failure behavior to be covered for the custom recipe and yield builders without entering the full KubeJS server recipe event.
* Deposit scanner icon availability is now derived locally from the loaded `deposit_spec` registry, but only for specs that are both marked `scannable` and filtered through the active `#create_rns:deposits` structure tag for the current dimension, rather than being synchronized from the server through dedicated scanner-icon query payloads.
* EMI support currently goes through EMI's JEI bridge rather than a separate native EMI plugin; any EMI-specific adjustments therefore belong in the JEI compat code and should stay limited to bridge-safety behavior.
* `neoforge.mods.toml` should declare optional client-side compat dependencies only for integrations the mod actually loads against at runtime.
* Xaero World Map overlay experiments use client-only pseudo-mixins targeting `xaero.map.gui.GuiMap`, because Xaero World Map does not expose a stable public overlay hook for custom renderers.
* Create Aeronautics deposit-mobility compatibility targets its bundled `simulated` mod through an optional pseudo-mixin. Physics assembly must reject blocks in `#create_rns:deposit_blocks` exactly when the server's `movableDeposits` setting is disabled, matching piston and Create contraption behavior without making the tag statically non-movable.
* Deposit claimer instance tracking stores owner block positions keyed by sided dimension/type and resolves live mining behaviors from the owning block entity on demand. Point-based queries carry a relevant operator/deposit/player position, while area-only queries derive a representative position from the bounding box; local operating boxes must remain wholly contained in one operating space. Resolved claimers are dynamically filtered through the operating-space adapter, and lookup must prune stale positions when the block entity or behaviour is gone.
* Deposit operation is split by responsibility: `IDepositBlockOperator` owns geometry and vein discovery, `IDepositBlockClaimer` adds exclusive ownership, `HybridOperatingBehaviour` owns claimed blocks plus transient `OperatingSelection` transitions, and `MiningBehaviour` owns process construction and execution. Disk NBT stores claims and process progress but not `OperatingSelection`; local selections are reconstructed from claims, while transient server-to-client packets synchronize selection mode, space, and positions atomically.
* Deposit targeting uses dependency-free operating-space value types and an adapter boundary. A target group contains one logical space identity and positions; an operator selects exactly one group in either local-exclusive or remote-shared mode, and `MiningProcess` accesses it through the miner's parent `Level`. The vanilla adapter identifies one main space per dimension. When Sable is loaded, the guarded Sable adapter uses the containing sublevel's stable UUID or the dimension-specific main-space fallback. Both adapters return no remote candidates until cross-sublevel mining is implemented.
* Sable API types must stay in `compat/sable`; common signatures and static initialization may reference only mod-owned operating-space types. Sable's Companion API is embedded in its distribution rather than declared by its Modrinth POM, so Gradle exposes that embedded jar only on the compile classpath and does not package it with this mod.
* A local miner's selected space is revalidated from its effective mine-head tip before server-side mining advances. An identity change clears stale targets and recomputes local claims. This check must remain a constant-time identity lookup rather than a deposit or sublevel scan.
* Translation keys follow `create_rns.<category>.<key>` for mod content and the standard Minecraft pattern (`block.create_rns.*`, `item.create_rns.*`) for blocks/items.
* When creating translatable components for mod-owned keys (`create_rns.*`), prefer `CreateRNS.translatable(...)` over direct `Component.translatable(...)` calls.
* Defaulted codec fields should prefer `optionalFieldOf(..., default)` over `fieldOf(...).orElse(default)` so omitted datapack properties still use defaults while malformed present values surface codec errors instead of silently falling back.
* Releases are published through the manual GitHub Actions workflow (`.github/workflows/release.yml`).
* Release workflow inputs are `bump_type` (`patch`, `minor`, `major`, `custom`) and `custom_version` (required when `bump_type`
  is `custom`).
* `custom_version` must match `x.x.x-<digits[.digits...]>-<digits>` (for example `1.2.3-1.21.1-7`).
* Release bumps the `mod_version` in `gradle.properties` and publishes a release for that version with autogenerated notes and the built mod jar asset.

## Instructions

While doing any feature work, this file must be updated as part of the same change whenever behavior or architecture changes.

For each meaningful feature, document the following at a high level:
* What the feature does from a player perspective:
    * How it is activated or used.
    * How it behaves in normal and edge-case interactions.
    * What outcomes matter to gameplay (for example, progression, drops, constraints, or failure states).
* Core behavior model:
    * Important state transitions and lifecycle events.
    * Conditions under which behavior succeeds, fails, or changes mode.
    * Any non-obvious edge cases that must remain stable.
* Interactions with other systems:
    * Dependencies and touchpoints with existing gameplay or technical systems.
    * Important assumptions that other features rely on.
    * Expected behavior when multiple systems overlap.
* Data and asset implications:
    * Whether behavior depends on code, datapack JSON, or generated assets.
* Maintenance notes:
    * Key invariants that should remain true after refactors.
    * Known limitations or intentional tradeoffs.

Avoid writing low-level implementation details that are likely to churn quickly.
Favor stable intent and invariants so future contributors understand what must be preserved.

* Documentation updates that apply to the project in general must go to the "Architecture And Design" section in this file.
* Feature-specific updates must go to `implementation.md`.
* If any fields that can be overridden via datapacks change, the documentation in `docs/datapack.unreleased.md` should be updated.
* Do not update README.md in the project root.
