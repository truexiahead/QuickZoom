package io.github.truexiahead.quickzoom;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue zoomMultiplier;
    public static final ModConfigSpec.BooleanValue enableCinematic;
    public static final ModConfigSpec.BooleanValue enableSmoothZoom;
    public static final ModConfigSpec.DoubleValue smoothSpeed;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("QuickZoom Configuration").push("general");

        zoomMultiplier = builder
                .comment("Zoom multiplier. 2.0 = 2x zoom, 16.0 = 16x zoom. Higher value = more zoomed in.")
                .defineInRange("zoomMultiplier", 4.0, 2.0, 16.0);

        enableCinematic = builder
                .comment("Enable cinematic camera (smooth camera / F8 effect) while zooming.")
                .define("enableCinematic", true);

        enableSmoothZoom = builder
                .comment("Enable telescopic smooth FOV transition. When enabled, FOV gradually changes\nlike looking through a telescope (disable for instant switch).")
                .define("enableSmoothZoom", false);

        smoothSpeed = builder
                .comment("Transition speed for smooth zoom. 0.01 = very slow, 1.0 = almost instant.")
                .defineInRange("smoothSpeed", 0.1, 0.01, 1.0);

        builder.pop();
        SPEC = builder.build();
    }
}
