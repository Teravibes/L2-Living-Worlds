# Server Control Panel

A standalone, modern editor for your server's `.ini` config files - rates, phantoms,
PvP, the Olympiad and more - **plus a visual editor for how phantoms
fight**. No install, no build, no login, **no Python/Flask**: one self-contained HTML file.

## Open it as an app (recommended, no browser)

In the packaged server it runs as its own desktop window - **no browser at all**. Open it with the
**Config Editor** button in the launcher (`LivingWorld.exe`).

The launcher hosts this page in a Microsoft WebView2 window (`ConfigEditorWindow` in the compiled
launcher) and reads and writes your server files through the launcher itself, using normal Windows
file access instead of the browser File System Access API. That removes the browser requirement and
the permission lapse that made "open the game folder" stop detecting the `.ini` files. It also opens
straight to your server's `game` folder with no folder picker: the launcher hands the window that path.

Requirements: the **WebView2 Runtime**, which ships with Windows 11 and with any recent Microsoft
Edge on Windows 10. If it is missing, the launcher shows the Microsoft download link. The WebView2
DLLs ride inside `LivingWorld.exe`; nothing else to install.

The file bridge is `native-bridge.js`, injected by the launcher before the page loads. In a normal
browser that file does nothing, so `index.html` still behaves exactly as documented below.

## Quick start (browser fallback)

The single-file browser mode still works if you ever want it (for example from a raw source
checkout without the pack).

1. Open `index.html` in **Chrome / Edge / Brave** (Chromium browsers can write files in place).
2. Click **Open game folder** and pick your server's `game` directory (the one that holds both
   `config` and `data`). The config panels read `game/config`, and the **Phantom Playstyles** and
   **Bot Clans** tabs read `game/data` from the same pick, so there is no separate data folder to open.
3. Edit with the friendly panels (toggles, number boxes) or the **Raw editor** (every key in
   any file). Changed files light up.
4. Click **Save changes** - the values are written straight back into the `.ini` files.
5. In-game, run `//reload config` (or restart the server) to apply. Some settings only take
   effect on a restart.

> You can still point it straight at `game/config` if you prefer; the data tabs then fall back to
> their own **Open data folder** button, exactly as before.

The folder you pick is **remembered** between visits. Next time you open the page it either
loads straight away, or (if the browser has dropped write permission) shows a single **Reopen
&lt;folder&gt;** button - one click, no re-navigating the picker. Use **Pick a different folder** /
**Change folder** to point it somewhere else.

> **Firefox / Safari:** those browsers can't write files directly. The panel still works -
> click **Load config files**, edit, and it **downloads** the edited `.ini`s for you to copy
> back into `game/config` yourself.

## What it edits

- **Curated panels** - the settings that matter for the Living World, grouped and explained:
  - **Experience & SP**, **Drops & Spoil**, **Quests & Economy** (`Rates.ini`)
  - **Phantoms** (`Custom/FakePlayers.ini`) - party, gear and hunting settings for phantoms
  - **Phantom PvP** (`Custom/FakePlayers.ini`) - phantom PvP switches and tuning
  - **Phantom Olympiad** (`Custom/PhantomOlympiad.ini`) - the phantom noble roster
  - **Raid Bosses** (`NPC.ini`) - scale every raid boss's attack, defense and regen at once (percent,
    100 = retail; restart to apply)
- **Raw editor** - every key in any `.ini` in the folder, for power users. The town bot settings
  (the NPC crowd in towns, the `FakePlayer...` keys in `Custom/FakePlayers.ini` that are not on the
  Phantoms tab) and the auto play settings (`Custom/AutoPlay.ini`) are only here, since they rarely
  need changing.
- **Phantom Playstyles** - a visual editor for `game/data/PhantomPlaystyles.xml` (see below).
- **Population** - the visual map editor for town bot and phantom populations, routes, spawn zones,
  and map overlays (see below).

## Phantom Playstyles

This tab edits **how recruited phantoms fight**: which skills each class lineage uses, under
what conditions, and - most importantly - **in what order**. Order *is* priority: the engine
casts the first listed entry whose conditions pass, so dragging a row up genuinely changes
behaviour. That ordering is invisible in a text editor, which is the main reason this tab exists.

It reads your server's `game/data` directory. If you opened the whole `game` folder on the start
screen, this loads automatically from the same pick - no separate step. If you opened `game/config`
directly instead, the tab shows its own **Open data folder** button (you can point it at `game/data`,
at `game`, or at the pack root and it will find the rest). The first open indexes the skill and
skill-tree data next to the playstyle file, which takes a moment; after that it's cached and instant
until the datapack changes.

What it gives you:

- **The real skill data** on every row - MP cost, reuse, cast range, weapon requirement - read
  from your own `stats/skills`, not a hardcoded table.
- **A level slider (1–80).** Rows the member can't use at that level are dimmed, with *learned
  at 36* on the ones it hasn't reached. This answers "what does my level-25 archer actually
  cast?" by dragging a slider. (Answer: Power Shot. That's the whole kit until 36.)
- **Drag to reorder**, or the ↑/↓ buttons.
- **A condition builder** generated from the schema: pick `MOBS_NEAR` and the "how many" box
  appears. You can't write a condition without its parameter, or a parameter without its condition.
- **An add-skill picker filtered to the lineage** - only skills those classes actually learn,
  each annotated with its learn level. Skills the party manager owns (taunts, Ultimate Defense,
  Sleep, Dryad Root, Resurrection) are blocked, because the manager casts those with its own
  threat/survival logic and two systems fighting over them is a bug.
- **New / clone / delete playstyles**, including for lineages that have none today.
- **Live warnings** - the same rules as the developer playstyle validator, shown inline: a skill the
  lineage never learns, a duplicate class claim that would make the entry dead code, a coverage
  hole that would leave a member with nothing to cast at some level.

Save, then run **`//phantom playstyle`** in-game. No restart, no jar rebuild - the change is live
immediately, so you can tune a rotation while standing next to the party.

### Skill icons (optional)

**There is no folder on your server you can point this at.** The server files contain no artwork at
all - every skill only *names* its icon (`<icon>icon.skill0056</icon>`) and the game client resolves
that name against its own textures at render time. So the folder has to be one you create.

The pictures live in your **client**, not your server:

```
<Lineage 2 client>\systextures\Icon.utx
```

(The reference `icon.skill0056` reads as package `Icon`, texture `skill0056` - so `Icon.utx` is the
package holding them.)

To get them out:

1. Open `systextures\Icon.utx` with an Unreal package tool - **UModel** (`umodel.exe`) is the usual
   choice; older L2 tools like Unreal Package Explorer also work.
2. Export the textures as **PNG** into any folder you like.
3. In this tab, click **Choose icon folder** and pick that folder.

The panel searches the folder **and one level of subfolders**, case-insensitively, so you don't have
to flatten or rename anything - most tools export into a `Icon\` subfolder and that works as-is. It
then tells you exactly what it found, e.g. *"Indexed 3,412 images - matched 91 of the 95 skill icons
in this file"*, so a wrong folder is obvious immediately rather than silently showing nothing.

Formats read: `.png`, `.jpg`, `.webp`, `.gif`, `.bmp`. **Not `.tga`** - browsers can't display it, so
if your tool defaults to TGA, switch the export to PNG.

Without an icon folder, rows show colour-coded type badges instead. That's the design's primary
visual language anyway: what you're tuning is *when* a skill fires and in what order, which a
ROTATION/CONTROL/AOE chip says more clearly than 32×32 client art does.

### Two things the editor will tell you, but that are worth knowing up front

- **Support classes** (Bishop, Prophet, Elder, Warcryer, Overlord and their lines) ignore
  playstyles entirely - the party manager's support tick plays those classes. You can write one,
  but nothing will read it.
- **Summoner lineages** work, but only the caster's own skills fire; servitor control isn't
  implemented yet.

## Bot Clans

This tab edits **which synthetic clans phantoms wear, how they group into alliances, and their crests**. It
reads the same **game/data** folder as the Phantom Playstyles tab (open it once and both tabs use it).

- **Membership chances** - two dials at the top of the tab set how often bots wear one of your clans: one for
  **town bots**, one for **recruited (LFM) phantoms**, each `0..100` (0 = never, 100 = always). A bot in a
  clan that belongs to an alliance shows that alliance crest too, so this one setting covers both. These write the
  `fakePlayerClanChance` / `recruitClanChance` attributes on the file's `<settings>` line (the panel creates that
  line if your file has none). Like the clans themselves, they are read at server boot, so **restart to apply**.
- **Clans** - add, rename, delete; set each clan's level and pick its crest from a **visual dropdown** that shows
  every crest set with a thumbnail. The key is the internal id that alliances and `PhantomPopulations.xml`
  reference; the name is what players see (<= 16 chars, no spaces).
- **Alliances** - create and delete alliances, pick the leader clan, choose the ally crest from the same visual
  picker, and choose members from a **multi-select dropdown**. That list only offers clans that are still free,
  because a clan can be in at most one alliance - the safeguard is enforced in the UI, so you can't double-book
  one. Deleting a clan removes it from any alliance automatically.
- **Crests** - make a crest from **any image** (PNG, BMP, JPG, ...) right in the browser. Each crest set writes
  the three `.dds` (DXT1) textures the client needs - pledge (16x16), ally (8x16), large (32x16) - padded to the
  power-of-two dimensions the client requires, plus a `.png` source you can re-edit later. Delete a set the same
  way. This replaces running `tools/crest_png_to_dds.py`; no Python needed.
- **Export / Import** - **Export setup** downloads your clans and alliances as a JSON file to share or back up.
  **Import setup** merges a file back in and **never overwrites what you already have**: a clan or alliance whose
  key or name already exists is skipped, and an imported member that is already in another alliance is dropped, so
  your own setup is always preserved. (Crest image files live in `data/crests/` and are shared separately.)

All the dropdowns are drawn by the panel itself, so they follow the dark/light theme - no native white popups.

`BotClans.xml` is held to the same comment-preserving standard as the playstyle file: editing one field rewrites
exactly one line, the big header comment and your layout are untouched, and if the panel can't reproduce your
file byte for byte on load it disables editing rather than reformat it.

> **Applies on restart.** Unlike playstyles, bot clans and alliances are rebuilt from this file only at server
> boot - there is no live reload - so save here and **restart the server** to see the changes. Crest files are
> written straight to `data/crests/` and are likewise picked up on the next boot.

## Population

This tab is the visual **map editor** for your server's populations: town bots (the NPC crowd), real-Player
phantoms and recruitable friends. Open a folder and it renders your `geodata` as a
height-relief world map, then you drop and drag population circles, draw patrol routes and spawn zones,
and overlay city / world-map images.

It reads the same `game/data` you already opened for the other tabs, so if you started from the `game`
folder it loads with no extra step (its own folder picker is hidden in that case). Full operating
details (behaviors, routes, zones, support buddies, colors) live in `tools/fpc-editor/README.md`, which
documents the same editor.

**Export / Import** - above the population list, **Export** downloads the current mode's populations as a
JSON file to share or back up. Tick the checkbox on the rows you want first to export just those, or leave
them all unticked to export the whole list (**Select all** / **Select none** do it in one click). **Import**
merges a file back in and **never overwrites what you already have**: a population whose name you already
have is skipped, and the toast tells you how many were added and how many were skipped. Export and Import are
per mode, so NPC and Phantom lists stay in their own files. After an import, click **Save** to write it out.

Raid-boss strength is **not** here: because it scales `game/config/NPC.ini`, it lives in the **Raid Bosses**
config panel above, next to the other `.ini` editors.

## Safety

For `.ini` files, saving only rewrites the **value** of keys that already exist, preserving every
comment, banner and blank line exactly (the same approach the in-game `//rates` panel uses). A key
that isn't in your file yet (older install) is appended at the end of that file. Nothing is
reordered or deleted.

`PhantomPlaystyles.xml` is held to the same standard, and then some. That file's header and
per-lineage comments are most of its value, so the editor never re-serialises it as XML: every line
keeps its original text and is only rewritten if you actually edit it. Editing one condition
changes exactly one line, and a row's own explanatory comment moves with it when you drag it.

On load the panel proves this to itself - it re-serialises the file it just read and compares byte
for byte. **If it can't reproduce your file exactly, editing is disabled** and it says so, rather
than risk reformatting something you hand-tuned. The **Self-test** button re-runs that check and
reports the first differing line.

Both parsers are covered by `tests/js/playstyle_editor_test.js`, which runs the panel's real code
against the shipped data files.
