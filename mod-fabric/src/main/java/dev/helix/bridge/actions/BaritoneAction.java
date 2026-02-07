package dev.helix.bridge.actions;

import com.google.gson.JsonObject;
import dev.helix.bridge.HelixBridgeMod;
import dev.helix.bridge.baritone.BaritoneIntegration;

public class BaritoneAction {
    
    public static JsonObject execute(JsonObject args) {
        JsonObject result = new JsonObject();
        
        BaritoneIntegration baritone = HelixBridgeMod.getInstance().getBaritoneIntegration();
        
        if (!baritone.isAvailable()) {
            result.addProperty("success", false);
            result.addProperty("error", "Baritone is not available. Please install Baritone mod.");
            return result;
        }
        
        if (args == null || !args.has("command")) {
            result.addProperty("success", false);
            result.addProperty("error", "Missing 'command' argument");
            return result;
        }
        
        String command = args.get("command").getAsString();
        if (command == null || command.trim().isEmpty()) {
            result.addProperty("success", false);
            result.addProperty("error", "Command cannot be empty");
            return result;
        }
        
        // Execute the Baritone command
        boolean success = baritone.executeCommand(command);
        
        result.addProperty("success", success);
        if (success) {
            result.addProperty("started", true);
            result.addProperty("message", "Command executed: " + command);
        } else {
            result.addProperty("error", "Failed to execute Baritone command");
        }
        
        return result;
    }
}
