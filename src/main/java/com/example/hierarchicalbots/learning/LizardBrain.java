package com.example.hierarchicalbots.learning;

import com.example.hierarchicalbots.perception.PerceptionSnapshot;
import com.example.hierarchicalbots.planning.GoalIntent;
import java.util.Random;
import net.minecraft.util.math.Vec3d;

public class LizardBrain {
    private final Random random = new Random();
    private double explorationRate = 1.0;
    private double learningProgress;

    public MotorCommand decide(PerceptionSnapshot snapshot, GoalIntent intent) {
        Vec3d baseTarget = intent.target().orElseGet(() -> snapshot.getPosition().add(random.nextDouble() - 0.5, 0.0, random.nextDouble() - 0.5).multiply(4.0));
        Vec3d noise = new Vec3d(random.nextGaussian(), 0.0, random.nextGaussian()).multiply(explorationRate * 0.2);
        Vec3d target = baseTarget.add(noise);
        double speed = 0.8 + (intent.priority() * 0.4);
        double energy = speed * 0.1;
        return new MotorCommand(target, speed, energy);
    }

    public void applyReward(MotorCommand command, double reward) {
        explorationRate = Math.max(0.1, explorationRate - reward * 0.01);
        learningProgress = Math.min(1.0, learningProgress + Math.abs(reward) * 0.005);
    }

    public double getLearningProgress() {
        return learningProgress;
    }

    public record MotorCommand(Vec3d target, double speed, double energyCost) {
    }
}
