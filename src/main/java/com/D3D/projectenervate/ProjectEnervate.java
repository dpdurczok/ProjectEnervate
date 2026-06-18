package com.D3D.projectenervate;

import com.D3D.projectenervate.client.AdaptiveEmcTooltipEvents;
import com.D3D.projectenervate.client.ProjectEnervateClientEvents;
import com.D3D.projectenervate.celestial.CelestialCourseSyncEvents;
import com.D3D.projectenervate.command.ProjectEnervateCommands;
import com.D3D.projectenervate.network.ProjectEnervateNetwork;
import com.D3D.projectenervate.registry.ProjectEnervateBlockEntities;
import com.D3D.projectenervate.registry.ProjectEnervateBlocks;
import com.D3D.projectenervate.registry.ProjectEnervateItems;
import com.D3D.projectenervate.registry.ProjectEnervateMenus;
import com.D3D.projectenervate.world.PlacedBlockAdaptiveEmcEvents;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;
import com.D3D.projectenervate.emc.KnownSourceEvents;

@Mod(ProjectEnervate.MOD_ID)
public final class ProjectEnervate {

    public static final String MOD_ID = "projectenervate";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ProjectEnervate(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("ProjectEnervate loaded.");

        modContainer.registerConfig(ModConfig.Type.COMMON, ProjectEnervateConfig.SPEC);

        ProjectEnervateBlocks.BLOCKS.register(modEventBus);
        ProjectEnervateItems.ITEMS.register(modEventBus);
        ProjectEnervateBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ProjectEnervateMenus.MENU_TYPES.register(modEventBus);
        modEventBus.addListener(ProjectEnervateNetwork::registerPayloads);
        modEventBus.addListener(ProjectEnervate::addCreativeTabItems);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ProjectEnervateClientEvents::registerScreens);
            ProjectEnervateClientEvents.registerConfigScreen(modContainer);
            ProjectEnervateClientEvents.registerClientRuntimeEvents();
        }

        NeoForge.EVENT_BUS.addListener(ProjectEnervateCommands::registerCommands);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, AdaptiveEmcTooltipEvents::onItemTooltip);

        NeoForge.EVENT_BUS.addListener(PlacedBlockAdaptiveEmcEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(PlacedBlockAdaptiveEmcEvents::onBlockPlaced);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, PlacedBlockAdaptiveEmcEvents::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(PlacedBlockAdaptiveEmcEvents::onPistonPre);
        NeoForge.EVENT_BUS.addListener(PlacedBlockAdaptiveEmcEvents::onPistonPost);
        NeoForge.EVENT_BUS.addListener(PlacedBlockAdaptiveEmcEvents::onLevelTickPost);
        NeoForge.EVENT_BUS.addListener(CelestialCourseSyncEvents::onLevelTickPost);
        NeoForge.EVENT_BUS.addListener(PlacedBlockAdaptiveEmcEvents::onPlayerLoggedOut);

        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, KnownSourceEvents::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, KnownSourceEvents::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(KnownSourceEvents::onItemToss);
    }
    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ProjectEnervateItems.CELESTIAL_MAPPING.get());
            event.accept(ProjectEnervateItems.CELESTIAL_SCOPE.get());
        }

        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ProjectEnervateItems.EMC_ASSIGNMENT_STATION.get());
        }
    }

}
