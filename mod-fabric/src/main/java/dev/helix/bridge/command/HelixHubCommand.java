package dev.helix.bridge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.helix.bridge.HelixBridgeMod;
import dev.helix.bridge.config.HelixConfig;
import dev.helix.bridge.network.HubConnection;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class HelixHubCommand {
    
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("helixhub")
                .then(ClientCommandManager.literal("status")
                        .executes(HelixHubCommand::showStatus))
                .then(ClientCommandManager.literal("connect")
                        .executes(HelixHubCommand::connect))
                .then(ClientCommandManager.literal("disconnect")
                        .executes(HelixHubCommand::disconnect))
                .then(ClientCommandManager.literal("reload")
                        .executes(HelixHubCommand::reloadConfig))
                .then(ClientCommandManager.literal("config")
                        .executes(HelixHubCommand::showConfig)
                        .then(ClientCommandManager.literal("token")
                                .then(ClientCommandManager.argument("value", StringArgumentType.string())
                                        .executes(HelixHubCommand::setToken)))
                        .then(ClientCommandManager.literal("instance")
                                .then(ClientCommandManager.argument("value", StringArgumentType.string())
                                        .executes(HelixHubCommand::setInstanceId)))
                        .then(ClientCommandManager.literal("url")
                                .then(ClientCommandManager.argument("value", StringArgumentType.string())
                                        .executes(HelixHubCommand::setHubUrl))))
                .executes(HelixHubCommand::showStatus));
    }
    
    private static int showStatus(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        HubConnection connection = HelixBridgeMod.getInstance().getHubConnection();
        HelixConfig config = HelixBridgeMod.getInstance().getConfig();
        
        source.sendFeedback(Text.literal("=== Helix Hub Status ===").formatted(Formatting.GOLD));
        
        String status = connection.getStatus();
        Formatting statusColor = switch (status) {
            case "CONNECTED" -> Formatting.GREEN;
            case "NOT CONFIGURED" -> Formatting.RED;
            case "DISCONNECTED" -> Formatting.YELLOW;
            default -> Formatting.WHITE;
        };
        
        source.sendFeedback(Text.literal("Status: ").append(Text.literal(status).formatted(statusColor)));
        source.sendFeedback(Text.literal("Instance ID: " + (config.getInstanceId().isEmpty() ? "(not set)" : config.getInstanceId())));
        source.sendFeedback(Text.literal("Session ID: " + connection.getSessionId()));
        source.sendFeedback(Text.literal("Hub URL: " + config.getHubUrl()));
        source.sendFeedback(Text.literal("Auth Token: " + (config.getAuthToken().isEmpty() ? "(not set)" : "****" + config.getAuthToken().substring(Math.max(0, config.getAuthToken().length() - 4)))));
        source.sendFeedback(Text.literal("Baritone: " + (HelixBridgeMod.getInstance().getBaritoneIntegration().isAvailable() ? "Available" : "Not available")).formatted(
                HelixBridgeMod.getInstance().getBaritoneIntegration().isAvailable() ? Formatting.GREEN : Formatting.YELLOW));
        
        return 1;
    }
    
    private static int connect(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        HubConnection connection = HelixBridgeMod.getInstance().getHubConnection();
        
        if (connection.isConnected()) {
            source.sendFeedback(Text.literal("Already connected to Hub").formatted(Formatting.YELLOW));
            return 0;
        }
        
        source.sendFeedback(Text.literal("Connecting to Hub...").formatted(Formatting.AQUA));
        connection.connect();
        return 1;
    }
    
    private static int disconnect(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        HubConnection connection = HelixBridgeMod.getInstance().getHubConnection();
        
        if (!connection.isConnected()) {
            source.sendFeedback(Text.literal("Not connected to Hub").formatted(Formatting.YELLOW));
            return 0;
        }
        
        connection.disconnect();
        source.sendFeedback(Text.literal("Disconnected from Hub").formatted(Formatting.GREEN));
        return 1;
    }
    
    private static int reloadConfig(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        HelixBridgeMod.getInstance().reloadConfig();
        source.sendFeedback(Text.literal("Configuration reloaded").formatted(Formatting.GREEN));
        return 1;
    }
    
    private static int showConfig(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        HelixConfig config = HelixBridgeMod.getInstance().getConfig();
        
        source.sendFeedback(Text.literal("=== Helix Hub Config ===").formatted(Formatting.GOLD));
        source.sendFeedback(Text.literal("Hub URL: " + config.getHubUrl()));
        source.sendFeedback(Text.literal("Instance ID: " + (config.getInstanceId().isEmpty() ? "(not set)" : config.getInstanceId())));
        source.sendFeedback(Text.literal("Auth Token: " + (config.getAuthToken().isEmpty() ? "(not set)" : "****")));
        source.sendFeedback(Text.literal("Update Rate: " + config.getUpdateRateMs() + "ms"));
        source.sendFeedback(Text.literal(""));
        source.sendFeedback(Text.literal("Use /helixhub config <option> <value> to change settings"));
        
        return 1;
    }
    
    private static int setToken(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        String value = StringArgumentType.getString(context, "value");
        
        HelixConfig config = HelixBridgeMod.getInstance().getConfig();
        config.setAuthToken(value);
        config.save();
        
        source.sendFeedback(Text.literal("Auth token updated").formatted(Formatting.GREEN));
        return 1;
    }
    
    private static int setInstanceId(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        String value = StringArgumentType.getString(context, "value");
        
        HelixConfig config = HelixBridgeMod.getInstance().getConfig();
        config.setInstanceId(value);
        config.save();
        
        source.sendFeedback(Text.literal("Instance ID updated to: " + value).formatted(Formatting.GREEN));
        return 1;
    }
    
    private static int setHubUrl(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        String value = StringArgumentType.getString(context, "value");
        
        HelixConfig config = HelixBridgeMod.getInstance().getConfig();
        config.setHubUrl(value);
        config.save();
        
        source.sendFeedback(Text.literal("Hub URL updated to: " + value).formatted(Formatting.GREEN));
        source.sendFeedback(Text.literal("Use /helixhub connect to reconnect").formatted(Formatting.YELLOW));
        return 1;
    }
}
