using System.Text.Json.Serialization;
using CommunityToolkit.Mvvm.ComponentModel;

namespace HelixHub.Models;

public partial class ConnectedInstance : ObservableObject
{
    [ObservableProperty]
    private string _instanceId = "";
    
    [ObservableProperty]
    private string _sessionId = "";
    
    [ObservableProperty]
    private string _modVersion = "";
    
    [ObservableProperty]
    private string _minecraftVersion = "";
    
    [ObservableProperty]
    private bool _baritoneAvailable;
    
    [ObservableProperty]
    private string? _baritoneVersion;
    
    [ObservableProperty]
    private List<string> _tools = new();
    
    [ObservableProperty]
    private List<string> _actions = new();
    
    [ObservableProperty]
    private string _status = "Connected";
    
    [ObservableProperty]
    private PlayerState? _lastPlayerState;
    
    [ObservableProperty]
    private BaritoneState? _lastBaritoneState;
    
    [ObservableProperty]
    private DateTime _lastUpdate = DateTime.Now;
    
    public string ConnectionId { get; set; } = "";
    
    public string DisplayName => $"{InstanceId} ({SessionId[..8]}...)";
}

public class PlayerState
{
    [JsonPropertyName("position")]
    public Position? Position { get; set; }
    
    [JsonPropertyName("dimension")]
    public string Dimension { get; set; } = "";
    
    [JsonPropertyName("health")]
    public double Health { get; set; }
    
    [JsonPropertyName("maxHealth")]
    public double MaxHealth { get; set; } = 20;
    
    [JsonPropertyName("hunger")]
    public int Hunger { get; set; }
    
    [JsonPropertyName("saturation")]
    public double Saturation { get; set; }
}

public class Position
{
    [JsonPropertyName("x")]
    public double X { get; set; }
    
    [JsonPropertyName("y")]
    public double Y { get; set; }
    
    [JsonPropertyName("z")]
    public double Z { get; set; }
    
    public override string ToString() => $"({X:F1}, {Y:F1}, {Z:F1})";
}

public class BaritoneState
{
    [JsonPropertyName("available")]
    public bool Available { get; set; }
    
    [JsonPropertyName("active")]
    public bool Active { get; set; }
    
    [JsonPropertyName("currentGoal")]
    public string? CurrentGoal { get; set; }
    
    [JsonPropertyName("currentProcess")]
    public string? CurrentProcess { get; set; }
    
    [JsonPropertyName("progress")]
    public string? Progress { get; set; }
}
