package com.example.hierarchicalbots.core;

import java.util.Optional;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class AgentEntityWrapper {
    private final Identifier agentId;
    private final ServerWorld world;
    private ServerPlayerEntity playerEntity;
    private Vec3d fallbackPosition = Vec3d.ZERO;

    public AgentEntityWrapper(Identifier agentId, ServerWorld world) {
        this.agentId = agentId;
        this.world = world;
    }

    public Identifier getAgentId() {
        return agentId;
    }

    public ServerWorld getWorld() {
        return world;
    }

    public Optional<ServerPlayerEntity> getPlayerEntity() {
        return Optional.ofNullable(playerEntity);
    }

    public void bindPlayerEntity(ServerPlayerEntity playerEntity) {
        this.playerEntity = playerEntity;
    }

    public Vec3d getPosition() {
        if (playerEntity != null) {
            return playerEntity.getPos();
        }
        return fallbackPosition;
    }

    public void setFallbackPosition(Vec3d position) {
        this.fallbackPosition = position;
    }

    public BlockPos getBlockPos() {
        return BlockPos.ofFloored(getPosition());
    }

    public Optional<Entity> getEntity() {
        return Optional.ofNullable(playerEntity);
    }
}
