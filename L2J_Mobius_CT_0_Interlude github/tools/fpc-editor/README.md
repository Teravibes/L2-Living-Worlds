# FPC Editor

A standalone visual editor for `FakePlayerBehavior.xml` (NPC fake-player populations) and `PhantomPopulations.xml` (real-Player phantom populations). No install, no build — single HTML file, open in a browser.

## Quick start

1. Click **📂 Open data folder** and pick your server's `game/data` folder. The editor loads:
   - `FakePlayerBehavior.xml` and `PhantomPopulations.xml`
   - `geodata/*.l2j` tiles (rendered as a real height-relief world map)
   - Named routes from `routes/*.xml`
   - Map images from `Cities/`, `world locations/`, and `World Map/` subfolders
2. The folder is **remembered** — reloads automatically next time.
3. Edit, then click **💾 Save** to write back to the same folder. There's also a Save button at the bottom of each population's form.

> Chrome/Edge: saves directly in place. Firefox: downloads the updated XML.

## Mode switch

The two buttons at the top switch what you're editing:
- **NPC fake players** → `FakePlayerBehavior.xml` (crowd bots: profiles, behaviors, routes, shops)
- **Phantoms** → `PhantomPopulations.xml` (real-Player field hunters: level range, respawn, zone)

Map and geodata stay loaded across both modes.

### Support buddies (phantom mode)

In phantom mode, a population's **role** dropdown turns it into a personal **support buddy** instead of a field hunter:
- **Elder / Prophet / Warcryer** — a support-class buffer/healer that idles in town until you whisper and party it, then keeps you buffed/healed and follows you (see PROGRESS.md "System D").
- Leave role as **none** for a normal hunting phantom.
- Place buddies at **level 40+** (their level rolls from the population's level range) so they have a real 2nd-class buff kit. A small count near a town's gatekeeper works well.

## Working with populations

- **Double-click** the map to add a population, or click **+ Population**.
- **Drag** a circle to move it.
- **Click** a circle or list row to open its form (name, count, level range, behavior, etc.).
- **👁 / 🚫** on each row shows/hides that population. **Hide all / Show all** toggles everything.
- All sections in the side panel are **collapsible** — click the header to expand/collapse.

## Behaviors (NPC mode)

| Behavior | What it does |
|---|---|
| **Idle / loiter** | Bots drift randomly within radius or drawn zone |
| **Runner** | Bots visit route waypoints in random order |
| **Patrol** | Bots walk route waypoints in order, looping |
| **Farm** | Bots seek and fight monsters in zone. Tick **respawn**. |
| **Shop** | Seated private-store vendors (SELL/BUY/CRAFT/PACKAGE) |

For **Shop** populations, the population's **level range** sets the tier of stock (gear *and* consumables), so a low-level town sells its own grade of shots/mats. Tick **market hub — stock every grade (ignore level)** to make it a hub (e.g. Giran) that carries everything regardless of level.

Movement timing defaults (run/walk, pause range) are set per behavior; tweak under **Movement timing (advanced)**.

## Routes

Routes define the path Runner and Patrol bots follow. Two ways to set one up:

### Named routes (recommended for reuse)
Named routes live in `routes/*.xml` and can be shared across many populations. Record one in-game with GM commands, or manage via the editor:

**In-game recording (GM):**
```
//record_route gludio_market   ← start recording; walk the path
//stop_route                   ← saves to data/routes/gludio_market.xml
//list_routes                  ← shows all saved routes
```
A waypoint is captured every ~150 units walked.

**In the editor:**
- Select a population with Runner or Patrol behavior.
- Pick a named route from the **Route** dropdown (click ↺ to rescan the folder).
- The route waypoints appear on the map and in the waypoint list.

### Manual (drawn) routes
- Click **✦ Draw route**, then click the map to place waypoints. Drag any dot to move it. **Esc** to finish.
- Use **⇅ Reverse** to flip traversal order. Use **✕ Clear** to start over.

### Per-population customization
Each population can independently customize a shared named route:
- **Delays**: click any waypoint row in the list to set a pause (seconds) at that point.
- **Reverse**: click **⇅ Reverse** to traverse in opposite order — only affects this population, not the route file or other populations.
- Delays and reversal are saved into the population's XML and survive save/reload.

Waypoints are also **draggable on the map**. For named routes, dragging saves back to the `routes/*.xml` file. You can also **delete** individual waypoints (✕ button on each row) and pan to any waypoint (⊙).

## Spawn zones

Instead of a circle radius, bots can spawn inside a drawn polygon:
- Select a population, click **Draw zone**, then click map points to outline the area.
- Drag vertices to adjust. **Esc** or double-click to finish. **Clear zone** reverts to circle.
- Works for any behavior. Great for town crowds and shop districts.

## Map images

The **Map image layers** section (collapsed by default) lets you overlay images on the geodata:
- **Load world map** — toggle on/off. Expects a file like `world map.png` or a `World Map/` folder.
- **Cities** dropdown — images from a `Cities/` subfolder.
- **World locations** dropdown — images from a `world locations/` subfolder.

Check **edit images (drag / resize)** to reposition layers. Image placements are saved to `fpc-map-images.json` alongside your XML when you click Save.

## Colors

| Color | Meaning |
|---|---|
| Blue | Idle / loiter |
| Purple | Runner / Patrol (routes drawn dashed) |
| Red | Farm (field hunters) |
| Gold | Shop vendors |
| Teal | Phantoms |
| Orange | Currently selected population's route |

## Notes

- The editor writes one `<profile>` per population on save (named `mv_<population>`). Existing `<assign>` and `<default>` entries in the file are preserved verbatim.
- Waypoint Z is seeded from the population's elevation so the server snaps each point to the correct ground layer.
- The editor connects to the File System Access API for direct saves (Chrome/Edge). Firefox falls back to download.
