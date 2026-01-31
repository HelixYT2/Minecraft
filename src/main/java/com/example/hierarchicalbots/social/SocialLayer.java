package com.example.hierarchicalbots.social;

import com.example.hierarchicalbots.HierarchicalBotsMod;
import com.example.hierarchicalbots.core.BotAgent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class SocialLayer {
    private static final double CHAT_RADIUS = 3.0;

    private final Map<Identifier, List<SocialMessage>> messageQueues = new ConcurrentHashMap<>();

    public void exchangeIfClose(BotAgent agent, List<BotAgent> agents) {
        Vec3d position = agent.getConsciousnessState().getLastKnownPosition();
        if (position == null) {
            return;
        }
        for (BotAgent other : agents) {
            if (other == agent) {
                continue;
            }
            Vec3d otherPosition = other.getConsciousnessState().getLastKnownPosition();
            if (otherPosition != null && otherPosition.isInRange(position, CHAT_RADIUS)) {
                SocialMessage message = new SocialMessage(agent.getAgentId(), "Ping", System.currentTimeMillis());
                messageQueues.computeIfAbsent(other.getAgentId(), key -> new ArrayList<>()).add(message);
                HierarchicalBotsMod.LOGGER.debug("Social exchange between {} and {}", agent.getAgentId(), other.getAgentId());
            }
        }
    }

    public List<SocialMessage> pullMessagesFor(Identifier agentId) {
        messageQueues.computeIfAbsent(agentId, key -> new ArrayList<>());
        List<SocialMessage> messages = new ArrayList<>(messageQueues.get(agentId));
        messageQueues.get(agentId).clear();
        return messages;
    }

    public void flushTick() {
        messageQueues.forEach((id, queue) -> queue.removeIf(message -> false));
    }
}
