# Testing the Hello World module

This walks through proving the module framework works end to end using this reference module. It doubles as the
template for testing any module: the three states at the end (enabled, disabled, removed) are the checks every
module must pass before release.

## Before you start

- The module rides on a normal server start, so your usual database and login/game setup must be working.
- The framework engine itself lives in `GameServer.jar`. If your jar predates the module framework, build and
  deploy it once first (see "Why a build is needed" at the bottom). A module's own scripts do NOT need a build;
  the server compiles them at startup.

## Test A: enabled works

1. Edit `config/module.ini` in this folder and set `Enabled = True`.
2. Start the server.
3. In the game server console, look for these lines:

   ```
   [Modules] Scanning ./modules
   [Modules] Found Hello World 1.0.0 (api 1), enabled=true
   [Modules] Hello World: compiling 1 script(s).
   [Modules] Hello World: enabled, 1 registration(s).
   [Modules] 1 module(s) enabled, 0 refused.
   ```

4. Log in with a character and type `.hello` in normal chat. You should see the greeting from `config/module.ini`
   (`Hello from the module framework!` by default).
5. Optional: change `Greeting` in `config/module.ini`, restart, and confirm the new text appears. This proves the
   module reads its own config.

## Test B: disabled behaves as stock

1. Set `Enabled = False` in `config/module.ini`.
2. Restart the server.
3. The console shows `enabled=false` and `0 module(s) enabled`.
4. Type `.hello` in chat. Nothing happens, because the command was never registered. The server is stock.

## Test C: removed behaves as stock

1. Stop the server.
2. Delete this whole `hello-world` directory.
3. Restart the server.
4. It starts normally. The console shows the scan line and `0 module(s) enabled, 0 refused`. Nothing is left
   behind, because everything the module owned lived in this one directory.

## Optional: the platform refuses a broken module

While the server is stopped, break `module.json` (for example change `"id"` to `"Hello"`, or remove a required
field), then restart. The server should still boot and log a single clear refusal, for example:

```
[Modules] Refused 'hello-world': Manifest .../module.json has an invalid id 'Hello'. Use lowercase letters, digits, and single hyphens, for example 'beast-taming'.
```

Restore the manifest afterward. This is the guarantee that a bad module is refused, not half-enabled, and never
takes the server down with it.

## Why a build is needed for this first test

Building `GameServer.jar` here is a one-time cost for installing the framework engine, not something every module
needs. See the repository's `docs/MODULE_AUTHORING_GUIDE.md`: a module's own
scripts, config, and data are loaded and compiled by the server at startup, so installing a normal module is
dropping its directory under `modules/` and restarting, with no build. A rebuild is needed only when the platform
itself changes, for example adding a new generic hook a module needs. Once that hook exists, that module and every
later one reuse it with no build.
