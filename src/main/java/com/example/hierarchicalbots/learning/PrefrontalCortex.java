package com.example.hierarchicalbots.learning;

import com.example.hierarchicalbots.perception.PerceptionSnapshot;
import com.example.hierarchicalbots.social.SocialMessage;
import java.util.List;
import java.util.Random;
import net.minecraft.util.math.Vec3d;

public class PrefrontalCortex {
    private final VectorMemoryStore memoryStore;
    private final Random random = new Random();

    public PrefrontalCortex(VectorMemoryStore memoryStore) {
        this.memoryStore = memoryStore;
    }

    public Plan plan(PerceptionSnapshot snapshot,
                     List<SocialMessage> messages,
                     BiologicalNeed need,
                     boolean repath) {
        double[] contextVector = memoryStore.encodeSnapshot(snapshot, messages);
        memoryStore.store("context", contextVector);

        Vec3d target = repath
            ? snapshot.getPosition().add(randomOffset())
            : memoryStore.suggestTarget(snapshot.getPosition())
                .orElseGet(() -> snapshot.getPosition().add(randomOffset()));
        Vec3d direction = target.subtract(snapshot.getPosition());
        Vec3d normalized = direction.lengthSquared() > 0 ? direction.normalize() : new Vec3d(0, 0, 0);
        return new Plan(normalized, target, need);
    }

    public void learnFromOutcome(PerceptionSnapshot snapshot, LizardBrain.MotorDecision decision, double reward) {
        double[] vector = memoryStore.encodeOutcome(snapshot, decision, reward);
        memoryStore.store("outcome", vector);
    }

    public BiologicalNeed pickNeed() {
        BiologicalNeed[] needs = BiologicalNeed.values();
        return needs[random.nextInt(needs.length)];
    }

    public int getKnowledgeSize() {
        return memoryStore.getVectorCount();
    }

    private Vec3d randomOffset() {
        return new Vec3d(random.nextDouble() * 6.0 - 3.0, 0.0, random.nextDouble() * 6.0 - 3.0);
    }

    public enum BiologicalNeed {
        FIND_WOOD,
        SOCIALIZE,
        AVOID_WATER,
        EXPLORE
    }

    public record Plan(Vec3d desiredMovement, Vec3d targetCoordinate, BiologicalNeed need) {
    }
}
