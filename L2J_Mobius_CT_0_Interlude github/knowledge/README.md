# LLM Knowledge Base (chat grounding)

Plain-text fact files that ground the chat brain (`fpc_brain.py`) so bots answer with real
Lineage 2 **Interlude** facts (zones, level ranges, classes, buffs, item shorthand) instead of
inventing them. Adapted from the tag-fact idea in the `l2-smartbot` project, corrected for Interlude.

## How it works

On startup `fpc_brain.py` loads every `*.txt` file in this folder. For each incoming chat, it scores
facts by how many of their **tag tokens** appear in the player's message and injects the top few into
the system prompt under a "Game facts you can rely on" block. When nothing matches, nothing is injected
— so chat behaves exactly as before for unrelated messages.

Used by these brain modes: WHISPER, SAY, SHOUT, BUDDY (all facts), and TRADE / ITEM (filtered to
`item` and `buff` facts for canonical item-name resolution).

## File format

One fact per line:

```
[tag tokens here] The sentence the bot can rely on and will not contradict.
```

- The bracketed part is the **searchable tags** (lowercase words/numbers), not shown to the player.
- The text after the `]` is what gets injected into the prompt.
- Lines starting with `#` and blank lines are ignored.
- For locations, include `level <lo> <hi>` in the tags — a level number in the player's message that
  falls inside that band gets a relevance boost (e.g. "where at 40?" → zones covering 40).

Example:

```
[location level 35 50 cruma tower giran] Cruma Tower near Giran is a multi-floor dungeon around levels 35-50.
```

## Current files

There are two kinds of file. **Curated** files are hand-written lore you edit directly.
**Generated** files (`*_generated.txt`) are built from the server's own data XML and must not be
hand-edited — re-run the generator instead (see below).

### Curated (hand-written)

| File | Covers |
|---|---|
| `00_general.txt` | Towns, gatekeepers, warehouses, shops, currency, grade basics |
| `10_locations.txt` | Hunting zones by level range |
| `20_buffs.txt` | Buffs and the buffer classes (PP / EE / SE / WC, etc.) |
| `30_roles.txt` | Classes and combat roles per race |
| `40_party_combat.txt` | Party composition and combat basics |
| `50_items.txt` | Shot/scroll/material shorthand and trade slang |

### Generated (from game data — do not hand-edit)

Built by `tools/build_knowledge.py`, which reads `dist/game/data` so the facts are guaranteed to
match what the live server actually uses (real zone level ranges, real mob names, real gatekeeper
destinations, real raid bosses, real item names). The curated files above stay authoritative for
lore/flavor; these add the accurate specifics so bots don't invent them.

| File | Covers | Answers |
|---|---|---|
| `60_zones_generated.txt` | Hunting zones: real level band + a few real mobs per zone | "where do I grind at 40?" |
| `61_mobs_generated.txt` | Every monster: level + where it spawns | "where is *&lt;mob&gt;*? what level is it?" |
| `70_teleports_generated.txt` | Gatekeeper destinations per town | "how do I get to *&lt;place&gt;*?" |
| `80_raidbosses_generated.txt` | Raid bosses: level + location | "where's *&lt;raid&gt;*?" |
| `90_items_generated.txt` | Exact real names for shot/scroll/enchant/gemstone families | trade-chat item accuracy |

## Regenerating the generated files

Run the generator whenever the game data changes (new zones, rebalanced levels, etc.):

```bash
cd "L2J_Mobius_CT_0_Interlude github"
python3 tools/build_knowledge.py   # rewrites the *_generated.txt files (pure stdlib, deterministic)
# then restart fpc_brain.py so it reloads knowledge/
```

Because retrieval only injects the top few matching facts per message, the large generated files
(the mob index is ~2,000 lines) cost nothing at chat time — they just sit in memory until a message
mentions one of them.

## Editing

Edit the **curated** files directly, then restart `fpc_brain.py` (it reloads the folder on boot).
Do **not** hand-edit `*_generated.txt` — your changes are overwritten on the next generator run;
fix the generator or the source XML instead. Keep each curated fact short and true; the goal is to
keep bots from inventing specifics, so only add facts you trust.
