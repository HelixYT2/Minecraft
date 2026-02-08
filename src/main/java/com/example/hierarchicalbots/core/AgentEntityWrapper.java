package com.example.hierarchicalbots.core;

import java.util.Optional;
import com.example.hierarchicalbots.entity.AgentEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class AgentEntityWrapper {
    private final Identifier agentId;
    private final ServerWorld world;
    private AgentEntity agentEntity;
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

    public Optional<AgentEntity> getPlayerEntity() {
        return Optional.ofNullable(agentEntity);
    }

    public void bindPlayerEntity(AgentEntity agentEntity) {
        this.agentEntity = agentEntity;
    }

    public Vec3d getPosition() {
        if (agentEntity != null) {
            return agentEntity.getPos();
        }
        return fallbackPosition;
    }

    public void setFallbackPosition(Vec3d position) {
        this.fallbackPosition = position;
    }

    public BlockPos getBlockPos() {
        return BlockPos.ofFloored(getPosition());
    }

    public Optional<AgentEntity> getEntity() {
        return Optional.ofNullable(agentEntity);
    }
}
