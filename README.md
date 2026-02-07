# Helix Minecraft Bridge

AI-powered Minecraft automation system that connects a desktop Hub application to Minecraft instances via a Fabric mod, using local LLM inference to generate and execute step-by-step plans.

## Features

- 🎮 **Fabric Mod** - Bridges Minecraft with the desktop Hub via WebSocket
- 🖥️ **Desktop Hub** - Modern chat UI for interacting with your Minecraft instances
- 🤖 **LM Studio Integration** - Uses local LLM for plan generation (OpenAI-compatible API)
- 🚀 **Prism Launcher Support** - Launch and manage Minecraft instances directly
- 🔄 **Baritone Integration** - Automated pathfinding, mining, crafting, and more
- 🔐 **Secure** - Token-based authentication, localhost-only communication
- 📊 **Multi-Instance** - Control multiple Minecraft instances simultaneously

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     HelixHub.exe                             │
│  - Chat UI with plan approval workflow                      │
│  - WebSocket server for mod connections                     │
│  - LM Studio client for plan generation                     │
│  - Prism Launcher integration                               │
└─────────────────────────┬───────────────────────────────────┘
                          │ WebSocket (127.0.0.1:9742)
                          │
┌─────────────────────────┴───────────────────────────────────┐
│              Minecraft + Fabric + Baritone                   │
│  ┌────────────────────────────────────────────────────────┐ │
│  │           HelixMinecraftBridge-1.21.1.jar              │ │
│  │  - WebSocket client connecting to Hub                   │ │
│  │  - Baritone API integration                            │ │
│  │  - Player state reporting                              │ │
│  │  - Action execution                                    │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Requirements

- **Windows 10/11** (Hub is Windows-only)
- **Java 21** (for Minecraft 1.21.1)
- **Minecraft 1.21.1** with Fabric Loader
- **Prism Launcher** (recommended for instance management)
- **Baritone** (for automation capabilities)
- **LM Studio** (for local LLM inference)
- **.NET 8 SDK** (only for building from source)

## Quick Start

### 1. Download Releases

Download from the Releases page:
- `HelixMinecraftBridge-1.21.1.jar` - Fabric mod
- `HelixHub.exe` - Desktop application

### 2. Install the Mod

1. Open Prism Launcher and create/select a Minecraft 1.21.1 instance
2. Install Fabric Loader if not already installed
3. Download and install [Baritone for Fabric 1.21.1](https://github.com/cabaletta/baritone/releases)
4. Copy `HelixMinecraftBridge-1.21.1.jar` to the instance's `mods` folder
5. Launch the instance once to generate the config file

### 3. Configure the Mod

Edit `config/helix-bridge.json` in your Minecraft instance:

```json
{
  "hubUrl": "ws://127.0.0.1:9742",
  "authToken": "YOUR_TOKEN_FROM_HUB",
  "instanceId": "your-instance-name",
  "updateRateMs": 1000
}
```

### 4. Start LM Studio

1. Download and install [LM Studio](https://lmstudio.ai/)
2. Download a model (recommended: Llama 3.1 8B or similar)
3. Start the local server (default: `http://localhost:1234/v1`)

### 5. Run the Hub

1. Run `HelixHub.exe`
2. Open Settings (⚙️ button)
3. Copy the **Auth Token** to your mod config
4. Configure Prism Launcher path if needed
5. Click "Test" next to LM Studio URL to verify connection

### 6. Connect and Play

1. Launch your Minecraft instance from the Hub or Prism Launcher
2. Wait for the mod to connect (shows in Hub's left panel)
3. Type a command like "Get me full diamond armor"
4. Review the generated plan
5. Click **Approve** to execute

## Building from Source

### Prerequisites

- Java 21 JDK
- .NET 8 SDK
- Git

### Build Steps

```powershell
# Clone the repository
git clone https://github.com/your-org/helix-bridge.git
cd helix-bridge

# Build everything
.\scripts\build.ps1

# Or build components separately
.\scripts\build.ps1 -SkipHub    # Only build mod
.\scripts\build.ps1 -SkipMod    # Only build hub

# Clean build artifacts
.\scripts\build.ps1 -Clean
```

Output files will be in the `dist/` directory.

## Usage Guide

### Available Commands

You can type natural language commands like:
- "Get me full diamond armor"
- "Mine 64 iron ore"
- "Go to coordinates 100, 64, -200"
- "Build a simple house"
- "Find and kill the nearest cow"

### In-Game Commands

The mod adds the `/helixhub` command:

```
/helixhub status    - Show connection status
/helixhub connect   - Manually connect to Hub
/helixhub disconnect - Disconnect from Hub
/helixhub reload    - Reload config
/helixhub config    - Show current config
/helixhub config token <value>    - Set auth token
/helixhub config instance <value> - Set instance ID
/helixhub config url <value>      - Set Hub URL
```

### Plan Execution

When you type a command:
1. Hub queries the mod for current game state (position, health, inventory)
2. Hub sends the request + state to LM Studio
3. LM Studio generates a step-by-step plan
4. Hub displays the plan for your review
5. You can **Approve** or **Reject** the plan
6. If approved, Hub executes steps sequentially
7. On failure, you can **Retry**, **Skip**, or **Abort**

### Supported Actions

| Action | Description |
|--------|-------------|
| `baritone_command` | Execute any Baritone command (mine, goto, farm, etc.) |
| `craft_item` | Craft items using Baritone's crafting system |
| `equip_armor` | Equip armor from inventory |
| `stop_all` | Cancel all running tasks |
| `wait` | Pause execution for specified seconds |

### Baritone Commands

Common Baritone commands used in plans:
- `mine <block>` - Mine specified block type
- `mine <block> <count>` - Mine until you have count items
- `goto <x> <y> <z>` - Navigate to coordinates
- `follow <player>` - Follow a player
- `farm` - Auto-farm nearby crops
- `stop` - Stop current task
- `pause` / `resume` - Pause/resume current task

## Troubleshooting

### Mod won't connect to Hub

1. **Check the Hub is running** - Look for the WebSocket server message in logs
2. **Verify the port** - Default is 9742, make sure it's not blocked
3. **Check auth token** - Must match exactly between Hub and mod config
4. **Firewall** - Add exception for HelixHub.exe if needed
5. **Try manual connect** - Use `/helixhub connect` in-game

### LM Studio connection fails

1. **Start the server** - In LM Studio, click "Start Server"
2. **Check the port** - Default is 1234
3. **Load a model** - Server needs a model loaded to respond
4. **Test the URL** - Open `http://localhost:1234/v1/models` in browser

### Baritone not detected

1. **Install Baritone** - Download the Fabric version for 1.21.1
2. **Check mod loading** - Baritone should appear in mod list
3. **Restart Minecraft** - Baritone detection happens on startup
4. **Check logs** - Look for "Baritone detected" or "Baritone not found"

### Plan generation fails

1. **Check LM Studio** - Model must be loaded and server running
2. **Try simpler requests** - Start with "mine 10 diamonds"
3. **Check logs** - Look for JSON parsing errors
4. **Model quality** - Larger models generate better plans

### Actions fail during execution

1. **Check Baritone status** - Use `/helixhub status`
2. **Verify prerequisites** - Does the player have required items?
3. **Manual intervention** - Sometimes the player needs to be unstuck
4. **Skip/Retry** - Use the failure dialog options

### Wrong Java version

Minecraft 1.21.1 requires Java 21. Check with:
```bash
java -version
```

Set JAVA_HOME if needed:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
```

### Port already in use

If port 9742 is in use:
1. Close any other HelixHub instances
2. Or change the port in Hub settings and mod config

## Project Structure

```
helix-bridge/
├── README.md                 # This file
├── TECHNICAL_PLAN.md        # Architecture documentation
├── shared/                   # Shared protocol definitions
│   ├── protocol.md          # Protocol specification
│   ├── message.schema.json  # Message validation schema
│   └── plan.schema.json     # Plan validation schema
├── mod-fabric/              # Fabric mod project
│   ├── build.gradle
│   ├── src/main/java/dev/helix/bridge/
│   │   ├── HelixBridgeMod.java
│   │   ├── config/
│   │   ├── network/
│   │   ├── protocol/
│   │   ├── tools/
│   │   ├── actions/
│   │   ├── baritone/
│   │   └── command/
│   └── src/main/resources/
├── hub-desktop/             # Desktop Hub project
│   ├── HelixHub.sln
│   ├── HelixHub/
│   │   ├── Services/
│   │   ├── ViewModels/
│   │   ├── Views/
│   │   └── Models/
│   └── HelixHub.Tests/
├── scripts/                 # Build scripts
│   ├── build.ps1
│   └── clean.ps1
└── dist/                    # Build outputs
    ├── HelixMinecraftBridge-1.21.1.jar
    └── HelixHub.exe
```

## Security

- All communication is localhost-only (127.0.0.1)
- Token-based authentication between Hub and mod
- Plans require explicit user approval before execution
- No external network calls from the mod
- No OS-level automation (keyboard/mouse) - uses in-game APIs only

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `dotnet test` and `./gradlew test`
5. Submit a pull request

## License

MIT License - see LICENSE file for details.

## Acknowledgments

- [Baritone](https://github.com/cabaletta/baritone) - The pathfinding and automation backbone
- [Fabric](https://fabricmc.net/) - Minecraft modding framework
- [LM Studio](https://lmstudio.ai/) - Local LLM inference
- [Avalonia](https://avaloniaui.net/) - Cross-platform .NET UI framework
- [Prism Launcher](https://prismlauncher.org/) - Minecraft instance management
