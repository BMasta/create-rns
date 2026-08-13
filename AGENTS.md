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
* Preferred line length is 120 characters (not a hard rule).
* Class members are ordered in the following way (earlier rules take precedence):
    * Static members come before non-static members (except classes).
    * Simple enums come first (with no methods), then properties, then methods, then inner classes, records, complex enums.
    * Final properties come before non-final properties.
    * Public members come first, then protected, then private.
    * Uninitialized properties come before initialized properties.
* Rough Order:
    1. public->protected->private simple enum.
    2. public->protected->private static final uninitialized property.
    3. public->protected->private static final initialized property.
    4. public->protected->private static non-final property.
    5. public->protected->private static method.
    6. Same as above, but non-static.
    7. public->protected->private class or record or complex enum.
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
* Resources in `src/generated/resources` are generated automatically using datagen and are not to be modified/added manually.
* Data-driven design: catalysts, deposit specs, and miner specs are defined as JSON datapack registry entries, not hardcoded. This makes them extensible by modpacks without code changes.
* In-memory pack outputs can be inspected via the Gradle task `dumpDynamicDatapacks`, which writes generated pack files to `src/generated/builtin_packs/default` and `src/generated/builtin_packs/with_compat`.
* This project uses the neoforge game test framework for testing. Test cases live in `src/gameTest`.

## Instructions
* If any fields that can be overridden via datapacks or KubeJS change, the documentation in `docs/` and examples in `docs/templates` should be updated.
* Do not update `README.md` in the project root.
* Do not stage, commit, or push code changes, create or delete branches, or use any other git commands for purposes other than inspecting code. 
