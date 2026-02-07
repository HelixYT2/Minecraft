package dev.helix.bridge;

import dev.helix.bridge.baritone.BaritoneIntegration;
import dev.helix.bridge.command.HelixHubCommand;
import dev.helix.bridge.config.HelixConfig;
import dev.helix.bridge.network.HubConnection;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelixBridgeMod implements ClientModInitializer {
    public static final String MOD_ID = "helix-bridge";
    public static final String MOD_VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static HelixBridgeMod instance;
    private HelixConfig config;
    private HubConnection hubConnection;
    private BaritoneIntegration baritoneIntegration;
    private int tickCounter = 0;

    public static HelixBridgeMod getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("Helix Bridge Mod initializing...");

        // Load configuration
        config = HelixConfig.load();
        LOGGER.info("Configuration loaded: Hub URL = {}", config.getHubUrl());

        // Initialize Baritone integration
        baritoneIntegration = new BaritoneIntegration();
        LOGGER.info("Baritone available: {}", baritoneIntegration.isAvailable());

        // Initialize Hub connection
        hubConnection = new HubConnection(config, baritoneIntegration);

        // Register client commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            HelixHubCommand.register(dispatcher);
        });

        // Connect to Hub when joining a world
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            LOGGER.info("Player joined world, connecting to Hub...");
            hubConnection.connect();
        });

        // Disconnect from Hub when leaving a world
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.info("Player left world, disconnecting from Hub...");
            hubConnection.disconnect();
        });

        // Tick events for state updates
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && hubConnection.isConnected()) {
                tickCounter++;
                int updateInterval = config.getUpdateRateMs() / 50; // Convert ms to ticks
                if (updateInterval < 1) updateInterval = 1;
                
                if (tickCounter >= updateInterval) {
                    tickCounter = 0;
                    hubConnection.sendStateUpdate();
                }
            }
        });

        LOGGER.info("Helix Bridge Mod initialized successfully!");
    }

    public HelixConfig getConfig() {
        return config;
    }

    public HubConnection getHubConnection() {
        return hubConnection;
    }

    public BaritoneIntegration getBaritoneIntegration() {
        return baritoneIntegration;
    }

    public void reloadConfig() {
        config = HelixConfig.load();
        LOGGER.info("Configuration reloaded");
    }
}
