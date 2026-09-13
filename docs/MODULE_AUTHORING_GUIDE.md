# Module Authoring Guide

This is the practical guide for writing a module for this server. If you want to add an optional feature, this
is the document to follow. The architecture behind it is in `docs/MODULE_FRAMEWORK.md`; you do not need to read
that to write a module, but it explains why the rules below exist.

A module is an optional feature the operator can install, enable, disable, and remove as one self-contained
directory, without rebuilding the server. Taming, a fishing system, a minigame, a custom shop: all of these are
modules and all follow this guide.

## Scope: this is server-side modding

A module changes what the server does. It can add scripts, config, gameplay rules, NPCs, and datapack
definitions the server owns. It cannot add new client assets. New icons, models, sounds, animations, or
interface windows need changes to the game client, which this framework does not install. If your idea needs a
new icon or model to exist on the client, that part is out of scope for now.

## The one rule

Never edit a stock game class. Not `Formulas`, not `Pet`, not a packet handler, not `MasterHandler`, not
`ConfigLoader`. Everything your module does, it does from its own directory by going through the `ModuleContext`
the platform hands you, or through the event system. If you find yourself wanting to change a stock class, stop:
that is a request for a new platform hook, not something you put in your module. Raise it with the core team.

Why: a module that edits a stock class is compiled into the server jar. It cannot be turned off without a
rebuild, it can collide with other modules, and it cannot be cleanly removed. The whole point of a module is
that it can.

You will still use ordinary game types such as `Player`, `Npc`, `Skill`, and `Item` directly. The framework
does not wrap those. It only asks that the supported extension operations, registering listeners and handlers,
reading config, contributing hook rules, go through `ModuleContext`.

## The mental model

- The platform is the server engine. It offers a fixed set of places to plug in, reached through
  `ModuleContext`.
- Your module is one directory of scripts, config, and data that plugs into them.
- Installing your module is placing your directory under the modules root. Removing it is deleting that
  directory.
- A master switch in your config decides whether your module does anything at all.
- Enabling or disabling a module takes effect on the next server restart. There is no hot toggle.

## Anatomy of a module

Everything your module owns lives in one directory:

```text
game/modules/<id>/
  module.json        # your manifest: what your module is and what it owns
  config/
    module.ini       # your settings, including the on/off switch
  scripts/
    <Entry>.java     # entry point: implements GameModule, checks the switch, registers behavior
    ...more classes
  data/              # your item, skill, NPC, HTML, multisell files
  sql/
    install.sql      # only if you need database tables
    remove.sql       # only if you have tables, the opt-in cleanup
  MODULE.md          # short notes for whoever maintains the server
```

Keep everything your module owns inside this directory, and list it in `module.json` with paths relative to the
directory. Never point a manifest path outside your directory. The platform rejects absolute or traversing
paths, and the launcher will never delete a path you name by hand.

## Step by step: your first module

### 1. Pick an id and reserve your ranges

Choose a short lowercase id, for example `fishing`. Use only lowercase letters, digits, and hyphens, for
example `beast-taming`. It becomes your directory name, so it must be a valid directory name, and it becomes
your Java package (see step 3), so keep it simple. Check the reserved range registry in `MODULE_FRAMEWORK.md`
section 7 and pick a free block for any items, skills, or NPCs you will add. You record these in the manifest
so the platform can stop two modules, or your module and the base game, from colliding. The registry is a
convenience for picking a free block up front; the real enforcement is the boot-time collision check. It refuses
both sides of an overlap between two enabled modules, and for items, skills, and npcs it also refuses a module whose
reserved range hits an id the stock or custom datapack already owns. Reserve only what has an id space: items,
skills, and npcs. Spawns reference npc ids and multisell references item ids, so they are resources you ship but not
ranges you reserve.

### 2. Create your config and switch

Your config file is always `config/module.ini`, a fixed name the platform looks for inside your module
directory, just as your manifest is always `module.json`. You do not name it after your module and you do not
declare it in the manifest.

`config/module.ini`:

```ini
# Fishing module
# Master on/off switch. With this false the module does nothing and the server is stock.
Enabled = False

# Your own settings.
BiteChance = 40
```

The first key is your master switch. Everything your module does is gated on it. The platform loads this file
for you and exposes it through `context.config()`. You do not write a Java config class and you do not edit any
central loader.

### 3. Write the entry point

Your entry point implements `GameModule`. The platform calls `onEnable` at startup and hands you a
`ModuleContext`. Check the switch first, and only then register anything.

Your Java package is `modules.<id>`, with any hyphens in the id removed, so `fishing` uses `modules.fishing`
and `beast-taming` uses `modules.beasttaming`. This mirrors where your scripts live under
`modules/<id>/scripts/` and keeps your classes from colliding with the base game or any other module. Do not
place module code in the stock `custom.*` datapack packages.

```java
package modules.fishing;

public final class FishingModule implements GameModule
{
    @Override
    public void onEnable(ModuleContext context)
    {
        if (!context.config().getBoolean("Enabled", false))
        {
            return; // Switch off: register nothing, behave as stock.
        }

        // Register through the context. Everything you register is tracked to your module.
        context.events().register(...);
        context.handlers().register(...);
        context.hooks().register(...);
    }
}
```

If the switch is off, `onEnable` returns immediately and the server is unchanged. Every module must behave this
way.

### 4. Attach your behavior

Use the right extension point for what you need. See the decision table below. Register through
`context.events()`, `context.handlers()`, or `context.hooks()` so the platform can track and, later, manage
your registrations.

### 5. Declare your data

Put your item, skill, NPC, HTML, and multisell files under your `data/` directory and list them in the manifest
`resources` block. The platform gives your resource roots to the data loaders so your definitions load. Do not
place your data in the stock datapack folders.

Note on current status: module resource roots are platform work in progress. Until they are in place for the
loader you need, a feature that requires new datapack definitions is not yet a fully isolated module. An early
prototype may temporarily keep namespaced XML in the stock roots, but that is a migration state, not the
supported way to package a module. Do not treat stock-root data as an approved workflow.

### 6. Write the manifest

`module.json` declares what your module is and everything it owns. See the reference below.

### 7. Test both off states

Before you call it done, verify two things, each after a restart:

- With `Enabled = False`, the server behaves exactly as stock.
- With your module directory removed, the server behaves exactly as stock.

If either changes stock behavior, your module is leaking, usually because something registered before the switch
check, or because you touched a shared file.

## Choosing where to plug in

Match your need to an extension point. Reach for the first row that fits.

| You want to... | Use | Needs platform work? |
|---|---|---|
| React to a game moment (login, kill, item equip, skill use, zone, clan, death) | `context.events()` | No, events exist |
| Handle a custom item being used | `context.handlers()` with an `IItemHandler` | No |
| Handle a bypass link from an html window | `context.handlers()` with an `IBypassHandler` | No |
| Add a voiced command (a dot command in chat) | `context.handlers()` with an `IVoicedCommandHandler` | No |
| Add an admin command | `context.handlers()` with an `IAdminCommandHandler` | No |
| Add a custom skill effect or target type | `context.handlers()` with an effect or target handler | No |
| Add an NPC, quest, or spawn behavior | A normal script in your `scripts/` directory | No |
| Scale damage by your own rule | `context.hooks()`, combat multiplier hook | Yes, if the hook does not exist yet |
| Key a creature's data by its physical object, not its template | `context.hooks()`, entity data resolver | Yes, if not built yet |
| Reinterpret a client request the client cannot be changed to send | `context.hooks()`, packet pre-dispatch | Yes, if not built yet |

The last three rows are platform hooks. If the hook your feature needs does not exist, do not edit the stock
class to get the behavior. Ask the core team to add the generic hook. Once it exists, your module attaches to it
like any other. If two modules attach to the same hook, order is by manifest `priority` (higher first), then by
module id, so behavior is deterministic.

## Reacting to game events

`context.events()` registers a callback for a game moment on one of four scopes: `onGlobal` hears every
occurrence, `onPlayers` only those on players, `onNpcs` on any npc, and `onMonsters` on monsters. Pass the
`EventType` and a callback that takes the matching `On*` event holder. The event type and the holder type must
line up, the same rule the core event system uses.

```java
@Override
public void onEnable(ModuleContext context)
{
    if (!context.config().getBoolean("Enabled", false))
    {
        return;
    }

    context.events().onPlayers(EventType.ON_PLAYER_LOGIN, (OnPlayerLogin event) ->
    {
        event.getPlayer().sendMessage("Welcome back.");
    });
}
```

Your listener is owned by your module and recorded with the rest of your registrations, so the platform can
report what you contributed. Reach for an event before asking for a new platform hook: if the moment already
fires an event, you need no core change. For a moment tied to one specific npc, such as talking to a single quest
giver, use a normal script or quest in your `scripts/` directory rather than a global listener.

## Reading your config

Read every tunable through `context.config()`: ids, chances, costs, and toggles. Never hardcode them. This lets
the operator tune your feature without touching code, and keeps everything about your feature inside your module.

## Items, skills, NPCs, and data

- Put your definitions in your `data/` directory and list them in the manifest `resources` block.
- Use only the id ranges you reserved. Do not reuse another module's ids, and do not reference another module's
  ids from your code.
- Keep your html, multisell, and other assets under your directory so removal is a clean delete.

## If you need a database

- Add your tables in `sql/install.sql`. Make them additive and give them a name clearly tied to your module.
- Make the script idempotent: use `CREATE TABLE IF NOT EXISTS`. The platform runs it on every enable, before your
  code, so your tables exist when `onEnable` runs. A statement that fails (for example a plain `CREATE TABLE` for a
  table that already exists) fails the install and refuses your module, so keep it safe to re-run.
- List those tables and your scripts in the manifest `database` block.
- Never change a stock table's structure. Store your own data in your own tables.
- Provide `sql/remove.sql` for the operator's opt-in cleanup. The platform never runs it on its own, and never drops
  your tables on a normal disable or remove, because they can hold player progress.

## Dependencies and conflicts

If your module needs another to be present, or must not run alongside one, declare it. Both are arrays of plain
module ids:

```json
"dependencies": [ "taming-core" ],
"conflicts": [ "another-pet-system" ]
```

The platform validates these at startup, before any module registers. Your module is refused with a clear message
when a dependency is missing, is installed but disabled, or was itself refused (the failure cascades down the
chain), when you declare a conflict with an enabled module, or when your dependencies form a cycle. Only enabled
modules count: a dependency that is installed but switched off is treated as missing, and a conflict with a
switched-off module is harmless. There is no automatic downloading or installation. Keep dependencies simple.

Version-qualified dependencies (requiring a specific version of another module) are not parsed yet in V1: a
dependency is an id only. Use plain ids for now; version predicates are a later addition.

## Enable, disable, and remove: what your code must honor

- Disable means your master switch is false. On the next restart your module registers nothing and changes
  nothing. Data already stored stays.
- Remove means your directory is deleted while the module is disabled or the server is stopped. Because
  everything you own is in your directory, this is a clean delete. Your tables are only dropped if the operator
  explicitly asks.
- Your module must never assume it is always on. Guard `onEnable` with the switch, and expect changes to apply
  only after a restart.

## The manifest reference

```json
{
  "id": "fishing",
  "name": "Fishing",
  "version": "1.0.0",
  "apiVersion": "1",
  "entrypoint": "modules.fishing.FishingModule",
  "description": "Catch fish at water's edge for rewards.",
  "author": "yourname",
  "priority": 100,
  "dependencies": [],
  "conflicts": [],
  "resources": {
    "items": ["data/items"],
    "skills": ["data/skills"],
    "npcs": ["data/npcs"],
    "spawns": ["data/spawns"],
    "multisell": ["data/multisell"]
  },
  "reserves": { "items": [[9400, 9419]], "skills": [[9400, 9409]] },
  "database": { "install": "sql/install.sql", "remove": "sql/remove.sql", "tables": ["fishing_log"] },
  "hooks": []
}
```

- `id`, `name`, `version`, `author`, `description`: what your module is. The id is your directory name.
- `apiVersion`: the modding API version you built against. The platform checks it before enabling you.
- `entrypoint`: your `GameModule` class.
- `priority`: your order relative to other modules for hooks and loading. Higher runs first, ties break by id.
- `dependencies`, `conflicts`: startup validation, as above.
- `resources`: your data directories, relative to your directory, given to the data loaders. Each entry is a
  folder (for example `data/items`), and the loader scans the `.xml` files in it. Glob patterns are not supported
  in V1. The item, skill, npc, spawn, and multisell loaders read these today. HTML is not wired as a resource root
  yet: build dialogue as a string in your script (`NpcHtmlMessage.setHtml`) for now.
- `reserves`: every id range you use. This is what stops collisions.
- `database`: your install script, your opt-in cleanup script, and the tables you own. Omit if you have none.
- `hooks`: the generic platform hooks your module contributes to, by name (for example `combat-multiplier`).
  Omit or leave empty if your module uses only events and handlers. See `MODULE_FRAMEWORK.md` section 3.4 for
  the available hooks and how their order is resolved.

Every path is relative to your module directory. Absolute or traversing paths are rejected.

## Checklist before release

- [ ] Everything the module owns lives under its directory and is listed in the manifest with relative paths.
- [ ] No stock class is edited. No shared file is edited except through a published hook.
- [ ] The entry point checks the enable switch before registering anything.
- [ ] All registrations go through `ModuleContext`.
- [ ] All ids come from a reserved range recorded in the manifest.
- [ ] Any database tables are additive, namespaced, and listed, with an opt-in remove script.
- [ ] Verified after a restart: switch off behaves as stock, directory removed behaves as stock.
- [ ] `MODULE.md` explains what the module does and how to disable and remove it.

## Anti-patterns, and why they fail

- Editing a stock class to add your logic. It compiles into the jar, cannot be turned off without a rebuild, and
  cannot be cleanly removed. This is exactly what the framework replaces. The delivered taming prototype did
  this in about twenty stock classes; that is the shape to avoid.
- Hardcoding an id or a rule inside a stock method. It hides your feature where no one can find or remove it, and
  it collides with other features.
- Registering behavior before checking the switch. The operator turns your module off, restarts, and it is still
  running.
- Placing files outside your directory, including data in the stock roots as a permanent choice. Removal cannot
  find them, so they linger after uninstall.
- Pointing a manifest path outside your directory. It is rejected, and it is how uninstall accidents happen.
- Dropping player tables on disable. You destroy progress the player expected to keep.
