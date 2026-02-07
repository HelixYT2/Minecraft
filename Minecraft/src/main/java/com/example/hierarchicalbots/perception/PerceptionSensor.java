package com.example.hierarchicalbots.perception;

import com.example.hierarchicalbots.core.AgentEntityWrapper;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class PerceptionSensor {
    private static final int BLOCK_RADIUS = 4;
    private static final double ENTITY_RADIUS = 8.0;

    public PerceptionSnapshot capture(AgentEntityWrapper wrapper) {
        ServerWorld world = wrapper.getWorld();
        BlockPos origin = wrapper.getBlockPos();
        List<BlockObservation> blocks = new ArrayList<>();

        for (int dx = -BLOCK_RADIUS; dx <= BLOCK_RADIUS; dx++) {
            for (int dy = -BLOCK_RADIUS; dy <= BLOCK_RADIUS; dy++) {
                for (int dz = -BLOCK_RADIUS; dz <= BLOCK_RADIUS; dz++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    BlockState state = world.getBlockState(pos);
                    blocks.add(new BlockObservation(pos, state.getBlock().getTranslationKey(), state.isAir()));
                }
            }
        }

        Vec3d center = wrapper.getPosition();
        Box searchBox = new Box(center, center).expand(ENTITY_RADIUS);
        List<EntityObservation> entities = new ArrayList<>();
        for (Entity entity : world.getOtherEntities(null, searchBox)) {
            entities.add(new EntityObservation(entity.getType().getTranslationKey(), entity.getPos(), entity.getVelocity()));
        }

        return new PerceptionSnapshot(center, blocks, entities);
    }
}
