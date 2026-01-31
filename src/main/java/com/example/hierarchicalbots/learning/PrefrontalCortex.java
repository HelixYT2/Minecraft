package com.example.hierarchicalbots.learning;

import com.example.hierarchicalbots.perception.PerceptionSnapshot;
import com.example.hierarchicalbots.social.SocialMessage;
import java.util.List;
import net.minecraft.util.math.Vec3d;

public class PrefrontalCortex {
    private final VectorMemoryStore memoryStore;

    public PrefrontalCortex(VectorMemoryStore memoryStore) {
        this.memoryStore = memoryStore;
    }

    public Plan plan(PerceptionSnapshot snapshot, List<SocialMessage> messages) {
        double[] contextVector = memoryStore.encodeSnapshot(snapshot, messages);
        memoryStore.store("context", contextVector);

        Vec3d target = memoryStore.suggestTarget(snapshot.getPosition())
            .orElseGet(() -> snapshot.getPosition().add(4.0, 0.0, 4.0));
        Vec3d direction = target.subtract(snapshot.getPosition());
        Vec3d normalized = direction.lengthSquared() > 0 ? direction.normalize() : new Vec3d(0, 0, 0);
        return new Plan(normalized, target);
    }

    public void learnFromOutcome(PerceptionSnapshot snapshot, LizardBrain.MotorDecision decision, double reward) {
        double[] vector = memoryStore.encodeOutcome(snapshot, decision, reward);
        memoryStore.store("outcome", vector);
    }

    public int getKnowledgeSize() {
        return memoryStore.getVectorCount();
    }

    public record Plan(Vec3d desiredMovement, Vec3d targetCoordinate) {
    }
}
