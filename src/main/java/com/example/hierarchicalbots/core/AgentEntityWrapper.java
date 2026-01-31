package com.example.hierarchicalbots.core;

import java.util.Optional;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class AgentEntityWrapper {
    private final Identifier agentId;
    private final ServerWorld world;
    private final BlockPos spawnPos;
    private HumanAgentEntity entity;

    public AgentEntityWrapper(Identifier agentId, ServerWorld world, BlockPos spawnPos) {
        this.agentId = agentId;
        this.world = world;
        this.spawnPos = spawnPos;
    }

    public Identifier getAgentId() {
        return agentId;
    }

    public ServerWorld getWorld() {
        return world;
    }

    public BlockPos getSpawnPos() {
        return spawnPos;
    }

    public void bindEntity(HumanAgentEntity entity) {
        this.entity = entity;
    }

    public Vec3d getPosition() {
        return entity != null ? entity.getPos() : Vec3d.ofCenter(spawnPos);
    }

    public BlockPos getBlockPos() {
        return BlockPos.ofFloored(getPosition());
    }

    public Optional<Entity> getEntity() {
        return Optional.ofNullable(entity);
    }

    public Optional<LivingEntity> getLivingEntity() {
        return Optional.ofNullable(entity);
    }

    public float getHealth() {
        return entity != null ? entity.getHealth() : 20.0f;
    }

    public void addVelocity(Vec3d movement) {
        if (entity != null) {
            entity.addVelocity(movement.x, movement.y, movement.z);
        }
    }

    public void jump() {
        if (entity != null) {
            entity.jump();
        }
    }

    public void rotateYaw(float yawDelta) {
        if (entity != null) {
            float newYaw = entity.getYaw() + yawDelta;
            entity.setYaw(newYaw);
            entity.setHeadYaw(newYaw);
        }
    }

    public boolean isOnGround() {
        return entity != null && entity.isOnGround();
    }

    public float getFallDistance() {
        return entity != null ? entity.fallDistance : 0.0f;
    }

    public boolean hasHorizontalCollision() {
        return entity != null && entity.horizontalCollision;
    }

    public void attackNearestEntity(double range) {
        if (entity == null) {
            return;
        }
        Vec3d center = entity.getPos();
        Entity nearest = world.getOtherEntities(entity, entity.getBoundingBox().expand(range)).stream()
            .filter(target -> target instanceof LivingEntity)
            .min((a, b) -> Double.compare(a.squaredDistanceTo(center), b.squaredDistanceTo(center)))
            .orElse(null);
        if (nearest instanceof LivingEntity living) {
            entity.tryAttack(living);
        }
    }
}
