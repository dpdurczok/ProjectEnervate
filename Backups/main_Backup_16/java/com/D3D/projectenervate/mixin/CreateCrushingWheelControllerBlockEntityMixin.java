package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.CreateEmcIntegrationHelper;
import com.D3D.projectenervate.emc.ProjectEMachineEmcHelper;
import java.math.BigDecimal;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity", remap = false)
public abstract class CreateCrushingWheelControllerBlockEntityMixin {
    private final ThreadLocal<Snapshot> projectenervate$crushingSnapshot = new ThreadLocal<>();

    @Inject(method = "applyRecipe", at = @At("HEAD"), require = 0)
    private void projectenervate$captureCrushingWheelBudget(CallbackInfo ci) {
        IItemHandler inventory = CreateEmcIntegrationHelper.getItemHandlerField(this, "inventory");
        ItemStack input = CreateEmcIntegrationHelper.getStackInHandlerSlot(this, "inventory", 0);
        BigDecimal inputBudget = CreateEmcIntegrationHelper.getStackInputBudget(input);
        List<ItemStack> beforeOutputs = ProjectEMachineEmcHelper.snapshotInventory(inventory);
        projectenervate$crushingSnapshot.set(new Snapshot(inputBudget, beforeOutputs));
    }

    @Inject(method = "applyRecipe", at = @At("RETURN"), require = 0)
    private void projectenervate$applyCrushingWheelBudget(CallbackInfo ci) {
        Snapshot snapshot = projectenervate$crushingSnapshot.get();
        projectenervate$crushingSnapshot.remove();

        if (snapshot == null) {
            return;
        }

        IItemHandler inventory = CreateEmcIntegrationHelper.getItemHandlerField(this, "inventory");
        ProjectEMachineEmcHelper.applyGeneratedOutputBudget(snapshot.beforeOutputs(), inventory, snapshot.inputBudget());
    }

    private record Snapshot(BigDecimal inputBudget, List<ItemStack> beforeOutputs) {
    }
}
