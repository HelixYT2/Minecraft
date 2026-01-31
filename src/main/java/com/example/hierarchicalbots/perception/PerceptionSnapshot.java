package com.example.hierarchicalbots.perception;

import java.util.List;
import net.minecraft.util.math.Vec3d;

public class PerceptionSnapshot {
    private final Vec3d position;
    private final List<BlockObservation> nearbyBlocks;
    private final List<EntityObservation> nearbyEntities;

    public PerceptionSnapshot(Vec3d position, List<BlockObservation> nearbyBlocks, List<EntityObservation> nearbyEntities) {
        this.position = position;
        this.nearbyBlocks = List.copyOf(nearbyBlocks);
        this.nearbyEntities = List.copyOf(nearbyEntities);
    }

    public Vec3d getPosition() {
        return position;
    }

    public List<BlockObservation> getNearbyBlocks() {
        return nearbyBlocks;
    }

    public List<EntityObservation> getNearbyEntities() {
        return nearbyEntities;
    }
}
