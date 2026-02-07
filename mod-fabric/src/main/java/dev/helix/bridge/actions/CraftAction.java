package dev.helix.bridge.actions;

import com.google.gson.JsonObject;
import dev.helix.bridge.HelixBridgeMod;
import dev.helix.bridge.baritone.BaritoneIntegration;

public class CraftAction {
    
    public static JsonObject execute(JsonObject args) {
        JsonObject result = new JsonObject();
        
        BaritoneIntegration baritone = HelixBridgeMod.getInstance().getBaritoneIntegration();
        
        if (!baritone.isAvailable()) {
            result.addProperty("success", false);
            result.addProperty("error", "Baritone is required for crafting automation. Please install Baritone mod.");
            return result;
        }
        
        if (args == null || !args.has("item")) {
            result.addProperty("success", false);
            result.addProperty("error", "Missing 'item' argument");
            return result;
        }
        
        String item = args.get("item").getAsString();
        int count = args.has("count") ? args.get("count").getAsInt() : 1;
        
        // Normalize item name (remove minecraft: prefix if present for Baritone)
        String baritoneItem = item.replace("minecraft:", "");
        
        // Use Baritone's craft command
        String command = "craft " + baritoneItem + " " + count;
        boolean success = baritone.executeCommand(command);
        
        result.addProperty("success", success);
        if (success) {
            result.addProperty("started", true);
            result.addProperty("message", "Started crafting " + count + "x " + item);
        } else {
            result.addProperty("error", "Failed to start crafting process");
        }
        
        return result;
    }
}
