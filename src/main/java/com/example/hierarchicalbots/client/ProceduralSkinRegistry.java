package com.example.hierarchicalbots.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

public final class ProceduralSkinRegistry {
    private static final Map<Integer, Identifier> CACHE = new ConcurrentHashMap<>();

    private ProceduralSkinRegistry() {
    }

    public static Identifier getTexture(int seed) {
        return CACHE.computeIfAbsent(seed, ProceduralSkinRegistry::createTexture);
    }

    private static Identifier createTexture(int seed) {
        try {
            NativeImage image = new NativeImage(64, 64, true);
            int base = seed & 0xff;
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    int color = computeColor(seed, x, y, base);
                    image.setColor(x, y, color);
                }
            }
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            TextureManager manager = MinecraftClient.getInstance().getTextureManager();
            return manager.registerDynamicTexture("agent_skin/" + seed, texture);
        } catch (RuntimeException ex) {
            return DefaultSkinHelper.getTexture(UUID.nameUUIDFromBytes(("fallback-" + seed).getBytes()));
        }
    }

    private static int computeColor(int seed, int x, int y, int base) {
        int r = (seed >> 16) & 0xff;
        int g = (seed >> 8) & 0xff;
        int b = seed & 0xff;
        int stripe = ((x / 4) + (y / 4)) % 2;
        int mod = stripe == 0 ? 20 : -20;
        int red = clamp(r + mod + base);
        int green = clamp((g + mod) & 0xff);
        int blue = clamp((b + mod) & 0xff);
        return 0xff000000 | (red << 16) | (green << 8) | blue;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
