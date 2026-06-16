package com.D3D.projectenervate;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ProjectEnervate.MOD_ID)
public final class ProjectEnervate {
    public static final String MOD_ID = "projectenervate";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ProjectEnervate() {
        LOGGER.info("ProjectEnervate loaded.");
    }
}