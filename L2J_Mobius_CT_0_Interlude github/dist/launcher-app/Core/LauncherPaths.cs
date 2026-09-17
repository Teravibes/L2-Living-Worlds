using System;
using System.IO;

namespace LivingWorld.Core;

// Resolves the well-known locations the launcher works with. The exe may sit in
// the dist folder (the shipped layout) or in launcher-app\ during development,
// so DistDir is discovered by walking up until a folder that clearly is the
// dist root is found (it contains game\ and login\, or launcher\launcher.ini).
public sealed class LauncherPaths
{
    public string DistDir { get; }

    public string LauncherDir => Path.Combine(DistDir, "launcher");
    public string IniPath => Path.Combine(LauncherDir, "launcher.ini");
    public string MarkerPath => Path.Combine(LauncherDir, ".db_installed");
    public string RegistryPath => Path.Combine(LauncherDir, ".processes.json");
    public string VersionPath => Path.Combine(LauncherDir, "version.txt");
    public string AssetsDir => Path.Combine(LauncherDir, "assets");
    public string L2AdminDir => Path.Combine(DistDir, "tools", "l2admin");
    public string L2AdminIndex => Path.Combine(L2AdminDir, "index.html");
    public string L2AdminBridge => Path.Combine(L2AdminDir, "native-bridge.js");
    public string WebView2UserData => Path.Combine(LauncherDir, "webview2", "userdata");
    public string BackgroundPath => Path.Combine(AssetsDir, "background.png");
    public string IconPath => Path.Combine(AssetsDir, "launcher.ico");
    public string LibsDir => Path.Combine(DistDir, "libs");
    public string LoginDir => Path.Combine(DistDir, "login");
    public string GameDir => Path.Combine(DistDir, "game");
    public string ModulesDir => Path.Combine(GameDir, "modules");
    public string LoginJar => Path.Combine(LibsDir, "LoginServer.jar");
    public string GameJar => Path.Combine(LibsDir, "GameServer.jar");
    public string DbInstallerDir => Path.Combine(DistDir, "db_installer");

    public LauncherPaths(string distDir) => DistDir = distDir;

    public static LauncherPaths Discover()
    {
        var dir = new DirectoryInfo(AppContext.BaseDirectory);
        for (int i = 0; i < 6 && dir != null; i++, dir = dir.Parent)
        {
            var full = dir.FullName;
            if (Directory.Exists(Path.Combine(full, "game")) &&
                Directory.Exists(Path.Combine(full, "login")))
                return new LauncherPaths(full);
            if (File.Exists(Path.Combine(full, "launcher", "launcher.ini")))
                return new LauncherPaths(full);
        }
        return new LauncherPaths(AppContext.BaseDirectory);
    }

    // Relative MysqlBin / DataDir are resolved against dist\ so a bundled pack
    // stays portable, mirroring Resolve-Rel in launcher.ps1.
    public string ResolveRel(string p)
    {
        if (string.IsNullOrEmpty(p)) return "";
        return Path.IsPathRooted(p) ? p : Path.Combine(DistDir, p);
    }

    // Best-effort location of the brain's persistent player memory. The pack ships
    // the brain under dist\brain\, so its memory sits at dist\brain\memory\; a raw
    // source checkout keeps it one level above dist\ at ...\memory\. Returns the
    // first that exists; when neither does, returns the pack-layout path as the
    // target to write a restored file to. The database is the real progress, so a
    // missing memory file is never fatal - the backup simply skips it.
    public string BrainMemoryFile
    {
        get
        {
            var packLevel = Path.Combine(DistDir, "brain", "memory", "fpc_memory.json");
            if (File.Exists(packLevel)) return packLevel;
            var parent = Directory.GetParent(DistDir);
            if (parent != null)
            {
                var srcLevel = Path.Combine(parent.FullName, "memory", "fpc_memory.json");
                if (File.Exists(srcLevel)) return srcLevel;
            }
            return packLevel;
        }
    }

    // The pack ships the brain at dist\brain\setup_brain.bat; a raw source
    // checkout keeps it one level above dist\. Try both, as launcher.ps1 does.
    public string? FindBrainSetup()
    {
        var packLevel = Path.Combine(DistDir, "brain", "setup_brain.bat");
        if (File.Exists(packLevel)) return packLevel;
        var parent = Directory.GetParent(DistDir);
        if (parent != null)
        {
            var srcLevel = Path.Combine(parent.FullName, "setup_brain.bat");
            if (File.Exists(srcLevel)) return srcLevel;
        }
        return null;
    }
}
