package com.example.hierarchicalbots.social;

import net.minecraft.util.Identifier;

public record SocialMessage(Identifier from, String content, long timestamp) {
}
