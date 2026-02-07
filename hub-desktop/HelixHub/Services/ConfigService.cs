using System.Text.Json;
using HelixHub.Models;

namespace HelixHub.Services;

public class ConfigService
{
    private static readonly string ConfigDirectory = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "HelixHub"
    );
    
    private static readonly string ConfigPath = Path.Combine(ConfigDirectory, "config.json");
    
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        WriteIndented = true,
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase
    };
    
    public HubConfig LoadConfig()
    {
        try
        {
            if (File.Exists(ConfigPath))
            {
                var json = File.ReadAllText(ConfigPath);
                var config = JsonSerializer.Deserialize<HubConfig>(json, JsonOptions);
                if (config != null)
                {
                    // Generate token if missing
                    if (string.IsNullOrEmpty(config.AuthToken))
                    {
                        config.AuthToken = HubConfig.GenerateToken();
                        SaveConfig(config);
                    }
                    return config;
                }
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error loading config: {ex.Message}");
        }
        
        // Create default config
        var defaultConfig = new HubConfig
        {
            AuthToken = HubConfig.GenerateToken(),
            PrismDataRoot = GetDefaultPrismDataRoot()
        };
        
        // Try to find Prism Launcher
        var prismPath = FindPrismLauncher();
        if (!string.IsNullOrEmpty(prismPath))
        {
            defaultConfig.PrismLauncherPath = prismPath;
        }
        
        SaveConfig(defaultConfig);
        return defaultConfig;
    }
    
    public void SaveConfig(HubConfig config)
    {
        try
        {
            Directory.CreateDirectory(ConfigDirectory);
            var json = JsonSerializer.Serialize(config, JsonOptions);
            File.WriteAllText(ConfigPath, json);
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error saving config: {ex.Message}");
        }
    }
    
    private string GetDefaultPrismDataRoot()
    {
        // Default Prism Launcher data location on Windows
        var appData = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
        var prismPath = Path.Combine(appData, "PrismLauncher");
        
        if (Directory.Exists(prismPath))
        {
            return prismPath;
        }
        
        // Try portable location next to executable
        var exeDir = AppContext.BaseDirectory;
        var portablePath = Path.Combine(exeDir, "PrismLauncher");
        if (Directory.Exists(portablePath))
        {
            return portablePath;
        }
        
        return prismPath; // Return default even if doesn't exist
    }
    
    private string? FindPrismLauncher()
    {
        var possiblePaths = new[]
        {
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "PrismLauncher", "prismlauncher.exe"),
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Programs", "PrismLauncher", "prismlauncher.exe"),
            @"C:\Program Files\PrismLauncher\prismlauncher.exe",
            @"C:\Program Files (x86)\PrismLauncher\prismlauncher.exe"
        };
        
        foreach (var path in possiblePaths)
        {
            if (File.Exists(path))
            {
                return path;
            }
        }
        
        return null;
    }
}
