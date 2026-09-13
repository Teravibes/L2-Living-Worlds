# Testing the Custom Item module

This proves the module resource-root mechanism end to end: a module ships its own item, a stock loader picks it up only
while the module is enabled, and the item disappears cleanly when the module is disabled or removed. The three states at
the end (enabled, disabled, removed) are the same checks every module must pass before release.

## Before you start

- The module rides on a normal server start, so your usual database and login/game setup must be working.
- The resource-root support in `ItemData` lives in `GameServer.jar`. If your jar predates the module framework
  Milestone 3, build and deploy it once first. A module's own item files do NOT need a build; the server loads them at
  startup from the module directory.

## Test A: enabled loads the item and grants it

1. Edit `config/module.ini` in this folder and set `Enabled = True`.
2. Start the server.
3. In the game server console, look for these lines:

   ```
   [Modules] Found Custom Item 1.0.0 (api 1), enabled=true
   [Modules] Custom Item: registered items resource root data/items
   [Modules] Custom Item: enabled, 1 registration(s).
   ```

4. Log in with a character and type `.token` in normal chat. You should receive the token, shown with a Coin of Luck
   icon and name (the item sets `displayId` to 4037 so the client can draw it; the server still tracks it as id 60000).
   That item only exists because the loader read it from this module's `data/items` folder. If you had used a brand-new
   id with no `displayId`, the client would show "You have obtained" with a blank name and no icon, because a stock
   client has no assets for a new item id. That is the framework's server-side boundary, not a loader failure.
5. Optional: change `GrantCount` in `config/module.ini`, restart, and confirm `.token` now grants that many. This proves
   the module reads its own config.

## Test B: disabled behaves as stock

1. Set `Enabled = False` in `config/module.ini`.
2. Restart the server.
3. The console shows `enabled=false`, no resource-root line for this module, and `0 module(s) enabled`.
4. Type `.token` in chat. Nothing happens, because the command was never registered. Item 60000 is not loaded. The server
   is stock.

## Test C: removed behaves as stock

1. Stop the server.
2. Delete this whole `custom-item` directory.
3. Restart the server.
4. It starts normally. Nothing is left behind, because everything the module owned, including its item definition, lived
   in this one directory.

## Why this is the resource-root proof

Hello World registered a command with no data. This module ships an item and nothing else changes in the stock datapack.
If `.token` gives you a real, named item that vanishes the moment the module is disabled or removed, then the loader saw
the module's own data root, and only while the module was enabled. That is exactly the guarantee the resource-root
mechanism has to provide.
