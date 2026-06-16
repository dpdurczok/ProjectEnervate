package com.D3D.projectenervate.compat;

import net.neoforged.fml.ModList;

public final class ProjectEnervateCompat {

    private static Boolean mouseTweaksLoaded;

    private ProjectEnervateCompat() {
    }

    public static boolean isMouseTweaksLoaded() {
        if (mouseTweaksLoaded == null) {
            mouseTweaksLoaded = ModList.get().isLoaded("mousetweaks");
        }

        return mouseTweaksLoaded;
    }
}