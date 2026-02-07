package dev.helix.bridge.network;

import com.google.gson.JsonObject;
import dev.helix.bridge.HelixBridgeMod;
import dev.helix.bridge.actions.ActionRegistry;
import dev.helix.bridge.protocol.Message;
import dev.helix.bridge.protocol.MessageType;
import dev.helix.bridge.protocol.Payloads;
import dev.helix.bridge.tools.ToolRegistry;
import net.minecraft.client.MinecraftClient;

public class MessageHandler {
    private final HubConnection connection;
    
    public MessageHandler(HubConnection connection) {
        this.connection = connection;
    }
    
    public void handleMessage(Message message) {
        if (message == null || message.getType() == null) {
            HelixBridgeMod.LOGGER.warn("Received null or invalid message");
            return;
        }
        
        switch (message.getType()) {
            case Hello -> handleHello(message);
            case ToolRequest -> handleToolRequest(message);
            case ActionRequest -> handleActionRequest(message);
            default -> HelixBridgeMod.LOGGER.warn("Unhandled message type: {}", message.getType());
        }
    }
    
    private void handleHello(Message message) {
        boolean accepted = message.getPayloadBoolean("accepted", false);
        
        if (accepted) {
            HelixBridgeMod.LOGGER.info("Hub accepted connection!");
            connection.setAuthenticated(true);
            connection.sendRegister();
        } else {
            String reason = message.getPayloadString("reason");
            HelixBridgeMod.LOGGER.error("Hub rejected connection: {}", reason);
            connection.disconnect();
        }
    }
    
    private void handleToolRequest(Message message) {
        String requestId = message.getRequestId();
        JsonObject payload = message.getPayload();
        
        if (payload == null || !payload.has("tool")) {
            sendToolError(requestId, "unknown", "Missing tool in request");
            return;
        }
        
        String tool = payload.get("tool").getAsString();
        JsonObject args = payload.has("args") ? payload.getAsJsonObject("args") : new JsonObject();
        
        // Execute on main thread to access Minecraft state
        MinecraftClient.getInstance().execute(() -> {
            if (!ToolRegistry.hasTool(tool)) {
                sendToolError(requestId, tool, "Unknown tool: " + tool);
                return;
            }
            
            try {
                JsonObject result = ToolRegistry.executeTool(tool, args);
                
                if (result.has("error")) {
                    sendToolError(requestId, tool, result.get("error").getAsString());
                } else {
                    sendToolResponse(requestId, tool, result);
                }
            } catch (Exception e) {
                HelixBridgeMod.LOGGER.error("Error executing tool: {}", tool, e);
                sendToolError(requestId, tool, "Execution error: " + e.getMessage());
            }
        });
    }
    
    private void handleActionRequest(Message message) {
        String requestId = message.getRequestId();
        JsonObject payload = message.getPayload();
        
        if (payload == null || !payload.has("action")) {
            sendActionError(requestId, "unknown", "Missing action in request");
            return;
        }
        
        String action = payload.get("action").getAsString();
        JsonObject args = payload.has("args") ? payload.getAsJsonObject("args") : new JsonObject();
        
        // Check if Baritone is required but not available
        if (requiresBaritone(action) && !HelixBridgeMod.getInstance().getBaritoneIntegration().isAvailable()) {
            sendActionError(requestId, action, "Baritone is not available. Please install Baritone mod.");
            return;
        }
        
        // Execute on main thread
        MinecraftClient.getInstance().execute(() -> {
            if (!ActionRegistry.hasAction(action)) {
                sendActionError(requestId, action, "Unknown action: " + action);
                return;
            }
            
            try {
                JsonObject result = ActionRegistry.executeAction(action, args);
                
                boolean success = result.has("success") && result.get("success").getAsBoolean();
                if (success) {
                    sendActionResult(requestId, action, result);
                } else {
                    String error = result.has("error") ? result.get("error").getAsString() : "Action failed";
                    sendActionError(requestId, action, error);
                }
            } catch (Exception e) {
                HelixBridgeMod.LOGGER.error("Error executing action: {}", action, e);
                sendActionError(requestId, action, "Execution error: " + e.getMessage());
            }
        });
    }
    
    private boolean requiresBaritone(String action) {
        return action.equals("baritone_command") || action.equals("craft_item");
    }
    
    private void sendToolResponse(String requestId, String tool, JsonObject data) {
        Message response = new Message(MessageType.ToolResponse)
                .withRequestId(requestId)
                .withInstanceId(connection.getInstanceId())
                .withSessionId(connection.getSessionId())
                .withPayload(Payloads.toolResponse(true, tool, data));
        
        connection.send(response);
    }
    
    private void sendToolError(String requestId, String tool, String error) {
        Message response = new Message(MessageType.ToolResponse)
                .withRequestId(requestId)
                .withInstanceId(connection.getInstanceId())
                .withSessionId(connection.getSessionId())
                .withPayload(Payloads.toolResponseError(tool, error));
        
        connection.send(response);
    }
    
    private void sendActionResult(String requestId, String action, JsonObject result) {
        Message response = new Message(MessageType.ActionResult)
                .withRequestId(requestId)
                .withInstanceId(connection.getInstanceId())
                .withSessionId(connection.getSessionId())
                .withPayload(Payloads.actionResult(true, action, result));
        
        connection.send(response);
    }
    
    private void sendActionError(String requestId, String action, String error) {
        Message response = new Message(MessageType.ActionResult)
                .withRequestId(requestId)
                .withInstanceId(connection.getInstanceId())
                .withSessionId(connection.getSessionId())
                .withPayload(Payloads.actionResultError(action, error));
        
        connection.send(response);
    }
}
