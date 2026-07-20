# One-Click Launcher + Bundled Pack

Goal: a player starts the whole offline "Living World" server by double-clicking one file,
having **installed nothing** — no JDK, no XAMPP, no manual database setup.

There are two roles:

- **You (developer)** run `build-pack` once to produce `L2J-Offline-OneClick.zip`.
- **The player** unzips it on any Windows PC and double-clicks `Start-Server.bat`.

Nothing here changes game logic — it only orchestrates and bundles the pieces that already exist.

---

## A. Build the pack (you, once, on your dev/live PC)

Requirements on the build PC: **JDK 25** (a full JDK, not a JRE) and **Ant** — the same tools you
already use to build `GameServer.jar`. Then, from `dist\launcher\`:

```
build-pack.bat
```

or with options:

```
powershell -ExecutionPolicy Bypass -File build-pack.ps1 -MariaDbZip C:\downloads\mariadb-11.4.5-winx64.zip
```

What it does:

1. Builds the server jars with `ant` (or reuse an existing build with `-SkipBuild`).
2. Stages the full server (the ant zip is already a complete `dist\`).
3. **Bundles a full JDK 25** into `pack\jre\`. It must be a full JDK: the server compiles datapack
   scripts at runtime via the system Java compiler, which a plain JRE does not have.
4. **Bundles portable MariaDB** into `pack\mariadb\` (downloads it, or use `-MariaDbZip` for an
   offline copy).
5. Wires `launcher.ini` to the bundled JDK + MariaDB and points backup tooling at it.
6. Zips everything to **`L2J-Offline-OneClick.zip`** (in the project root by default).

Useful switches: `-JdkHome <path>` (which JDK to bundle), `-MariaDbVersion 11.4.5`,
`-MariaDbUrl <url>`, `-OutDir <path>`, `-SkipBuild`.

> The zip is large (~0.5 GB) because a JDK and a database engine are inside it. That's the price of
> "installs nothing."

## B. Run the pack (the player)

1. Unzip `L2J-Offline-OneClick.zip` anywhere.
2. Double-click **`Start-Server.bat`**.
3. In the game client, log in as **`admin` / `admin`** — accounts are auto-created on first login
   (`AutoCreateAccounts`), and every character on the `admin` account is automatically granted
   **master (GM) access** the moment it enters the world (hook in `EnterWorld.java`), so the player
   gets full admin commands (`//admin` etc.) without ever opening the database. Any other
   username/password works too and creates a normal player account.

On first run it initializes the bundled MariaDB, imports the schema into an empty database, then
launches the login and game servers. Later runs skip straight to launching. `Stop-Server.bat` shuts
the servers and the bundled DB down cleanly.

---

## What the launcher does, in order

1. **Pre-flight** — one screen listing anything missing (Java / DB engine / jars) before it starts.
2. **Java** — bundled `dist\jre\` → `JAVA_HOME` → `PATH`.
3. **Database** —
   - *Bundled MariaDB* (`DataDir` set in `launcher.ini`): initializes the data dir on first run
     (root, no password), then starts it with that data dir.
   - *External MySQL/XAMPP* (`DataDir` blank): uses a running instance, or auto-starts `mysqld`
     from `MysqlBin`.
4. **Schema — only when the database is empty.** Imports all `db_installer/sql/{login,game}` tables
   through the `mysql` client, then writes a `.db_installed` marker.
   **Safe on an existing server:** it first counts tables in the target DB and *skips the import if
   any exist* — because ~14 SQL files `DROP TABLE` before recreating (e.g. `accounts`), importing
   over a live DB would wipe those. Auto-skip means running on a set-up machine never touches data.
5. **Login server, then Game server** — each in its own console window, using each `java.cfg`.
6. Optional **Python brain** (off by default).

## Testing without the full pack (external DB path)

You can also run the launcher against your own MySQL/XAMPP without building a pack: copy `launcher\`,
`Start-Server.bat`, `Stop-Server.bat` into a `dist\` that has the built jars, leave `DataDir` blank,
set `MysqlBin`/credentials in `launcher.ini`, and run. This is the quick smoke test; the bundled pack
is the shippable end-user experience.

To reinstall the schema from scratch (empty DB only): delete `launcher\.db_installed`, drop the DB, run again.

## Known caveats / next

- The MariaDB **first-run init** is the least-tested step across MariaDB versions; the pack pins a
  version so behavior is known. If init ever misbehaves, check the DB console window.
- A real `.exe` wrapper for `Start-Server.bat` (icon, no console flash) via Launch4j is a nice polish
  step but not required — the `.bat` already gives the one-click experience.
- Linux/macOS `.sh` equivalents are a straightforward port if ever needed.
