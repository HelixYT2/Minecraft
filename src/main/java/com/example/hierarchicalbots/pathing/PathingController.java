package com.example.hierarchicalbots.pathing;

import net.minecraft.util.math.Vec3d;

public interface PathingController {
    void setTarget(Vec3d target, double speed);

    void tick();

    void stop();
}
