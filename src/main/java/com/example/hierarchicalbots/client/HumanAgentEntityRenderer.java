package com.example.hierarchicalbots.client;

import com.example.hierarchicalbots.core.HumanAgentEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class HumanAgentEntityRenderer extends BipedEntityRenderer<HumanAgentEntity, PlayerEntityModel<HumanAgentEntity>> {
    private static final Identifier TEXTURE = Identifier.of("minecraft", "textures/entity/steve.png");

    public HumanAgentEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public Identifier getTexture(HumanAgentEntity entity) {
        return TEXTURE;
    }
}
