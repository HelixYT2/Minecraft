package com.example.hierarchicalbots.client;

import com.example.hierarchicalbots.HierarchicalBotsMod;
import com.example.hierarchicalbots.core.BotAgent;
import com.example.hierarchicalbots.core.BotManager;
import com.example.hierarchicalbots.state.ConsciousnessState;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class AgentMonitorScreen extends Screen {
    public AgentMonitorScreen() {
        super(Text.literal("Hierarchical Bot Monitor"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawTextWithShadow(textRenderer, "Hierarchical Bots - Consciousness Monitor", 12, 12, 0xFFFFFF);

        BotManager manager = HierarchicalBotsMod.getBotManager();
        if (manager == null) {
            context.drawTextWithShadow(textRenderer, "No active bot manager.", 12, 32, 0xFFAA00);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        List<BotAgent> agents = manager.getAgents();
        int y = 32;
        for (BotAgent agent : agents) {
            ConsciousnessState state = agent.getConsciousnessState();
            String line = String.format(
                "%s | Pos: %s | Blocks: %d | Entities: %d | Social: %d | Learning: %.2f | Memory: %d",
                state.getAgentName(),
                state.getLastKnownPosition() == null ? "?" : shortVec(state.getLastKnownPosition()),
                state.getPerceivedBlocks(),
                state.getPerceivedEntities(),
                state.getSocialMessages(),
                state.getLearningProgress(),
                state.getKnowledgeEntries()
            );
            context.drawTextWithShadow(textRenderer, line, 12, y, 0xB0E0FF);
            y += 12;
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private String shortVec(net.minecraft.util.math.Vec3d vec) {
        return String.format("%.1f, %.1f, %.1f", vec.x, vec.y, vec.z);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
