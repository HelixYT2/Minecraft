package com.example.hierarchicalbots.entity;

import com.example.hierarchicalbots.HierarchicalBotsMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class AgentEntities {
    public static final EntityType<AgentEntity> AGENT = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(HierarchicalBotsMod.MOD_ID, "agent"),
        EntityType.Builder.create(AgentEntity::new, SpawnGroup.CREATURE)
            .dimensions(0.6f, 1.8f)
            .trackRangeChunks(8)
            .build()
    );

    private AgentEntities() {
    }

    public static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(AGENT, AgentEntity.createAttributes());
    }
}
