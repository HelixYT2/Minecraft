package dev.helix.bridge.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.helix.bridge.HelixBridgeMod;

import java.util.List;

public class Payloads {
    
    public static JsonObject helloRequest(String authToken) {
        JsonObject payload = new JsonObject();
        payload.addProperty("protocolVersion", "1.0");
        payload.addProperty("authToken", authToken);
        return payload;
    }
    
    public static JsonObject registerPayload(String modVersion, String mcVersion, 
            boolean baritoneAvailable, String baritoneVersion,
            List<String> tools, List<String> actions) {
        JsonObject payload = new JsonObject();
        payload.addProperty("modVersion", modVersion);
        payload.addProperty("minecraftVersion", mcVersion);
        payload.addProperty("baritoneAvailable", baritoneAvailable);
        if (baritoneVersion != null) {
            payload.addProperty("baritoneVersion", baritoneVersion);
        }
        
        JsonObject capabilities = new JsonObject();
        JsonArray toolsArray = new JsonArray();
        tools.forEach(toolsArray::add);
        capabilities.add("tools", toolsArray);
        
        JsonArray actionsArray = new JsonArray();
        actions.forEach(actionsArray::add);
        capabilities.add("actions", actionsArray);
        
        payload.add("capabilities", capabilities);
        return payload;
    }
    
    public static JsonObject toolResponse(boolean success, String tool, JsonObject data) {
        JsonObject payload = new JsonObject();
        payload.addProperty("success", success);
        payload.addProperty("tool", tool);
        if (data != null) {
            payload.add("data", data);
        }
        return payload;
    }
    
    public static JsonObject toolResponseError(String tool, String error) {
        JsonObject payload = new JsonObject();
        payload.addProperty("success", false);
        payload.addProperty("tool", tool);
        payload.addProperty("error", error);
        return payload;
    }
    
    public static JsonObject actionResult(boolean success, String action, JsonObject result) {
        JsonObject payload = new JsonObject();
        payload.addProperty("success", success);
        payload.addProperty("action", action);
        if (result != null) {
            payload.add("result", result);
        }
        return payload;
    }
    
    public static JsonObject actionResultError(String action, String error) {
        JsonObject payload = new JsonObject();
        payload.addProperty("success", false);
        payload.addProperty("action", action);
        payload.addProperty("error", error);
        return payload;
    }
    
    public static JsonObject stateUpdate(JsonObject player, JsonObject baritone, JsonObject inventory) {
        JsonObject payload = new JsonObject();
        if (player != null) payload.add("player", player);
        if (baritone != null) payload.add("baritone", baritone);
        if (inventory != null) payload.add("inventory", inventory);
        return payload;
    }
    
    public static JsonObject error(String code, String message, boolean recoverable) {
        JsonObject payload = new JsonObject();
        payload.addProperty("code", code);
        payload.addProperty("message", message);
        payload.addProperty("recoverable", recoverable);
        return payload;
    }
}
