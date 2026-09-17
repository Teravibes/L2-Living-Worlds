using System;
using System.Diagnostics;
using System.IO;
using System.Windows;
using System.Windows.Input;
using LivingWorld.Core;
using Microsoft.Win32;

namespace LivingWorld;

// Edits launcher.ini in place (comments preserved) so the player never has to
// open the file by hand. Only the keys shown here are written; everything else
// in the file is left untouched.
public partial class SettingsWindow : Window
{
    private readonly LauncherPaths _paths;
    private readonly Ini _ini;

    public SettingsWindow(LauncherPaths paths)
    {
        InitializeComponent();
        _paths = paths;
        _ini = new Ini(paths.IniPath);

        TitleBar.MouseLeftButtonDown += (_, __) => { try { DragMove(); } catch { } };
        JavaBrowse.Click += JavaBrowse_Click;
        ClientBrowse.Click += ClientBrowse_Click;
        BrainSetupButton.Click += BrainSetup_Click;
        SaveButton.Click += Save_Click;
        CancelButton.Click += (_, __) => Close();

        var cfg = Config.Load(_ini);
        JavaHomeBox.Text = cfg.JavaHome;
        HostBox.Text = cfg.DbHost;
        PortBox.Text = cfg.DbPort;
        UserBox.Text = cfg.DbUser;
        PasswordBox.Password = cfg.DbPassword;
        DatabaseBox.Text = cfg.Database;
        MysqlBinBox.Text = cfg.MysqlBin;
        DataDirBox.Text = cfg.DataDir;
        AutoStartMysqlCheck.IsChecked = cfg.AutoStartMysql;
        StartLoginCheck.IsChecked = cfg.StartLogin;
        StartGameCheck.IsChecked = cfg.StartGame;
        StartBrainCheck.IsChecked = cfg.StartBrain;
        ClientExeBox.Text = cfg.ClientExe;
        LaunchClientCheck.IsChecked = cfg.LaunchClient;
    }

    private void JavaBrowse_Click(object sender, RoutedEventArgs e)
    {
        var dlg = new OpenFileDialog
        {
            Title = "Select java.exe (inside the JDK's bin folder)",
            Filter = "java.exe|java.exe|Executables|*.exe|All files|*.*"
        };
        if (dlg.ShowDialog() == true)
        {
            // JavaHome is the folder that CONTAINS bin\java.exe.
            var bin = Path.GetDirectoryName(dlg.FileName);
            var home = bin != null ? Path.GetDirectoryName(bin) : null;
            JavaHomeBox.Text = home ?? "";
        }
    }

    private void ClientBrowse_Click(object sender, RoutedEventArgs e)
    {
        var dlg = new OpenFileDialog
        {
            Title = "Select the L2 client exe",
            Filter = "Executables|*.exe|All files|*.*"
        };
        if (dlg.ShowDialog() == true)
            ClientExeBox.Text = dlg.FileName;
    }

    // Opens the interactive brain setup (setup_brain.bat) in its own console
    // window so the user can install Python and pick a provider and model. Passes
    // --reconfigure so the questions run every time this button is clicked, even
    // when the brain is already configured (a plain run would just start it). The
    // saved .env values are offered as defaults, so existing API keys are reused.
    // cmd /k keeps the window up.
    private void BrainSetup_Click(object sender, RoutedEventArgs e)
    {
        var brain = _paths.FindBrainSetup();
        if (brain == null)
        {
            MessageBox.Show(
                "Brain setup was not found (brain\\setup_brain.bat). It ships with the pack; a source checkout keeps setup_brain.bat beside the dist folder.",
                "Living World Launcher", MessageBoxButton.OK, MessageBoxImage.Warning);
            return;
        }
        try
        {
            Process.Start(new ProcessStartInfo
            {
                FileName = "cmd.exe",
                Arguments = $"/k \"{brain}\" --reconfigure",
                UseShellExecute = true,
                WorkingDirectory = Path.GetDirectoryName(brain) ?? _paths.DistDir
            });
        }
        catch (Exception ex)
        {
            MessageBox.Show("Could not open the brain setup:\n" + ex.Message,
                "Living World Launcher", MessageBoxButton.OK, MessageBoxImage.Warning);
        }
    }

    private void Save_Click(object sender, RoutedEventArgs e)
    {
        _ini.Set("java", "JavaHome", JavaHomeBox.Text.Trim());

        _ini.Set("database", "Host", HostBox.Text.Trim());
        _ini.Set("database", "Port", PortBox.Text.Trim());
        _ini.Set("database", "User", UserBox.Text.Trim());
        _ini.Set("database", "Password", PasswordBox.Password);
        _ini.Set("database", "Database", DatabaseBox.Text.Trim());
        _ini.Set("database", "MysqlBin", MysqlBinBox.Text.Trim());
        _ini.Set("database", "DataDir", DataDirBox.Text.Trim());
        _ini.Set("database", "AutoStartMysql", Bool(AutoStartMysqlCheck.IsChecked));

        _ini.Set("servers", "StartLogin", Bool(StartLoginCheck.IsChecked));
        _ini.Set("servers", "StartGame", Bool(StartGameCheck.IsChecked));
        _ini.Set("servers", "StartBrain", Bool(StartBrainCheck.IsChecked));

        _ini.Set("client", "ClientExe", ClientExeBox.Text.Trim());
        _ini.Set("client", "LaunchClient", Bool(LaunchClientCheck.IsChecked));

        _ini.Save();
        Close();
    }

    private static string Bool(bool? v) => v == true ? "true" : "false";
}
