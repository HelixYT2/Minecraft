package dev.helix.bridge.baritone;

import com.google.gson.JsonObject;
import dev.helix.bridge.HelixBridgeMod;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Method;

public class BaritoneIntegration {
    private boolean available = false;
    private String version = null;
    private Object baritoneApi = null;
    private Class<?> baritoneApiClass = null;

    public BaritoneIntegration() {
        try {
            // Try to load Baritone API class
            baritoneApiClass = Class.forName("baritone.api.BaritoneAPI");
            Method getProvider = baritoneApiClass.getMethod("getProvider");
            Object provider = getProvider.invoke(null);
            
            if (provider != null) {
                available = true;
                
                // Try to get version
                try {
                    Method getVersion = baritoneApiClass.getMethod("getVersion");
                    version = (String) getVersion.invoke(null);
                } catch (Exception e) {
                    version = "unknown";
                }
                
                HelixBridgeMod.LOGGER.info("Baritone detected! Version: {}", version);
            }
        } catch (ClassNotFoundException e) {
            HelixBridgeMod.LOGGER.info("Baritone not found - Baritone features will be disabled");
        } catch (Exception e) {
            HelixBridgeMod.LOGGER.warn("Error initializing Baritone integration", e);
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public String getVersion() {
        return version;
    }

    public JsonObject getStatus() {
        JsonObject status = new JsonObject();
        status.addProperty("available", available);
        
        if (!available) {
            status.addProperty("active", false);
            return status;
        }

        try {
            Object provider = getProvider();
            if (provider == null) {
                status.addProperty("active", false);
                return status;
            }

            Object primaryBaritone = getPrimaryBaritone(provider);
            if (primaryBaritone == null) {
                status.addProperty("active", false);
                return status;
            }

            Object pathingBehavior = getPathingBehavior(primaryBaritone);
            if (pathingBehavior != null) {
                boolean isPathing = (boolean) pathingBehavior.getClass()
                        .getMethod("isPathing")
                        .invoke(pathingBehavior);
                status.addProperty("active", isPathing);

                // Get current goal
                Object goal = pathingBehavior.getClass()
                        .getMethod("getGoal")
                        .invoke(pathingBehavior);
                if (goal != null) {
                    status.addProperty("currentGoal", goal.toString());
                }
            }

            // Get current process
            Object pathingControlManager = primaryBaritone.getClass()
                    .getMethod("getPathingControlManager")
                    .invoke(primaryBaritone);
            if (pathingControlManager != null) {
                Object mostRecentInControl = pathingControlManager.getClass()
                        .getMethod("mostRecentInControl")
                        .invoke(pathingControlManager);
                if (mostRecentInControl != null) {
                    // It's an Optional
                    Method isPresent = mostRecentInControl.getClass().getMethod("isPresent");
                    if ((boolean) isPresent.invoke(mostRecentInControl)) {
                        Object process = mostRecentInControl.getClass().getMethod("get").invoke(mostRecentInControl);
                        if (process != null) {
                            String processName = process.getClass().getSimpleName();
                            status.addProperty("currentProcess", processName);
                            
                            // Try to get progress info
                            try {
                                Method displayName = process.getClass().getMethod("displayName0");
                                String progress = (String) displayName.invoke(process);
                                if (progress != null) {
                                    status.addProperty("progress", progress);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            HelixBridgeMod.LOGGER.error("Error getting Baritone status", e);
            status.addProperty("error", e.getMessage());
        }

        return status;
    }

    public boolean executeCommand(String command) {
        if (!available) {
            HelixBridgeMod.LOGGER.warn("Cannot execute command - Baritone not available");
            return false;
        }

        try {
            Object provider = getProvider();
            Object primaryBaritone = getPrimaryBaritone(provider);
            
            if (primaryBaritone == null) {
                HelixBridgeMod.LOGGER.warn("No primary Baritone instance");
                return false;
            }

            // Get command manager
            Object commandManager = primaryBaritone.getClass()
                    .getMethod("getCommandManager")
                    .invoke(primaryBaritone);
            
            if (commandManager != null) {
                // Execute command
                Method execute = commandManager.getClass().getMethod("execute", String.class);
                execute.invoke(commandManager, command);
                HelixBridgeMod.LOGGER.info("Executed Baritone command: {}", command);
                return true;
            }
        } catch (Exception e) {
            HelixBridgeMod.LOGGER.error("Error executing Baritone command: {}", command, e);
        }
        return false;
    }

    public void stop() {
        if (!available) return;
        
        try {
            Object provider = getProvider();
            Object primaryBaritone = getPrimaryBaritone(provider);
            
            if (primaryBaritone != null) {
                Object pathingBehavior = getPathingBehavior(primaryBaritone);
                if (pathingBehavior != null) {
                    Method cancelEverything = pathingBehavior.getClass().getMethod("cancelEverything");
                    cancelEverything.invoke(pathingBehavior);
                    HelixBridgeMod.LOGGER.info("Stopped all Baritone tasks");
                }
            }
        } catch (Exception e) {
            HelixBridgeMod.LOGGER.error("Error stopping Baritone", e);
        }
    }

    private Object getProvider() throws Exception {
        if (baritoneApiClass == null) return null;
        Method getProvider = baritoneApiClass.getMethod("getProvider");
        return getProvider.invoke(null);
    }

    private Object getPrimaryBaritone(Object provider) throws Exception {
        if (provider == null) return null;
        Method getPrimaryBaritone = provider.getClass().getMethod("getPrimaryBaritone");
        return getPrimaryBaritone.invoke(provider);
    }

    private Object getPathingBehavior(Object baritone) throws Exception {
        if (baritone == null) return null;
        Method getPathingBehavior = baritone.getClass().getMethod("getPathingBehavior");
        return getPathingBehavior.invoke(baritone);
    }
}
