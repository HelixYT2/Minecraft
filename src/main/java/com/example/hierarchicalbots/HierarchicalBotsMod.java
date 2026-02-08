package com.example.hierarchicalbots;

import com.example.hierarchicalbots.core.BotManager;
import com.example.hierarchicalbots.entity.AgentEntities;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HierarchicalBotsMod implements ModInitializer {
    public static final String MOD_ID = "hierarchical_bots";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static BotManager botManager;

    @Override
    public void onInitialize() {
        AgentEntities.registerAttributes();
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);
    }

    private void onServerStarted(MinecraftServer server) {
        botManager = new BotManager(server);
        botManager.bootstrapAgents();
        LOGGER.info("Initialized {} hierarchical bots.", botManager.getAgentCount());
    }

    private void onServerStopping(MinecraftServer server) {
        if (botManager != null) {
            botManager.shutdown();
        }
    }

    private void onServerTick(MinecraftServer server) {
        if (botManager != null) {
            botManager.tick();
        }
    }

    public static BotManager getBotManager() {
        return botManager;
    }
}
