package com.example.hierarchicalbots.planning;

import com.example.hierarchicalbots.perception.PerceptionSnapshot;
import com.example.hierarchicalbots.social.SocialMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.util.math.Vec3d;

public class HttpLLMPlanner implements HighLevelPlanner {
    private static final Gson GSON = new Gson();

    private final HttpClient client;
    private final URI endpoint;
    private final AtomicReference<GoalIntent> latestIntent = new AtomicReference<>(GoalIntent.idle());
    private final AtomicBoolean requestInFlight = new AtomicBoolean(false);
    private Instant nextRequestTime = Instant.EPOCH;

    public HttpLLMPlanner(URI endpoint) {
        this.endpoint = endpoint;
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    }

    @Override
    public GoalIntent plan(PerceptionSnapshot snapshot, List<SocialMessage> messages) {
        if (Instant.now().isAfter(nextRequestTime) && requestInFlight.compareAndSet(false, true)) {
            nextRequestTime = Instant.now().plusMillis(ThreadLocalRandom.current().nextLong(2000, 5000));
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildPayload(snapshot, messages)))
                .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(body -> latestIntent.set(parseResponse(body)))
                .whenComplete((result, error) -> requestInFlight.set(false));
        }
        return latestIntent.get();
    }

    private String buildPayload(PerceptionSnapshot snapshot, List<SocialMessage> messages) {
        JsonObject root = new JsonObject();
        Vec3d pos = snapshot.getPosition();
        root.addProperty("x", pos.x);
        root.addProperty("y", pos.y);
        root.addProperty("z", pos.z);
        root.addProperty("blockCount", snapshot.getNearbyBlocks().size());
        root.addProperty("entityCount", snapshot.getNearbyEntities().size());
        root.addProperty("messageCount", messages.size());
        return GSON.toJson(root);
    }

    private GoalIntent parseResponse(String body) {
        try {
            JsonObject json = GSON.fromJson(body, JsonObject.class);
            if (json == null || !json.has("goal")) {
                return latestIntent.get();
            }
            String goal = json.get("goal").getAsString();
            Optional<Vec3d> target = Optional.empty();
            if (json.has("target")) {
                JsonObject targetJson = json.getAsJsonObject("target");
                if (targetJson.has("x") && targetJson.has("y") && targetJson.has("z")) {
                    target = Optional.of(new Vec3d(
                        targetJson.get("x").getAsDouble(),
                        targetJson.get("y").getAsDouble(),
                        targetJson.get("z").getAsDouble()
                    ));
                }
            }
            double priority = json.has("priority") ? json.get("priority").getAsDouble() : 0.5;
            return new GoalIntent(goal, target, priority);
        } catch (RuntimeException ex) {
            return latestIntent.get();
        }
    }
}
