package com.example.hierarchicalbots.learning;

import com.example.hierarchicalbots.perception.PerceptionSnapshot;
import java.util.Random;
import net.minecraft.util.math.Vec3d;

public class LizardBrain {
    private final Random random = new Random();
    private double explorationRate = 1.0;
    private double learningProgress;

    public MotorCommand decide(PerceptionSnapshot snapshot, PrefrontalCortex.Plan plan) {
        Vec3d desired = plan.desiredMovement();
        Vec3d noise = new Vec3d(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).multiply(explorationRate * 0.05);
        Vec3d movement = desired.add(noise).multiply(0.1);
        double energy = movement.length();
        return new MotorCommand(movement, energy);
    }

    public void applyReward(MotorCommand command, double reward) {
        explorationRate = Math.max(0.1, explorationRate - reward * 0.01);
        learningProgress = Math.min(1.0, learningProgress + Math.abs(reward) * 0.005);
    }

    public double getLearningProgress() {
        return learningProgress;
    }

    public record MotorCommand(Vec3d movement, double energyCost) {
    }
}
