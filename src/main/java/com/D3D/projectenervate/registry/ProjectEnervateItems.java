package com.D3D.projectenervate.registry;

import com.D3D.projectenervate.ProjectEnervate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ProjectEnervateItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            BuiltInRegistries.ITEM,
            ProjectEnervate.MOD_ID
    );

    public static final DeferredHolder<Item, BlockItem> EMC_ASSIGNMENT_STATION = ITEMS.register(
            "emc_assignment_station",
            () -> new BlockItem(ProjectEnervateBlocks.EMC_ASSIGNMENT_STATION.get(), new Item.Properties())
    );

    private ProjectEnervateItems() {
    }
}
