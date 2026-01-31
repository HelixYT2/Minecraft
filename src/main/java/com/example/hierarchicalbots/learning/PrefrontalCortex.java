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
        Vec3d direction = new Vec3d(
            snapshot.getNearbyBlocks().size() % 3 - 1,
            snapshot.getNearbyEntities().size() % 2,
            (snapshot.getNearbyBlocks().size() / 3) % 3 - 1
        ).normalize();
        return new Plan(direction);
    }

    public void learnFromOutcome(PerceptionSnapshot snapshot, LizardBrain.MotorCommand command, double reward) {
        double[] vector = memoryStore.encodeOutcome(snapshot, command, reward);
        memoryStore.store("outcome", vector);
    }

    public int getKnowledgeSize() {
        return memoryStore.getVectorCount();
    }

    public record Plan(Vec3d desiredMovement) {
    }
}
