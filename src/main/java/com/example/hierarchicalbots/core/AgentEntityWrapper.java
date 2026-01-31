package com.example.hierarchicalbots.core;

import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class AgentEntityWrapper {
    private final Identifier agentId;
    private final ServerWorld world;
    private final UUID uuid;
    private final BlockPos spawnPos;
    private final Path inventoryFile;
    private FakePlayer fakePlayer;
    private Vec3d fallbackPosition = Vec3d.ZERO;

    public AgentEntityWrapper(Identifier agentId, ServerWorld world, UUID uuid, BlockPos spawnPos, Path inventoryFile) {
        this.agentId = agentId;
        this.world = world;
        this.uuid = uuid;
        this.spawnPos = spawnPos;
        this.inventoryFile = inventoryFile;
    }

    public Identifier getAgentId() {
        return agentId;
    }

    public ServerWorld getWorld() {
        return world;
    }

    public BlockPos getSpawnPos() {
        return spawnPos;
    }

    public FakePlayer spawn() {
        GameProfile profile = new GameProfile(uuid, agentId.getPath());
        fakePlayer = FakePlayer.get(world, profile);
        fakePlayer.refreshPositionAndAngles(spawnPos, 0.0f, 0.0f);
        fakePlayer.setHealth(fakePlayer.getMaxHealth());
        loadInventory();
        return fakePlayer;
    }

    public Optional<ServerPlayerEntity> getPlayerEntity() {
        return Optional.ofNullable(fakePlayer);
    }

    public void setFallbackPosition(Vec3d position) {
        this.fallbackPosition = position;
    }

    public Vec3d getPosition() {
        if (fakePlayer != null) {
            return fakePlayer.getPos();
        }
        return fallbackPosition;
    }

    public BlockPos getBlockPos() {
        return BlockPos.ofFloored(getPosition());
    }

    public Optional<Entity> getEntity() {
        return Optional.ofNullable(fakePlayer);
    }

    public float getHealth() {
        if (fakePlayer != null) {
            return fakePlayer.getHealth();
        }
        return 20.0f;
    }

    public void saveInventory() {
        if (fakePlayer == null) {
            return;
        }
        NbtCompound compound = new NbtCompound();
        compound.put("Inventory", fakePlayer.getInventory().writeNbt(new NbtList()));
        try {
            Files.createDirectories(inventoryFile.getParent());
            NbtIo.write(compound, inventoryFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save inventory for " + agentId, e);
        }
    }

    private void loadInventory() {
        if (fakePlayer == null || !Files.exists(inventoryFile)) {
            return;
        }
        try {
            NbtCompound compound = NbtIo.read(inventoryFile);
            if (compound != null && compound.contains("Inventory")) {
                fakePlayer.getInventory().readNbt(compound.getList("Inventory", 10));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load inventory for " + agentId, e);
        }
    }

    public void attackNearestEntity(double range) {
        if (fakePlayer == null) {
            return;
        }
        Vec3d center = fakePlayer.getPos();
        Entity nearest = world.getOtherEntities(fakePlayer, fakePlayer.getBoundingBox().expand(range)).stream()
            .filter(entity -> entity instanceof LivingEntity)
            .min((a, b) -> Double.compare(a.squaredDistanceTo(center), b.squaredDistanceTo(center)))
            .orElse(null);
        if (nearest != null) {
            fakePlayer.attack(nearest);
        }
    }
}
