package dev.helix.bridge.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.helix.bridge.HelixBridgeMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EquipArmorAction {
    
    private static final Map<String, List<Item>> ARMOR_BY_MATERIAL = new HashMap<>();
    
    static {
        ARMOR_BY_MATERIAL.put("diamond", List.of(
            Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS
        ));
        ARMOR_BY_MATERIAL.put("netherite", List.of(
            Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS
        ));
        ARMOR_BY_MATERIAL.put("iron", List.of(
            Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS
        ));
        ARMOR_BY_MATERIAL.put("gold", List.of(
            Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS
        ));
        ARMOR_BY_MATERIAL.put("leather", List.of(
            Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS
        ));
        ARMOR_BY_MATERIAL.put("chainmail", List.of(
            Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS
        ));
    }
    
    public static JsonObject execute(JsonObject args) {
        JsonObject result = new JsonObject();
        
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        
        if (player == null) {
            result.addProperty("success", false);
            result.addProperty("error", "Player not in game");
            return result;
        }
        
        String slotArg = args != null && args.has("slot") ? args.get("slot").getAsString() : "all";
        String material = args != null && args.has("material") ? args.get("material").getAsString() : "any";
        
        List<EquipmentSlot> slotsToEquip = new ArrayList<>();
        switch (slotArg.toLowerCase()) {
            case "head" -> slotsToEquip.add(EquipmentSlot.HEAD);
            case "chest" -> slotsToEquip.add(EquipmentSlot.CHEST);
            case "legs" -> slotsToEquip.add(EquipmentSlot.LEGS);
            case "feet" -> slotsToEquip.add(EquipmentSlot.FEET);
            default -> {
                slotsToEquip.add(EquipmentSlot.HEAD);
                slotsToEquip.add(EquipmentSlot.CHEST);
                slotsToEquip.add(EquipmentSlot.LEGS);
                slotsToEquip.add(EquipmentSlot.FEET);
            }
        }
        
        List<String> equipped = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        
        for (EquipmentSlot slot : slotsToEquip) {
            boolean success = equipSlot(player, slot, material);
            if (success) {
                equipped.add(slot.getName());
            } else {
                failed.add(slot.getName());
            }
        }
        
        JsonArray equippedArray = new JsonArray();
        equipped.forEach(equippedArray::add);
        result.add("equipped", equippedArray);
        
        if (!failed.isEmpty()) {
            JsonArray failedArray = new JsonArray();
            failed.forEach(failedArray::add);
            result.add("failed", failedArray);
        }
        
        result.addProperty("success", !equipped.isEmpty());
        
        if (equipped.isEmpty()) {
            result.addProperty("message", "No armor found to equip for material: " + material);
        } else {
            result.addProperty("message", "Equipped armor in slots: " + String.join(", ", equipped));
        }
        
        return result;
    }
    
    private static boolean equipSlot(ClientPlayerEntity player, EquipmentSlot slot, String material) {
        PlayerInventory inventory = player.getInventory();
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Check if slot is already equipped with desired material
        ItemStack currentArmor = player.getEquippedStack(slot);
        if (!currentArmor.isEmpty() && currentArmor.getItem() instanceof ArmorItem armorItem) {
            if (material.equals("any") || matchesMaterial(currentArmor.getItem(), material)) {
                return true; // Already equipped
            }
        }
        
        // Find armor piece in inventory
        int armorSlot = findArmorInInventory(inventory, slot, material);
        if (armorSlot == -1) {
            return false; // No armor found
        }
        
        // Equip the armor using screen interaction
        try {
            // Get the container sync ID (0 for player inventory)
            int syncId = player.currentScreenHandler.syncId;
            
            // Calculate the screen slot index
            // Armor slots in player screen: 5=head, 6=chest, 7=legs, 8=feet
            int armorScreenSlot = switch (slot) {
                case HEAD -> 5;
                case CHEST -> 6;
                case LEGS -> 7;
                case FEET -> 8;
                default -> -1;
            };
            
            if (armorScreenSlot == -1) return false;
            
            // Convert inventory slot to screen slot (main inventory starts at 9)
            int inventoryScreenSlot;
            if (armorSlot < 9) {
                // Hotbar: slots 36-44 in the screen
                inventoryScreenSlot = 36 + armorSlot;
            } else {
                // Main inventory: slots 9-35 in the screen
                inventoryScreenSlot = armorSlot;
            }
            
            // Shift-click to auto-equip
            ClientPlayerInteractionManager interactionManager = client.interactionManager;
            if (interactionManager != null) {
                interactionManager.clickSlot(syncId, inventoryScreenSlot, 0, SlotActionType.QUICK_MOVE, player);
                return true;
            }
        } catch (Exception e) {
            HelixBridgeMod.LOGGER.error("Error equipping armor", e);
        }
        
        return false;
    }
    
    private static int findArmorInInventory(PlayerInventory inventory, EquipmentSlot slot, String material) {
        for (int i = 0; i < inventory.main.size(); i++) {
            ItemStack stack = inventory.main.get(i);
            if (stack.isEmpty()) continue;
            
            Item item = stack.getItem();
            if (!(item instanceof ArmorItem armorItem)) continue;
            
            // Check if it's for the right slot
            if (armorItem.getSlotType() != slot) continue;
            
            // Check material if specified
            if (!material.equals("any") && !matchesMaterial(item, material)) continue;
            
            return i;
        }
        return -1;
    }
    
    private static boolean matchesMaterial(Item item, String material) {
        List<Item> materialItems = ARMOR_BY_MATERIAL.get(material.toLowerCase());
        if (materialItems == null) return false;
        return materialItems.contains(item);
    }
}
