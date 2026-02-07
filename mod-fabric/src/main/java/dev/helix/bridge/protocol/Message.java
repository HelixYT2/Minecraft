package dev.helix.bridge.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Message {
    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create();

    private MessageType type;
    private String requestId;
    private String timestamp;
    private String instanceId;
    private String sessionId;
    private JsonObject payload;

    public Message() {
        this.requestId = UUID.randomUUID().toString();
        this.timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        this.payload = new JsonObject();
    }

    public Message(MessageType type) {
        this();
        this.type = type;
    }

    public static Message fromJson(String json) {
        return GSON.fromJson(json, Message.class);
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static Gson getGson() {
        return GSON;
    }

    // Builder pattern methods
    public Message withType(MessageType type) {
        this.type = type;
        return this;
    }

    public Message withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public Message withInstanceId(String instanceId) {
        this.instanceId = instanceId;
        return this;
    }

    public Message withSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public Message withPayload(JsonObject payload) {
        this.payload = payload;
        return this;
    }

    public Message addPayloadProperty(String key, String value) {
        if (this.payload == null) this.payload = new JsonObject();
        this.payload.addProperty(key, value);
        return this;
    }

    public Message addPayloadProperty(String key, Number value) {
        if (this.payload == null) this.payload = new JsonObject();
        this.payload.addProperty(key, value);
        return this;
    }

    public Message addPayloadProperty(String key, Boolean value) {
        if (this.payload == null) this.payload = new JsonObject();
        this.payload.addProperty(key, value);
        return this;
    }

    public Message addPayloadElement(String key, JsonElement element) {
        if (this.payload == null) this.payload = new JsonObject();
        this.payload.add(key, element);
        return this;
    }

    // Getters and setters
    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public JsonObject getPayload() {
        return payload;
    }

    public void setPayload(JsonObject payload) {
        this.payload = payload;
    }

    public String getPayloadString(String key) {
        if (payload == null || !payload.has(key)) return null;
        JsonElement elem = payload.get(key);
        return elem.isJsonNull() ? null : elem.getAsString();
    }

    public boolean getPayloadBoolean(String key, boolean defaultValue) {
        if (payload == null || !payload.has(key)) return defaultValue;
        return payload.get(key).getAsBoolean();
    }

    public JsonObject getPayloadObject(String key) {
        if (payload == null || !payload.has(key)) return null;
        return payload.getAsJsonObject(key);
    }
}
