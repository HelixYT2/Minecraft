package com.example.hierarchicalbots.core;

import com.example.hierarchicalbots.learning.LizardBrain;
import com.example.hierarchicalbots.learning.PrefrontalCortex;
import com.example.hierarchicalbots.learning.VectorMemoryStore;
import com.example.hierarchicalbots.perception.PerceptionSensor;
import com.example.hierarchicalbots.perception.PerceptionSnapshot;
import com.example.hierarchicalbots.social.SocialLayer;
import com.example.hierarchicalbots.social.SocialMessage;
import com.example.hierarchicalbots.state.ConsciousnessState;
import java.util.List;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class BotAgent {
    private final Identifier agentId;
    private final AgentEntityWrapper wrapper;
    private final PerceptionSensor perceptionSensor;
    private final PrefrontalCortex prefrontalCortex;
    private final LizardBrain lizardBrain;
    private final SocialLayer socialLayer;
    private final ConsciousnessState consciousnessState;

    public BotAgent(Identifier agentId,
                    AgentEntityWrapper wrapper,
                    VectorMemoryStore memoryStore,
                    SocialLayer socialLayer) {
        this.agentId = agentId;
        this.wrapper = wrapper;
        this.perceptionSensor = new PerceptionSensor();
        this.prefrontalCortex = new PrefrontalCortex(memoryStore);
        this.lizardBrain = new LizardBrain();
        this.socialLayer = socialLayer;
        this.consciousnessState = new ConsciousnessState(agentId.toString());
    }

    public Identifier getAgentId() {
        return agentId;
    }

    public ConsciousnessState getConsciousnessState() {
        return consciousnessState;
    }

    public void tick(List<BotAgent> allAgents) {
        PerceptionSnapshot snapshot = perceptionSensor.capture(wrapper);
        consciousnessState.updatePerception(snapshot);

        List<SocialMessage> messages = socialLayer.pullMessagesFor(agentId, wrapper.getPosition(), allAgents);
        consciousnessState.updateSocial(messages);

        PrefrontalCortex.Plan plan = prefrontalCortex.plan(snapshot, messages);
        LizardBrain.MotorCommand command = lizardBrain.decide(snapshot, plan);

        executeMotorCommand(command);
        double reward = evaluateReward(snapshot, command);
        lizardBrain.applyReward(command, reward);
        prefrontalCortex.learnFromOutcome(snapshot, command, reward);

        consciousnessState.updateLearning(lizardBrain.getLearningProgress(), prefrontalCortex.getKnowledgeSize());
    }

    private void executeMotorCommand(LizardBrain.MotorCommand command) {
        Vec3d movement = command.movement();
        wrapper.getPlayerEntity().ifPresent(player -> player.addVelocity(movement.x, movement.y, movement.z));
    }

    private double evaluateReward(PerceptionSnapshot snapshot, LizardBrain.MotorCommand command) {
        return snapshot.getNearbyEntities().size() * 0.1 + command.energyCost() * -0.05;
    }
}
