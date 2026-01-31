package com.example.hierarchicalbots;

import com.example.hierarchicalbots.client.AgentMonitorScreen;
import com.example.hierarchicalbots.client.HumanAgentEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class HierarchicalBotsClient implements ClientModInitializer {
    private KeyBinding monitorKey;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(HierarchicalBotsMod.HUMAN_AGENT_ENTITY_TYPE, HumanAgentEntityRenderer::new);
        monitorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.hierarchical_bots.monitor",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "category.hierarchical_bots"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (monitorKey.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new AgentMonitorScreen());
            }
        });
    }
}
