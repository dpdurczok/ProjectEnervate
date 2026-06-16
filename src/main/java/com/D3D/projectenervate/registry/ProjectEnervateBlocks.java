package com.D3D.projectenervate.registry;

import com.D3D.projectenervate.ProjectEnervate;
import com.D3D.projectenervate.block.EmcAssignmentStationBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ProjectEnervateBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            BuiltInRegistries.BLOCK,
            ProjectEnervate.MOD_ID
    );

    public static final DeferredHolder<Block, EmcAssignmentStationBlock> EMC_ASSIGNMENT_STATION = BLOCKS.register(
            "emc_assignment_station",
            () -> new EmcAssignmentStationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(4.0F, 8.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE))
    );

    private ProjectEnervateBlocks() {
    }
}
