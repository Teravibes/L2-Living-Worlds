# Living World Module Framework

This document defines the module framework for this server: the platform contract that lets any optional
feature be installed, enabled, disabled, and removed as one self-contained unit, without editing stock game
code and without rebuilding the server to turn a feature off.

It is the architecture and specification. People writing a module should read
`docs/MODULE_AUTHORING_GUIDE.md`, which is the practical how-to built on this contract.

The server here is a single-player Living World, so the operator and the player are the same person. When
this document says the operator installs or removes a module, that is the player deciding what they want.

## Scope: server-side gameplay modules

V1 is a server-side gameplay module framework. A module can add and change server behavior: scripts, config,
gameplay rules, NPCs, and datapack definitions that the server owns. It cannot ship new client assets. New
icons, models, animations, sounds, textures, and interface windows require changes to the game client, which
is a separate future installation concern and is out of scope here. A launcher package format that carries a
`server/` and a `client/` side may come later; this framework is the server side of it.

## 1. Goals and principles

1. A feature is a self-contained package under one directory. Installing it is placing that directory. Removing
   it is deleting that directory.
2. Turning a feature on or off never requires recompiling the server. It requires a restart (see section 4).
3. A module never edits stock game classes. It attaches only through published extension points.
4. The framework is general and mandatory. Every optional feature is built this way, not just the first one.
5. Removing a module leaves the server behaving exactly as if the module had never been installed.
6. Player data is never destroyed silently. Disable keeps data. Removal deletes the module directory. Deleting
   stored data is a separate, explicit, opt-in action.

## 2. The platform and the module: the central split

The word "core" hides two different things, and keeping them apart is the whole idea.

- The platform is the server engine, compiled into `GameServer.jar`. It publishes a fixed set of extension
  points, the modding API, exposed to modules through a stable `ModuleContext`. Only the core team changes the
  platform.
- A module is an optional feature that plugs into those extension points. It ships as scripts, config, data,
  and optional SQL. Anyone can write one. A module never contains an edit to a stock class.

Some features need a hook the engine does not expose yet (for example, identifying a pet by its physical item
instead of its template, or reinterpreting a client packet). The answer is never to edit a stock class from
the module. The answer is to add one generic, feature-neutral extension point to the platform, once, and let
the module attach to it. That extension point is inert until a module registers against it, so the stock
server is unchanged.

Consequence: "does feature X need core changes" has two answers. The module needs none. The platform may need
a new generic hook added once, after which that feature and every later feature reuse it.

The guiding rule: core provides capabilities, modules provide features.

## 3. The module API surface: ModuleContext

Modules receive a single stable object, `ModuleContext`, that exposes the supported extension operations.
Modules still use ordinary game types such as `Player`, `Npc`, `Skill`, and `Item` directly; the framework
does not wrap the whole game model. The boundary is deliberately partial: supported extension operations go
through `ModuleContext`, everyday gameplay objects do not get artificial wrappers.

The V1 `ModuleContext` surface:

```text
ModuleContext
  config       generic typed access to the module's own configuration
  events       register game event listeners
  handlers     register item, bypass, voiced, admin, effect, and target handlers
  hooks        contribute rules to generic behavior hooks (section 3.4)
  logging      a module-scoped logger
  resources    the module's own resource roots, exposed to data loaders
```

`GameModule` is the entry-point contract every module implements:

```java
public interface GameModule
{
    void onEnable(ModuleContext context);

    // Reserved for a future hot-unload path. Not called in V1, which is restart-based (section 4).
    default void onDisable(ModuleContext context) {}
}
```

A module's entry point implements it and receives the context:

```java
public final class FishingModule implements GameModule
{
    @Override
    public void onEnable(ModuleContext context)
    {
        if (!context.config().getBoolean("Enabled", false))
        {
            return;
        }
        context.events().register(...);
        context.handlers().register(...);
    }
}
```

Two fixed naming conventions bind a module to the platform, so nothing has to be declared for them:

- The entry-point class lives in the package `modules.<id>`, with any hyphens in the id removed. A module with
  id `beast-taming` uses the package `modules.beasttaming`. This mirrors the module's `scripts/` location and
  guarantees package uniqueness, since ids are already unique. Module code never uses the stock `custom.*`
  datapack packages.
- The module's configuration is a single file at `config/module.ini`, a fixed name the platform loads and
  exposes through `context.config()`, exactly as the manifest is always `module.json`. It is not named after
  the module and not declared in the manifest.

## 3.1 What the platform must provide (the modding API)

This is the catalogue of extension points, marked available where the engine already offers it and build where
it must be created.

- Module discovery and lifecycle (build). A `ModuleManager` scans the modules root, reads each manifest, runs
  the startup validation sequence (section 6), and enables or refuses each module. It is the single authority
  the launcher and server read, and it owns the installed-module list.
- Manifest (build). Every module ships a machine-readable `module.json`. See section 5.
- Configuration service (build). The platform loads each module's config from the fixed file `config/module.ini`
  in its own directory and exposes it through `context.config()`. A module never edits `ConfigLoader` or any
  central config class, and a small module needs no dedicated Java config class.
- Module resource roots (build). Data loaders must be given the enabled modules' resource roots in addition to
  the stock datapack roots, so a module's own item, skill, NPC, spawn, and multisell files load. The item, skill,
  npc, spawn, and multisell loaders are wired: each scans the enabled modules' roots from
  `ModuleResourceRegistry` alongside its stock folders, and contributes nothing when no module is enabled. HTML is
  the exception and is not wired as a module root yet: a module's dialogue should be built as a string in code for
  now (`NpcHtmlMessage.setHtml`), and a clean module HTML root keyed per module is a later step.
- Script compilation from the modules root (build). The script engine compiles module scripts from
  `modules/<id>/scripts/`. Today it compiles only under `data/scripts`.
- Handler registration (available). `ItemHandler`, `SkillHandler`, `BypassHandler`, `VoicedCommandHandler`,
  `AdminCommandHandler`, `EffectHandler`, and `TargetHandler` each expose `registerHandler(...)`. A module
  registers through `context.handlers()`, never by editing `MasterHandler`.
- Event listeners (available). The event system (`@RegisterEvent`, `ListenerRegisterType`, the `On*` holders)
  lets a module react to game moments without touching the class that fires them. This is the first tool to
  reach for. If an event exists, no platform change is needed. A module registers through `context.events()`,
  which is live: it exposes the four global scopes (global, players, npcs, monsters), records each listener
  against the module, and owns it for a future unload. Id-specific moments (one npc's dialogue) stay the domain
  of a quest or handler.
- Generic behavior hooks (build, as needed). See section 3.4.
- Identity range registry (build). See section 7.
- Database lifecycle (build plus convention). See section 3.5.
- Launcher integration (build). See section 8.

## 3.4 Generic behavior hooks

Some behavior has no event, because events are notifications and cannot change an outcome mid-calculation. For
those, the platform provides small, generic, inert-by-default registries that modules contribute rules to.
Each returns a neutral value when empty, so the stock server is unaffected. Known needs so far:

- A combat multiplier hook, for a module that scales outgoing damage by its own rule.
- A per-entity data resolver, for a module that must key a creature's data by its physical object instead of
  its template (for example, an individual tamed pet identified by its collar item).
- A packet pre-dispatch hook on a small set of client packets, for a module that must reinterpret a request the
  untouched client cannot be changed to send differently.

These are added one at a time, only when a real module needs them, and always generic. A module-specific
constant never enters a stock method.

Every hook declares its composition rule and a deterministic order, so the result never depends on filesystem
iteration:

| Hook type | Composition rule |
|---|---|
| Multiplier | Multiply all contributors |
| First result | Use the first non-empty result |
| Transform | Apply contributors in sequence, each on the previous output |
| Cancellable | A contributor may stop further processing per the hook's documented rule |
| Notification | Invoke every contributor |

Order is by declared priority, then module id as tie-breaker. The direction is fixed: higher priority runs
first, and equal priority is ordered by ascending module id.

## 3.5 Database lifecycle

A module that needs storage creates additive, namespaced tables listed in its manifest. Disabling a module
never touches its tables. Removing a module deletes its directory; deleting its tables is a separate, explicit,
opt-in step, because those tables can hold player progress. The platform provides the install path and an
opt-in cleanup path. It never auto-drops data. A module is trusted executable code, not a safe data-only
package (section 9), so its SQL runs with the same trust as its Java.

Implementation status: the install path is live. A module declares a `database` block (`install`, optional `remove`,
`tables`); the platform runs the install script against the server's connection pool when the module is enabled,
before the module's code runs, so its tables exist for `onEnable`. The script runs on every enable, so it must be
idempotent (`CREATE TABLE IF NOT EXISTS`); a failing statement fails the install and refuses the module. The platform
never runs the `remove` script on its own: dropping a module's tables stays an explicit, opt-in action, because they
can hold player progress. Disabling and removing a module never touch its tables.

## 4. Lifecycle and the levels of removal

The V1 lifecycle is restart-based. Flipping the enable flag prevents a module from registering at the next
startup; it does not unregister listeners, handlers, or hooks from a running server, and data already loaded
into global registries stays until restart. This is honest and reliable for a launcher-controlled offline
server.

1. Install. Place the module directory under the modules root.
2. Enable or disable. Set the enable flag in the module's config.
3. Restart to apply. The change takes effect on the next start. A disabled module does not compile or register.
4. Uninstall. While the module is disabled or the server is stopped, delete its directory. Optionally, as a
   separate opt-in, drop its tables.

None of this rebuilds `GameServer.jar`. That is the point, and it is only possible because no module logic
lives in the jar. The launcher and `ModuleManager` own module state and present it to the user (section 8).
`config/Scripts.xml` remains an internal exclusion mechanism and a proof that exclusion works; it is not the
user-facing switch.

Registration ownership tracking is built in V1 even though changes apply on restart: every listener, handler,
hook, and scheduled task registered through `ModuleContext` is tracked against its module. This gives
diagnostics now and leaves a foundation for possible later hot unloading, which is deferred.

## 5. Canonical module package layout

Each module is one self-contained directory under the modules root.

```text
game/modules/<id>/
  module.json          # manifest: identity, ownership, lifecycle
  config/
    module.ini         # the module's config, a fixed name, including its enable flag
  scripts/             # the entry point (implements GameModule) and module classes, package modules.<id>
  data/                # module-owned item, skill, NPC, HTML, multisell files
  sql/
    install.sql        # optional, additive tables
    remove.sql         # optional, the explicit opt-in cleanup
  MODULE.md            # human-readable notes for maintainers
```

Every path a module owns is inside this directory and is declared in the manifest relative to the module root.
Nothing a module owns lives inside a stock class or a shared file. This single-directory ownership is what
makes uninstall safe (section 3.8 of the review response, and section 8 here).

## 6. The manifest

`module.json` is the contract between a module, the platform, and the launcher.

```json
{
  "id": "beast-taming",
  "name": "Beast Taming",
  "version": "1.0.0",
  "apiVersion": "1",
  "entrypoint": "modules.beasttaming.TamingModule",
  "description": "Adds a configurable beast-taming system.",
  "author": "Author Name",
  "priority": 100,
  "dependencies": [],
  "conflicts": [],
  "resources": {
    "items": ["data/items/*.xml"],
    "skills": ["data/skills/*.xml"],
    "npcs": ["data/npcs/*.xml"],
    "html": ["data/html/"]
  },
  "reserves": {
    "items": [[9300, 9309]],
    "skills": [[9300, 9309]]
  },
  "database": { "install": "sql/install.sql", "remove": "sql/remove.sql", "tables": ["tamed_pet"] },
  "hooks": ["combat-multiplier", "entity-data-resolver"]
}
```

Rules for the manifest:

- Every path is relative to the validated module root. Absolute or traversing paths (for example `../`) are
  rejected. The launcher and platform never delete a path taken verbatim from a manifest.
- The module id is validated before it is ever used as a directory name.
- `apiVersion` names a defined, supported API surface. A module the platform cannot satisfy is refused with a
  clear message.
- `entrypoint` is the `GameModule` class the platform instantiates.
- `priority` sets hook and load order (higher first, then ascending id).
- `dependencies` and `conflicts` drive startup validation (section 6.1). No resolver, no downloading.
- `reserves` declares every id range the module claims (section 7).
- `database` declares install, opt-in cleanup, and owned tables. Omit if the module has none.
- Unknown fields follow a documented schema policy: warn or reject, not silently ignore.

## 6.1 Startup validation sequence

Order is a correctness requirement. Collision and compatibility checks complete before any module resource
enters a live registry or any module code runs.

1. Discover module manifests under the modules root.
2. Validate each module id, API compatibility, dependencies, conflicts, and declared paths.
3. Load or index existing stock and custom datapack ids.
4. Validate module id reservations and resource definitions against that index and against each other.
5. Only then load module resources and execute module entry points.

Failures at step 2 or 4 refuse the offending module with a clear reason and do not load its resources.

Dependency and conflict handling in V1 is intentionally small: direct required dependencies and declared
conflicts only. A required module missing, an incompatible version, or a declared conflict refuses the
affected module. A dependency cycle (A requires B, B requires A) is an explicit validation failure, not
something the platform resolves. These fields are in the V1 schema, but dependency handling does not block the
first Hello World milestone.

Implementation status: dependency, conflict, and cycle handling is live and runs before any module registers.
A module is refused when a required dependency is missing, is installed but disabled, or was itself refused (the
failure cascades); when it declares a conflict with an enabled module; or when it sits on a dependency cycle. Only
enabled modules take part, so a disabled module neither collides nor satisfies a dependency. Version-qualified
dependencies are not parsed yet: a dependency is an id only, and version compatibility is deferred.

## 7. Identity range governance

- A module declares every id range it uses in `reserves`.
- The platform validates ranges three ways: module against module, module against loaded stock datapack ids,
  and module against project custom datapack ids. This catches a range that no other module claims but that the
  base game already uses.
- Validation runs at step 4 of the startup sequence, before resources load, so a collision is caught before a
  definition can overwrite another.
- A published range table lists taken ranges so authors pick a free block up front, but the boot-time check,
  not the table, is the enforcement.

Implementation status: the module-against-module check is live. Two enabled modules whose reserved ranges overlap
are both refused before either registers resources or runs, with a message naming the type and the overlapping
ranges. The module-against-base-game check is live for items, skills, and npcs: each of those loaders captures the stock and
custom ids as a separate base game set, and a module whose reserved range hits one of those ids is refused before its
code runs. Because that check runs after the loaders have merged module data, a colliding module's data has already
loaded for that one boot; the refusal stops its code and logs the offending id so the operator fixes the range, and
the next start is clean. Spawns and multisell have no reserve of their own (a spawn references npc ids; a multisell
references item ids), so only items, skills, and npcs are reserved and checked.

### Reserved range registry

This is the published registry. When a module reserves a range, add a row here in the same change so other
authors can pick a free block without waiting for a boot-time collision. Keep item, skill, and NPC ranges in
their own columns; leave a cell blank when a module reserves nothing of that kind.

| Module id | Items | Skills | NPCs |
|---|---|---|---|
| `custom-item` | 60000-60009 | | |

The `custom-item` reference module is the first to claim a range (items 60000-60009, using 60000 for the Living World
Token). Item ids are capped at 65535 by the item schema (`xs:unsignedShort`), so authors pick a free block below that
ceiling. Boot-time collision checking is a later milestone; for now this table is the manual coordination point. The
example ranges used elsewhere in these documents (`fishing` items 9400-9419, `beast-taming` items and skills 9300-9309)
are illustrations, not reservations, and are not recorded here until a real module claims them.

## 8. Launcher integration

The launcher reads the installed-module list from `ModuleManager` and shows an Installed Modules panel. Per
module it presents state and actions:

```text
Installed: Yes
Enabled:   Yes
Status:    Restart required
```

- Disable and Enable flip the module's enable flag; the panel shows that a restart is required to apply.
- Remove deletes the module's single validated directory. Dropping its tables is a separate, clearly labelled,
  opt-in action, never part of a normal remove.
- The launcher never deletes a path supplied by a manifest. Any path outside the module directory is computed
  by trusted platform code.
- The launcher shows a trust warning (section 9).

The launcher cannot delete platform hooks compiled into the jar, and does not need to: those hooks are inert
without a module registered.

Implementation status: the Installed Modules panel exists in the compiled launcher (`ModulesWindow`, backed by
`Core/Modules`). It scans `game/modules`, shows each module's state, toggles the enable switch in the module's own
`config/module.ini` with a restart-required note, removes a module by deleting only its one validated folder (never
its database tables), and shows the trust warning. It reads and writes the same `config/module.ini` switch the game
server reads, so the launcher and the server never disagree about what is enabled. This code is written but not yet
built or run, since the launcher is a .NET application built on Windows.

## 9. Trust model

Modules are trusted, executable Java code. A dynamically compiled module runs with the server process's
permissions and can read files, start processes, and make network requests. The framework does not attempt a
Java security sandbox; building one would be a large project of its own and is out of scope. Instead the
launcher states plainly:

> Modules may contain executable code. Install modules only from sources you trust.

Module SQL carries the same trust. A module is never presented as a safe data-only package.

## 10. Rules

- A module never edits a stock game class. If it needs behavior the engine does not expose, that becomes a
  generic platform hook, added once, not a module edit.
- A generic hook returns a neutral value when no module is registered, so the stock server is unchanged, and
  declares its composition and order.
- Every module owns a manifest that fully declares its files, ids, and tables, with all paths inside the module
  directory.
- Every entry point checks its enable flag first and does nothing when it is false.
- No module hardcodes another module's ids, and no id range is used without a validated reservation.
- Disable and Remove never delete player data. Deleting stored data is always a separate, explicit action.
- A module is verified in two states before release: enable flag off, and directory removed. In both, the
  server must behave exactly as stock.

## 11. V1 scope and milestones

V1 proves the architecture on the smallest possible feature, then lets the first real feature drive real hooks.

1. Hello World module. The server discovers a directory under the modules root, validates its manifest and API
   version, loads its config generically, compiles its scripts from the module root, invokes its `GameModule`
   entry point, and the module registers one low-risk feature such as a voiced command. Disabling it and
   restarting prevents registration. Removing its directory lets the server start normally. This proves the new
   behavior, not that the stock engine can already compile scripts.
2. Registration ownership tracking through `ModuleContext`, as described in section 4.
3. Module resource roots, added only to the loaders the first real module needs.
4. Beast Taming as the first serious module, replacing its stock edits with generic hooks and revealing which
   hooks and data services are genuinely necessary. When it needs a core change, add the smallest reusable
   platform capability, not a taming-specific shortcut.

Deferred beyond V1: hot unloading, client patch installation, arbitrary SQL uninstall automation, complex
dependency graphs, a module marketplace, exhaustive hook coverage, and broad resource support for every
datapack type.

## 12. Versioning and compatibility

- The modding API has a version, recorded as `apiVersion` in each manifest.
- The platform refuses, with a clear message, to enable a module whose `apiVersion` it cannot satisfy.
- New extension points are additive within a major API version. A breaking change bumps the major version and
  ships with a migration note.

## 13. Acceptance criteria for the first release

V1 is successful when all of the following hold:

- A module installs by placing one self-contained directory under the modules root.
- No stock source file is edited for any installed module.
- A module has a validated manifest and an explicit API compatibility check.
- Configuration loads generically from inside the module directory.
- Module startup order is deterministic.
- Enabled state is controlled by the platform or launcher, not by hand-editing `Scripts.xml`.
- Enable and disable clearly require a restart.
- A disabled module neither compiles nor registers gameplay behavior.
- Removing a disabled module's directory leaves no scattered files.
- Resource and id conflicts fail early with readable errors.
- The launcher warns that modules are trusted executable code.
- The documentation labels V1 as server-side modding.

## 14. Components to build

| Component | Responsibility |
|---|---|
| `ModuleManager` | Discovery, validation, deterministic ordering, enable-state, startup orchestration |
| `GameModule` | Stable entry-point contract implemented by every module |
| `ModuleContext` | Supported access to config, events, handlers, hooks, logging, and resources |
| `ModuleManifest` | Parsed and validated `module.json` |
| `ModuleConfig` | Generic typed access to module-local configuration |
| `ModuleResourceRegistry` | Enabled module resource roots exposed to the data loaders that need them |
| Generic hooks | Combat multiplier, entity-data resolver, packet pre-dispatch, each inert by default |
| Launcher integration | Install, enable, disable, status, restart workflow, safe single-directory removal, trust warning |

Before implementation, inspect the current startup sequence to find the exact point at which `ModuleManager`
initializes before ordinary script registration is finalized.
