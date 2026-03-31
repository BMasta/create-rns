# Create: Rock & Stone

## Project Description
This branch targets Minecraft 1.20.1 on Forge and serves as the backport line for Create: Rock & Stone.
Its primary goal is to keep parity with features added in the main 1.21.1 NeoForge branch by porting them to
the 1.20.1 Forge while preserving the same gameplay behavior wherever possible.

Backport work in this branch should prioritize:
1. Porting new gameplay features from main with equivalent player-facing behavior.
2. Adapting implementation details to Forge/Create 1.20.1 API differences without changing feature intent.
3. Keeping data formats, progression expectations, and cross-system interactions as consistent as possible with main.


## Dependencies

All versions are defined in `gradle.properties`. Java version is 17.

| Dependency        | Type | Version Property         | Role                                                                          |
|-------------------|------|--------------------------|-------------------------------------------------------------------------------|
| Minecraft         | Hard | `minecraft_version`      | Base game (1.20.1)                                                            |
| Forge             | Hard | `forge_version`          | Mod loader                                                                    |
| Create            | Hard | `create_version`         | Parent mod this is an addon for                                               |
| Flywheel          | Hard | `flywheel_version`       | Rendering engine (transitive via Create)                                      |
| Registrate        | Hard | `registrate_version`     | Content registration framework                                                |
| Ponder            | Hard | `ponder_version`         | In-game tutorial system                                                       |
| JEI               | Soft | `jei_version`            | Recipe viewer; mod provides a JEI plugin for mining recipes and catalyst info |
| JourneyMap        | Soft | `journeymap_version`     | Optional client waypoint integration surface used for deposit markers         |
| Xaero's Minimap   | Soft | `xaero_minimap_version`  | Optional client waypoint integration surface used for deposit markers         |
| Xaero's World Map | Soft | `xaero_worldmap_version` | Optional client map UI that displays Xaero waypoint data                      |

## Architecture and Design
* When Minecraft/loader codec behavior diverges between the 1.20.1 Forge backport and the 1.21.1 NeoForge branch, prefer mod-owned compatibility codecs over branch-specific datapack format forks.
* Attachment catalyst holder sets use a shared compatibility codec so datapack authors can keep using a single block id, a list of block ids, or a block tag on both branches.

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
