package com.example.hierarchicalbots.learning;

import com.example.hierarchicalbots.perception.PerceptionSnapshot;
import com.example.hierarchicalbots.social.SocialMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.util.math.Vec3d;

public class VectorMemoryStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path saveFile;
    private final List<MemoryVector> vectors = new ArrayList<>();

    public VectorMemoryStore(Path worldRootPath) {
        this.saveFile = worldRootPath.resolve("hierarchical-bots-memory.json");
    }

    public void store(String tag, double[] vector) {
        vectors.add(new MemoryVector(tag, vector));
    }

    public int getVectorCount() {
        return vectors.size();
    }

    public void load() {
        if (Files.exists(saveFile)) {
            try {
                String json = Files.readString(saveFile);
                MemoryVector[] stored = GSON.fromJson(json, MemoryVector[].class);
                if (stored != null) {
                    vectors.clear();
                    vectors.addAll(List.of(stored));
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load memory store", e);
            }
        }
    }

    public void save() {
        try {
            Files.writeString(saveFile, GSON.toJson(vectors));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save memory store", e);
        }
    }

    public double[] encodeSnapshot(PerceptionSnapshot snapshot, List<SocialMessage> messages) {
        Vec3d pos = snapshot.getPosition();
        double[] vector = new double[] {
            pos.x,
            pos.y,
            pos.z,
            snapshot.getNearbyBlocks().size(),
            snapshot.getNearbyEntities().size(),
            messages.size()
        };
        return normalize(vector);
    }

    public double[] encodeOutcome(PerceptionSnapshot snapshot, LizardBrain.MotorDecision decision, double reward) {
        Vec3d movement = decision.desiredMovement();
        double[] vector = new double[] {
            movement.x,
            movement.y,
            movement.z,
            reward,
            snapshot.getNearbyEntities().size()
        };
        return normalize(vector);
    }

    public Optional<Vec3d> suggestTarget(Vec3d currentPosition) {
        List<Vec3d> samples = new ArrayList<>();
        for (MemoryVector vector : vectors) {
            if ("context".equals(vector.tag()) && vector.vector().length >= 3) {
                samples.add(new Vec3d(vector.vector()[0], vector.vector()[1], vector.vector()[2]));
            }
        }
        if (samples.isEmpty()) {
            return Optional.empty();
        }
        Vec3d average = samples.stream().reduce(Vec3d.ZERO, Vec3d::add).multiply(1.0 / samples.size());
        Vec3d offset = average.subtract(currentPosition);
        if (offset.lengthSquared() == 0) {
            return Optional.empty();
        }
        return Optional.of(currentPosition.add(offset.normalize().multiply(6.0)));
    }

    private double[] normalize(double[] vector) {
        double norm = 0.0;
        for (double value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        double[] normalized = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = norm == 0 ? 0 : vector[i] / norm;
        }
        return normalized;
    }

    public record MemoryVector(String tag, double[] vector) {
        public MemoryVector {
            Objects.requireNonNull(tag, "tag");
            Objects.requireNonNull(vector, "vector");
        }

        public String encode() {
            return Base64.getEncoder().encodeToString(GSON.toJson(vector).getBytes());
        }
    }
}
