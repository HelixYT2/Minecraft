package com.example.hierarchicalbots.core;

import com.example.hierarchicalbots.HierarchicalBotsMod;
import com.example.hierarchicalbots.learning.VectorMemoryStore;
import com.example.hierarchicalbots.social.SocialLayer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

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
        memoryStore = new VectorMemoryStore(server.getSavePath(net.minecraft.util.WorldSavePath.ROOT));
        memoryStore.load();

        for (int i = 0; i < BOT_COUNT; i++) {
            Identifier id = new Identifier(HierarchicalBotsMod.MOD_ID, "bot_" + i);
            AgentEntityWrapper wrapper = new AgentEntityWrapper(id, world);
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
