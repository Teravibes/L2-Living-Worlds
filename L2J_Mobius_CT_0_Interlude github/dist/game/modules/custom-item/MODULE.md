# Custom Item module

The second reference module for the server module framework. Where Hello World proved discovery, config, script
compilation, and handler registration, this one proves the next piece: a module shipping its own datapack content and
having a stock loader pick it up.

## What it does

When enabled, it:

- Ships one item of its own, the Living World Token (id 60000), under `data/items/`.
- Declares that folder as an item resource root in `module.json`, so the platform hands it to `ItemData` at startup.
- Registers one voiced command, `.token`, that grants the token so you can confirm the loaded item exists in game.

It touches no stock class and owns everything under this directory, including its item definition.

## The resource root

The proof is in `module.json`:

```json
"resources": {
  "items": ["data/items"]
}
```

At startup the platform resolves that path against this module directory and, only while the module is enabled, registers
it with the item loader. With the module disabled or removed, the loader never sees it and item 60000 does not exist.

## Client display and the server-side boundary

Item id 60000 is new, so a stock Lineage II client has no name, icon, or grp entry for it and would render it blank
(you would still receive it, and `.token` would still say "You have obtained", just with no visible name or icon). That
is the framework's documented boundary: a server-side module cannot ship client assets.

To make the item visible without touching the client, its definition sets `displayId` to 4037 (Coin of Luck), an item
the client already knows. The server still tracks the item as id 60000; `displayId` only tells the client which existing
icon and name to draw. So `.token` shows a real Coin of Luck icon and name, while the item under it is the module's own
60000. A module that wants its own distinct icon and name is a later, client-side installation concern.

## Reserved ids

This module reserves item id range 60000-60009 and uses 60000. The range is recorded in the framework's reserved range
registry in `docs/MODULE_FRAMEWORK.md`. Item ids are capped at 65535 by the item schema, so module authors pick a free
block below that ceiling.

## Enable it

1. Open `config/module.ini`.
2. Set `Enabled = True`.
3. Restart the server.

Type `.token` in game and you should receive a Living World Token.

## Disable it

Set `Enabled = False` in `config/module.ini` and restart. The item is not loaded, the command is not registered, and the
server is stock.

## Remove it

While the server is stopped or the module is disabled, delete this whole `custom-item` directory. Nothing it owns lives
anywhere else, including its item definition, so removal is a clean delete. The module has no database tables.
