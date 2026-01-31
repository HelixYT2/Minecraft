package com.example.hierarchicalbots.core;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.ai.pathing.PathAwareEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class HumanAgentEntity extends PathAwareEntity {
    public HumanAgentEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(8, new WanderAroundGoal(this, 0.6));
        goalSelector.add(9, new LookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
    }

    @Override
    public void onSpawn(ServerWorld world) {
        super.onSpawn(world);
    }
}
