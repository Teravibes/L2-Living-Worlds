using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Text.Json;

namespace LivingWorld.Core;

// One installed module as the launcher sees it: its identity from module.json and
// its on/off switch from config/module.ini. The launcher only reads and toggles;
// the game server is the authority that actually validates and enables a module.
public sealed class ModuleInfo
{
    public string Id { get; init; } = "";
    public string Name { get; init; } = "";
    public string Version { get; init; } = "";
    public string Author { get; init; } = "";
    public string Description { get; init; } = "";
    public string Directory { get; init; } = "";
    public bool Enabled { get; set; }
    public bool Valid { get; init; }
    public string Problem { get; init; } = "";
}

// Reads the installed-module list from game\modules and flips each module's enable
// switch or removes its folder. It never touches anything outside a module's own
// directory: a remove deletes exactly the one validated folder, and disabling only
// rewrites that module's config\module.ini in place, keeping its comments.
public sealed class Modules
{
    private readonly string _root;

    public Modules(LauncherPaths paths) => _root = paths.ModulesDir;

    public string Root => _root;

    // All installed modules, ordered by display name. Invalid ones are still listed
    // (with a reason) so a player can see and remove them rather than being puzzled.
    public List<ModuleInfo> Scan()
    {
        var list = new List<ModuleInfo>();
        if (!System.IO.Directory.Exists(_root))
            return list;

        foreach (var dir in System.IO.Directory.GetDirectories(_root))
        {
            var name = Path.GetFileName(dir);
            var manifest = Path.Combine(dir, "module.json");
            if (!File.Exists(manifest))
                continue;

            list.Add(Read(dir, name, manifest));
        }

        list.Sort((a, b) => string.Compare(a.Name, b.Name, StringComparison.OrdinalIgnoreCase));
        return list;
    }

    private ModuleInfo Read(string dir, string dirName, string manifest)
    {
        string id = "", name = "", version = "", author = "", description = "", problem = "";
        var valid = true;
        try
        {
            using var doc = JsonDocument.Parse(File.ReadAllText(manifest));
            var root = doc.RootElement;
            id = Str(root, "id");
            name = Str(root, "name");
            version = Str(root, "version");
            author = Str(root, "author");
            description = Str(root, "description");
        }
        catch (Exception ex)
        {
            valid = false;
            problem = "module.json could not be read: " + ex.Message;
        }

        if (valid && id.Length == 0)
        {
            valid = false;
            problem = "module.json is missing an id.";
        }
        else if (valid && !string.Equals(id, dirName, StringComparison.Ordinal))
        {
            valid = false;
            problem = $"id '{id}' does not match its folder name '{dirName}'.";
        }

        return new ModuleInfo
        {
            Id = id,
            Name = name.Length > 0 ? name : dirName,
            Version = version,
            Author = author,
            Description = description,
            Directory = dir,
            Enabled = ReadEnabled(dir),
            Valid = valid,
            Problem = problem
        };
    }

    private static string Str(JsonElement obj, string key)
        => obj.ValueKind == JsonValueKind.Object && obj.TryGetProperty(key, out var v) && v.ValueKind == JsonValueKind.String
            ? v.GetString() ?? ""
            : "";

    // module.ini is a flat, section-less ini (the same shape the game server's
    // ConfigReader reads), so the launcher's section-based Ini class does not fit;
    // the Enabled switch is read and written directly here.
    private static string IniPath(string dir) => Path.Combine(dir, "config", "module.ini");

    private static bool ReadEnabled(string dir)
    {
        var ini = IniPath(dir);
        if (!File.Exists(ini))
            return false;

        foreach (var raw in File.ReadAllLines(ini))
        {
            var line = raw.Trim();
            if (line.Length == 0 || line[0] == '#' || line[0] == ';')
                continue;

            int idx = line.IndexOf('=');
            if (idx <= 0)
                continue;

            if (string.Equals(line[..idx].Trim(), "Enabled", StringComparison.OrdinalIgnoreCase))
            {
                var val = line[(idx + 1)..].Trim().ToLowerInvariant();
                return val is "true" or "1" or "yes" or "on";
            }
        }
        return false;
    }

    // Flips the Enabled line in place, preserving every comment and the file's
    // leading whitespace on that line. Adds the line if the file has none.
    public void SetEnabled(ModuleInfo module, bool enabled)
    {
        var ini = IniPath(module.Directory);
        var value = enabled ? "True" : "False";
        var lines = File.Exists(ini) ? new List<string>(File.ReadAllLines(ini)) : new List<string>();

        for (int i = 0; i < lines.Count; i++)
        {
            var line = lines[i];
            var trimmed = line.Trim();
            if (trimmed.Length == 0 || trimmed[0] == '#' || trimmed[0] == ';')
                continue;

            int idx = trimmed.IndexOf('=');
            if (idx <= 0)
                continue;

            if (string.Equals(trimmed[..idx].Trim(), "Enabled", StringComparison.OrdinalIgnoreCase))
            {
                var lead = line[..(line.Length - line.TrimStart().Length)];
                lines[i] = $"{lead}Enabled = {value}";
                Write(ini, lines);
                module.Enabled = enabled;
                return;
            }
        }

        lines.Add($"Enabled = {value}");
        System.IO.Directory.CreateDirectory(Path.GetDirectoryName(ini)!);
        Write(ini, lines);
        module.Enabled = enabled;
    }

    private static void Write(string path, List<string> lines)
    {
        var tmp = path + ".tmp";
        File.WriteAllLines(tmp, lines, new UTF8Encoding(false));
        File.Copy(tmp, path, true);
        File.Delete(tmp);
    }

    // Deletes exactly the one module folder, after proving it really is a direct
    // child of the modules root. This is the only destructive action, and it never
    // follows a path from anywhere but the scanned directory itself.
    public void Remove(ModuleInfo module)
    {
        var full = Path.GetFullPath(module.Directory);
        var rootFull = Path.GetFullPath(_root);
        var parent = System.IO.Directory.GetParent(full)?.FullName;

        if (parent == null || !string.Equals(parent, rootFull, StringComparison.OrdinalIgnoreCase))
            throw new InvalidOperationException("Refusing to remove a path outside the modules folder.");

        if (!System.IO.Directory.Exists(full))
            return;

        System.IO.Directory.Delete(full, true);
    }
}
