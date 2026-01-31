package com.example.hierarchicalbots;

import com.example.hierarchicalbots.core.BotManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class SimulationCommand {
    private SimulationCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                LiteralArgumentBuilder.<ServerCommandSource>literal("openartemis")
                    .then(CommandManager.literal("start")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            ServerPlayerEntity player = source.getPlayer();
                            BotManager manager = HierarchicalBotsMod.getBotManager();
                            if (manager == null) {
                                source.sendError(Text.literal("Bot manager not initialized yet."));
                                return 0;
                            }
                            manager.startSimulation(player);
                            source.sendFeedback(() -> Text.literal("OpenArtemis simulation started."), true);
                            return Command.SINGLE_SUCCESS;
                        }))
            );
        });
    }
}
