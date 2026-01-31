package com.example.hierarchicalbots.learning;

import com.example.hierarchicalbots.perception.PerceptionSnapshot;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class LizardBrain {
    private static final double LEARNING_RATE = 0.2;
    private static final double DISCOUNT = 0.9;

    private final Random random = new Random();
    private final Map<StateActionKey, Double> actionWeights = new HashMap<>();
    private double explorationRate = 0.6;
    private double learningProgress;

    public MotorDecision decide(PerceptionSnapshot snapshot, PrefrontalCortex.Plan plan) {
        StateKey stateKey = StateKey.from(snapshot);
        MotorAction action = selectAction(stateKey);
        return new MotorDecision(stateKey, action, plan.desiredMovement(), plan.targetCoordinate());
    }

    public void updateWeights(StateKey stateKey, MotorAction action, double reward, StateKey nextState) {
        StateActionKey key = new StateActionKey(stateKey, action);
        double current = actionWeights.getOrDefault(key, 0.0);
        double maxNext = maxQ(nextState);
        double updated = current + LEARNING_RATE * (reward + DISCOUNT * maxNext - current);
        actionWeights.put(key, updated);
        explorationRate = Math.max(0.05, explorationRate * 0.995);
        learningProgress = Math.min(1.0, learningProgress + Math.abs(reward) * 0.01);
    }

    public double calculateReward(BlockPos spawnPos, Vec3d lastPosition, Vec3d currentPosition, float lastHealth, float currentHealth) {
        double reward = 0.0;
        if (lastPosition != null && currentPosition != null) {
            double lastDistance = lastPosition.distanceTo(Vec3d.ofCenter(spawnPos));
            double currentDistance = currentPosition.distanceTo(Vec3d.ofCenter(spawnPos));
            if (currentDistance > lastDistance) {
                reward += 1.0;
            }
        }
        if (currentHealth < lastHealth) {
            reward -= 0.5;
        }
        return reward;
    }

    public double getLearningProgress() {
        return learningProgress;
    }

    public Map<StateActionKey, Double> getActionWeights() {
        return Map.copyOf(actionWeights);
    }

    private MotorAction selectAction(StateKey stateKey) {
        if (random.nextDouble() < explorationRate || actionWeights.isEmpty()) {
            MotorAction[] actions = MotorAction.values();
            return actions[random.nextInt(actions.length)];
        }
        double bestValue = Double.NEGATIVE_INFINITY;
        MotorAction bestAction = MotorAction.MOVE_FORWARD;
        for (MotorAction action : MotorAction.values()) {
            double value = actionWeights.getOrDefault(new StateActionKey(stateKey, action), 0.0);
            if (value > bestValue) {
                bestValue = value;
                bestAction = action;
            }
        }
        return bestAction;
    }

    private double maxQ(StateKey stateKey) {
        double bestValue = Double.NEGATIVE_INFINITY;
        for (MotorAction action : MotorAction.values()) {
            double value = actionWeights.getOrDefault(new StateActionKey(stateKey, action), 0.0);
            bestValue = Math.max(bestValue, value);
        }
        return bestValue == Double.NEGATIVE_INFINITY ? 0.0 : bestValue;
    }

    public enum MotorAction {
        MOVE_FORWARD,
        JUMP,
        ROTATE_YAW,
        ATTACK
    }

    public record MotorDecision(StateKey stateKey, MotorAction action, Vec3d desiredMovement, Vec3d targetCoordinate) {
    }

    public record StateKey(int blockBucket, int entityBucket) {
        public static StateKey from(PerceptionSnapshot snapshot) {
            int blockBucket = Math.min(snapshot.getNearbyBlocks().size() / 50, 5);
            int entityBucket = Math.min(snapshot.getNearbyEntities().size(), 5);
            return new StateKey(blockBucket, entityBucket);
        }
    }

    public record StateActionKey(StateKey stateKey, MotorAction action) {
    }
}
