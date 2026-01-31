package com.example.hierarchicalbots.planning;

import java.util.Optional;
import net.minecraft.util.math.Vec3d;

public record GoalIntent(String goal, Optional<Vec3d> target, double priority) {
    public GoalIntent {
        if (goal == null || goal.isBlank()) {
            throw new IllegalArgumentException("goal must be non-empty");
        }
        if (target == null) {
            target = Optional.empty();
        }
    }

    public static GoalIntent of(String goal, Vec3d target, double priority) {
        return new GoalIntent(goal, Optional.ofNullable(target), priority);
    }

    public static GoalIntent idle() {
        return new GoalIntent("IDLE", Optional.empty(), 0.0);
    }
}
