package com.example.hierarchicalbots.core;

import com.example.hierarchicalbots.HierarchicalBotsMod;
import com.example.hierarchicalbots.entity.AgentEntities;
import com.example.hierarchicalbots.entity.AgentEntity;
import com.example.hierarchicalbots.learning.VectorMemoryStore;
import com.example.hierarchicalbots.social.SocialLayer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

public class BotManager {
    private static final int BOT_COUNT = 50;
    private static final int SPAWN_ATTEMPTS = 16;
    private static final int SPAWN_RADIUS = 32;

    private final MinecraftServer server;
    private final List<BotAgent> agents = new ArrayList<>();
    private final SocialLayer socialLayer = new SocialLayer();
    private VectorMemoryStore memoryStore;
    private final Random random = new Random();

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
            spawnAgentEntity(world, wrapper, i);
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

    private void spawnAgentEntity(ServerWorld world, AgentEntityWrapper wrapper, int index) {
        BlockPos spawn = findSafeSpawn(world, world.getSpawnPos());
        AgentEntity entity = new AgentEntity(AgentEntities.AGENT, world);
        entity.setSkinSeed(random.nextInt());
        entity.refreshPositionAndAngles(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, random.nextFloat() * 360.0f, 0.0f);
        entity.applySpawnReset();
        world.spawnEntity(entity);
        wrapper.bindPlayerEntity(entity);
        wrapper.setFallbackPosition(new Vec3d(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5));
        HierarchicalBotsMod.LOGGER.info("Spawned agent {} at {}", index, spawn);
    }

    private BlockPos findSafeSpawn(ServerWorld world, BlockPos centerPos) {
        BlockPos best = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, centerPos).up();
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            int dx = random.nextInt(SPAWN_RADIUS * 2 + 1) - SPAWN_RADIUS;
            int dz = random.nextInt(SPAWN_RADIUS * 2 + 1) - SPAWN_RADIUS;
            BlockPos candidateBase = centerPos.add(dx, 0, dz);
            BlockPos candidate = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, candidateBase).up();
            if (isSafeSpawn(world, candidate)) {
                return candidate;
            }
        }
        return best;
    }

    private boolean isSafeSpawn(ServerWorld world, BlockPos pos) {
        if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
            return false;
        }
        BlockPos below = pos.down();
        return world.getBlockState(below).isSolidBlock(world, below);
    }
}
