package com.example.hierarchicalbots.core;

import com.example.hierarchicalbots.HierarchicalBotsMod;
import com.example.hierarchicalbots.learning.VectorMemoryStore;
import com.example.hierarchicalbots.social.SocialLayer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private static final int BOT_COUNT = 50;
    private static final int MAX_BOTS = 50;
    private static final double SPAWN_RADIUS = 15.0;
    private static final int REPRODUCTION_TICKS = 200;
    private static final double REPRODUCTION_RADIUS = 2.0;
    private static final double ENERGY_THRESHOLD = 0.8;

    private final MinecraftServer server;
    private final List<BotAgent> agents = new ArrayList<>();
    private final SocialLayer socialLayer = new SocialLayer();
    private final Map<String, Integer> proximityTicks = new HashMap<>();
    private final Random random = new Random();
    private VectorMemoryStore memoryStore;
    private int nextBotIndex;

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
            spawnAgent(world, spawnPos, GeneticTraits.random(random));
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
            spawnAgent(world, spawnPos.add(i * 2, 0, 0), GeneticTraits.random(random));
        }
    }

    public void tick() {
        for (BotAgent agent : agents) {
            agent.tick(agents);
        }
        handleReproduction();
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

    private void spawnAgent(ServerWorld world, BlockPos spawnPos, GeneticTraits traits) {
        Identifier id = Identifier.of(HierarchicalBotsMod.MOD_ID, "bot_" + nextBotIndex);
        UUID uuid = new UUID(0L, nextBotIndex + 1L);
        Path inventoryPath = server.getSavePath(WorldSavePath.ROOT)
            .resolve("hierarchical-bots")
            .resolve("inventories")
            .resolve(id.getPath() + ".dat");
        AgentEntityWrapper wrapper = new AgentEntityWrapper(id, world, uuid, spawnPos, inventoryPath);
        FakePlayer fakePlayer = wrapper.spawn();
        fakePlayer.refreshPositionAndAngles(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0.0f, 0.0f);
        world.spawnEntity(fakePlayer);
        fakePlayer.networkHandler.onPlayerJoin();
        BotAgent agent = new BotAgent(id, wrapper, memoryStore, socialLayer, traits);
        agents.add(agent);
        nextBotIndex += 1;
    }

    private void handleReproduction() {
        if (agents.size() >= MAX_BOTS) {
            return;
        }
        for (int i = 0; i < agents.size(); i++) {
            for (int j = i + 1; j < agents.size(); j++) {
                BotAgent a = agents.get(i);
                BotAgent b = agents.get(j);
                if (a.getEnergy() < ENERGY_THRESHOLD || b.getEnergy() < ENERGY_THRESHOLD) {
                    proximityTicks.remove(pairKey(a, b));
                    continue;
                }
                double distance = a.getWrapper().getPosition().distanceTo(b.getWrapper().getPosition());
                if (distance <= REPRODUCTION_RADIUS) {
                    int ticks = proximityTicks.getOrDefault(pairKey(a, b), 0) + 1;
                    proximityTicks.put(pairKey(a, b), ticks);
                    if (ticks >= REPRODUCTION_TICKS && agents.size() < MAX_BOTS) {
                        spawnOffspring(a, b);
                        proximityTicks.put(pairKey(a, b), 0);
                    }
                } else {
                    proximityTicks.remove(pairKey(a, b));
                }
            }
        }
    }

    private void spawnOffspring(BotAgent parentA, BotAgent parentB) {
        ServerWorld world = parentA.getWrapper().getWorld();
        Vec3d midpoint = parentA.getWrapper().getPosition().add(parentB.getWrapper().getPosition()).multiply(0.5);
        BlockPos spawnPos = BlockPos.ofFloored(midpoint.x + random.nextDouble(), midpoint.y, midpoint.z + random.nextDouble());
        GeneticTraits childTraits = GeneticTraits.fromParents(parentA.getTraits(), parentB.getTraits(), random);
        parentA.drainEnergy(0.4);
        parentB.drainEnergy(0.4);
        spawnAgent(world, spawnPos, childTraits);
    }

    private String pairKey(BotAgent a, BotAgent b) {
        return a.getAgentId().toString().compareTo(b.getAgentId().toString()) < 0
            ? a.getAgentId() + ":" + b.getAgentId()
            : b.getAgentId() + ":" + a.getAgentId();
    }

    private void resetAgents() {
        for (BotAgent agent : agents) {
            agent.getWrapper().saveInventory();
            agent.getWrapper().getEntity().ifPresent(entity -> entity.remove(net.minecraft.entity.Entity.RemovalReason.DISCARDED));
        }
        agents.clear();
        proximityTicks.clear();
        nextBotIndex = 0;
    }

    private void ensureMemoryStore() {
        if (memoryStore == null) {
            memoryStore = new VectorMemoryStore(server.getSavePath(WorldSavePath.ROOT));
            memoryStore.load();
        }
    }
}
