package com.example.hierarchicalbots.core;

import com.example.hierarchicalbots.learning.LizardBrain;
import com.example.hierarchicalbots.learning.PrefrontalCortex;
import com.example.hierarchicalbots.learning.VectorMemoryStore;
import com.example.hierarchicalbots.perception.PerceptionSensor;
import com.example.hierarchicalbots.perception.PerceptionSnapshot;
import com.example.hierarchicalbots.social.SocialLayer;
import com.example.hierarchicalbots.social.SocialMessage;
import com.example.hierarchicalbots.state.ConsciousnessState;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class BotAgent {
    private static final int LEARNING_INTERVAL_TICKS = 20;
    private static final int NEED_INTERVAL_TICKS = 100;
    private static final int AGE_INTERVAL_TICKS = 24000;
    private static final int DECAY_INTERVAL_TICKS = 1200;
    private static final int REWARD_INTERVAL_TICKS = 5;
    private static final double BASE_MOVE_SPEED = 0.12;
    private static final double ROTATION_STEP = 15.0;

    private final Identifier agentId;
    private final AgentEntityWrapper wrapper;
    private final PerceptionSensor perceptionSensor;
    private final PrefrontalCortex prefrontalCortex;
    private final LizardBrain lizardBrain;
    private final SocialLayer socialLayer;
    private final ConsciousnessState consciousnessState;
    private final GeneticTraits traits;

    private int tickCounter;
    private int biologicalAge;
    private int decayTicks;
    private int resourcesCollected;
    private double energy = 1.0;
    private double accumulatedReward;
    private Vec3d lastPosition;
    private float lastHealth = 20.0f;
    private LizardBrain.MotorDecision lastDecision;
    private PrefrontalCortex.BiologicalNeed currentNeed = PrefrontalCortex.BiologicalNeed.EXPLORE;

    public BotAgent(Identifier agentId,
                    AgentEntityWrapper wrapper,
                    VectorMemoryStore memoryStore,
                    SocialLayer socialLayer,
                    GeneticTraits traits) {
        this.agentId = agentId;
        this.wrapper = wrapper;
        this.perceptionSensor = new PerceptionSensor();
        this.prefrontalCortex = new PrefrontalCortex(memoryStore);
        this.lizardBrain = new LizardBrain();
        this.socialLayer = socialLayer;
        this.consciousnessState = new ConsciousnessState(agentId.toString());
        this.traits = traits;
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

    public GeneticTraits getTraits() {
        return traits;
    }

    public int getBiologicalAge() {
        return biologicalAge;
    }

    public double getEnergy() {
        return energy;
    }

    public void drainEnergy(double amount) {
        energy = Math.max(0.0, energy - amount);
    }

    public double getSocialStatus() {
        return traits.intelligence() + resourcesCollected * 0.01;
    }

    public int getResourcesCollected() {
        return resourcesCollected;
    }

    public void tick(List<BotAgent> allAgents) {
        PerceptionSnapshot snapshot = perceptionSensor.capture(wrapper);
        consciousnessState.updatePerception(snapshot);

        socialLayer.exchangeIfClose(this, allAgents);
        List<SocialMessage> messages = socialLayer.pullMessagesFor(agentId);
        consciousnessState.updateSocial(messages);

        if (tickCounter % NEED_INTERVAL_TICKS == 0) {
            currentNeed = prefrontalCortex.pickNeed();
            consciousnessState.updateNeed(currentNeed, biologicalAge);
        }

        if (tickCounter % AGE_INTERVAL_TICKS == 0 && tickCounter > 0) {
            biologicalAge += 1;
        }

        if (tickCounter % DECAY_INTERVAL_TICKS == 0 && tickCounter > 0) {
            decayTicks += 1;
        }

        energy = Math.min(1.5, energy + 0.002);
        resourcesCollected += snapshot.getNearbyBlocks().size() / 200;

        Optional<BotAgent> alpha = findAlpha(allAgents);
        Vec3d leadershipVector = alpha
            .filter(a -> a.getSocialStatus() > getSocialStatus())
            .map(a -> a.getWrapper().getPosition().subtract(wrapper.getPosition()))
            .orElse(Vec3d.ZERO);

        boolean repath = wrapper.getPlayerEntity().map(player -> player.horizontalCollision).orElse(false)
            || wrapper.getPlayerEntity().map(player -> !player.isOnGround() && player.fallDistance > 2.5f).orElse(false);
        PrefrontalCortex.Plan plan = prefrontalCortex.plan(snapshot, messages, currentNeed, repath);
        Vec3d desiredMovement = leadershipVector.lengthSquared() > 0
            ? leadershipVector.normalize().multiply(0.6).add(plan.desiredMovement().multiply(0.4))
            : plan.desiredMovement();
        LizardBrain.MotorDecision decision = lastDecision;
        if (tickCounter % LEARNING_INTERVAL_TICKS == 0 || decision == null) {
            decision = lizardBrain.decide(snapshot, plan.withMovement(desiredMovement));
        }

        executeMotorDecision(decision);

        boolean hitWall = wrapper.getPlayerEntity().map(player -> player.horizontalCollision).orElse(false);
        boolean fell = wrapper.getPlayerEntity().map(player -> !player.isOnGround() && player.fallDistance > 2.5f).orElse(false);

        if (tickCounter % REWARD_INTERVAL_TICKS == 0) {
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
        }

        if (lastDecision != null && tickCounter % LEARNING_INTERVAL_TICKS == 0) {
            LizardBrain.StateKey nextState = LizardBrain.StateKey.from(snapshot);
            lizardBrain.updateWeights(lastDecision.stateKey(), lastDecision.action(), accumulatedReward, nextState);
            accumulatedReward = 0.0;
        }

        prefrontalCortex.learnFromOutcome(snapshot, decision, lizardBrain.getSurprisePain());
        consciousnessState.updateLearning(lizardBrain.getLearningProgress(), prefrontalCortex.getKnowledgeSize(), lizardBrain.getSurprisePain());

        lastDecision = decision;
        lastPosition = wrapper.getPosition();
        lastHealth = wrapper.getHealth();
        tickCounter++;
    }

    private Optional<BotAgent> findAlpha(List<BotAgent> agents) {
        Vec3d position = wrapper.getPosition();
        return agents.stream()
            .filter(agent -> agent != this)
            .filter(agent -> agent.getWrapper().getPosition().isInRange(position, 20.0))
            .max(Comparator.comparingDouble(BotAgent::getSocialStatus));
    }

    private void executeMotorDecision(LizardBrain.MotorDecision decision) {
        if (decision == null) {
            return;
        }
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
        double decayPenalty = decayTicks * 0.05;
        double speed = Math.max(0.02, BASE_MOVE_SPEED * traits.speed() - decayPenalty);
        Vec3d movement = direction.multiply(speed);
        energy = Math.max(0.0, energy - 0.01);
        wrapper.getPlayerEntity().ifPresent(player -> player.addVelocity(movement.x, movement.y, movement.z));
    }
}
