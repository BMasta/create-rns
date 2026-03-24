# Dynamic Mining Recipe Plan

## Goal

Add a fluent `MiningRecipeBuilder` that can:

- register mining recipes for inclusion in the main dynamic built-in datapack,
- support compat-gated recipes through `requireMod(...)`,
- keep recipe definitions centralized in code instead of splitting compat recipes into separate handwritten JSON files.

This plan covers the first implementation pass only. It does not assume that every existing handwritten mining recipe is migrated immediately.

## Current State

- `MiningRecipe` is a runtime recipe type with codec/network support in `content.deposit.mining.recipe`.
- Default mining recipes are still handwritten JSON files under `src/main/resources/data/create_rns/recipe`.
- Compat deposit worldgen already uses a code-driven registration path plus dynamic built-in pack content.
- The main dynamic built-in pack currently emits deposit worldgen content only; it does not yet emit recipe JSON.

## Desired Builder Shape

`MiningRecipeBuilder` should follow the same overall ergonomics as `ShapedRecipeBuilder`:

- static entrypoint: `MiningRecipeBuilder.create(ResourceLocation depositBlockId)`
- fluent non-static methods for every configurable mining recipe field
- `requireMod(String modId)` to mark a recipe as compat-gated
- a final save/register method that adds the recipe to `MiningRecipeBuilder`'s static list of configured mining recipe entries
- recipe ids are always derived from the corresponding deposit block id

The builder should own the full recipe definition for:

- `deposit_block`
- `replace_when_depleted`
- `durability`
- `yields`
- yield item weights
- catalysts
- JEI slot color

The builder remains a standalone recipe-definition API, but deposit registration should expose a convenience hook so mining recipes can stay colocated with their corresponding deposit block definitions:

```java
DynamicDatapackDepositEntry
        .create("gold")
        ...
        .block("gold_deposit_block")
        .transform(depositBlock(MapColor.GOLD))
        .recipe(id -> MiningRecipeBuilder.create(id)
                ...
                .save())
        .register();
```

`DepositBlockBuilder.recipe(...)` should be optional and should only provide the remembered deposit block id to the callback. `MiningRecipeBuilder` remains responsible for validation and `save()`, matching the existing `ShapedRecipeBuilder`-style workflow where the recipe builder itself performs the final save step.

## Proposed Architecture

### 1. Add a dedicated builder for mining recipe definitions

Introduce `MiningRecipeBuilder` as a recipe-data builder for dynamic-pack emission.

Recommended responsibilities:

- store the target recipe id
- store the deposit block id
- store optional replacement block id
- store optional durability payload
- store ordered yields
- store optional required mod id
- leave JSON serialization to the later dynamic-pack emission step

Recommended defaults:

- default recipe id derived from the deposit block id
- omit optional JSON sections unless explicitly configured
- validate required fields before saving

Registration should live in the existing `RNSDeposits` definition chains through `DepositBlockBuilder.recipe(...)`; `RNSRecipes` is not involved in mining recipe default registration.

### 2. Model yields with a standalone builder

To keep the API readable, introduce `YieldBuilder` as a standalone class used by `MiningRecipeBuilder`, with lightweight yield-spec data behind it as needed.

Minimum capabilities:

- add a yield with one or more weighted items
- optional `chance`
- optional catalyst name list
- optional JEI slot color
- item entrypoints accept string registry ids and validate them when configured

This keeps callsites compact while still mirroring the recipe structure without requiring the builder to own serialized JSON.

### 3. Route builder output through a static configured-entry list

Every mining recipe defined through the builder should register into a static list owned by `MiningRecipeBuilder`.

`requireMod(...)` should only affect whether the recipe is emitted from that registry:

- no required mod:
  always emit the recipe into the main dynamic built-in pack
- required mod present on builder:
  emit the recipe only when the mod is enabled

This keeps recipe inspection and generation in one place and makes the dump utility the primary way to inspect the full recipe set.

### 4. Add a mining recipe configured-entry list

`MiningRecipeBuilder` should expose a `ConfiguredEntry` record similar in role to `DynamicDatapackDepositEntry.ConfiguredEntry`, and recipe definitions should accumulate into a simple static list on `MiningRecipeBuilder` itself.

Suggested shape:

- stores recipe id
- stores nullable required mod id
- stores immutable structured mining recipe data derived from the builder when `save()` is called
- exposes `isEnabled()` using the same runtime-vs-dump rules already established for dynamic pack content
- exposes a static list getter used by `DynamicDatapackContent`

This should remain separate from `DynamicDatapackDepositEntry`, since the recipe lifecycle and JSON target path are different even if the compat gating rules are similar. `ConfiguredEntry` should be an immutable snapshot created from builder state at save time, rather than a wrapper around mutable builder state or prebuilt `JsonObject` payloads.

### 5. Extend dynamic pack content generation

Add a new content method to `DynamicDatapackContent` responsible for mining recipe files.

Recommended behavior:

- read the configured mining recipe entry list
- filter to enabled entries
- emit files to `create_rns/recipe/<recipe_name>.json`

Then add this content method to `RNSPacks.createMainPack()` so mining recipe JSON is part of the main built-in pack.

This keeps all mining recipes in the same generated built-in pack output, with compat recipes synchronized to the same mod-presence checks that already control compat deposits.

### 6. Keep dump-tool behavior aligned

The dump tool already supports dump-mode mod selection for dynamic built-in pack content.

Mining recipe entries should use the same dump-mode behavior so that:

- `default` dumps exclude compat mining recipes
- `with_compat` dumps include them

This should happen automatically if the compat mining recipe registry uses the same enable-check pattern as other dynamic pack entries. Since mining recipe registration is intended to live in `RNSDeposits`, the dump bootstrap path must initialize that same registration flow before pack snapshots are built.

## Suggested API Draft

High-level example only:

```java
MiningRecipeBuilder.create(CreateRNS.asResource("iron_deposit_block"))
        .replaceWhenDepleted(CreateRNS.asResource("depleted_deposit_block"))
        .durability(200000, 75000, 0.2f)
        .yield(y -> y.item(Items.COBBLESTONE))
        .yield(y -> y
                .chance(0.05f)
                .item(Items.RAW_IRON)
                .catalyst("faint_resonance")
                .catalyst("overclock")
                .jeiSlotColor(-6910797))
        .save();
```

Compat example:

```java
MiningRecipeBuilder.create(CreateRNS.asResource("uranium_deposit_block"))
        .requireMod("createnuclear")
        .replaceWhenDepleted(CreateRNS.asResource("depleted_deposit_block"))
        .durability(...)
        .yield(...)
        .save();
```

Both examples would register into `MiningRecipeBuilder`'s configured-entry list; `requireMod(...)` would only affect whether the compat recipe is emitted.

For ergonomics, field setters should validate string registry ids at configuration time:

- `MiningRecipeBuilder.create(...)` still accepts the already-known deposit block `ResourceLocation`
- `replaceWhenDepleted(String blockId)` validates and stores a block id string
- `YieldBuilder.item(String itemId)` and `YieldBuilder.item(String itemId, int weight)` validate and store item id strings

Using strings for all non-entrypoint registry references keeps callsites dump-safe and allows compat recipes to reference blocks/items from mods that are not compile-time dependencies of this project.

## Implementation Phases

### Phase 1. Builder and dynamic registry scaffolding

- add `MiningRecipeBuilder`
- add the configured-entry list and `ConfiguredEntry` record to `MiningRecipeBuilder`
- add dynamic content emission for compat mining recipes
- wire the main dynamic pack to include compat mining recipe files

Deliverable:

- builder exists
- compat recipe entries can be registered in code
- dynamic pack can emit recipe JSON for enabled compat entries

### Phase 2. Default recipe migration

- migrate one existing mining recipe through `MiningRecipeBuilder`'s configured-entry list
- add one compat mining recipe for the uranium deposit
- compare dumped JSON against current handwritten recipes
- validate the generated recipe behavior and dumped output before migrating the rest
- remove the handwritten JSON file for any default recipe in the same phase it is migrated, to avoid duplicate recipe ids between static resources and dynamic pack output

Deliverable:

- one existing recipe and one uranium compat recipe are emitted through the dynamic pack and match expected behavior

### Phase 3. Compat recipe migration

- migrate the remaining handwritten mining recipes into code
- remove now-redundant handwritten mining recipe JSON
- verify `default` vs `with_compat` dump differences

Deliverable:

- the full mining recipe set is emitted from the dynamic pack, with compat recipes appearing only in the compat-enabled dynamic dump and runtime pack

## Recommended First Slice

Implement the minimum end-to-end path in this order:

1. add the configured-entry list and `ConfiguredEntry` record to `MiningRecipeBuilder`
2. add `MiningRecipeBuilder` with a minimal but complete recipe-definition model
3. route `requireMod(...)` recipes into the configured-entry list
4. add `DynamicDatapackContent.miningRecipes()`
5. add mining recipe content to `RNSPacks.createMainPack()`
6. migrate one existing recipe and one uranium compat recipe as proof of the pattern

That will validate the architecture before converting the full mining recipe set.

## Documentation Follow-Up

When the implementation starts changing behavior rather than just scaffolding, the accompanying docs should be updated as part of the same change:

- `docs/implementation.md` for the new code-driven mining recipe registration model
- `docs/datapack.md` to reflect that default mining recipes now come from the dynamic built-in pack rather than only from handwritten resource files
