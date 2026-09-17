using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Text;

namespace LivingWorld.Core;

// Whole-server backup and restore for the one-click launcher buttons.
//
// A backup is a single portable .zip a player can copy to a USB stick or the
// cloud and keep somewhere safe. It contains:
//   * database.sql    - a full dump of the game database, where every account,
//                        character, item, clan, and quest lives
//   * fpc_memory.json - the brain's persistent player memory, when present
//   * manifest.txt    - server version, date, and database name, for reference
//
// Restore recreates the database from database.sql and puts the memory file
// back. It replaces ALL current progress, so the caller confirms first and
// stops the servers before calling in.
//
// Both paths reuse the launcher's existing pieces: the configured MysqlBin, the
// launcher.ini database credentials, and Database.EnsureRunning so the engine is
// up (a backup or a restore is usually run with the game servers stopped).
public sealed class Backup
{
    public const string DatabaseEntry = "database.sql";
    public const string MemoryEntry = "fpc_memory.json";
    public const string ManifestEntry = "manifest.txt";

    private readonly LauncherPaths _paths;
    private readonly Config _cfg;
    private readonly Action<string> _log;
    private readonly string _mysqlBin;
    private readonly string _mysqlExe;

    public Backup(LauncherPaths paths, Config cfg, Action<string> log)
    {
        _paths = paths;
        _cfg = cfg;
        _log = log;
        _mysqlBin = paths.ResolveRel(cfg.MysqlBin);
        _mysqlExe = Path.Combine(_mysqlBin, "mysql.exe");
    }

    // A sensible default file name for the Save dialog.
    public static string SuggestedFileName() =>
        $"LivingWorld-backup-{DateTime.Now:yyyyMMdd-HHmm}.zip";

    // ---- backup -----------------------------------------------------------

    public void CreateBackup(string zipPath)
    {
        var dumpExe = FindDumpExe();
        // The DB engine must be up to read from it; start the bundled one if needed.
        new Database(_paths, _cfg, _log).EnsureRunning();

        var work = NewTempDir();
        try
        {
            var sqlFile = Path.Combine(work, DatabaseEntry);
            _log($"Backing up database '{_cfg.Database}' ...");
            DumpDatabase(dumpExe, sqlFile);

            var mem = _paths.BrainMemoryFile;
            bool haveMem = File.Exists(mem);
            if (haveMem)
            {
                File.Copy(mem, Path.Combine(work, MemoryEntry), true);
                _log("Included brain memory.");
            }
            else
            {
                _log("No brain memory file found - skipping it (nothing to include).");
            }

            File.WriteAllText(Path.Combine(work, ManifestEntry), BuildManifest(haveMem));

            _log("Packaging ...");
            if (File.Exists(zipPath)) File.Delete(zipPath);
            ZipFile.CreateFromDirectory(work, zipPath, CompressionLevel.Optimal, includeBaseDirectory: false);
            var mb = new FileInfo(zipPath).Length / 1024 / 1024;
            _log($"Backup written: {zipPath} ({mb} MB).");
        }
        finally
        {
            TryDeleteDir(work);
        }
    }

    private void DumpDatabase(string dumpExe, string outFile)
    {
        var args = new List<string> { "-h", _cfg.DbHost, "-P", _cfg.DbPort, "-u", _cfg.DbUser };
        if (!string.IsNullOrEmpty(_cfg.DbPassword)) args.Add($"--password={_cfg.DbPassword}");
        args.Add("--default-character-set=utf8");
        // --databases carries CREATE DATABASE + USE so a restore is self-contained;
        // --add-drop-database makes the restore replace the old database cleanly.
        args.Add("--add-drop-database");
        args.Add("--routines");
        args.Add("--events");
        args.Add("--databases");
        args.Add(_cfg.Database);

        var err = RunToFile(dumpExe, args.ToArray(), outFile);
        if (!File.Exists(outFile) || new FileInfo(outFile).Length == 0)
            throw new LauncherException(
                "The database dump was empty. Is the database engine running, and are the credentials in launcher.ini correct?\n" + err);
    }

    // ---- restore ----------------------------------------------------------

    public void RestoreBackup(string zipPath)
    {
        if (!File.Exists(zipPath))
            throw new LauncherException($"Backup file not found: {zipPath}");
        if (!File.Exists(_mysqlExe))
            throw new LauncherException($"Restore needs mysql.exe at {_mysqlExe}. Fix MysqlBin in launcher.ini.");

        var work = NewTempDir();
        try
        {
            _log("Opening backup ...");
            ZipFile.ExtractToDirectory(zipPath, work);
            var sqlFile = Path.Combine(work, DatabaseEntry);
            if (!File.Exists(sqlFile))
                throw new LauncherException(
                    $"This zip does not look like a Living World backup ({DatabaseEntry} is missing).");

            // The DB engine must be up to import into it.
            new Database(_paths, _cfg, _log).EnsureRunning();

            _log($"Restoring database '{_cfg.Database}' (this replaces current progress) ...");
            ImportSql(sqlFile);

            var mem = Path.Combine(work, MemoryEntry);
            if (File.Exists(mem))
            {
                var target = _paths.BrainMemoryFile;
                var dir = Path.GetDirectoryName(target);
                if (!string.IsNullOrEmpty(dir)) Directory.CreateDirectory(dir);
                File.Copy(mem, target, true);
                _log("Restored brain memory.");
            }

            // The database now has tables, so mark the schema installed to stop the
            // first-run installer from ever importing over the restored data.
            File.WriteAllText(_paths.MarkerPath, $"restored {DateTime.Now:s}");
            _log("Restore complete. Start the server to play on the restored data.");
        }
        finally
        {
            TryDeleteDir(work);
        }
    }

    private void ImportSql(string sqlFile)
    {
        var args = new List<string> { "-h", _cfg.DbHost, "-P", _cfg.DbPort, "-u", _cfg.DbUser };
        if (!string.IsNullOrEmpty(_cfg.DbPassword)) args.Add($"--password={_cfg.DbPassword}");
        args.Add("--default-character-set=utf8");
        // The dump carries CREATE DATABASE / USE (from --databases), so no db arg here.

        var (code, err) = RunWithStdinFile(_mysqlExe, args.ToArray(), sqlFile);
        if (code != 0)
            throw new LauncherException("Database import failed. Nothing was started.\n" + err);
    }

    // ---- process helpers --------------------------------------------------
    // These stream to and from a file so a large dump is never held whole in
    // memory (a mature server's dump can be tens of MB).

    private string RunToFile(string exe, string[] args, string outFile)
    {
        var psi = new ProcessStartInfo
        {
            FileName = exe,
            UseShellExecute = false,
            CreateNoWindow = true,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            StandardErrorEncoding = Encoding.UTF8,
            WorkingDirectory = _mysqlBin,
        };
        foreach (var a in args) psi.ArgumentList.Add(a);

        using var p = Process.Start(psi) ?? throw new LauncherException($"Failed to start: {exe}");
        var err = new StringBuilder();
        p.ErrorDataReceived += (_, e) => { if (e.Data != null) err.AppendLine(e.Data); };
        p.BeginErrorReadLine();

        using (var fs = new FileStream(outFile, FileMode.Create, FileAccess.Write))
        {
            p.StandardOutput.BaseStream.CopyTo(fs);
        }
        p.WaitForExit();
        var errText = err.ToString().Trim();
        if (p.ExitCode != 0)
            throw new LauncherException($"{Path.GetFileName(exe)} failed (code {p.ExitCode}).\n" + errText);
        return errText;
    }

    private (int code, string err) RunWithStdinFile(string exe, string[] args, string inFile)
    {
        var psi = new ProcessStartInfo
        {
            FileName = exe,
            UseShellExecute = false,
            CreateNoWindow = true,
            RedirectStandardInput = true,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            StandardErrorEncoding = Encoding.UTF8,
            WorkingDirectory = _mysqlBin,
        };
        foreach (var a in args) psi.ArgumentList.Add(a);

        using var p = Process.Start(psi) ?? throw new LauncherException($"Failed to start: {exe}");
        var err = new StringBuilder();
        p.ErrorDataReceived += (_, e) => { if (e.Data != null) err.AppendLine(e.Data); };
        p.OutputDataReceived += (_, __) => { };
        p.BeginErrorReadLine();
        p.BeginOutputReadLine();

        using (var fs = new FileStream(inFile, FileMode.Open, FileAccess.Read))
        {
            fs.CopyTo(p.StandardInput.BaseStream);
            p.StandardInput.Close();
        }
        p.WaitForExit();
        return (p.ExitCode, err.ToString().Trim());
    }

    // ---- misc -------------------------------------------------------------

    private string FindDumpExe()
    {
        var candidate = new[] { "mariadb-dump.exe", "mysqldump.exe" }
            .Select(n => Path.Combine(_mysqlBin, n))
            .FirstOrDefault(File.Exists);
        if (candidate == null)
            throw new LauncherException(
                $"No mariadb-dump.exe / mysqldump.exe found in {_mysqlBin}. Fix MysqlBin in launcher.ini.");
        return candidate;
    }

    private string BuildManifest(bool haveMem)
    {
        var version = File.Exists(_paths.VersionPath)
            ? File.ReadAllText(_paths.VersionPath).Trim()
            : "unknown";
        var sb = new StringBuilder();
        sb.AppendLine("Living World backup");
        sb.AppendLine($"created={DateTime.Now:yyyy-MM-dd HH:mm:ss}");
        sb.AppendLine($"server_version={version}");
        sb.AppendLine($"database={_cfg.Database}");
        sb.AppendLine($"includes_brain_memory={(haveMem ? "yes" : "no")}");
        return sb.ToString();
    }

    private static string NewTempDir()
    {
        var dir = Path.Combine(Path.GetTempPath(), "lw-backup-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(dir);
        return dir;
    }

    private static void TryDeleteDir(string dir)
    {
        try { if (Directory.Exists(dir)) Directory.Delete(dir, true); } catch { /* best effort */ }
    }
}
