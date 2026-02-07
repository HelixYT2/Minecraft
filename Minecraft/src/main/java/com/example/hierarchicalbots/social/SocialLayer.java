package com.example.hierarchicalbots.social;

import com.example.hierarchicalbots.core.BotAgent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class SocialLayer {
    private static final double CHAT_RADIUS = 12.0;

    private final Map<Identifier, List<SocialMessage>> messageQueues = new ConcurrentHashMap<>();

    public void broadcast(Identifier from, Vec3d position, String content, List<BotAgent> agents) {
        SocialMessage message = new SocialMessage(from, content, System.currentTimeMillis());
        for (BotAgent agent : agents) {
            if (agent.getAgentId().equals(from)) {
                continue;
            }
            Vec3d otherPosition = agent.getConsciousnessState().getLastKnownPosition();
            if (otherPosition != null && otherPosition.isInRange(position, CHAT_RADIUS)) {
                messageQueues.computeIfAbsent(agent.getAgentId(), key -> new ArrayList<>()).add(message);
            }
        }
    }

    public List<SocialMessage> pullMessagesFor(Identifier agentId, Vec3d position, List<BotAgent> agents) {
        messageQueues.computeIfAbsent(agentId, key -> new ArrayList<>());
        List<SocialMessage> messages = new ArrayList<>(messageQueues.get(agentId));
        if (Math.random() < 0.02) {
            broadcast(agentId, position, "Exploring sector " + position.toString(), agents);
        }
        messageQueues.get(agentId).clear();
        return messages;
    }

    public void flushTick() {
        messageQueues.forEach((id, queue) -> queue.removeIf(message -> false));
    }
}
