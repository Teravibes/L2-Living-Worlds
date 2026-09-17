using System;
using System.Diagnostics;
using System.IO;
using System.Linq;

namespace LivingWorld.Core;

// Launches and stops the Java servers, the optional brain, and the game client.
// Ports step 4 (and the client step) of launcher.ps1 and all of stop.ps1.
public sealed class Servers
{
    private readonly LauncherPaths _paths;
    private readonly Config _cfg;
    private readonly Action<string> _log;

    public Servers(LauncherPaths paths, Config cfg, Action<string> log)
    {
        _paths = paths;
        _cfg = cfg;
        _log = log;
    }

    public void StartLoginAndGame(ProcessRegistry registry, string javaExe)
    {
        var serverExe = Java.ServerExe(javaExe);

        if (_cfg.StartLogin)
        {
            StartJavaServer(registry, serverExe, "Login Server", _paths.LoginDir, @"..\libs\LoginServer.jar", _paths.LoginJar);
            // Let the login server bind before the game server registers with it.
            System.Threading.Thread.Sleep(3000);
        }
        if (_cfg.StartGame)
        {
            StartJavaServer(registry, serverExe, "Game Server", _paths.GameDir, @"..\libs\GameServer.jar", _paths.GameJar);
        }
    }

    private void StartJavaServer(ProcessRegistry registry, string serverExe, string name, string workDir, string jarRelative, string jarFull)
    {
        if (!File.Exists(jarFull))
            throw new LauncherException($"{name} jar not found at {jarFull} (build with 'ant' and copy the jar into libs\\).");

        var cfgPath = Path.Combine(workDir, "java.cfg");
        var jvmParams = File.Exists(cfgPath) ? File.ReadAllText(cfgPath).Trim() : "";
        var args = $"{jvmParams} -jar \"{jarRelative}\"".Trim();

        _log($"Launching {name} ...");
        var process = Proc.StartHidden(serverExe, args, workDir);
        registry.Register(name, process, Path.GetFileName(jarFull));
        _log($"{name} started (PID {process.Id}).");
    }

    public void StartBrainIfEnabled(ProcessRegistry registry)
    {
        if (!_cfg.StartBrain) return;

        var brainBat = _paths.FindBrainSetup();
        if (brainBat == null)
        {
            _log("StartBrain=true but setup_brain.bat not found in brain\\ or next to dist\\ - skipping.");
            return;
        }

        // --auto is non-interactive: the brain starts only if it has already been
        // configured (.env + .venv); otherwise it exits without prompting, so boot
        // never hangs waiting for input.
        //
        // Launch it VISIBLE. The brain's console is its only interface - it shows
        // the request/response log and is where the user presses CTRL+C to stop it.
        // A hidden window would leave the brain running with nothing to see, which
        // looks like it never started. This matches double-clicking setup_brain.bat.
        _log("Launching FPC brain (auto - only if already configured) ...");
        var process = Proc.StartVisible("cmd.exe", $"/c \"{brainBat}\" --auto", _paths.DistDir);
        registry.Register("FPC Brain", process, Path.GetFileName(brainBat));
        _log($"Brain launch requested (PID {process.Id}). Run setup_brain.bat once to configure it if needed.");
    }

    // Optionally launch the L2 client, waiting for the game port so it does not
    // come up to a dead server list. The client is deliberately NOT registered:
    // Stop leaves it open (it simply disconnects).
    public void LaunchClientIfEnabled()
    {
        if (!_cfg.LaunchClient) return;

        if (string.IsNullOrEmpty(_cfg.ClientExe))
        {
            _log("LaunchClient=true but ClientExe is blank in launcher.ini - skipping.");
            return;
        }
        if (!File.Exists(_cfg.ClientExe))
        {
            _log($"Game client not found at: {_cfg.ClientExe} - skipping (fix the client path in settings).");
            return;
        }

        if (_cfg.StartGame)
        {
            _log("Waiting for the game server (port 7777) before launching the client ...");
            // First boot compiles datapack scripts, which can take a while; launch
            // anyway if it is still not up by then.
            if (!Ports.WaitOpen(Ports.Game, 180))
                _log("Game port 7777 not open yet - launching the client anyway.");
        }

        var clientDir = Path.GetDirectoryName(_cfg.ClientExe) ?? _paths.DistDir;
        _log($"Launching game client: {_cfg.ClientExe}");
        Proc.StartVisible(_cfg.ClientExe, clientDir);
        _log("Game client launched.");
    }

    // ---- shutdown ----------------------------------------------------------

    public bool StopAll()
    {
        bool hadFailure = StopRecordedProcesses();
        StopBundledDatabase(ref hadFailure);
        return !hadFailure;
    }

    private bool StopRecordedProcesses()
    {
        _log("Stopping launcher-owned processes ...");
        if (!File.Exists(_paths.RegistryPath))
        {
            _log("No process registry found; nothing the launcher started is being targeted.");
            return false;
        }

        System.Collections.Generic.List<ProcessRecord> records;
        try { records = ProcessRegistry.Load(_paths.RegistryPath); }
        catch
        {
            _log("[FAIL] Process registry is unreadable; refusing to guess which processes belong to the server.");
            return true;
        }

        bool hadFailure = false;
        foreach (var r in records)
        {
            Process process;
            try { process = Process.GetProcessById(r.Id); }
            catch
            {
                _log($"{r.Role} is already stopped (stale PID {r.Id}).");
                continue;
            }

            if (!ProcessRegistry.IsAlive(r))
            {
                _log($"[FAIL] PID {r.Id} no longer matches the recorded {r.Role}; it was not terminated.");
                hadFailure = true;
                continue;
            }

            try
            {
                process.Kill(entireProcessTree: true);
                process.WaitForExit(10000);
                _log($"{r.Role} stopped (PID {r.Id}).");
            }
            catch (Exception ex)
            {
                _log($"[FAIL] Could not stop {r.Role} (PID {r.Id}): {ex.Message}");
                hadFailure = true;
            }
        }

        if (!hadFailure)
        {
            try { File.Delete(_paths.RegistryPath); } catch { /* best effort */ }
        }
        else
        {
            _log($"The process registry was retained for inspection: {_paths.RegistryPath}");
        }
        return hadFailure;
    }

    private void StopBundledDatabase(ref bool hadFailure)
    {
        if (!_cfg.BundledDb)
        {
            _log("External MySQL/MariaDB mode; database left running.");
            return;
        }

        int port = _cfg.DbPortNumber;
        if (!Ports.IsOpen(port))
        {
            _log($"Bundled MariaDB is already stopped on port {port}.");
            return;
        }

        var mysqlBin = _paths.ResolveRel(_cfg.MysqlBin);
        string? adminExe = new[] { "mariadb-admin.exe", "mysqladmin.exe" }
            .Select(n => Path.Combine(mysqlBin, n))
            .FirstOrDefault(File.Exists);
        if (adminExe == null)
        {
            _log($"[FAIL] MariaDB admin client not found in configured MysqlBin: {mysqlBin}");
            hadFailure = true;
            return;
        }

        var args = new System.Collections.Generic.List<string> { "-h", _cfg.DbHost, $"--port={port}", "-u", _cfg.DbUser };
        if (!string.IsNullOrEmpty(_cfg.DbPassword)) args.Add($"--password={_cfg.DbPassword}");
        args.Add("shutdown");

        var r = Proc.Run(adminExe, args.ToArray());
        if (r.ExitCode != 0)
        {
            _log($"[FAIL] Bundled MariaDB shutdown failed on {_cfg.DbHost}:{port} (exit code {r.ExitCode}).");
            hadFailure = true;
            return;
        }
        if (!Ports.WaitClosed(port, 15))
        {
            _log($"[FAIL] MariaDB accepted shutdown but port {port} is still open.");
            hadFailure = true;
            return;
        }
        _log($"Bundled MariaDB stopped on port {port}.");
    }
}
