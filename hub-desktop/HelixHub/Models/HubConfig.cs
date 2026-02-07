using System.Text.Json.Serialization;

namespace HelixHub.Models;

public class HubConfig
{
    [JsonPropertyName("webSocketPort")]
    public int WebSocketPort { get; set; } = 9742;
    
    [JsonPropertyName("authToken")]
    public string AuthToken { get; set; } = "";
    
    [JsonPropertyName("lmStudioBaseUrl")]
    public string LmStudioBaseUrl { get; set; } = "http://localhost:1234/v1";
    
    [JsonPropertyName("lmStudioModel")]
    public string LmStudioModel { get; set; } = "default";
    
    [JsonPropertyName("prismLauncherPath")]
    public string PrismLauncherPath { get; set; } = "";
    
    [JsonPropertyName("prismDataRoot")]
    public string PrismDataRoot { get; set; } = "";
    
    [JsonPropertyName("requestTimeoutMs")]
    public int RequestTimeoutMs { get; set; } = 30000;
    
    [JsonPropertyName("maxPlanRepairAttempts")]
    public int MaxPlanRepairAttempts { get; set; } = 3;
    
    public static string GenerateToken()
    {
        const string chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        var random = new Random();
        return new string(Enumerable.Range(0, 32).Select(_ => chars[random.Next(chars.Length)]).ToArray());
    }
}
