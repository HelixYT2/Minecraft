package dev.helix.bridge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.helix.bridge.HelixBridgeMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HelixConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "helix-bridge.json";

    private String hubUrl = "ws://127.0.0.1:9742";
    private String authToken = "";
    private String instanceId = "";
    private int updateRateMs = 1000;
    private int connectionTimeoutMs = 10000;
    private int requestTimeoutMs = 30000;
    private int reconnectDelayMs = 5000;
    private int maxReconnectDelayMs = 60000;

    public static HelixConfig load() {
        Path configPath = getConfigPath();
        
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                HelixConfig config = GSON.fromJson(json, HelixConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (IOException e) {
                HelixBridgeMod.LOGGER.error("Failed to load config", e);
            }
        }
        
        // Create default config
        HelixConfig config = new HelixConfig();
        config.save();
        return config;
    }

    public void save() {
        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(this));
        } catch (IOException e) {
            HelixBridgeMod.LOGGER.error("Failed to save config", e);
        }
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }

    public String getHubUrl() {
        return hubUrl;
    }

    public void setHubUrl(String hubUrl) {
        this.hubUrl = hubUrl;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public int getUpdateRateMs() {
        return updateRateMs;
    }

    public void setUpdateRateMs(int updateRateMs) {
        this.updateRateMs = updateRateMs;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(int requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public int getReconnectDelayMs() {
        return reconnectDelayMs;
    }

    public void setReconnectDelayMs(int reconnectDelayMs) {
        this.reconnectDelayMs = reconnectDelayMs;
    }

    public int getMaxReconnectDelayMs() {
        return maxReconnectDelayMs;
    }

    public void setMaxReconnectDelayMs(int maxReconnectDelayMs) {
        this.maxReconnectDelayMs = maxReconnectDelayMs;
    }

    public boolean isConfigured() {
        return authToken != null && !authToken.isEmpty() && instanceId != null && !instanceId.isEmpty();
    }
}
