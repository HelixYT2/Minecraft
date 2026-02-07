# Helix Minecraft Bridge - Technical Plan

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        HelixHub.exe                              │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐  ┌────────────────┐  │
│  │ Chat UI  │  │ Instance │  │ Plan View │  │ State Monitor  │  │
│  │          │  │ Selector │  │ + Approve │  │ + Logs         │  │
│  └────┬─────┘  └────┬─────┘  └─────┬─────┘  └───────┬────────┘  │
│       │             │              │                │           │
│  ┌────┴─────────────┴──────────────┴────────────────┴────────┐  │
│  │                    Hub Core Services                       │  │
│  │  - WebSocket Server (127.0.0.1:9742)                      │  │
│  │  - LM Studio Client (http://localhost:1234/v1)            │  │
│  │  - Plan Executor                                           │  │
│  │  - Prism Launcher Manager                                  │  │
│  └────────────────────────────────┬──────────────────────────┘  │
└───────────────────────────────────┼─────────────────────────────┘
                                    │ WebSocket
                                    │ JSON Protocol
          ┌─────────────────────────┼─────────────────────────┐
          │                         │                         │
    ┌─────┴─────┐            ┌─────┴─────┐            ┌─────┴─────┐
    │ Instance1 │            │ Instance2 │            │ Instance3 │
    │ MC 1.21.1 │            │ MC 1.21.1 │            │ MC 1.21.1 │
    │ +Fabric   │            │ +Fabric   │            │ +Fabric   │
    │ +Baritone │            │ +Baritone │            │ +Baritone │
    │ +HelixMod │            │ +HelixMod │            │ +HelixMod │
    └───────────┘            └───────────┘            └───────────┘
```

## Technology Stack

### Fabric Mod (mod-fabric/)
- **Language**: Java 21
- **Build**: Gradle with Fabric Loom
- **MC Version**: 1.21.1
- **Dependencies**: Fabric API, Baritone (optional runtime)
- **WebSocket Client**: Java-WebSocket library

### Desktop Hub (hub-desktop/)
- **Framework**: .NET 8 + Avalonia UI
- **Output**: Single-file self-contained .exe (win-x64)
- **WebSocket Server**: System.Net.WebSockets
- **HTTP Client**: HttpClient for LM Studio API

## Protocol Design

### Message Envelope
```json
{
  "type": "Hello|Register|ToolRequest|ToolResponse|ActionRequest|ActionResult|StateUpdate|Error",
  "requestId": "uuid",
  "timestamp": "2024-01-01T00:00:00Z",
  "instanceId": "prism-instance-name",
  "sessionId": "uuid",
  "payload": { ... }
}
```

### Message Flow
1. Mod connects to Hub WS server
2. Mod sends `Hello` with auth token
3. Hub validates, sends `Hello` response
4. Mod sends `Register` with instanceId, sessionId, capabilities
5. Hub can send `ToolRequest` (read-only queries)
6. Mod responds with `ToolResponse`
7. Hub can send `ActionRequest` (state-changing)
8. Mod responds with `ActionResult`
9. Mod sends periodic `StateUpdate`

## File Tree

```
/workspace/project/
├── README.md
├── TECHNICAL_PLAN.md
├── .gitignore
│
├── shared/
│   ├── protocol.md
│   ├── message.schema.json
│   └── plan.schema.json
│
├── mod-fabric/
│   ├── build.gradle
│   ├── gradle.properties
│   ├── settings.gradle
│   ├── gradle/
│   │   └── wrapper/
│   │       ├── gradle-wrapper.jar
│   │       └── gradle-wrapper.properties
│   ├── gradlew
│   ├── gradlew.bat
│   └── src/
│       └── main/
│           ├── java/
│           │   └── dev/helix/bridge/
│           │       ├── HelixBridgeMod.java
│           │       ├── config/
│           │       │   └── HelixConfig.java
│           │       ├── network/
│           │       │   ├── HubConnection.java
│           │       │   └── MessageHandler.java
│           │       ├── protocol/
│           │       │   ├── Message.java
│           │       │   ├── MessageType.java
│           │       │   └── Payloads.java
│           │       ├── tools/
│           │       │   ├── ToolRegistry.java
│           │       │   ├── PlayerStateTool.java
│           │       │   └── BaritoneTool.java
│           │       ├── actions/
│           │       │   ├── ActionRegistry.java
│           │       │   ├── BaritoneAction.java
│           │       │   ├── CraftAction.java
│           │       │   ├── EquipArmorAction.java
│           │       │   └── StopAllAction.java
│           │       ├── baritone/
│           │       │   └── BaritoneIntegration.java
│           │       └── command/
│           │           └── HelixHubCommand.java
│           └── resources/
│               ├── fabric.mod.json
│               ├── helix-bridge.mixins.json
│               └── assets/
│                   └── helix-bridge/
│                       └── icon.png
│
├── hub-desktop/
│   ├── HelixHub.sln
│   ├── HelixHub/
│   │   ├── HelixHub.csproj
│   │   ├── Program.cs
│   │   ├── App.axaml
│   │   ├── App.axaml.cs
│   │   ├── ViewModels/
│   │   │   ├── MainWindowViewModel.cs
│   │   │   ├── ChatViewModel.cs
│   │   │   ├── InstanceViewModel.cs
│   │   │   └── PlanViewModel.cs
│   │   ├── Views/
│   │   │   ├── MainWindow.axaml
│   │   │   ├── MainWindow.axaml.cs
│   │   │   ├── ChatView.axaml
│   │   │   ├── InstanceView.axaml
│   │   │   └── PlanView.axaml
│   │   ├── Services/
│   │   │   ├── WebSocketServer.cs
│   │   │   ├── LmStudioClient.cs
│   │   │   ├── PrismLauncherService.cs
│   │   │   ├── PlanExecutor.cs
│   │   │   └── ConfigService.cs
│   │   ├── Models/
│   │   │   ├── Message.cs
│   │   │   ├── ConnectedInstance.cs
│   │   │   ├── Plan.cs
│   │   │   └── HubConfig.cs
│   │   └── Protocol/
│   │       ├── MessageEnvelope.cs
│   │       └── PayloadTypes.cs
│   └── HelixHub.Tests/
│       ├── HelixHub.Tests.csproj
│       ├── ProtocolTests.cs
│       └── PlanSchemaTests.cs
│
├── scripts/
│   ├── build.ps1
│   └── clean.ps1
│
└── dist/
    ├── .gitkeep
    ├── HelixMinecraftBridge-1.21.1.jar (output)
    └── HelixHub.exe (output)
```

## Build Process

1. **Mod Build**: `./gradlew build` → `build/libs/helix-bridge-1.0.0.jar`
2. **Hub Build**: `dotnet publish -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true`
3. **Copy to dist/**: Rename and copy artifacts

## Key Implementation Details

### Baritone Integration
- Check for Baritone at runtime via reflection
- If present, use BaritoneAPI for commands and status
- If absent, report `baritoneAvailable: false` and reject actions

### LM Studio Integration
- Use function calling format for tool calls
- System prompt enforces JSON plan output
- Validate against plan.schema.json
- Auto-repair invalid JSON up to 3 retries

### Security
- Shared secret token in both configs
- WebSocket bound to 127.0.0.1 only
- All traffic stays local
