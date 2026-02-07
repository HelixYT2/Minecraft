using System.Diagnostics;
using System.Text.Json;
using HelixHub.Models;

namespace HelixHub.Services;

public class PrismLauncherService
{
    private readonly HubConfig _config;
    
    public event Action<string>? LogMessage;
    
    public PrismLauncherService(HubConfig config)
    {
        _config = config;
    }
    
    public List<PrismInstance> GetInstances()
    {
        var instances = new List<PrismInstance>();
        
        if (string.IsNullOrEmpty(_config.PrismDataRoot) || !Directory.Exists(_config.PrismDataRoot))
        {
            Log($"Prism data root not found: {_config.PrismDataRoot}");
            return instances;
        }
        
        var instancesPath = Path.Combine(_config.PrismDataRoot, "instances");
        if (!Directory.Exists(instancesPath))
        {
            Log($"Instances directory not found: {instancesPath}");
            return instances;
        }
        
        foreach (var instanceDir in Directory.GetDirectories(instancesPath))
        {
            var instanceCfgPath = Path.Combine(instanceDir, "instance.cfg");
            var mccPath = Path.Combine(instanceDir, "mmc-pack.json");
            
            if (!File.Exists(instanceCfgPath)) continue;
            
            var instance = new PrismInstance
            {
                Id = Path.GetFileName(instanceDir),
                Path = instanceDir
            };
            
            // Parse instance.cfg
            try
            {
                var lines = File.ReadAllLines(instanceCfgPath);
                foreach (var line in lines)
                {
                    if (line.StartsWith("name="))
                    {
                        instance.Name = line.Substring(5);
                    }
                    else if (line.StartsWith("iconKey="))
                    {
                        var iconKey = line.Substring(8);
                        instance.IconPath = FindIcon(iconKey);
                    }
                }
            }
            catch (Exception ex)
            {
                Log($"Error parsing instance.cfg for {instance.Id}: {ex.Message}");
            }
            
            if (string.IsNullOrEmpty(instance.Name))
            {
                instance.Name = instance.Id;
            }
            
            // Parse mmc-pack.json for version info
            if (File.Exists(mccPath))
            {
                try
                {
                    var json = File.ReadAllText(mccPath);
                    var doc = JsonDocument.Parse(json);
                    
                    if (doc.RootElement.TryGetProperty("components", out var components))
                    {
                        foreach (var component in components.EnumerateArray())
                        {
                            if (component.TryGetProperty("uid", out var uid))
                            {
                                var uidStr = uid.GetString();
                                if (uidStr == "net.minecraft" && component.TryGetProperty("version", out var version))
                                {
                                    instance.MinecraftVersion = version.GetString();
                                }
                            }
                        }
                    }
                }
                catch (Exception ex)
                {
                    Log($"Error parsing mmc-pack.json for {instance.Id}: {ex.Message}");
                }
            }
            
            // Check for Helix mod and Baritone
            var modsPath = Path.Combine(instanceDir, ".minecraft", "mods");
            if (Directory.Exists(modsPath))
            {
                var modFiles = Directory.GetFiles(modsPath, "*.jar");
                instance.HasHelixMod = modFiles.Any(f => Path.GetFileName(f).Contains("helix", StringComparison.OrdinalIgnoreCase));
                instance.HasBaritone = modFiles.Any(f => Path.GetFileName(f).Contains("baritone", StringComparison.OrdinalIgnoreCase));
            }
            
            instances.Add(instance);
        }
        
        return instances.OrderBy(i => i.Name).ToList();
    }
    
    public async Task<bool> LaunchInstance(string instanceId, int waitTimeoutSeconds = 60)
    {
        if (string.IsNullOrEmpty(_config.PrismLauncherPath) || !File.Exists(_config.PrismLauncherPath))
        {
            Log($"Prism Launcher not found at: {_config.PrismLauncherPath}");
            return false;
        }
        
        try
        {
            var args = $"--launch \"{instanceId}\"";
            
            // Add --dir if using custom data root
            var defaultRoot = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                "PrismLauncher"
            );
            
            if (!string.IsNullOrEmpty(_config.PrismDataRoot) && 
                !_config.PrismDataRoot.Equals(defaultRoot, StringComparison.OrdinalIgnoreCase))
            {
                args = $"--dir \"{_config.PrismDataRoot}\" {args}";
            }
            
            Log($"Launching: {_config.PrismLauncherPath} {args}");
            
            var process = new Process
            {
                StartInfo = new ProcessStartInfo
                {
                    FileName = _config.PrismLauncherPath,
                    Arguments = args,
                    UseShellExecute = false,
                    CreateNoWindow = false
                }
            };
            
            process.Start();
            
            Log($"Prism Launcher started for instance: {instanceId}");
            return true;
        }
        catch (Exception ex)
        {
            Log($"Error launching instance: {ex.Message}");
            return false;
        }
    }
    
    private string? FindIcon(string iconKey)
    {
        if (string.IsNullOrEmpty(_config.PrismDataRoot)) return null;
        
        var iconsPath = Path.Combine(_config.PrismDataRoot, "icons");
        if (!Directory.Exists(iconsPath)) return null;
        
        var possibleExtensions = new[] { ".png", ".jpg", ".jpeg", ".ico", ".svg" };
        
        foreach (var ext in possibleExtensions)
        {
            var iconPath = Path.Combine(iconsPath, iconKey + ext);
            if (File.Exists(iconPath))
            {
                return iconPath;
            }
        }
        
        return null;
    }
    
    public bool IsPrismConfigured()
    {
        return !string.IsNullOrEmpty(_config.PrismLauncherPath) && 
               File.Exists(_config.PrismLauncherPath) &&
               !string.IsNullOrEmpty(_config.PrismDataRoot) &&
               Directory.Exists(_config.PrismDataRoot);
    }
    
    private void Log(string message)
    {
        LogMessage?.Invoke($"[Prism] {message}");
    }
}
