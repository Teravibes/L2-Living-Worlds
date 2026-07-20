# Interlude: Living World - an offline/solo Lineage 2 server

A fork of **L2J Mobius CT_0 Interlude** turned into a single-player **"Living World"**:
you log in and the server *feels* populated - towns full of NPCs, working private
shops, trade chat, field hunters, and recruitable combat parties - without any other
humans online. Play the classic Interlude chronicle solo, at your own pace, on your own
machine.

> Lineage 2 and all related assets are trademarks of NCSOFT. This is a non-commercial
> fan project and is not affiliated with or endorsed by NCSOFT.

---

## What makes it a "Living World"

- **Populated towns** - data-driven NPC "fake players" wander the cities with procedural
  identities and run **functional private shops** you can actually buy from.
- **Field hunters** - clientless phantom characters auto-hunt the field zones, so the
  world outside town isn't empty either.
- **Recruitable parties** - shout *Looking for Members / Looking for Party* and a
  level-matched party spawns, walks over, and joins you.
- **Personal support buddy** - party a phantom as your own dedicated buffer/healer.
- **Living chat (optional)** - an optional LLM "brain" lets bots hold in-character
  whisper / say / trade / shout conversations. Fully optional; the server runs without it.

Everything else is stock **Interlude (Chronicle 6 / CT_0)** gameplay from the L2J Mobius
core - augmentation, Seven Signs, sieges, raids, the full quest and skill set.

---

## Community & support

For help with installation, to report bugs, or to share your experience and
suggestions, join our [L2 Living Worlds Discord](https://discord.gg/6KTyDG55PA).

---

## Quick start (Windows, install nothing)

The easiest way to play. You do **not** need Java, a database, or any setup.

1. Download the latest **`L2J-Offline-OneClick.zip`** from the
   [**Releases**](../../releases) page.
2. Unzip it anywhere.
3. Double-click **`Start-Server.bat`**.

On first run it initializes a bundled, portable MariaDB, imports the schema, then starts
the login and game servers - a bundled JDK and database are inside the zip, so there's
nothing to install. Later runs start straight up. `Stop-Server.bat` shuts everything down
cleanly.

Then connect your **Interlude game client** to the server (see
[Connecting the client](#connecting-the-client) below) and log in:

- Accounts are **auto-created on first login** - just type any username and password.
- Log in with the username **`admin`** (any password) to get a **GM account**: every
  character on the `admin` account is automatically granted master access on entering the
  world, so admin commands (`//admin`, etc.) work out of the box - no database editing.
- **Any other username** creates a **normal player account** with no admin rights.

> The pack is large (~0.5 GB) because a full JDK and a database engine are bundled
> inside it. That is the price of "installs nothing."

### Connecting the client

You need a **Lineage 2 Interlude** client (the server does not ship one).

- **Easiest - pre-configured L2.exe:** find and download an interlude cliend and then
  download the ready-to-play l2.exe from **[here](https://www.mediafire.com/file/4rom0v9yuc7za4y/L2.exe/file)**. It's already
  set to connect to `127.0.0.1`, so just run it and log in.
- **Manual - your own Interlude client:** point it at the local server by editing the
  client's `system/l2.ini` so the login server host is `127.0.0.1` (or add a `hosts`
  entry mapping the login server's hostname to `127.0.0.1`). Then launch and log in.

> The Lineage 2 client is NCSOFT's property; distribute or download it at your own
> discretion, the same as any fan project.

---

## Optional: the LLM chat brain

The bots work fully without this. If you want them to hold natural, in-character
conversations, run the small Python "brain" alongside the server. It's a local Flask
service the game server calls over HTTP.

You can use a **local model via [Ollama](https://ollama.com)** (no API key, runs on your
machine) or a hosted API. On Linux/macOS the one-step script does everything:

```bash
cd "L2J_Mobius_CT_0_Interlude github"
./setup_brain.sh          # installs Ollama, pulls a model, sets up a venv, launches
```

On Windows use `setup_brain.bat` or if you want to run it manually, do:

```bash
cd "L2J_Mobius_CT_0_Interlude github"
python3 -m venv .venv && source .venv/bin/activate   # (Windows: .venv\Scripts\activate)
pip install -r requirements.txt
python fpc_brain.py       # serves http://127.0.0.1:5000
```

Configure it with a `.env` file in that folder:

```ini
# Local model (recommended, no key needed):
PROVIDER=ollama
OLLAMA_MODEL=llama3.1

# — or — a hosted provider:
# PROVIDER=deepseek
# DEEPSEEK_API_KEY=your-key-here
```

Then set `StartBrain=true` in `dist/launcher/launcher.ini` (or start it yourself) so the
server talks to it.

---

## Building from source (maintainers / developers)

You only need this if you want to change the Java server or rebuild the one-click pack.

**Requirements:** a full **JDK 25** (not a JRE - the server compiles datapack scripts at
runtime and needs `javac`) and **Apache Ant**.

Build the server jars:

```bash
cd "L2J_Mobius_CT_0_Interlude github"
ant
```

### Producing the one-click pack

Two ways:

- **In the cloud (no local JDK/Ant needed):** run the **"Build one-click pack"** GitHub
  Action from the repository's **Actions** tab ("Run workflow"). When it finishes,
  download the `L2J-Offline-OneClick` artifact and publish it on the Releases page.
- **Locally on Windows:** from `L2J_Mobius_CT_0_Interlude github/dist/launcher/` run
  `build-pack.bat` (or `build-pack.ps1` with options). It builds the jars, bundles a full
  JDK 25 and a portable MariaDB, and produces `L2J-Offline-OneClick.zip`.

See `dist/launcher/README.md` for the full build-and-run details, launcher options, and
the external-database path (running against your own MySQL/XAMPP instead of the bundled
DB).

---

## Configuration

Server settings live in `L2J_Mobius_CT_0_Interlude github/dist/game/config/` (standard
L2J Mobius `.ini` files). The custom "Living World" systems are configured under
`config/Custom/` (e.g. `FakePlayers.ini`) and via the XML behaviour/route files under
`dist/game/data/`. Rates, spawns, and the fake-player populations are all adjustable
there.

---

## Repository layout

```
L2J_Mobius_CT_0_Interlude github/   # the server project (note the space in the folder name)
  java/org/l2jmobius/                # game/login server source (JDK 25 + Ant build)
  dist/                              # runnable server: config, data, scripts, launcher
  fpc_brain.py                       # optional Python LLM chat service
  knowledge/                         # fact files that ground the chat brain
  tools/fpc-editor/                  # visual editor for bot zones/routes (open index.html)
  build.xml                          # Ant build
fpc data/                            # geodata + world map assets for the editor
.github/workflows/                   # CI: pack build + regression tests
```

---
## Support the project

This is a free, non-commercial fan project. If you enjoy it and want to help,
you can buy me a coffee. Totally optional!

[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-support-yellow?logo=buymeacoffee&logoColor=black)](https://buymeacoffee.com/industrialeve)


---

## Credits & license

This project is a fork of the **[L2J Mobius](https://l2jmobius.org/)** project
(CT_0 Interlude), which provides the Lineage 2 server core. Huge thanks to the L2J Mobius
team and the wider L2J community.

Licensed under the **GNU General Public License v3.0** - the same license as L2J Mobius.
See [`LICENSE`](LICENSE). You are free to use, modify, and redistribute it under the terms
of the GPL v3; if you distribute it, you must make your source available under the same
license.
