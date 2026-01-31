package com.example.hierarchicalbots;

import com.example.hierarchicalbots.core.BotManager;
import com.example.hierarchicalbots.core.HumanAgentEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HierarchicalBotsMod implements ModInitializer {
    public static final String MOD_ID = "hierarchical_bots";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final EntityType<HumanAgentEntity> HUMAN_AGENT_ENTITY_TYPE = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of(MOD_ID, "human_agent"),
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, HumanAgentEntity::new)
            .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
            .build()
    );
    private static BotManager botManager;

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(HUMAN_AGENT_ENTITY_TYPE, HumanAgentEntity.createAttributes());
        SimulationCommand.register();
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
