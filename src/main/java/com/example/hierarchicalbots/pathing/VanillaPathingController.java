package com.example.hierarchicalbots.pathing;

import com.example.hierarchicalbots.entity.AgentEntity;
import net.minecraft.util.math.Vec3d;

public class VanillaPathingController implements PathingController {
    private static final int REPLAN_TICKS = 10;

    private final AgentEntity entity;
    private Vec3d target;
    private double speed = 1.0;
    private int tickCounter = 0;

    public VanillaPathingController(AgentEntity entity) {
        this.entity = entity;
    }

    @Override
    public void setTarget(Vec3d target, double speed) {
        this.target = target;
        this.speed = speed;
    }

    @Override
    public void tick() {
        if (target == null || entity.getNavigation() == null) {
            return;
        }
        if (tickCounter++ % REPLAN_TICKS == 0) {
            entity.getNavigation().startMovingTo(target.x, target.y, target.z, speed);
        }
    }

    @Override
    public void stop() {
        if (entity.getNavigation() != null) {
            entity.getNavigation().stop();
        }
        target = null;
    }
}
