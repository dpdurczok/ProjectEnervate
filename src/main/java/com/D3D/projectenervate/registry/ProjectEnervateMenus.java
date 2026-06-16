package com.D3D.projectenervate.registry;

import com.D3D.projectenervate.ProjectEnervate;
import com.D3D.projectenervate.menu.EmcAssignmentMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ProjectEnervateMenus {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(
            BuiltInRegistries.MENU,
            ProjectEnervate.MOD_ID
    );

    public static final DeferredHolder<MenuType<?>, MenuType<EmcAssignmentMenu>> EMC_ASSIGNMENT_STATION = MENU_TYPES.register(
            "emc_assignment_station",
            () -> IMenuTypeExtension.create(EmcAssignmentMenu::new)
    );

    private ProjectEnervateMenus() {
    }
}
