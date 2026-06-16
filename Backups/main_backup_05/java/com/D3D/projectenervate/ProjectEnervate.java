package com.D3D.projectenervate;

import com.D3D.projectenervate.client.AdaptiveEmcTooltipEvents;
import com.D3D.projectenervate.command.ProjectEnervateCommands;
import com.D3D.projectenervate.world.PlacedBlockAdaptiveEmcEvents;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import com.D3D.projectenervate.emc.KnownSourceEvents;

@Mod(ProjectEnervate.MOD_ID)
public final class ProjectEnervate {

    public static final String MOD_ID = "projectenervate";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ProjectEnervate() {
        LOGGER.info("ProjectEnervate loaded.");

        NeoForge.EVENT_BUS.addListener(ProjectEnervateCommands::registerCommands);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, AdaptiveEmcTooltipEvents::onItemTooltip);

        NeoForge.EVENT_BUS.addListener(PlacedBlockAdaptiveEmcEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(PlacedBlockAdaptiveEmcEvents::onBlockPlaced);
        NeoForge.EVENT_BUS.addListener(PlacedBlockAdaptiveEmcEvents::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(PlacedBlockAdaptiveEmcEvents::onPlayerLoggedOut);

        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, KnownSourceEvents::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, KnownSourceEvents::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(KnownSourceEvents::onItemToss);
    }
}
