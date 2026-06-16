package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.CreateEmcIntegrationHelper;
import com.D3D.projectenervate.emc.ProjectEMachineEmcHelper;
import java.math.BigDecimal;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity", remap = false)
public abstract class CreateMillstoneBlockEntityMixin {
    private final ThreadLocal<Snapshot> projectenervate$millstoneSnapshot = new ThreadLocal<>();

    @Shadow(remap = false)
    public ItemStackHandler inputInv;

    @Shadow(remap = false)
    public ItemStackHandler outputInv;

    @Inject(method = "process", at = @At("HEAD"), require = 0)
    private void projectenervate$captureMillstoneBudget(CallbackInfo ci) {
        ItemStack input = inputInv == null ? ItemStack.EMPTY : inputInv.getStackInSlot(0).copy();
        BigDecimal inputBudget = CreateEmcIntegrationHelper.getStackInputBudget(input);
        List<ItemStack> beforeOutputs = ProjectEMachineEmcHelper.snapshotInventory(outputInv);
        projectenervate$millstoneSnapshot.set(new Snapshot(inputBudget, beforeOutputs));
    }

    @Inject(method = "process", at = @At("RETURN"), require = 0)
    private void projectenervate$applyMillstoneBudget(CallbackInfo ci) {
        Snapshot snapshot = projectenervate$millstoneSnapshot.get();
        projectenervate$millstoneSnapshot.remove();

        if (snapshot == null) {
            return;
        }

        ProjectEMachineEmcHelper.applyGeneratedOutputBudget(snapshot.beforeOutputs(), outputInv, snapshot.inputBudget());
    }

    private record Snapshot(BigDecimal inputBudget, List<ItemStack> beforeOutputs) {
    }
}
