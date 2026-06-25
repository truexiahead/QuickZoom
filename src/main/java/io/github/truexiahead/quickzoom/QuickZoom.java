package io.github.truexiahead.quickzoom;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@Mod(QuickZoom.MOD_ID)
public class QuickZoom {
    public static final String MOD_ID = "quickzoom";

    private static Minecraft mc() { return Minecraft.getInstance(); }

    private final KeyMapping zoomKey;

    /** Whether the zoom key is currently held */
    private boolean isZooming;

    /** Saved cinematic-camera (F8) state before zoom, for restoration */
    private boolean savedCinematicCamera;

    // ---- Smooth-zoom state ----
    private float targetFovFactor = 1.0f;
    private float prevFovFactor = 1.0f;
    private float currentFovFactor = 1.0f;

    // ---- Cached config values (read once per state transition) ----
    private boolean cachedSmoothZoom;
    private float cachedSmoothLerp;

    public QuickZoom(IEventBus modEventBus, ModContainer modContainer) {
        zoomKey = new KeyMapping(
                "key.quickzoom.zoom",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "key.categories.misc"
        );

        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(this::onRegisterKeys);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onComputeFov);
    }

    // ---- Key registration ----

    private void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(zoomKey);
    }

    // ---- Per-tick: state transitions + smooth-zoom interpolation ----

    private void onClientTick(ClientTickEvent.Pre event) {
        if (mc().player == null) return;

        boolean keyDown = zoomKey.isDown();

        // State transitions (read config only when state changes)
        if (keyDown && !isZooming) {
            targetFovFactor = (float) (1.0 / Config.zoomMultiplier.get());
            cacheSmoothSettings();
            startZoom();
        } else if (!keyDown && isZooming) {
            targetFovFactor = 1.0f;
            cacheSmoothSettings();
            stopZoom();
        }

        // Interpolate toward target
        if (cachedSmoothZoom) {
            prevFovFactor = currentFovFactor;
            currentFovFactor += (targetFovFactor - currentFovFactor) * cachedSmoothLerp;
            if (Math.abs(currentFovFactor - targetFovFactor) < 0.0005f) {
                currentFovFactor = targetFovFactor;
            }
        } else {
            // Instant: no interpolation at all — both prev and current snap to target
            prevFovFactor = targetFovFactor;
            currentFovFactor = targetFovFactor;
        }
    }

    // ---- FOV modification (frame-level, partial-tick interpolation) ----

    private void onComputeFov(ViewportEvent.ComputeFov event) {
        if (mc().player == null) return;

        float partialTick = (float) event.getPartialTick();
        float rendered = prevFovFactor + (currentFovFactor - prevFovFactor) * partialTick;

        if (Math.abs(rendered - 1.0f) < 0.0001f) return;
        event.setFOV(event.getFOV() * rendered);
    }

    // ---- Internal helpers ----

    private void cacheSmoothSettings() {
        cachedSmoothZoom = Config.enableSmoothZoom.get();
        if (cachedSmoothZoom) {
            float s = Config.smoothSpeed.get().floatValue();
            cachedSmoothLerp = 1.0f - (float) Math.exp(-s * 8.0);
        } else {
            cachedSmoothLerp = 1.0f;
        }
    }

    private void startZoom() {
        isZooming = true;
        if (Config.enableCinematic.get()) {
            savedCinematicCamera = mc().options.smoothCamera;
            mc().options.smoothCamera = true;
        }
    }

    private void stopZoom() {
        isZooming = false;
        if (Config.enableCinematic.get()) {
            mc().options.smoothCamera = savedCinematicCamera;
        }
    }
}
