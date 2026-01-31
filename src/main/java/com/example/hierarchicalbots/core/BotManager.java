package com.example.hierarchicalbots.core;

import com.example.hierarchicalbots.HierarchicalBotsMod;
import com.example.hierarchicalbots.learning.VectorMemoryStore;
import com.example.hierarchicalbots.social.SocialLayer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BotManager {
    private static final int BOT_COUNT = 10;
    private static final double SPAWN_RADIUS = 6.0;

    private final MinecraftServer server;
    private final List<BotAgent> agents = new ArrayList<>();
    private final SocialLayer socialLayer = new SocialLayer();
    private VectorMemoryStore memoryStore;

    public BotManager(MinecraftServer server) {
        this.server = server;
    }

    public void startSimulation(ServerPlayerEntity commander) {
        commander.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
        ServerWorld world = commander.getServerWorld();
        Vec3d center = commander.getPos();
        resetAgents();
        ensureMemoryStore();

        for (int i = 0; i < BOT_COUNT; i++) {
            double angle = (Math.PI * 2.0 * i) / BOT_COUNT;
            BlockPos spawnPos = BlockPos.ofFloored(
                center.x + Math.cos(angle) * SPAWN_RADIUS,
                center.y,
                center.z + Math.sin(angle) * SPAWN_RADIUS
            );
            spawnAgent(world, spawnPos, i);
        }
    }

    public void bootstrapAgents() {
        if (!agents.isEmpty()) {
            return;
        }
        ServerWorld world = server.getOverworld();
        ensureMemoryStore();
        BlockPos spawnPos = world.getSpawnPos();
        for (int i = 0; i < BOT_COUNT; i++) {
            spawnAgent(world, spawnPos.add(i * 2, 0, 0), i);
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

    private void spawnAgent(ServerWorld world, BlockPos spawnPos, int index) {
        Identifier id = Identifier.of(HierarchicalBotsMod.MOD_ID, "bot_" + index);
        UUID uuid = new UUID(0L, index + 1L);
        Path inventoryPath = server.getSavePath(WorldSavePath.ROOT)
            .resolve("hierarchical-bots")
            .resolve("inventories")
            .resolve(id.getPath() + ".dat");
        AgentEntityWrapper wrapper = new AgentEntityWrapper(id, world, uuid, spawnPos, inventoryPath);
        FakePlayer fakePlayer = wrapper.spawn();
        fakePlayer.refreshPositionAndAngles(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0.0f, 0.0f);
        world.spawnEntity(fakePlayer);
        world.getChunkManager().addEntity(fakePlayer);
        fakePlayer.networkHandler.onPlayerJoin();
        BotAgent agent = new BotAgent(id, wrapper, memoryStore, socialLayer);
        agents.add(agent);
    }

    private void resetAgents() {
        for (BotAgent agent : agents) {
            agent.getWrapper().saveInventory();
            agent.getWrapper().getEntity().ifPresent(entity -> entity.remove(net.minecraft.entity.Entity.RemovalReason.DISCARDED));
        }
        agents.clear();
    }

    private void ensureMemoryStore() {
        if (memoryStore == null) {
            memoryStore = new VectorMemoryStore(server.getSavePath(WorldSavePath.ROOT));
            memoryStore.load();
        }
    }
}
