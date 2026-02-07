# Helix Minecraft Bridge - Agent Knowledge

## Project Overview
This is a Minecraft automation system consisting of:
1. **Fabric Mod** (`mod-fabric/`) - Connects to Hub via WebSocket, integrates with Baritone
2. **Desktop Hub** (`hub-desktop/`) - .NET 8 + Avalonia app that controls the mod via LLM-generated plans

## Build Commands

### Build Everything (Windows PowerShell)
```powershell
.\scripts\build.ps1
```

### Build Mod Only (Linux/macOS)
```bash
cd mod-fabric
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew build --no-daemon
# Output: build/libs/helix-bridge-1.0.0.jar
```

### Build Hub Only
```bash
cd hub-desktop
dotnet publish HelixHub/HelixHub.csproj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true
# Output: HelixHub/bin/Release/net8.0/win-x64/publish/HelixHub.exe
```

### Run Tests
```bash
cd hub-desktop
dotnet test HelixHub.Tests/HelixHub.Tests.csproj
```

## Architecture Notes

### Protocol
- WebSocket on `ws://127.0.0.1:9742`
- Hub is the server, mod is the client
- Authentication via shared token
- Message envelope: `{type, requestId, timestamp, instanceId, sessionId, payload}`

### Key Message Types
- `Hello` - Authentication handshake
- `Register` - Instance registration with capabilities
- `ToolRequest/ToolResponse` - Read-only queries
- `ActionRequest/ActionResult` - State-changing commands
- `StateUpdate` - Periodic player/baritone state

### Baritone Integration
- Detected at runtime via reflection (no compile dependency)
- If Baritone absent, actions requiring it will fail gracefully
- Common commands: `mine`, `goto`, `farm`, `stop`

## Key Files

### Mod
- `HelixBridgeMod.java` - Entry point
- `HubConnection.java` - WebSocket client
- `BaritoneIntegration.java` - Baritone API via reflection
- `ToolRegistry.java` / `ActionRegistry.java` - Tool/action handlers

### Hub
- `WebSocketServer.cs` - Manages mod connections
- `LmStudioClient.cs` - Generates plans via LM Studio API
- `PlanExecutor.cs` - Executes approved plans
- `MainWindowViewModel.cs` - UI logic

### Schemas
- `shared/plan.schema.json` - Validates LLM-generated plans
- `shared/message.schema.json` - Protocol message schema

## Troubleshooting

### Gradle build fails with Baritone error
The mod uses reflection for Baritone, no compile dependency needed. Check build.gradle doesn't reference Baritone.

### Tests can't find schema file
Schema paths are resolved dynamically. Ensure `/workspace/project/shared/plan.schema.json` exists.

### WebSocket connection refused
- Check Hub is running
- Verify port 9742 is available
- Check auth tokens match
