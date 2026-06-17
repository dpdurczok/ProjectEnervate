package com.D3D.projectenervate.registry;

import com.D3D.projectenervate.ProjectEnervate;
import com.D3D.projectenervate.blockentity.EmcAssignmentStationBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ProjectEnervateBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ProjectEnervate.MOD_ID
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EmcAssignmentStationBlockEntity>> EMC_ASSIGNMENT_STATION =
            BLOCK_ENTITY_TYPES.register(
                    "emc_assignment_station",
                    () -> BlockEntityType.Builder.of(
                            EmcAssignmentStationBlockEntity::new,
                            ProjectEnervateBlocks.EMC_ASSIGNMENT_STATION.get()
                    ).build(null)
            );

    private ProjectEnervateBlockEntities() {
    }
}
