package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.ProjectEMachineEmcHelper;
import java.math.BigDecimal;
import java.util.List;
import moze_intel.projecte.gameObjs.block_entities.DMFurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DMFurnaceBlockEntity.class, remap = false)
public abstract class ProjectEMatterFurnaceBlockEntityMixin {

    @Unique
    private static final ThreadLocal<ProjectEnervateMatterFurnaceSnapshot> PROJECTENERVATE_FURNACE_SNAPSHOT = new ThreadLocal<>();

    @Inject(
            method = "tickServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lmoze_intel/projecte/gameObjs/block_entities/DMFurnaceBlockEntity;smeltItem(Lnet/minecraft/world/level/Level;Lmoze_intel/projecte/gameObjs/block_entities/DMFurnaceBlockEntity$RecipeResult;)V"
            )
    )
    private static void projectenervate$captureMatterFurnaceInputBudget(
            Level level,
            BlockPos pos,
            BlockState state,
            DMFurnaceBlockEntity furnace,
            CallbackInfo ci
    ) {
        IItemHandler inputInventory = furnace.getInput();
        IItemHandler outputInventory = furnace.getOutput();

        ItemStack inputStack = inputInventory.getStackInSlot(0);
        BigDecimal inputBudget = ProjectEMachineEmcHelper.getOneItemInputBudget(inputStack);
        List<ItemStack> outputSnapshot = ProjectEMachineEmcHelper.snapshotInventory(outputInventory);

        PROJECTENERVATE_FURNACE_SNAPSHOT.set(new ProjectEnervateMatterFurnaceSnapshot(inputBudget, outputSnapshot));
    }

    @Inject(
            method = "tickServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lmoze_intel/projecte/gameObjs/block_entities/DMFurnaceBlockEntity;smeltItem(Lnet/minecraft/world/level/Level;Lmoze_intel/projecte/gameObjs/block_entities/DMFurnaceBlockEntity$RecipeResult;)V",
                    shift = At.Shift.AFTER
            )
    )
    private static void projectenervate$applyMatterFurnaceOutputBudget(
            Level level,
            BlockPos pos,
            BlockState state,
            DMFurnaceBlockEntity furnace,
            CallbackInfo ci
    ) {
        ProjectEnervateMatterFurnaceSnapshot snapshot = PROJECTENERVATE_FURNACE_SNAPSHOT.get();
        PROJECTENERVATE_FURNACE_SNAPSHOT.remove();

        if (snapshot == null) {
            return;
        }

        ProjectEMachineEmcHelper.applyGeneratedOutputBudget(
                snapshot.beforeOutputs(),
                furnace.getOutput(),
                snapshot.inputBudget()
        );
    }

    @Unique
    private record ProjectEnervateMatterFurnaceSnapshot(
            BigDecimal inputBudget,
            List<ItemStack> beforeOutputs
    ) {
    }
}
