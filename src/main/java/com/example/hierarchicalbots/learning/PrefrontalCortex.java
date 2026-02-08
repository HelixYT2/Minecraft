package com.example.hierarchicalbots.learning;

import com.example.hierarchicalbots.perception.PerceptionSnapshot;
import com.example.hierarchicalbots.planning.GoalIntent;
import com.example.hierarchicalbots.planning.HighLevelPlanner;
import com.example.hierarchicalbots.planning.HttpLLMPlanner;
import com.example.hierarchicalbots.planning.StubPlanner;
import com.example.hierarchicalbots.social.SocialMessage;
import java.net.URI;
import java.util.List;

public class PrefrontalCortex {
    private final VectorMemoryStore memoryStore;
    private final HighLevelPlanner planner;

    public PrefrontalCortex(VectorMemoryStore memoryStore) {
        this.memoryStore = memoryStore;
        this.planner = buildPlanner(memoryStore);
    }

    public GoalIntent plan(PerceptionSnapshot snapshot, List<SocialMessage> messages) {
        return planner.plan(snapshot, messages);
    }

    public void learnFromOutcome(PerceptionSnapshot snapshot, LizardBrain.MotorCommand command, double reward) {
        double[] vector = memoryStore.encodeOutcome(snapshot, command, reward);
        memoryStore.store("outcome", vector);
    }

    public int getKnowledgeSize() {
        return memoryStore.getVectorCount();
    }

    private HighLevelPlanner buildPlanner(VectorMemoryStore memoryStore) {
        String endpoint = System.getenv("OPENARTEMIS_LLM_ENDPOINT");
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = System.getProperty("openartemis.llm.endpoint");
        }
        if (endpoint != null && !endpoint.isBlank()) {
            return new HttpLLMPlanner(URI.create(endpoint));
        }
        return new StubPlanner(memoryStore);
    }
}
