using System.Text.Json;
using HelixHub.Protocol;
using Xunit;

namespace HelixHub.Tests;

public class ProtocolTests
{
    [Fact]
    public void MessageEnvelope_Create_SetsRequiredFields()
    {
        var envelope = MessageEnvelope.Create("Hello");
        
        Assert.Equal("Hello", envelope.Type);
        Assert.NotNull(envelope.RequestId);
        Assert.NotNull(envelope.Timestamp);
        Assert.True(Guid.TryParse(envelope.RequestId, out _));
    }
    
    [Fact]
    public void MessageEnvelope_Create_WithPayload_SerializesCorrectly()
    {
        var payload = new HelloPayload
        {
            ProtocolVersion = "1.0",
            AuthToken = "test-token"
        };
        
        var envelope = MessageEnvelope.Create("Hello", payload);
        var json = envelope.ToJson();
        
        Assert.Contains("\"type\":\"Hello\"", json);
        Assert.Contains("\"protocolVersion\":\"1.0\"", json);
        Assert.Contains("\"authToken\":\"test-token\"", json);
    }
    
    [Fact]
    public void MessageEnvelope_FromJson_ParsesCorrectly()
    {
        var json = @"{
            ""type"": ""Register"",
            ""requestId"": ""550e8400-e29b-41d4-a716-446655440000"",
            ""timestamp"": ""2024-01-01T00:00:00Z"",
            ""instanceId"": ""test-instance"",
            ""sessionId"": ""660e8400-e29b-41d4-a716-446655440000"",
            ""payload"": {
                ""modVersion"": ""1.0.0"",
                ""minecraftVersion"": ""1.21.1"",
                ""baritoneAvailable"": true
            }
        }";
        
        var envelope = MessageEnvelope.FromJson(json);
        
        Assert.NotNull(envelope);
        Assert.Equal("Register", envelope.Type);
        Assert.Equal("test-instance", envelope.InstanceId);
        Assert.NotNull(envelope.Payload);
    }
    
    [Fact]
    public void MessageEnvelope_GetPayload_DeserializesCorrectly()
    {
        var json = @"{
            ""type"": ""Hello"",
            ""payload"": {
                ""protocolVersion"": ""1.0"",
                ""accepted"": true,
                ""hubVersion"": ""1.0.0""
            }
        }";
        
        var envelope = MessageEnvelope.FromJson(json);
        var payload = envelope?.GetPayload<HelloPayload>();
        
        Assert.NotNull(payload);
        Assert.Equal("1.0", payload.ProtocolVersion);
        Assert.True(payload.Accepted);
        Assert.Equal("1.0.0", payload.HubVersion);
    }
    
    [Fact]
    public void MessageEnvelope_FromJson_InvalidJson_ReturnsNull()
    {
        var json = "not valid json";
        var envelope = MessageEnvelope.FromJson(json);
        Assert.Null(envelope);
    }
    
    [Fact]
    public void MessageEnvelope_CreateResponse_SetsAllFields()
    {
        var envelope = MessageEnvelope.CreateResponse(
            "ToolResponse",
            "request-123",
            "instance-1",
            "session-1",
            new ToolResponsePayload { Success = true, Tool = "get_player_state" }
        );
        
        Assert.Equal("ToolResponse", envelope.Type);
        Assert.Equal("request-123", envelope.RequestId);
        Assert.Equal("instance-1", envelope.InstanceId);
        Assert.Equal("session-1", envelope.SessionId);
    }
    
    [Theory]
    [InlineData(MessageTypes.Hello)]
    [InlineData(MessageTypes.Register)]
    [InlineData(MessageTypes.ToolRequest)]
    [InlineData(MessageTypes.ToolResponse)]
    [InlineData(MessageTypes.ActionRequest)]
    [InlineData(MessageTypes.ActionResult)]
    [InlineData(MessageTypes.StateUpdate)]
    [InlineData(MessageTypes.Error)]
    public void MessageTypes_AllConstantsAreDefined(string messageType)
    {
        Assert.NotNull(messageType);
        Assert.NotEmpty(messageType);
    }
    
    [Fact]
    public void RegisterPayload_Capabilities_SerializesCorrectly()
    {
        var payload = new RegisterPayload
        {
            ModVersion = "1.0.0",
            MinecraftVersion = "1.21.1",
            BaritoneAvailable = true,
            BaritoneVersion = "1.10.2",
            Capabilities = new CapabilitiesPayload
            {
                Tools = new List<string> { "get_player_state", "get_baritone_status" },
                Actions = new List<string> { "baritone_command", "stop_all" }
            }
        };
        
        var json = JsonSerializer.Serialize(payload);
        
        Assert.Contains("get_player_state", json);
        Assert.Contains("baritone_command", json);
    }
    
    [Fact]
    public void ActionResultPayload_Error_DeserializesCorrectly()
    {
        var json = @"{
            ""success"": false,
            ""action"": ""baritone_command"",
            ""error"": ""Baritone not available""
        }";
        
        var payload = JsonSerializer.Deserialize<ActionResultPayload>(json, new JsonSerializerOptions
        {
            PropertyNameCaseInsensitive = true
        });
        
        Assert.NotNull(payload);
        Assert.False(payload.Success);
        Assert.Equal("Baritone not available", payload.Error);
    }
}
