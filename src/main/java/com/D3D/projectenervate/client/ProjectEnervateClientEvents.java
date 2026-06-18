package com.D3D.projectenervate.client;

import com.D3D.projectenervate.registry.ProjectEnervateMenus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

public final class ProjectEnervateClientEvents {
    private ProjectEnervateClientEvents() {
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ProjectEnervateMenus.EMC_ASSIGNMENT_STATION.get(), EmcAssignmentScreen::new);
        event.register(ProjectEnervateMenus.CELESTIAL_MAPPING.get(), CelestialMappingScreen::new);
    }

    public static void registerConfigScreen(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    public static void registerClientRuntimeEvents() {
        NeoForge.EVENT_BUS.addListener(CelestialSkyOverlay::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(CelestialScopeHudOverlay::onRenderGui);
    }
}