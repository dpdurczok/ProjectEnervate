package com.D3D.projectenervate.client;

import com.D3D.projectenervate.registry.ProjectEnervateMenus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class ProjectEnervateClientEvents {
    private ProjectEnervateClientEvents() {
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ProjectEnervateMenus.EMC_ASSIGNMENT_STATION.get(), EmcAssignmentScreen::new);
    }

    public static void registerConfigScreen(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
