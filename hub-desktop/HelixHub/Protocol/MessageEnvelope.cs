using System.Text.Json;
using System.Text.Json.Serialization;

namespace HelixHub.Protocol;

public class MessageEnvelope
{
    [JsonPropertyName("type")]
    public string Type { get; set; } = "";
    
    [JsonPropertyName("requestId")]
    public string? RequestId { get; set; }
    
    [JsonPropertyName("timestamp")]
    public string Timestamp { get; set; } = DateTime.UtcNow.ToString("o");
    
    [JsonPropertyName("instanceId")]
    public string? InstanceId { get; set; }
    
    [JsonPropertyName("sessionId")]
    public string? SessionId { get; set; }
    
    [JsonPropertyName("payload")]
    public JsonElement? Payload { get; set; }
    
    public static MessageEnvelope Create(string type, object? payload = null)
    {
        var envelope = new MessageEnvelope
        {
            Type = type,
            RequestId = Guid.NewGuid().ToString(),
            Timestamp = DateTime.UtcNow.ToString("o")
        };
        
        if (payload != null)
        {
            var json = JsonSerializer.Serialize(payload);
            envelope.Payload = JsonSerializer.Deserialize<JsonElement>(json);
        }
        
        return envelope;
    }
    
    public static MessageEnvelope CreateResponse(string type, string requestId, string instanceId, string sessionId, object? payload = null)
    {
        var envelope = Create(type, payload);
        envelope.RequestId = requestId;
        envelope.InstanceId = instanceId;
        envelope.SessionId = sessionId;
        return envelope;
    }
    
    public string ToJson() => JsonSerializer.Serialize(this, new JsonSerializerOptions 
    { 
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull
    });
    
    public static MessageEnvelope? FromJson(string json)
    {
        try
        {
            return JsonSerializer.Deserialize<MessageEnvelope>(json, new JsonSerializerOptions
            {
                PropertyNameCaseInsensitive = true
            });
        }
        catch
        {
            return null;
        }
    }
    
    public T? GetPayload<T>() where T : class
    {
        if (Payload == null) return null;
        try
        {
            return JsonSerializer.Deserialize<T>(Payload.Value.GetRawText(), new JsonSerializerOptions
            {
                PropertyNameCaseInsensitive = true
            });
        }
        catch
        {
            return null;
        }
    }
}

public static class MessageTypes
{
    public const string Hello = "Hello";
    public const string Register = "Register";
    public const string ToolRequest = "ToolRequest";
    public const string ToolResponse = "ToolResponse";
    public const string ActionRequest = "ActionRequest";
    public const string ActionResult = "ActionResult";
    public const string StateUpdate = "StateUpdate";
    public const string Error = "Error";
}
