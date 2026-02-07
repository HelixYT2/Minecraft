package dev.helix.bridge.actions;

import com.google.gson.JsonObject;
import dev.helix.bridge.HelixBridgeMod;
import dev.helix.bridge.baritone.BaritoneIntegration;

public class StopAllAction {
    
    public static JsonObject execute(JsonObject args) {
        JsonObject result = new JsonObject();
        
        BaritoneIntegration baritone = HelixBridgeMod.getInstance().getBaritoneIntegration();
        
        if (baritone.isAvailable()) {
            baritone.stop();
        }
        
        result.addProperty("success", true);
        result.addProperty("stopped", true);
        result.addProperty("message", "All tasks stopped");
        
        return result;
    }
}
