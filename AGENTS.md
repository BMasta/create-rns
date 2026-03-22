# Create: Rock & Stone

## Project Description
The goal of this project is to develop a mod for Minecraft 1.21.1 and later.
The mod is called "Create: Rock & Stone" and is itself an addon to Create mod.

Create: Rock & Stone adds ore deposits in the world for player to find using a Deposit Scanner.
The deposits are scattered across the world and cannot be moved.
Instead, player is encouraged to set up miners and mine resources from deposits on site.
The miners are multiblock contraptions that are created with a special type of bearing added by the mod,
the Miner Bearing. The resources that a miner can mine as well as its efficiency can be modified by adding
the so-called Catalysts.

The current catalysts are:
1. Various types of resonance - achieved by attaching a sufficient number of resonators to the miner contraption.
2. Overclock - achieved by attaching a fluid container to the miner contraption and filling it with lava, which is then consumed as the miner is working. More catalysts may be added in the future.


## Dependencies

All versions are defined in `gradle.properties`. Java version is 21.

| Dependency        | Type | Version Property         | Role                                                                          |
|-------------------|------|--------------------------|-------------------------------------------------------------------------------|
| Minecraft         | Hard | `minecraft_version`      | Base game (1.21.1)                                                            |
| NeoForge          | Hard | `neo_version`            | Mod loader                                                                    |
| Create            | Hard | `create_version`         | Parent mod this is an addon for                                               |
| Flywheel          | Hard | `flywheel_version`       | Rendering engine (transitive via Create)                                      |
| Registrate        | Hard | `registrate_version`     | Content registration framework                                                |
| Ponder            | Hard | `ponder_version`         | In-game tutorial system                                                       |
| JEI               | Soft | `jei_version`            | Recipe viewer; mod provides a JEI plugin for mining recipes and catalyst info |
| Jade              | Soft | `jade_version`           | Tooltip overlay; mod provides a plugin showing remaining deposit resources    |
| JourneyMap API    | Soft | `journeymap_api_version` | Optional client waypoint integration surface used for deposit markers         |
| Xaero's Minimap   | Soft | `xaero_minimap_version`  | Optional client waypoint integration surface used for deposit markers         |
| Xaero's World Map | Soft | `xaero_worldmap_version` | Optional client map UI that displays Xaero waypoint data                      |

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
* In-memory pack outputs can be inspected via the Gradle task `dumpDynamicDatapacks`, which writes generated pack files to `src/generated/builtin_packs`.
* Targeted vanilla integration points may be implemented via Mixins declared in `${mod_id}.mixins.json` when no stable mod API hook exists.
* Compat plugins (JEI, Jade, optional Xaero and JourneyMap map integrations) live in a `compat/` package and are conditionally loaded when the respective mod is present.
* Xaero World Map overlay experiments use client-only pseudo-mixins targeting `xaero.map.gui.GuiMap`, because Xaero World Map does not expose a stable public overlay hook for custom renderers.
* Translation keys follow `create_rns.<category>.<key>` for mod content and the standard Minecraft pattern (`block.create_rns.*`, `item.create_rns.*`) for blocks/items.
* When creating translatable components for mod-owned keys (`create_rns.*`), prefer `CreateRNS.translatable(...)` over direct `Component.translatable(...)` calls.
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
* Feature-specific updates must go to `docs/implementation.md`.
* If any fields that can be overridden via datapacks change, the documentation in `docs/datapack.md` should be updated.
* Do not update README.md in the project root.
