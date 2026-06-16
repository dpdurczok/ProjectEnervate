package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.AdaptiveEmcConversionHelper;
import com.D3D.projectenervate.emc.AdaptiveEmcHelper;
import com.D3D.projectenervate.emc.AdaptiveEmcOutputHelper;
import java.math.BigDecimal;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Unique
    private static final int PROJECTENERVATE_FURNACE_INPUT_SLOT = 0;

    @Unique
    private static final int PROJECTENERVATE_FURNACE_RESULT_SLOT = 2;

    @Unique
    private static final ThreadLocal<FurnaceSnapshot> PROJECTENERVATE_FURNACE_SNAPSHOT =
            new ThreadLocal<>();

    @Inject(method = "burn", at = @At("HEAD"))
    private static void projectenervate$captureFurnaceInputBudget(
            RegistryAccess registryAccess,
            @Nullable RecipeHolder<?> recipe,
            NonNullList<ItemStack> items,
            int maxStackSize,
            AbstractFurnaceBlockEntity furnace,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ItemStack inputStack = items.get(PROJECTENERVATE_FURNACE_INPUT_SLOT);
        ItemStack beforeResult = items.get(PROJECTENERVATE_FURNACE_RESULT_SLOT).copy();

        BigDecimal inputBudget = AdaptiveEmcConversionHelper.getOneItemBudget(inputStack);

        PROJECTENERVATE_FURNACE_SNAPSHOT.set(
                new FurnaceSnapshot(beforeResult, inputBudget)
        );
    }

    @Inject(method = "burn", at = @At("RETURN"))
    private static void projectenervate$applyFurnaceOutputBudget(
            RegistryAccess registryAccess,
            @Nullable RecipeHolder<?> recipe,
            NonNullList<ItemStack> items,
            int maxStackSize,
            AbstractFurnaceBlockEntity furnace,
            CallbackInfoReturnable<Boolean> cir
    ) {
        try {
            if (!cir.getReturnValue()) {
                return;
            }

            FurnaceSnapshot snapshot = PROJECTENERVATE_FURNACE_SNAPSHOT.get();

            if (snapshot == null) {
                return;
            }

            ItemStack afterResult = items.get(PROJECTENERVATE_FURNACE_RESULT_SLOT);

            if (afterResult.isEmpty()) {
                return;
            }

            int generatedCount = projectenervate$getGeneratedCount(
                    snapshot.beforeResult(),
                    afterResult
            );

            AdaptiveEmcOutputHelper.mergeGeneratedIntoResultStack(
                    snapshot.beforeResult(),
                    afterResult,
                    generatedCount,
                    snapshot.inputBudget()
            );
        } finally {
            PROJECTENERVATE_FURNACE_SNAPSHOT.remove();
        }
    }

    @Unique
    private static int projectenervate$getGeneratedCount(
            ItemStack beforeResult,
            ItemStack afterResult
    ) {
        if (afterResult.isEmpty()) {
            return 0;
        }

        if (beforeResult.isEmpty()) {
            return afterResult.getCount();
        }

        if (!AdaptiveEmcHelper.canMergeIgnoringAdaptiveEmc(beforeResult, afterResult)) {
            return afterResult.getCount();
        }

        return Math.max(0, afterResult.getCount() - beforeResult.getCount());
    }

    private record FurnaceSnapshot(
            ItemStack beforeResult,
            BigDecimal inputBudget
    ) {
    }
}
