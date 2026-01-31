package com.example.hierarchicalbots.perception;

import net.minecraft.util.math.Vec3d;

public record EntityObservation(String entityType, Vec3d position, Vec3d velocity) {
}
