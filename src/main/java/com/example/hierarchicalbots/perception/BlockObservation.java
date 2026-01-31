package com.example.hierarchicalbots.perception;

import net.minecraft.util.math.BlockPos;

public record BlockObservation(BlockPos position, String blockType, boolean isAir) {
}
