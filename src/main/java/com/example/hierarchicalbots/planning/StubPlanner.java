package com.example.hierarchicalbots.planning;

import com.example.hierarchicalbots.learning.VectorMemoryStore;
import com.example.hierarchicalbots.perception.PerceptionSnapshot;
import com.example.hierarchicalbots.social.SocialMessage;
import java.util.List;
import java.util.Random;
import net.minecraft.util.math.Vec3d;

public class StubPlanner implements HighLevelPlanner {
    private final VectorMemoryStore memoryStore;
    private final Random random = new Random();

    public StubPlanner(VectorMemoryStore memoryStore) {
        this.memoryStore = memoryStore;
    }

    @Override
    public GoalIntent plan(PerceptionSnapshot snapshot, List<SocialMessage> messages) {
        double[] contextVector = memoryStore.encodeSnapshot(snapshot, messages);
        memoryStore.store("context", contextVector);

        Vec3d wander = new Vec3d(
            random.nextDouble() - 0.5,
            0.0,
            random.nextDouble() - 0.5
        ).normalize();
        Vec3d target = snapshot.getPosition().add(wander.multiply(6.0));

        if (!snapshot.getNearbyEntities().isEmpty()) {
            target = snapshot.getNearbyEntities().get(0).position();
            return GoalIntent.of("INVESTIGATE_ENTITY", target, 0.6);
        }

        return GoalIntent.of("WANDER", target, 0.3);
    }
}
