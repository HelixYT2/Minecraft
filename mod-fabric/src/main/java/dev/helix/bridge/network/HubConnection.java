package dev.helix.bridge.network;

import com.google.gson.JsonObject;
import dev.helix.bridge.HelixBridgeMod;
import dev.helix.bridge.actions.ActionRegistry;
import dev.helix.bridge.baritone.BaritoneIntegration;
import dev.helix.bridge.config.HelixConfig;
import dev.helix.bridge.protocol.Message;
import dev.helix.bridge.protocol.MessageType;
import dev.helix.bridge.protocol.Payloads;
import dev.helix.bridge.tools.PlayerStateTool;
import dev.helix.bridge.tools.ToolRegistry;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HubConnection {
    private final HelixConfig config;
    private final BaritoneIntegration baritone;
    private final MessageHandler messageHandler;
    private final String sessionId;
    private final ScheduledExecutorService scheduler;
    
    private WebSocketClient client;
    private boolean connected = false;
    private boolean authenticated = false;
    private int reconnectAttempts = 0;
    private boolean shouldReconnect = true;
    
    public HubConnection(HelixConfig config, BaritoneIntegration baritone) {
        this.config = config;
        this.baritone = baritone;
        this.messageHandler = new MessageHandler(this);
        this.sessionId = UUID.randomUUID().toString();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }
    
    public void connect() {
        if (!config.isConfigured()) {
            HelixBridgeMod.LOGGER.warn("Hub connection not configured. Please set authToken and instanceId in config.");
            return;
        }
        
        if (connected) {
            HelixBridgeMod.LOGGER.info("Already connected to Hub");
            return;
        }
        
        shouldReconnect = true;
        doConnect();
    }
    
    private void doConnect() {
        try {
            URI uri = new URI(config.getHubUrl());
            
            client = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    HelixBridgeMod.LOGGER.info("Connected to Hub at {}", config.getHubUrl());
                    connected = true;
                    reconnectAttempts = 0;
                    sendHello();
                }
                
                @Override
                public void onMessage(String messageStr) {
                    try {
                        Message message = Message.fromJson(messageStr);
                        messageHandler.handleMessage(message);
                    } catch (Exception e) {
                        HelixBridgeMod.LOGGER.error("Error parsing message: {}", messageStr, e);
                    }
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    HelixBridgeMod.LOGGER.info("Disconnected from Hub: {} (code: {})", reason, code);
                    connected = false;
                    authenticated = false;
                    
                    if (shouldReconnect) {
                        scheduleReconnect();
                    }
                }
                
                @Override
                public void onError(Exception ex) {
                    HelixBridgeMod.LOGGER.error("WebSocket error", ex);
                }
            };
            
            client.setConnectionLostTimeout(config.getConnectionTimeoutMs() / 1000);
            client.connectBlocking(config.getConnectionTimeoutMs(), TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            HelixBridgeMod.LOGGER.error("Failed to connect to Hub", e);
            if (shouldReconnect) {
                scheduleReconnect();
            }
        }
    }
    
    private void scheduleReconnect() {
        reconnectAttempts++;
        int delay = Math.min(
            config.getReconnectDelayMs() * reconnectAttempts,
            config.getMaxReconnectDelayMs()
        );
        
        HelixBridgeMod.LOGGER.info("Scheduling reconnect attempt {} in {}ms", reconnectAttempts, delay);
        
        scheduler.schedule(this::doConnect, delay, TimeUnit.MILLISECONDS);
    }
    
    public void disconnect() {
        shouldReconnect = false;
        
        if (client != null && connected) {
            try {
                client.closeBlocking();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        connected = false;
        authenticated = false;
    }
    
    public void send(Message message) {
        if (client != null && connected) {
            try {
                client.send(message.toJson());
            } catch (Exception e) {
                HelixBridgeMod.LOGGER.error("Error sending message", e);
            }
        }
    }
    
    private void sendHello() {
        Message hello = new Message(MessageType.Hello)
                .withPayload(Payloads.helloRequest(config.getAuthToken()));
        
        send(hello);
    }
    
    public void sendRegister() {
        Message register = new Message(MessageType.Register)
                .withInstanceId(config.getInstanceId())
                .withSessionId(sessionId)
                .withPayload(Payloads.registerPayload(
                        HelixBridgeMod.MOD_VERSION,
                        "1.21.1",
                        baritone.isAvailable(),
                        baritone.getVersion(),
                        ToolRegistry.getAvailableTools(),
                        ActionRegistry.getAvailableActions()
                ));
        
        send(register);
        HelixBridgeMod.LOGGER.info("Registered with Hub as instance: {}, session: {}", 
                config.getInstanceId(), sessionId);
    }
    
    public void sendStateUpdate() {
        if (!connected || !authenticated) return;
        
        JsonObject player = PlayerStateTool.getPlayerStateForUpdate();
        JsonObject baritoneStatus = baritone.getStatus();
        
        Message update = new Message(MessageType.StateUpdate)
                .withInstanceId(config.getInstanceId())
                .withSessionId(sessionId)
                .withPayload(Payloads.stateUpdate(player, baritoneStatus, null));
        
        send(update);
    }
    
    public void sendError(String code, String message, boolean recoverable) {
        Message error = new Message(MessageType.Error)
                .withInstanceId(config.getInstanceId())
                .withSessionId(sessionId)
                .withPayload(Payloads.error(code, message, recoverable));
        
        send(error);
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
    
    public String getInstanceId() {
        return config.getInstanceId();
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public String getStatus() {
        if (!config.isConfigured()) {
            return "NOT CONFIGURED";
        } else if (!connected) {
            return "DISCONNECTED";
        } else if (!authenticated) {
            return "AUTHENTICATING";
        } else {
            return "CONNECTED";
        }
    }
}
