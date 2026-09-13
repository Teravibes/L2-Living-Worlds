using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Threading;
using LivingWorld.Core;

namespace LivingWorld;

public partial class MainWindow : Window
{
    private readonly LauncherPaths _paths;
    private Config _cfg;
    private readonly Updater _updater;
    private readonly DispatcherTimer _statusTimer;

    private static readonly SolidColorBrush DotOn = new(Color.FromRgb(0x6F, 0xBF, 0x5B));
    private static readonly SolidColorBrush DotOff = new(Color.FromRgb(0x5A, 0x55, 0x4C));

    private bool _busy;
    private UpdateInfo? _pendingUpdate;

    public MainWindow()
    {
        InitializeComponent();

        _paths = LauncherPaths.Discover();
        _cfg = Config.Load(new Ini(_paths.IniPath));
        _updater = new Updater(_paths);

        // Remove a leftover LivingWorld.exe.old from a previous self-update.
        Updater.CleanupOldExe();

        LoadBackground();
        VersionText.Text = "Version: " + Coalesce(_updater.InstalledTag(), "unknown");

        // Window chrome.
        TitleBar.MouseLeftButtonDown += (_, __) => { try { DragMove(); } catch { } };
        MinButton.Click += (_, __) => WindowState = WindowState.Minimized;
        CloseButton.Click += (_, __) => Close();

        // Actions.
        PlayButton.Click += Play_Click;
        StopButton.Click += Stop_Click;
        SettingsButton.Click += Settings_Click;
        UpdateButton.Click += Update_Click;
        ConfigButton.Click += Config_Click;
        ModulesButton.Click += Modules_Click;

        // Live status lights.
        _statusTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(2) };
        _statusTimer.Tick += (_, __) => RefreshStatus();
        _statusTimer.Start();
        RefreshStatus();

        Loaded += async (_, __) =>
        {
            await LoadNewsAsync();
            await CheckUpdatesAsync(verbose: false);
        };
    }

    // ---- artwork ----------------------------------------------------------

    private void LoadBackground()
    {
        try
        {
            if (!File.Exists(_paths.BackgroundPath)) return;
            var bmp = new BitmapImage();
            bmp.BeginInit();
            bmp.CacheOption = BitmapCacheOption.OnLoad;
            bmp.CreateOptions = BitmapCreateOptions.IgnoreImageCache;
            bmp.UriSource = new Uri(_paths.BackgroundPath);
            bmp.EndInit();
            bmp.Freeze();
            BgImage.Source = bmp;

            if (File.Exists(_paths.IconPath))
                Icon = BitmapFrame.Create(new Uri(_paths.IconPath));
        }
        catch { /* artwork is optional - the gradient still looks fine */ }
    }

    // ---- logging / progress ----------------------------------------------

    private void Log(string line)
    {
        LogBox.AppendText((LogBox.Text.Length == 0 ? "" : Environment.NewLine) + line);
        LogBox.ScrollToEnd();
    }

    private void SetStage(string text, double progress)
    {
        StageText.Text = text;
        Progress.Value = Math.Clamp(progress, 0, 1);
    }

    private Action<string> UiLog => s => Dispatcher.Invoke(() => Log(s));
    private Action<string, double> UiStage => (t, v) => Dispatcher.Invoke(() => SetStage(t, v));

    // ---- Play / Stop ------------------------------------------------------

    private async void Play_Click(object sender, RoutedEventArgs e)
    {
        if (_busy) return;
        SetBusy(true);
        LogBox.Clear();
        _cfg = Config.Load(new Ini(_paths.IniPath));
        var boot = new Boot(_paths, _cfg, UiLog, UiStage);
        try
        {
            await Task.Run(boot.Run);
        }
        catch (LauncherException ex)
        {
            Log("[FAIL] " + ex.Message);
            SetStage("Startup failed - see the log.", 0);
        }
        catch (Exception ex)
        {
            Log("[FAIL] " + ex.Message);
            SetStage("Startup failed - see the log.", 0);
        }
        finally
        {
            SetBusy(false);
            RefreshStatus();
        }
    }

    private async void Stop_Click(object sender, RoutedEventArgs e)
    {
        if (_busy) return;
        SetBusy(true);
        SetStage("Stopping the server ...", 0);
        _cfg = Config.Load(new Ini(_paths.IniPath));
        var servers = new Servers(_paths, _cfg, UiLog);
        try
        {
            bool ok = await Task.Run(servers.StopAll);
            SetStage(ok ? "Server stopped." : "Shutdown finished with warnings - see the log.", 0);
        }
        catch (Exception ex)
        {
            Log("[FAIL] " + ex.Message);
        }
        finally
        {
            SetBusy(false);
            RefreshStatus();
        }
    }

    // ---- settings ---------------------------------------------------------

    private void Settings_Click(object sender, RoutedEventArgs e)
    {
        var dlg = new SettingsWindow(_paths) { Owner = this };
        dlg.ShowDialog();
        _cfg = Config.Load(new Ini(_paths.IniPath));
    }

    // Opens the config editor (l2admin) in its own in-app WebView2 window, no
    // browser. ConfigEditorWindow hosts tools\l2admin\index.html and hands it the
    // server game folder so it opens straight to the config panels. It is modeless,
    // so the server can be up or down while it is used.
    private void Config_Click(object sender, RoutedEventArgs e)
    {
        var index = _paths.L2AdminIndex;
        var bridge = _paths.L2AdminBridge;
        if (!File.Exists(index) || !File.Exists(bridge))
        {
            MessageBox.Show(
                "The config editor files were not found (tools\\l2admin\\index.html and native-bridge.js). They ship with the pack.",
                "Living World Launcher", MessageBoxButton.OK, MessageBoxImage.Warning);
            return;
        }
        try
        {
            var gameDir = Directory.Exists(_paths.GameDir) ? _paths.GameDir : null;
            var win = new ConfigEditorWindow(index, bridge, gameDir, _paths.WebView2UserData) { Owner = this };
            win.Show();
            Log("Opening the config editor ...");
        }
        catch (Exception ex)
        {
            Log("[FAIL] could not open the config editor: " + ex.Message);
        }
    }

    // Opens the Installed Modules panel: a modal list of what is in game\modules,
    // with enable, disable, and remove per module. Changes it makes to a module's
    // config\module.ini take effect on the next server start.
    private void Modules_Click(object sender, RoutedEventArgs e)
    {
        var dlg = new ModulesWindow(_paths) { Owner = this };
        dlg.ShowDialog();
    }

    // ---- updates ----------------------------------------------------------

    private async void Update_Click(object sender, RoutedEventArgs e)
    {
        if (_busy) return;

        if (_pendingUpdate == null)
        {
            UpdateButton.IsEnabled = false;
            var before = _pendingUpdate;
            await CheckUpdatesAsync(verbose: true);
            UpdateButton.IsEnabled = true;
            if (_pendingUpdate == null || _pendingUpdate == before) return;
        }

        var info = _pendingUpdate;
        if (info == null) return;

        var answer = MessageBox.Show(
            $"Update {info.InstalledTag}  ->  {info.LatestTag}?\n\n" +
            "This stops the server, then overlays the patch. Your database and customized configs are not touched.",
            "Living World Launcher", MessageBoxButton.YesNo, MessageBoxImage.Question);
        if (answer != MessageBoxResult.Yes) return;

        SetBusy(true);
        LogBox.Clear();
        SetStage($"Updating to {info.LatestTag} ...", 0);
        var progress = new System.Progress<double>(v => Progress.Value = v);
        var stopServers = new Action(() => new Servers(_paths, Config.Load(new Ini(_paths.IniPath)), UiLog).StopAll());
        try
        {
            await _updater.ApplyPatchAsync(info.LatestTag, progress, UiLog, stopServers);
            VersionText.Text = "Version: " + info.LatestTag;

            // The launcher exe ships as its own release asset (too large for the
            // patch). If a newer one is published, swap it in for the next launch.
            try
            {
                var exeUrl = await _updater.LauncherExeUpdateUrlAsync(info.LatestTag);
                if (exeUrl != null)
                {
                    SetStage("Updating the launcher ...", 0);
                    await _updater.RefreshLauncherExeAsync(exeUrl, progress, UiLog);
                    MessageBox.Show(
                        "The launcher itself was updated. Close and reopen LivingWorld.exe to run the new version.",
                        "Living World Launcher", MessageBoxButton.OK, MessageBoxImage.Information);
                }
            }
            catch (Exception ex)
            {
                Log("[warn] launcher self-update skipped: " + ex.Message);
            }

            UpdateText.Text = "";
            UpdateButton.Content = "Check for updates";
            _pendingUpdate = null;
            SetStage("Update complete.", 1);
        }
        catch (LauncherException ex) { Log("[FAIL] " + ex.Message); SetStage("Update failed - see the log.", 0); }
        catch (Exception ex) { Log("[FAIL] " + ex.Message); SetStage("Update failed - see the log.", 0); }
        finally
        {
            SetBusy(false);
            RefreshStatus();
        }
    }

    private async Task CheckUpdatesAsync(bool verbose)
    {
        try
        {
            var info = await _updater.CheckAsync();
            if (info != null)
            {
                _pendingUpdate = info;
                UpdateText.Text = "Update available: " + info.LatestTag;
                UpdateButton.Content = "Update now";
            }
            else if (verbose)
            {
                MessageBox.Show("You are up to date.", "Living World Launcher",
                    MessageBoxButton.OK, MessageBoxImage.Information);
            }
        }
        catch (Exception ex)
        {
            if (verbose)
                MessageBox.Show("Could not check for updates:\n" + ex.Message, "Living World Launcher",
                    MessageBoxButton.OK, MessageBoxImage.Warning);
        }
    }

    // ---- news -------------------------------------------------------------

    private async Task LoadNewsAsync()
    {
        try
        {
            var items = await _updater.FetchNewsAsync();
            if (items.Count == 0)
            {
                ShowNewsMessage("No news yet. Check back after the next release.");
                return;
            }
            var views = new List<object>();
            foreach (var n in items)
                views.Add(new
                {
                    n.Title,
                    Body = string.IsNullOrWhiteSpace(n.Body) ? "(no notes)" : n.Body,
                    DateText = n.Date?.ToLocalTime().ToString("d MMM yyyy") ?? ""
                });
            NewsList.ItemsSource = views;
            NewsEmpty.Visibility = Visibility.Collapsed;
        }
        catch (Exception ex)
        {
            ShowNewsMessage("News is offline right now (" + ex.Message + "). You can still play.");
        }
    }

    private void ShowNewsMessage(string text)
    {
        NewsList.ItemsSource = null;
        NewsEmpty.Text = text;
        NewsEmpty.Visibility = Visibility.Visible;
    }

    // ---- status -----------------------------------------------------------

    private void RefreshStatus()
    {
        bool db = Ports.IsOpen(Ports.Database);
        bool login = Ports.IsOpen(Ports.Login);
        bool game = Ports.IsOpen(Ports.Game);
        bool brain = Ports.IsOpen(Ports.Brain);

        SetDot(DbDot, DbState, db);
        SetDot(LoginDot, LoginState, login);
        SetDot(GameDot, GameState, game);
        SetDot(BrainDot, BrainState, brain);

        bool anyServer = login || game || brain;
        if (!_busy)
        {
            PlayButton.IsEnabled = !anyServer;
            StopButton.IsEnabled = anyServer || File.Exists(_paths.RegistryPath);
            if (anyServer && StageText.Text == "Ready to launch.")
                StageText.Text = "Server is running.";
        }
    }

    private static void SetDot(System.Windows.Shapes.Ellipse dot, System.Windows.Controls.TextBlock label, bool on)
    {
        dot.Fill = on ? DotOn : DotOff;
        label.Text = on ? "online" : "offline";
    }

    private void SetBusy(bool busy)
    {
        _busy = busy;
        PlayButton.IsEnabled = !busy;
        StopButton.IsEnabled = !busy && (Ports.IsOpen(Ports.Login) || Ports.IsOpen(Ports.Game) || File.Exists(_paths.RegistryPath));
        SettingsButton.IsEnabled = !busy;
        UpdateButton.IsEnabled = !busy;
    }

    private static string Coalesce(string v, string fallback) =>
        string.IsNullOrWhiteSpace(v) ? fallback : v;
}
