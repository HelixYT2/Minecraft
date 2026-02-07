package dev.helix.bridge.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.helix.bridge.HelixBridgeMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class PlayerStateTool {
    
    public static JsonObject execute(JsonObject args) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        
        if (player == null) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Player not in game");
            return error;
        }
        
        JsonObject result = new JsonObject();
        
        // Position
        JsonObject position = new JsonObject();
        position.addProperty("x", player.getX());
        position.addProperty("y", player.getY());
        position.addProperty("z", player.getZ());
        result.add("position", position);
        
        // Dimension
        Identifier dimId = player.getWorld().getRegistryKey().getValue();
        result.addProperty("dimension", dimId.toString());
        
        // Health & Hunger
        result.addProperty("health", player.getHealth());
        result.addProperty("maxHealth", player.getMaxHealth());
        result.addProperty("hunger", player.getHungerManager().getFoodLevel());
        result.addProperty("saturation", player.getHungerManager().getSaturationLevel());
        
        // Armor slots
        JsonObject armor = new JsonObject();
        armor.add("head", getArmorSlot(player, EquipmentSlot.HEAD));
        armor.add("chest", getArmorSlot(player, EquipmentSlot.CHEST));
        armor.add("legs", getArmorSlot(player, EquipmentSlot.LEGS));
        armor.add("feet", getArmorSlot(player, EquipmentSlot.FEET));
        result.add("armor", armor);
        
        // Hotbar
        JsonArray hotbar = new JsonArray();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            JsonObject slot = new JsonObject();
            slot.addProperty("slot", i);
            if (!stack.isEmpty()) {
                slot.addProperty("item", Registries.ITEM.getId(stack.getItem()).toString());
                slot.addProperty("count", stack.getCount());
                if (stack.isDamageable()) {
                    slot.addProperty("durability", stack.getMaxDamage() - stack.getDamage());
                    slot.addProperty("maxDurability", stack.getMaxDamage());
                }
            } else {
                slot.addProperty("item", "minecraft:air");
                slot.addProperty("count", 0);
            }
            hotbar.add(slot);
        }
        result.add("hotbar", hotbar);
        
        // Inventory summary (counts by item type)
        JsonObject inventorySummary = new JsonObject();
        Map<String, Integer> counts = new HashMap<>();
        
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                String itemId = Registries.ITEM.getId(stack.getItem()).toString();
                counts.merge(itemId, stack.getCount(), Integer::sum);
            }
        }
        
        counts.forEach(inventorySummary::addProperty);
        result.add("inventorySummary", inventorySummary);
        
        return result;
    }
    
    private static JsonObject getArmorSlot(ClientPlayerEntity player, EquipmentSlot slot) {
        JsonObject obj = new JsonObject();
        ItemStack stack = player.getEquippedStack(slot);
        
        if (stack.isEmpty()) {
            obj.addProperty("item", "minecraft:air");
        } else {
            obj.addProperty("item", Registries.ITEM.getId(stack.getItem()).toString());
            if (stack.isDamageable()) {
                obj.addProperty("durability", stack.getMaxDamage() - stack.getDamage());
                obj.addProperty("maxDurability", stack.getMaxDamage());
            }
        }
        
        return obj;
    }
    
    public static JsonObject getPlayerStateForUpdate() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        
        if (player == null) {
            return null;
        }
        
        JsonObject result = new JsonObject();
        
        // Position
        JsonObject position = new JsonObject();
        position.addProperty("x", Math.round(player.getX() * 100.0) / 100.0);
        position.addProperty("y", Math.round(player.getY() * 100.0) / 100.0);
        position.addProperty("z", Math.round(player.getZ() * 100.0) / 100.0);
        result.add("position", position);
        
        // Dimension
        Identifier dimId = player.getWorld().getRegistryKey().getValue();
        result.addProperty("dimension", dimId.toString());
        
        // Health & Hunger
        result.addProperty("health", player.getHealth());
        result.addProperty("hunger", player.getHungerManager().getFoodLevel());
        result.addProperty("saturation", player.getHungerManager().getSaturationLevel());
        
        return result;
    }
}
