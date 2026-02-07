package dev.helix.bridge.tools;

import com.google.gson.JsonObject;
import dev.helix.bridge.HelixBridgeMod;

public class BaritoneTool {
    
    public static JsonObject getStatus(JsonObject args) {
        return HelixBridgeMod.getInstance().getBaritoneIntegration().getStatus();
    }
}
