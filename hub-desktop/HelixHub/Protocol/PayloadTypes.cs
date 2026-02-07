using System.Text.Json.Serialization;

namespace HelixHub.Protocol;

public class HelloPayload
{
    [JsonPropertyName("protocolVersion")]
    public string ProtocolVersion { get; set; } = "1.0";
    
    [JsonPropertyName("authToken")]
    public string? AuthToken { get; set; }
    
    [JsonPropertyName("accepted")]
    public bool? Accepted { get; set; }
    
    [JsonPropertyName("hubVersion")]
    public string? HubVersion { get; set; }
    
    [JsonPropertyName("reason")]
    public string? Reason { get; set; }
}

public class RegisterPayload
{
    [JsonPropertyName("modVersion")]
    public string ModVersion { get; set; } = "";
    
    [JsonPropertyName("minecraftVersion")]
    public string MinecraftVersion { get; set; } = "";
    
    [JsonPropertyName("baritoneAvailable")]
    public bool BaritoneAvailable { get; set; }
    
    [JsonPropertyName("baritoneVersion")]
    public string? BaritoneVersion { get; set; }
    
    [JsonPropertyName("capabilities")]
    public CapabilitiesPayload? Capabilities { get; set; }
}

public class CapabilitiesPayload
{
    [JsonPropertyName("tools")]
    public List<string> Tools { get; set; } = new();
    
    [JsonPropertyName("actions")]
    public List<string> Actions { get; set; } = new();
}

public class ToolRequestPayload
{
    [JsonPropertyName("tool")]
    public string Tool { get; set; } = "";
    
    [JsonPropertyName("args")]
    public Dictionary<string, object>? Args { get; set; }
}

public class ToolResponsePayload
{
    [JsonPropertyName("success")]
    public bool Success { get; set; }
    
    [JsonPropertyName("tool")]
    public string Tool { get; set; } = "";
    
    [JsonPropertyName("data")]
    public System.Text.Json.JsonElement? Data { get; set; }
    
    [JsonPropertyName("error")]
    public string? Error { get; set; }
}

public class ActionRequestPayload
{
    [JsonPropertyName("action")]
    public string Action { get; set; } = "";
    
    [JsonPropertyName("args")]
    public Dictionary<string, object>? Args { get; set; }
}

public class ActionResultPayload
{
    [JsonPropertyName("success")]
    public bool Success { get; set; }
    
    [JsonPropertyName("action")]
    public string Action { get; set; } = "";
    
    [JsonPropertyName("result")]
    public System.Text.Json.JsonElement? Result { get; set; }
    
    [JsonPropertyName("error")]
    public string? Error { get; set; }
}

public class StateUpdatePayload
{
    [JsonPropertyName("player")]
    public System.Text.Json.JsonElement? Player { get; set; }
    
    [JsonPropertyName("baritone")]
    public System.Text.Json.JsonElement? Baritone { get; set; }
    
    [JsonPropertyName("inventory")]
    public System.Text.Json.JsonElement? Inventory { get; set; }
}

public class ErrorPayload
{
    [JsonPropertyName("code")]
    public string Code { get; set; } = "";
    
    [JsonPropertyName("message")]
    public string Message { get; set; } = "";
    
    [JsonPropertyName("recoverable")]
    public bool Recoverable { get; set; }
}
