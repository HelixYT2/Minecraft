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
    private static final int LEARNING_INTERVAL_TICKS = 20;
    private static final int NEED_INTERVAL_TICKS = 100;
    private static final double MOVE_SPEED = 0.12;
    private static final double ROTATION_STEP = 15.0;

    private final Identifier agentId;
    private final AgentEntityWrapper wrapper;
    private final PerceptionSensor perceptionSensor;
    private final PrefrontalCortex prefrontalCortex;
    private final LizardBrain lizardBrain;
    private final SocialLayer socialLayer;
    private final ConsciousnessState consciousnessState;

    private int tickCounter;
    private double accumulatedReward;
    private Vec3d lastPosition;
    private float lastHealth = 20.0f;
    private LizardBrain.MotorDecision lastDecision;
    private PrefrontalCortex.BiologicalNeed currentNeed = PrefrontalCortex.BiologicalNeed.EXPLORE;

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

    public AgentEntityWrapper getWrapper() {
        return wrapper;
    }

    public ConsciousnessState getConsciousnessState() {
        return consciousnessState;
    }

    public void tick(List<BotAgent> allAgents) {
        PerceptionSnapshot snapshot = perceptionSensor.capture(wrapper);
        consciousnessState.updatePerception(snapshot);

        socialLayer.exchangeIfClose(this, allAgents);
        List<SocialMessage> messages = socialLayer.pullMessagesFor(agentId);
        consciousnessState.updateSocial(messages);

        if (tickCounter % NEED_INTERVAL_TICKS == 0) {
            currentNeed = prefrontalCortex.pickNeed();
            consciousnessState.updateNeed(currentNeed, tickCounter / NEED_INTERVAL_TICKS);
        }

        boolean repath = wrapper.getPlayerEntity().map(player -> player.horizontalCollision).orElse(false)
            || wrapper.getPlayerEntity().map(player -> !player.isOnGround() && player.fallDistance > 2.5f).orElse(false);
        PrefrontalCortex.Plan plan = prefrontalCortex.plan(snapshot, messages, currentNeed, repath);
        LizardBrain.MotorDecision decision = lastDecision;
        if (tickCounter % LEARNING_INTERVAL_TICKS == 0 || decision == null) {
            decision = lizardBrain.decide(snapshot, plan);
        }

        executeMotorDecision(decision);

        boolean hitWall = wrapper.getPlayerEntity().map(player -> player.horizontalCollision).orElse(false);
        boolean fell = wrapper.getPlayerEntity().map(player -> !player.isOnGround() && player.fallDistance > 2.5f).orElse(false);

        double reward = lizardBrain.calculateReward(
            wrapper.getSpawnPos(),
            lastPosition,
            wrapper.getPosition(),
            lastHealth,
            wrapper.getHealth(),
            hitWall,
            fell
        );
        accumulatedReward += reward;

        if (lastDecision != null && tickCounter % LEARNING_INTERVAL_TICKS == 0) {
            LizardBrain.StateKey nextState = LizardBrain.StateKey.from(snapshot);
            lizardBrain.updateWeights(lastDecision.stateKey(), lastDecision.action(), accumulatedReward, nextState);
            accumulatedReward = 0.0;
        }

        prefrontalCortex.learnFromOutcome(snapshot, decision, reward);
        consciousnessState.updateLearning(lizardBrain.getLearningProgress(), prefrontalCortex.getKnowledgeSize(), lizardBrain.getSurprisePain());

        lastDecision = decision;
        lastPosition = wrapper.getPosition();
        lastHealth = wrapper.getHealth();
        tickCounter++;
    }

    private void executeMotorDecision(LizardBrain.MotorDecision decision) {
        switch (decision.action()) {
            case MOVE_FORWARD -> applyMovement(decision.desiredMovement());
            case JUMP -> wrapper.getPlayerEntity().ifPresent(net.minecraft.server.network.ServerPlayerEntity::jump);
            case ROTATE_YAW -> wrapper.getPlayerEntity().ifPresent(player -> {
                float newYaw = (float) (player.getYaw() + ROTATION_STEP);
                player.setYaw(newYaw);
                player.setHeadYaw(newYaw);
            });
            case ATTACK -> wrapper.attackNearestEntity(2.5);
        }
    }

    private void applyMovement(Vec3d desiredMovement) {
        Vec3d direction = desiredMovement.lengthSquared() > 0 ? desiredMovement.normalize() : Vec3d.ZERO;
        Vec3d movement = direction.multiply(MOVE_SPEED);
        wrapper.getPlayerEntity().ifPresent(player -> player.addVelocity(movement.x, movement.y, movement.z));
    }
}
