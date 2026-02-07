using Avalonia;
using Avalonia.Controls.ApplicationLifetimes;
using Avalonia.Markup.Xaml;
using HelixHub.Services;
using HelixHub.ViewModels;
using HelixHub.Views;

namespace HelixHub;

public partial class App : Application
{
    public override void Initialize()
    {
        AvaloniaXamlLoader.Load(this);
    }

    public override void OnFrameworkInitializationCompleted()
    {
        if (ApplicationLifetime is IClassicDesktopStyleApplicationLifetime desktop)
        {
            var configService = new ConfigService();
            var config = configService.LoadConfig();
            
            var webSocketServer = new WebSocketServer(config);
            var prismService = new PrismLauncherService(config);
            var lmStudioClient = new LmStudioClient(config);
            var planExecutor = new PlanExecutor(webSocketServer);
            
            var mainViewModel = new MainWindowViewModel(
                webSocketServer,
                prismService,
                lmStudioClient,
                planExecutor,
                configService,
                config
            );
            
            desktop.MainWindow = new MainWindow
            {
                DataContext = mainViewModel
            };
            
            desktop.Exit += (s, e) =>
            {
                webSocketServer.Stop();
            };
            
            webSocketServer.Start();
        }

        base.OnFrameworkInitializationCompleted();
    }
}
