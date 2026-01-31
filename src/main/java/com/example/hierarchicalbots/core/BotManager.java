package com.example.hierarchicalbots.core;

import com.example.hierarchicalbots.HierarchicalBotsMod;
import com.example.hierarchicalbots.learning.VectorMemoryStore;
import com.example.hierarchicalbots.social.SocialLayer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;

public class BotManager {
    private static final int BOT_COUNT = 10;

    private final MinecraftServer server;
    private final List<BotAgent> agents = new ArrayList<>();
    private final SocialLayer socialLayer = new SocialLayer();
    private VectorMemoryStore memoryStore;

    public BotManager(MinecraftServer server) {
        this.server = server;
    }

    public void bootstrapAgents() {
        ServerWorld world = server.getOverworld();
        memoryStore = new VectorMemoryStore(server.getSavePath(WorldSavePath.ROOT));
        memoryStore.load();

        for (int i = 0; i < BOT_COUNT; i++) {
            Identifier id = Identifier.of(HierarchicalBotsMod.MOD_ID, "bot_" + i);
            UUID uuid = new UUID(0L, i + 1L);
            BlockPos spawnPos = world.getSpawnPos().add(i * 2, 0, 0);
            Path inventoryPath = server.getSavePath(WorldSavePath.ROOT)
                .resolve("hierarchical-bots")
                .resolve("inventories")
                .resolve(id.getPath() + ".dat");
            AgentEntityWrapper wrapper = new AgentEntityWrapper(id, world, uuid, spawnPos, inventoryPath);
            wrapper.spawn();
            BotAgent agent = new BotAgent(id, wrapper, memoryStore, socialLayer);
            agents.add(agent);
        }
    }

    public void tick() {
        for (BotAgent agent : agents) {
            agent.tick(agents);
        }
        socialLayer.flushTick();
    }

    public void shutdown() {
        for (BotAgent agent : agents) {
            agent.getWrapper().saveInventory();
        }
        if (memoryStore != null) {
            memoryStore.save();
        }
    }

    public int getAgentCount() {
        return agents.size();
    }

    public List<BotAgent> getAgents() {
        return Collections.unmodifiableList(agents);
    }
}
