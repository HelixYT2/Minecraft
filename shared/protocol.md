# Helix Bridge Protocol Specification

## Overview

The Helix Bridge Protocol defines communication between the HelixHub desktop application and Fabric mod instances running in Minecraft. Communication uses WebSocket with JSON messages.

## Connection

- **Transport**: WebSocket over TCP
- **Default Port**: 9742
- **Binding**: 127.0.0.1 (localhost only)
- **Hub Role**: WebSocket Server
- **Mod Role**: WebSocket Client

## Authentication

1. Hub generates a random 32-character alphanumeric token on first run
2. Token is stored in Hub's config file
3. Token must be copied to each mod instance's config
4. On connection, mod sends `Hello` message with token
5. Hub validates token; if invalid, closes connection with error

## Message Envelope

All messages follow this structure:

```json
{
  "type": "string",
  "requestId": "uuid",
  "timestamp": "iso8601",
  "instanceId": "string",
  "sessionId": "uuid",
  "payload": { }
}
```

### Fields

| Field | Type | Description |
|-------|------|-------------|
| type | string | Message type (see below) |
| requestId | UUID | Unique ID for request/response correlation |
| timestamp | ISO8601 | Message creation time |
| instanceId | string | Prism Launcher instance ID |
| sessionId | UUID | Unique session ID per mod instance |
| payload | object | Type-specific payload |

## Message Types

### Hello (Mod → Hub, Hub → Mod)

Initial handshake message.

**Mod → Hub:**
```json
{
  "type": "Hello",
  "requestId": "...",
  "timestamp": "...",
  "instanceId": "",
  "sessionId": "",
  "payload": {
    "protocolVersion": "1.0",
    "authToken": "your-secret-token"
  }
}
```

**Hub → Mod (success):**
```json
{
  "type": "Hello",
  "payload": {
    "protocolVersion": "1.0",
    "accepted": true,
    "hubVersion": "1.0.0"
  }
}
```

**Hub → Mod (failure):**
```json
{
  "type": "Hello",
  "payload": {
    "accepted": false,
    "reason": "Invalid auth token"
  }
}
```

### Register (Mod → Hub)

Sent after successful Hello to register instance.

```json
{
  "type": "Register",
  "instanceId": "my-mc-instance",
  "sessionId": "uuid",
  "payload": {
    "modVersion": "1.0.0",
    "minecraftVersion": "1.21.1",
    "baritoneAvailable": true,
    "baritoneVersion": "1.10.2",
    "capabilities": {
      "tools": ["get_player_state", "get_baritone_status"],
      "actions": ["baritone_command", "craft_item", "equip_armor", "stop_all"]
    }
  }
}
```

### ToolRequest (Hub → Mod)

Request read-only information from the mod.

```json
{
  "type": "ToolRequest",
  "requestId": "...",
  "instanceId": "...",
  "sessionId": "...",
  "payload": {
    "tool": "get_player_state",
    "args": {}
  }
}
```

### ToolResponse (Mod → Hub)

Response to a ToolRequest.

```json
{
  "type": "ToolResponse",
  "requestId": "...",
  "instanceId": "...",
  "sessionId": "...",
  "payload": {
    "success": true,
    "tool": "get_player_state",
    "data": { ... }
  }
}
```

### ActionRequest (Hub → Mod)

Request the mod to perform an action.

```json
{
  "type": "ActionRequest",
  "requestId": "...",
  "instanceId": "...",
  "sessionId": "...",
  "payload": {
    "action": "baritone_command",
    "args": {
      "command": "mine diamond_ore"
    }
  }
}
```

### ActionResult (Mod → Hub)

Result of an ActionRequest.

```json
{
  "type": "ActionResult",
  "requestId": "...",
  "instanceId": "...",
  "sessionId": "...",
  "payload": {
    "success": true,
    "action": "baritone_command",
    "result": {
      "started": true,
      "message": "Mining task started"
    }
  }
}
```

### StateUpdate (Mod → Hub)

Periodic state update from the mod.

```json
{
  "type": "StateUpdate",
  "instanceId": "...",
  "sessionId": "...",
  "payload": {
    "player": {
      "position": { "x": 100.5, "y": 64.0, "z": -200.3 },
      "dimension": "minecraft:overworld",
      "health": 20.0,
      "hunger": 18,
      "saturation": 5.0
    },
    "baritone": {
      "active": true,
      "currentGoal": "Mine diamond_ore",
      "currentProcess": "MineProcess",
      "progress": "Found 2 veins, mining..."
    },
    "inventory": {
      "armorSlots": [...],
      "hotbar": [...],
      "mainInventory": {...}
    }
  }
}
```

### Error (Any direction)

Error message for protocol or execution errors.

```json
{
  "type": "Error",
  "requestId": "...",
  "payload": {
    "code": "BARITONE_NOT_AVAILABLE",
    "message": "Baritone is not installed in this instance",
    "recoverable": false
  }
}
```

## Tools Reference

### get_player_state

Returns player position, health, hunger, armor, and inventory summary.

**Args:** None

**Response data:**
```json
{
  "position": { "x": 0.0, "y": 64.0, "z": 0.0 },
  "dimension": "minecraft:overworld",
  "health": 20.0,
  "maxHealth": 20.0,
  "hunger": 20,
  "saturation": 5.0,
  "armor": {
    "head": { "item": "minecraft:diamond_helmet", "durability": 363 },
    "chest": { "item": "minecraft:diamond_chestplate", "durability": 528 },
    "legs": { "item": "minecraft:diamond_leggings", "durability": 495 },
    "feet": { "item": "minecraft:diamond_boots", "durability": 429 }
  },
  "hotbar": [
    { "slot": 0, "item": "minecraft:diamond_pickaxe", "count": 1 },
    ...
  ],
  "inventorySummary": {
    "minecraft:diamond": 12,
    "minecraft:iron_ingot": 45,
    ...
  }
}
```

### get_baritone_status

Returns Baritone's current status.

**Args:** None

**Response data:**
```json
{
  "available": true,
  "active": true,
  "currentGoal": "GoalBlock{x=100, y=12, z=-50}",
  "currentProcess": "MineProcess",
  "processState": "mining",
  "eta": "2m 30s"
}
```

## Actions Reference

### baritone_command

Execute a Baritone command.

**Args:**
```json
{
  "command": "mine diamond_ore"
}
```

**Result:**
```json
{
  "started": true,
  "message": "Mining task started for diamond_ore"
}
```

Common commands:
- `mine <block>` - Mine specified block type
- `goto <x> <y> <z>` - Go to coordinates
- `farm` - Auto-farm nearby crops
- `stop` - Stop current task
- `pause` - Pause current task
- `resume` - Resume paused task

### craft_item

Craft an item (uses Baritone crafting if available).

**Args:**
```json
{
  "item": "minecraft:diamond_chestplate",
  "count": 1
}
```

**Result:**
```json
{
  "success": true,
  "crafted": 1,
  "message": "Crafted 1 diamond_chestplate"
}
```

### equip_armor

Equip armor pieces from inventory.

**Args:**
```json
{
  "slot": "all" | "head" | "chest" | "legs" | "feet",
  "material": "diamond" | "netherite" | "iron" | "gold" | "leather" | "chainmail" | "any"
}
```

**Result:**
```json
{
  "equipped": ["head", "chest", "legs", "feet"],
  "message": "Equipped full diamond armor"
}
```

### stop_all

Cancel all current tasks and stop Baritone.

**Args:** None

**Result:**
```json
{
  "stopped": true,
  "message": "All tasks stopped"
}
```

## Error Codes

| Code | Description |
|------|-------------|
| AUTH_FAILED | Invalid authentication token |
| BARITONE_NOT_AVAILABLE | Baritone mod not installed |
| PLAYER_NOT_INGAME | Player not in a world |
| INVALID_TOOL | Unknown tool requested |
| INVALID_ACTION | Unknown action requested |
| ACTION_FAILED | Action execution failed |
| TIMEOUT | Request timed out |
| PROTOCOL_ERROR | Malformed message |

## Timeouts and Retries

- Connection timeout: 10 seconds
- Request timeout: 30 seconds (configurable)
- Reconnect delay: 5 seconds (exponential backoff up to 60s)
- Max retries per request: 3
