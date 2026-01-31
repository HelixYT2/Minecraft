package com.example.hierarchicalbots.client;

import com.example.hierarchicalbots.entity.AgentEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class AgentEntityRenderer extends MobEntityRenderer<AgentEntity, PlayerEntityModel<AgentEntity>> {
    public AgentEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public Identifier getTexture(AgentEntity entity) {
        return ProceduralSkinRegistry.getTexture(entity.getSkinSeed());
    }
}
