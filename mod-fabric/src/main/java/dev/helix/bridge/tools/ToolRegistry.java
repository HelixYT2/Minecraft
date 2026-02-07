package dev.helix.bridge.tools;

import com.google.gson.JsonObject;
import dev.helix.bridge.HelixBridgeMod;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ToolRegistry {
    private static final Map<String, Function<JsonObject, JsonObject>> tools = new HashMap<>();
    
    static {
        tools.put("get_player_state", PlayerStateTool::execute);
        tools.put("get_baritone_status", BaritoneTool::getStatus);
    }
    
    public static List<String> getAvailableTools() {
        return Arrays.asList("get_player_state", "get_baritone_status");
    }
    
    public static boolean hasTool(String name) {
        return tools.containsKey(name);
    }
    
    public static JsonObject executeTool(String name, JsonObject args) {
        Function<JsonObject, JsonObject> tool = tools.get(name);
        if (tool == null) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Unknown tool: " + name);
            return error;
        }
        
        try {
            return tool.apply(args);
        } catch (Exception e) {
            HelixBridgeMod.LOGGER.error("Error executing tool: {}", name, e);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Tool execution failed: " + e.getMessage());
            return error;
        }
    }
}
