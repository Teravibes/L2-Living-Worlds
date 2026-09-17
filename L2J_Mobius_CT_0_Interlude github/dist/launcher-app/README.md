# Living World Launcher (compiled)

A real, compiled Windows launcher for the offline server. It is the single
self-contained `LivingWorld.exe` the player runs: install nothing, no console
windows, never touch PowerShell to install or launch. `Start-Server.bat` /
`Stop-Server.bat` remain as a script-only fallback. It also hosts the config
editor (l2admin) in an in-app WebView2 window via its Config Editor button.

## What it does

- **One Play button**: pre-flight, start the DB engine, install the schema on
  the first run only, start the login and game servers, optionally start the FPC
  brain, then optionally launch the game client, all with a live progress bar.
- **Status lights**: live DB / login / game / brain readouts.
- **News**: the latest full releases from the public repo.
- **Update**: downloads and applies the `-patch` overlay with a progress bar;
  your database and customized configs are never touched.
- **Settings**: edits `launcher.ini` in place (comments preserved), so no file
  needs to be hand-edited, and includes a **Set up / configure brain** button
  that runs the interactive `setup_brain.bat` (installs Python, pick Ollama or
  DeepSeek) in its own window for first-time brain setup.

It reuses the existing on-disk contract exactly: `dist\launcher\launcher.ini`,
the `.db_installed` marker, and the `.processes.json` registry are the same
files the PowerShell scripts use, so the two can coexist during the transition.

## Build

Needs the .NET 8 SDK (build machine only): <https://dotnet.microsoft.com/download/dotnet/8.0>

From the `dist` folder:

```bat
build-launcher.bat
```

That runs `dotnet publish -c Release` and copies the result to
`dist\LivingWorld.exe`. Publish settings (single file, self-contained, win-x64)
live in `LivingWorld.csproj`.

## Layout

```
launcher-app/
  LivingWorld.csproj        project + publish settings
  App.xaml(.cs)             app entry, global error guard
  MainWindow.xaml(.cs)      launcher window (news, status, Play, progress)
  SettingsWindow.xaml(.cs)  launcher.ini editor
  Core/
    LauncherPaths.cs        dist-root discovery + well-known paths
    Ini.cs / Config.cs      launcher.ini read/write + snapshot
    Java.cs / Ports.cs      Java detection, TCP port probes
    ProcessRegistry.cs      launcher-owned process tracking (start/stop safety)
    Proc.cs                 process launch + command capture helpers
    Database.cs             engine start, first-run init, schema install
    Backup.cs               whole-server backup + restore (one .zip)
    Servers.cs              login/game/brain/client start + safe stop
    Boot.cs                 the Play sequence
    Updater.cs              news feed + patch download/apply
```

The Core classes are deliberately free of UI so the boot/stop/update logic can
be unit-tested or reused without WPF.
