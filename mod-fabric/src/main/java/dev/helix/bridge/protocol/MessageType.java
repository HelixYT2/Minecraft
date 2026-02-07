package dev.helix.bridge.protocol;

public enum MessageType {
    Hello,
    Register,
    ToolRequest,
    ToolResponse,
    ActionRequest,
    ActionResult,
    StateUpdate,
    Error
}
