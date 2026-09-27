using System;
using System.IO;
using System.IO.Pipes;
using System.Text;
using System.Text.Json;
using System.Threading;

namespace LivingWorld.Core;

// Discord Rich Presence for the player's character. The game server writes
// game\presence.json (name, level, class) every few seconds; this reads it on a
// background timer and shows it on the player's Discord profile through the local
// Discord client's IPC pipe. Nothing here touches the UI thread, and nothing goes
// over the network: Discord itself does that part.
//
// The line above the details ("Playing L2 Living Worlds") is the name of the
// Discord application whose ID is used. The large image is that application's
// Rich Presence art asset named "logo".
public sealed class DiscordPresence : IDisposable
{
    // Application ID from the Discord Developer Portal. launcher.ini
    // [discord] ClientId overrides it; blank in both turns the feature off.
    private const string DefaultClientId = "1553759100238696518";

    private const int IntervalMs = 5000;
    private const long StaleMs = 20000;

    private readonly LauncherPaths _paths;
    private readonly Timer _timer;
    private readonly object _lock = new();
    private NamedPipeClientStream? _pipe;
    private string _connectedId = "";
    private string _shown = "";
    private volatile bool _disposed;

    public DiscordPresence(LauncherPaths paths)
    {
        _paths = paths;
        _timer = new Timer(_ => Tick(), null, 1000, Timeout.Infinite);
    }

    private string PresenceFile => Path.Combine(_paths.GameDir, "presence.json");

    private void Tick()
    {
        lock (_lock)
        {
            if (_disposed) return;
            try
            {
                var ini = new Ini(_paths.IniPath);
                bool enabled = ini.GetBool("discord", "Enabled", true);
                var clientId = ini.Get("discord", "ClientId", "").Trim();
                if (clientId.Length == 0) clientId = DefaultClientId;

                var activity = enabled && clientId.Length > 0 ? ReadActivity() : null;
                if (activity == null)
                {
                    if (_shown.Length > 0 && _pipe != null)
                        Send(1, SetActivityJson(null));
                    _shown = "";
                    if (!enabled || clientId.Length == 0) Disconnect();
                }
                else if (activity != _shown || clientId != _connectedId || _pipe == null)
                {
                    if (clientId != _connectedId) Disconnect();
                    if (_pipe == null && !Connect(clientId)) return;
                    Send(1, SetActivityJson(activity));
                    _shown = activity;
                }
            }
            catch
            {
                // Discord closed or restarted: drop the pipe and try again next tick.
                Disconnect();
                _shown = "";
            }
            finally
            {
                if (!_disposed) _timer.Change(IntervalMs, Timeout.Infinite);
            }
        }
    }

    // Returns the activity JSON for the character in presence.json, or null when
    // nobody is logged in or the file is missing or left over from a stopped server.
    private string? ReadActivity()
    {
        if (!File.Exists(PresenceFile)) return null;
        using var doc = JsonDocument.Parse(File.ReadAllText(PresenceFile));
        var root = doc.RootElement;
        if (!root.TryGetProperty("online", out var online) || !online.GetBoolean()) return null;
        long updated = root.TryGetProperty("updated", out var u) ? u.GetInt64() : 0;
        if (DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() - updated > StaleMs) return null;

        var name = root.GetProperty("name").GetString() ?? "";
        int level = root.GetProperty("level").GetInt32();
        var cls = root.TryGetProperty("className", out var c) ? c.GetString() ?? "" : "";
        long since = root.TryGetProperty("since", out var s) ? s.GetInt64() : 0;
        if (name.Length == 0) return null;

        using var ms = new MemoryStream();
        using (var w = new Utf8JsonWriter(ms))
        {
            w.WriteStartObject();
            w.WriteString("details", Pad(name));
            w.WriteString("state", Pad(("Lv. " + level + " " + cls).Trim()));
            if (since > 0)
            {
                w.WriteStartObject("timestamps");
                w.WriteNumber("start", since / 1000);
                w.WriteEndObject();
            }
            w.WriteStartObject("assets");
            w.WriteString("large_image", "logo");
            w.WriteString("large_text", "L2 Living Worlds");
            w.WriteEndObject();
            w.WriteEndObject();
        }
        return Encoding.UTF8.GetString(ms.ToArray());
    }

    // Discord rejects details and state shorter than two characters.
    private static string Pad(string v) => v.Length >= 2 ? v : v.PadRight(2);

    private static string SetActivityJson(string? activity)
    {
        var args = "{\"pid\":" + Environment.ProcessId + (activity != null ? ",\"activity\":" + activity : "") + "}";
        return "{\"cmd\":\"SET_ACTIVITY\",\"args\":" + args + ",\"nonce\":\"" + Guid.NewGuid().ToString("N") + "\"}";
    }

    private bool Connect(string clientId)
    {
        for (int i = 0; i < 10; i++)
        {
            var pipe = new NamedPipeClientStream(".", "discord-ipc-" + i, PipeDirection.InOut);
            try
            {
                pipe.Connect(200);
            }
            catch
            {
                pipe.Dispose();
                continue;
            }
            _pipe = pipe;
            try
            {
                // Handshake; Discord answers READY, or CLOSE for an unknown application ID.
                int op = Send(0, "{\"v\":1,\"client_id\":\"" + clientId + "\"}");
                if (op == 1)
                {
                    _connectedId = clientId;
                    return true;
                }
            }
            catch { }
            Disconnect();
        }
        return false;
    }

    // Writes one IPC frame and reads Discord's reply so the pipe never backs up.
    // Returns the reply's opcode.
    private int Send(int op, string json)
    {
        var pipe = _pipe ?? throw new IOException("not connected");
        var payload = Encoding.UTF8.GetBytes(json);
        var frame = new byte[8 + payload.Length];
        BitConverter.GetBytes(op).CopyTo(frame, 0);
        BitConverter.GetBytes(payload.Length).CopyTo(frame, 4);
        payload.CopyTo(frame, 8);
        pipe.Write(frame, 0, frame.Length);
        pipe.Flush();

        var header = new byte[8];
        pipe.ReadExactly(header, 0, 8);
        int replyOp = BitConverter.ToInt32(header, 0);
        int len = BitConverter.ToInt32(header, 4);
        if (len < 0 || len > 1 << 20) throw new IOException("bad frame");
        var body = new byte[len];
        pipe.ReadExactly(body, 0, len);
        if (replyOp == 2) throw new IOException("Discord closed the connection");
        return replyOp;
    }

    private void Disconnect()
    {
        try { _pipe?.Dispose(); } catch { }
        _pipe = null;
        _connectedId = "";
    }

    // Clears the presence and closes the pipe when the launcher exits. Waits at
    // most a second for a tick in progress so closing the window never hangs.
    public void Dispose()
    {
        if (_disposed) return;
        _disposed = true;
        _timer.Dispose();
        if (Monitor.TryEnter(_lock, 1000))
        {
            try
            {
                if (_pipe != null && _shown.Length > 0) Send(1, SetActivityJson(null));
            }
            catch { }
            finally
            {
                Disconnect();
                Monitor.Exit(_lock);
            }
        }
        else
        {
            try { _pipe?.Dispose(); } catch { }
        }
    }
}
