package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.CreateEmcIntegrationHelper;
import com.D3D.projectenervate.emc.ProjectEMachineEmcHelper;
import java.math.BigDecimal;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.processing.basin.BasinRecipe", remap = false)
public abstract class CreateBasinRecipeMixin {
    private static final ThreadLocal<Snapshot> PROJECTENERVATE_BASIN_SNAPSHOT = new ThreadLocal<>();

    @Inject(
            method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;)Z",
            at = @At("HEAD"),
            require = 0
    )
    private static void projectenervate$captureBasinRecipeBudget(
            @Coerce Object basin,
            Recipe<?> recipe,
            CallbackInfoReturnable<Boolean> cir
    ) {
        IItemHandler availableItems = CreateEmcIntegrationHelper.getItemHandlerField(basin, "itemCapability");
        IItemHandler outputInventory = CreateEmcIntegrationHelper.getItemHandlerField(basin, "outputInventory");

        if (availableItems == null) {
            availableItems = CreateEmcIntegrationHelper.getItemHandlerField(basin, "inputInventory");
        }

        PROJECTENERVATE_BASIN_SNAPSHOT.set(new Snapshot(
                ProjectEMachineEmcHelper.snapshotInventory(availableItems),
                ProjectEMachineEmcHelper.snapshotInventory(outputInventory),
                CreateEmcIntegrationHelper.snapshotItemStackListField(basin, "spoutputBuffer")
        ));
    }

    @Inject(
            method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;)Z",
            at = @At("RETURN"),
            require = 0
    )
    private static void projectenervate$applyBasinRecipeBudget(
            @Coerce Object basin,
            Recipe<?> recipe,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Snapshot snapshot = PROJECTENERVATE_BASIN_SNAPSHOT.get();
        PROJECTENERVATE_BASIN_SNAPSHOT.remove();

        if (snapshot == null || !cir.getReturnValueZ()) {
            return;
        }

        IItemHandler availableItems = CreateEmcIntegrationHelper.getItemHandlerField(basin, "itemCapability");

        if (availableItems == null) {
            availableItems = CreateEmcIntegrationHelper.getItemHandlerField(basin, "inputInventory");
        }

        BigDecimal inputBudget = ProjectEMachineEmcHelper.getConsumedInputBudget(
                snapshot.beforeInputs(),
                availableItems
        );

        IItemHandler outputInventory = CreateEmcIntegrationHelper.getItemHandlerField(basin, "outputInventory");
        ProjectEMachineEmcHelper.applyGeneratedOutputBudget(
                snapshot.beforeOutputs(),
                outputInventory,
                inputBudget
        );

        List<ItemStack> spoutputBuffer = CreateEmcIntegrationHelper.getItemStackListField(basin, "spoutputBuffer");
        ProjectEMachineEmcHelper.applyGeneratedOutputBudgetToList(
                snapshot.beforeSpoutputBuffer(),
                spoutputBuffer,
                inputBudget
        );
    }

    private record Snapshot(
            List<ItemStack> beforeInputs,
            List<ItemStack> beforeOutputs,
            List<ItemStack> beforeSpoutputBuffer
    ) {
    }
}
