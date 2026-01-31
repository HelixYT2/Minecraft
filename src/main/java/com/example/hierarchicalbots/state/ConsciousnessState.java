package com.example.hierarchicalbots.state;

import com.example.hierarchicalbots.perception.PerceptionSnapshot;
import com.example.hierarchicalbots.social.SocialMessage;
import java.util.List;
import net.minecraft.util.math.Vec3d;

public class ConsciousnessState {
    private final String agentName;
    private Vec3d lastKnownPosition;
    private int perceivedBlocks;
    private int perceivedEntities;
    private int socialMessages;
    private double learningProgress;
    private int knowledgeEntries;

    public ConsciousnessState(String agentName) {
        this.agentName = agentName;
    }

    public void updatePerception(PerceptionSnapshot snapshot) {
        this.lastKnownPosition = snapshot.getPosition();
        this.perceivedBlocks = snapshot.getNearbyBlocks().size();
        this.perceivedEntities = snapshot.getNearbyEntities().size();
    }

    public void updateSocial(List<SocialMessage> messages) {
        this.socialMessages = messages.size();
    }

    public void updateLearning(double learningProgress, int knowledgeEntries) {
        this.learningProgress = learningProgress;
        this.knowledgeEntries = knowledgeEntries;
    }

    public String getAgentName() {
        return agentName;
    }

    public Vec3d getLastKnownPosition() {
        return lastKnownPosition;
    }

    public int getPerceivedBlocks() {
        return perceivedBlocks;
    }

    public int getPerceivedEntities() {
        return perceivedEntities;
    }

    public int getSocialMessages() {
        return socialMessages;
    }

    public double getLearningProgress() {
        return learningProgress;
    }

    public int getKnowledgeEntries() {
        return knowledgeEntries;
    }
}
