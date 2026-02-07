using System.Collections.Concurrent;
using System.Net;
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using HelixHub.Models;
using HelixHub.Protocol;

namespace HelixHub.Services;

public class WebSocketServer
{
    private readonly HubConfig _config;
    private HttpListener? _listener;
    private CancellationTokenSource? _cts;
    private readonly ConcurrentDictionary<string, WebSocketConnection> _connections = new();
    private readonly ConcurrentDictionary<string, ConnectedInstance> _instances = new();
    private readonly ConcurrentDictionary<string, TaskCompletionSource<MessageEnvelope>> _pendingRequests = new();
    
    public event Action<ConnectedInstance>? InstanceConnected;
    public event Action<string>? InstanceDisconnected;
    public event Action<string, PlayerState>? PlayerStateUpdated;
    public event Action<string, BaritoneState>? BaritoneStateUpdated;
    public event Action<string>? LogMessage;
    
    public IReadOnlyDictionary<string, ConnectedInstance> Instances => _instances;
    
    public WebSocketServer(HubConfig config)
    {
        _config = config;
    }
    
    public void Start()
    {
        _cts = new CancellationTokenSource();
        _listener = new HttpListener();
        _listener.Prefixes.Add($"http://127.0.0.1:{_config.WebSocketPort}/");
        
        try
        {
            _listener.Start();
            Log($"WebSocket server started on ws://127.0.0.1:{_config.WebSocketPort}");
            
            Task.Run(() => AcceptConnections(_cts.Token));
        }
        catch (Exception ex)
        {
            Log($"Failed to start server: {ex.Message}");
        }
    }
    
    public void Stop()
    {
        _cts?.Cancel();
        
        foreach (var conn in _connections.Values)
        {
            try
            {
                conn.WebSocket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Server shutdown", CancellationToken.None).Wait(1000);
            }
            catch { }
        }
        
        _connections.Clear();
        _instances.Clear();
        
        try
        {
            _listener?.Stop();
        }
        catch { }
        
        Log("WebSocket server stopped");
    }
    
    private async Task AcceptConnections(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested && _listener != null)
        {
            try
            {
                var context = await _listener.GetContextAsync();
                
                if (context.Request.IsWebSocketRequest)
                {
                    var wsContext = await context.AcceptWebSocketAsync(null);
                    var connectionId = Guid.NewGuid().ToString();
                    
                    var connection = new WebSocketConnection
                    {
                        ConnectionId = connectionId,
                        WebSocket = wsContext.WebSocket,
                        Authenticated = false
                    };
                    
                    _connections[connectionId] = connection;
                    Log($"New connection: {connectionId}");
                    
                    _ = Task.Run(() => HandleConnection(connection, ct));
                }
                else
                {
                    context.Response.StatusCode = 400;
                    context.Response.Close();
                }
            }
            catch (OperationCanceledException)
            {
                break;
            }
            catch (Exception ex)
            {
                if (!ct.IsCancellationRequested)
                {
                    Log($"Error accepting connection: {ex.Message}");
                }
            }
        }
    }
    
    private async Task HandleConnection(WebSocketConnection connection, CancellationToken ct)
    {
        var buffer = new byte[8192];
        var messageBuilder = new StringBuilder();
        
        try
        {
            while (connection.WebSocket.State == WebSocketState.Open && !ct.IsCancellationRequested)
            {
                var result = await connection.WebSocket.ReceiveAsync(new ArraySegment<byte>(buffer), ct);
                
                if (result.MessageType == WebSocketMessageType.Close)
                {
                    break;
                }
                
                if (result.MessageType == WebSocketMessageType.Text)
                {
                    messageBuilder.Append(Encoding.UTF8.GetString(buffer, 0, result.Count));
                    
                    if (result.EndOfMessage)
                    {
                        var message = messageBuilder.ToString();
                        messageBuilder.Clear();
                        
                        await ProcessMessage(connection, message);
                    }
                }
            }
        }
        catch (OperationCanceledException)
        {
        }
        catch (WebSocketException)
        {
        }
        catch (Exception ex)
        {
            Log($"Connection error: {ex.Message}");
        }
        finally
        {
            CleanupConnection(connection);
        }
    }
    
    private async Task ProcessMessage(WebSocketConnection connection, string messageJson)
    {
        var envelope = MessageEnvelope.FromJson(messageJson);
        if (envelope == null)
        {
            Log("Received invalid message");
            return;
        }
        
        switch (envelope.Type)
        {
            case MessageTypes.Hello:
                await HandleHello(connection, envelope);
                break;
                
            case MessageTypes.Register:
                await HandleRegister(connection, envelope);
                break;
                
            case MessageTypes.ToolResponse:
            case MessageTypes.ActionResult:
                HandleResponse(envelope);
                break;
                
            case MessageTypes.StateUpdate:
                HandleStateUpdate(connection, envelope);
                break;
                
            case MessageTypes.Error:
                HandleError(connection, envelope);
                break;
                
            default:
                Log($"Unknown message type: {envelope.Type}");
                break;
        }
    }
    
    private async Task HandleHello(WebSocketConnection connection, MessageEnvelope envelope)
    {
        var payload = envelope.GetPayload<HelloPayload>();
        
        if (payload?.AuthToken != _config.AuthToken)
        {
            Log($"Connection {connection.ConnectionId} failed authentication");
            
            var response = MessageEnvelope.Create(MessageTypes.Hello, new HelloPayload
            {
                ProtocolVersion = "1.0",
                Accepted = false,
                Reason = "Invalid auth token"
            });
            
            await SendMessage(connection, response);
            await connection.WebSocket.CloseAsync(WebSocketCloseStatus.PolicyViolation, "Auth failed", CancellationToken.None);
            return;
        }
        
        connection.Authenticated = true;
        Log($"Connection {connection.ConnectionId} authenticated");
        
        var successResponse = MessageEnvelope.Create(MessageTypes.Hello, new HelloPayload
        {
            ProtocolVersion = "1.0",
            Accepted = true,
            HubVersion = "1.0.0"
        });
        
        await SendMessage(connection, successResponse);
    }
    
    private async Task HandleRegister(WebSocketConnection connection, MessageEnvelope envelope)
    {
        if (!connection.Authenticated)
        {
            Log($"Unauthenticated registration attempt from {connection.ConnectionId}");
            return;
        }
        
        var payload = envelope.GetPayload<RegisterPayload>();
        if (payload == null || string.IsNullOrEmpty(envelope.InstanceId) || string.IsNullOrEmpty(envelope.SessionId))
        {
            Log("Invalid registration payload");
            return;
        }
        
        var instance = new ConnectedInstance
        {
            InstanceId = envelope.InstanceId,
            SessionId = envelope.SessionId,
            ConnectionId = connection.ConnectionId,
            ModVersion = payload.ModVersion,
            MinecraftVersion = payload.MinecraftVersion,
            BaritoneAvailable = payload.BaritoneAvailable,
            BaritoneVersion = payload.BaritoneVersion,
            Tools = payload.Capabilities?.Tools ?? new List<string>(),
            Actions = payload.Capabilities?.Actions ?? new List<string>(),
            Status = "Connected"
        };
        
        connection.InstanceId = envelope.InstanceId;
        connection.SessionId = envelope.SessionId;
        
        _instances[envelope.SessionId] = instance;
        
        Log($"Instance registered: {envelope.InstanceId} (session: {envelope.SessionId})");
        Log($"  Baritone: {(payload.BaritoneAvailable ? $"v{payload.BaritoneVersion}" : "Not available")}");
        Log($"  Tools: {string.Join(", ", instance.Tools)}");
        Log($"  Actions: {string.Join(", ", instance.Actions)}");
        
        InstanceConnected?.Invoke(instance);
    }
    
    private void HandleResponse(MessageEnvelope envelope)
    {
        if (envelope.RequestId != null && _pendingRequests.TryRemove(envelope.RequestId, out var tcs))
        {
            tcs.TrySetResult(envelope);
        }
    }
    
    private void HandleStateUpdate(WebSocketConnection connection, MessageEnvelope envelope)
    {
        if (envelope.SessionId == null || !_instances.TryGetValue(envelope.SessionId, out var instance))
        {
            return;
        }
        
        var payload = envelope.GetPayload<StateUpdatePayload>();
        if (payload == null) return;
        
        instance.LastUpdate = DateTime.Now;
        
        if (payload.Player.HasValue)
        {
            try
            {
                var playerState = JsonSerializer.Deserialize<PlayerState>(payload.Player.Value.GetRawText());
                if (playerState != null)
                {
                    instance.LastPlayerState = playerState;
                    PlayerStateUpdated?.Invoke(envelope.SessionId, playerState);
                }
            }
            catch { }
        }
        
        if (payload.Baritone.HasValue)
        {
            try
            {
                var baritoneState = JsonSerializer.Deserialize<BaritoneState>(payload.Baritone.Value.GetRawText());
                if (baritoneState != null)
                {
                    instance.LastBaritoneState = baritoneState;
                    BaritoneStateUpdated?.Invoke(envelope.SessionId, baritoneState);
                }
            }
            catch { }
        }
    }
    
    private void HandleError(WebSocketConnection connection, MessageEnvelope envelope)
    {
        var payload = envelope.GetPayload<ErrorPayload>();
        if (payload != null)
        {
            Log($"Error from {connection.InstanceId}: [{payload.Code}] {payload.Message}");
        }
    }
    
    public async Task<MessageEnvelope?> SendToolRequest(string sessionId, string tool, Dictionary<string, object>? args = null, int timeoutMs = 30000)
    {
        if (!_instances.TryGetValue(sessionId, out var instance))
        {
            return null;
        }
        
        var connection = _connections.Values.FirstOrDefault(c => c.SessionId == sessionId);
        if (connection == null)
        {
            return null;
        }
        
        var request = MessageEnvelope.Create(MessageTypes.ToolRequest, new ToolRequestPayload
        {
            Tool = tool,
            Args = args
        });
        request.InstanceId = instance.InstanceId;
        request.SessionId = sessionId;
        
        return await SendRequestAndWait(connection, request, timeoutMs);
    }
    
    public async Task<MessageEnvelope?> SendActionRequest(string sessionId, string action, Dictionary<string, object>? args = null, int timeoutMs = 30000)
    {
        if (!_instances.TryGetValue(sessionId, out var instance))
        {
            return null;
        }
        
        var connection = _connections.Values.FirstOrDefault(c => c.SessionId == sessionId);
        if (connection == null)
        {
            return null;
        }
        
        var request = MessageEnvelope.Create(MessageTypes.ActionRequest, new ActionRequestPayload
        {
            Action = action,
            Args = args
        });
        request.InstanceId = instance.InstanceId;
        request.SessionId = sessionId;
        
        return await SendRequestAndWait(connection, request, timeoutMs);
    }
    
    private async Task<MessageEnvelope?> SendRequestAndWait(WebSocketConnection connection, MessageEnvelope request, int timeoutMs)
    {
        var tcs = new TaskCompletionSource<MessageEnvelope>();
        _pendingRequests[request.RequestId!] = tcs;
        
        try
        {
            await SendMessage(connection, request);
            
            using var cts = new CancellationTokenSource(timeoutMs);
            cts.Token.Register(() => tcs.TrySetCanceled());
            
            return await tcs.Task;
        }
        catch (OperationCanceledException)
        {
            Log($"Request {request.RequestId} timed out");
            return null;
        }
        finally
        {
            _pendingRequests.TryRemove(request.RequestId!, out _);
        }
    }
    
    private async Task SendMessage(WebSocketConnection connection, MessageEnvelope message)
    {
        if (connection.WebSocket.State != WebSocketState.Open)
        {
            return;
        }
        
        var json = message.ToJson();
        var bytes = Encoding.UTF8.GetBytes(json);
        
        await connection.WebSocket.SendAsync(
            new ArraySegment<byte>(bytes),
            WebSocketMessageType.Text,
            true,
            CancellationToken.None
        );
    }
    
    private void CleanupConnection(WebSocketConnection connection)
    {
        _connections.TryRemove(connection.ConnectionId, out _);
        
        if (connection.SessionId != null)
        {
            _instances.TryRemove(connection.SessionId, out _);
            InstanceDisconnected?.Invoke(connection.SessionId);
            Log($"Instance disconnected: {connection.InstanceId}");
        }
    }
    
    private void Log(string message)
    {
        LogMessage?.Invoke($"[{DateTime.Now:HH:mm:ss}] {message}");
    }
    
    private class WebSocketConnection
    {
        public string ConnectionId { get; set; } = "";
        public WebSocket WebSocket { get; set; } = null!;
        public bool Authenticated { get; set; }
        public string? InstanceId { get; set; }
        public string? SessionId { get; set; }
    }
}
