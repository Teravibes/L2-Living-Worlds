using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using LivingWorld.Core;

namespace LivingWorld;

// The Installed Modules panel. It lists what is in game\modules, lets the player
// turn a module on or off (which rewrites that module's config\module.ini and takes
// effect on the next server start), and removes a module by deleting its one folder.
// It never drops database tables: player data outlives a removed module.
public partial class ModulesWindow : Window
{
    private readonly Modules _modules;

    public ModulesWindow(LauncherPaths paths)
    {
        InitializeComponent();
        _modules = new Modules(paths);

        TitleBar.MouseLeftButtonDown += (_, __) => { try { DragMove(); } catch { } };
        CloseButton.Click += (_, __) => Close();

        RenderList();
    }

    private void RenderList()
    {
        ModuleList.Children.Clear();
        var modules = _modules.Scan();
        EmptyText.Visibility = modules.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
        foreach (var module in modules)
            ModuleList.Children.Add(BuildCard(module));
    }

    private UIElement BuildCard(ModuleInfo module)
    {
        var body = new StackPanel();

        // Header: name + version on the left, a state chip on the right.
        var header = new DockPanel { LastChildFill = true };
        var chip = BuildChip(module);
        DockPanel.SetDock(chip, Dock.Right);
        header.Children.Add(chip);

        var titleRow = new StackPanel { Orientation = Orientation.Horizontal, VerticalAlignment = VerticalAlignment.Center };
        titleRow.Children.Add(new TextBlock
        {
            Text = module.Name,
            Foreground = Res("Gold"),
            FontFamily = new FontFamily("Georgia"),
            FontSize = 15
        });
        if (module.Version.Length > 0)
            titleRow.Children.Add(new TextBlock
            {
                Text = "  v" + module.Version,
                Foreground = Res("InkDim"),
                FontSize = 12,
                VerticalAlignment = VerticalAlignment.Center
            });
        header.Children.Add(titleRow);
        body.Children.Add(header);

        if (module.Author.Length > 0)
            body.Children.Add(new TextBlock
            {
                Text = "by " + module.Author,
                Foreground = Res("InkDim"),
                FontSize = 11,
                Margin = new Thickness(0, 2, 0, 0)
            });

        if (!module.Valid)
            body.Children.Add(new TextBlock
            {
                Text = module.Problem,
                Foreground = Hex("#E0A06A"),
                FontSize = 12,
                TextWrapping = TextWrapping.Wrap,
                Margin = new Thickness(0, 6, 0, 0)
            });
        else if (module.Description.Length > 0)
            body.Children.Add(new TextBlock
            {
                Text = module.Description,
                Foreground = Res("Ink"),
                FontSize = 12.5,
                TextWrapping = TextWrapping.Wrap,
                Margin = new Thickness(0, 6, 0, 0)
            });

        // Actions.
        var actions = new StackPanel { Orientation = Orientation.Horizontal, Margin = new Thickness(0, 12, 0, 0) };
        if (module.Valid)
        {
            var toggle = MakeButton(module.Enabled ? "Disable" : "Enable");
            toggle.Click += (_, __) =>
            {
                try
                {
                    _modules.SetEnabled(module, !module.Enabled);
                    MarkChanged();
                    RenderList();
                }
                catch (Exception ex)
                {
                    Warn("Could not change this module:\n" + ex.Message);
                }
            };
            actions.Children.Add(toggle);
        }

        var remove = MakeButton("Remove");
        remove.Margin = new Thickness(8, 0, 0, 0);
        remove.Click += (_, __) => Remove(module);
        actions.Children.Add(remove);
        body.Children.Add(actions);

        return new Border
        {
            CornerRadius = new CornerRadius(8),
            Background = Hex("#22000000"),
            BorderBrush = Res("CardEdge"),
            BorderThickness = new Thickness(1),
            Padding = new Thickness(14, 12, 14, 12),
            Margin = new Thickness(0, 0, 0, 10),
            Child = body
        };
    }

    private Border BuildChip(ModuleInfo module)
    {
        string text;
        Brush fg;
        if (!module.Valid)
        {
            text = "Invalid";
            fg = Hex("#E0A06A");
        }
        else if (module.Enabled)
        {
            text = "Enabled";
            fg = Hex("#8BC98B");
        }
        else
        {
            text = "Disabled";
            fg = Res("InkDim");
        }

        return new Border
        {
            CornerRadius = new CornerRadius(4),
            BorderBrush = fg,
            BorderThickness = new Thickness(1),
            Padding = new Thickness(8, 2, 8, 2),
            VerticalAlignment = VerticalAlignment.Center,
            Child = new TextBlock { Text = text, Foreground = fg, FontSize = 11 }
        };
    }

    private void Remove(ModuleInfo module)
    {
        var answer = MessageBox.Show(
            $"Remove the module \"{module.Name}\"?\n\nThis deletes its folder from game\\modules. It does not delete any database tables, so player progress a module stored is kept.",
            "Living World Launcher", MessageBoxButton.YesNo, MessageBoxImage.Warning);
        if (answer != MessageBoxResult.Yes)
            return;

        try
        {
            _modules.Remove(module);
            MarkChanged();
            RenderList();
        }
        catch (Exception ex)
        {
            Warn("Could not remove this module:\n" + ex.Message);
        }
    }

    private void MarkChanged() => RestartNote.Visibility = Visibility.Visible;

    private Button MakeButton(string text) => new()
    {
        Content = text,
        Style = (Style) FindResource("Btn")
    };

    private static void Warn(string message)
        => MessageBox.Show(message, "Living World Launcher", MessageBoxButton.OK, MessageBoxImage.Warning);

    private Brush Res(string key) => (Brush) FindResource(key);

    private static Brush Hex(string hex) => new SolidColorBrush((Color) ColorConverter.ConvertFromString(hex));
}
