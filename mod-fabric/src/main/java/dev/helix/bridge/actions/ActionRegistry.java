package dev.helix.bridge.actions;

import com.google.gson.JsonObject;
import dev.helix.bridge.HelixBridgeMod;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ActionRegistry {
    private static final Map<String, Function<JsonObject, JsonObject>> actions = new HashMap<>();
    
    static {
        actions.put("baritone_command", BaritoneAction::execute);
        actions.put("craft_item", CraftAction::execute);
        actions.put("equip_armor", EquipArmorAction::execute);
        actions.put("stop_all", StopAllAction::execute);
    }
    
    public static List<String> getAvailableActions() {
        return Arrays.asList("baritone_command", "craft_item", "equip_armor", "stop_all");
    }
    
    public static boolean hasAction(String name) {
        return actions.containsKey(name);
    }
    
    public static JsonObject executeAction(String name, JsonObject args) {
        Function<JsonObject, JsonObject> action = actions.get(name);
        if (action == null) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Unknown action: " + name);
            error.addProperty("success", false);
            return error;
        }
        
        try {
            return action.apply(args);
        } catch (Exception e) {
            HelixBridgeMod.LOGGER.error("Error executing action: {}", name, e);
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", "Action execution failed: " + e.getMessage());
            return error;
        }
    }
}
