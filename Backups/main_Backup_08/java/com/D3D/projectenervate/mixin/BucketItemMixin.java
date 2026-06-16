package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.AdaptiveEmcOutputHelper;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin {

    @Unique
    private static final ThreadLocal<BucketUseSnapshot> PROJECTENERVATE_BUCKET_USE =
            new ThreadLocal<>();

    @Inject(method = "use", at = @At("HEAD"))
    private void projectenervate$captureBucketUseInput(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir
    ) {
        if (level.isClientSide) {
            return;
        }

        ItemStack sourceStack = player.getItemInHand(hand);

        if (sourceStack.isEmpty()) {
            PROJECTENERVATE_BUCKET_USE.remove();
            return;
        }

        if (!projectenervate$isBucketLike(sourceStack)) {
            PROJECTENERVATE_BUCKET_USE.remove();
            return;
        }

        BigDecimal sourceSingleEmc = AdaptiveEmcOutputHelper.getEffectiveSingleEmc(sourceStack);

        if (sourceSingleEmc.signum() <= 0) {
            PROJECTENERVATE_BUCKET_USE.remove();
            return;
        }

        PROJECTENERVATE_BUCKET_USE.set(
                new BucketUseSnapshot(
                        sourceStack.getItem(),
                        sourceSingleEmc,
                        projectenervate$getExistingBucketOutputSlots(player.getInventory())
                )
        );
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void projectenervate$applyAdaptiveEmcToBucketUseOutput(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir
    ) {
        try {
            if (level.isClientSide) {
                return;
            }

            BucketUseSnapshot snapshot = PROJECTENERVATE_BUCKET_USE.get();

            if (snapshot == null) {
                return;
            }

            InteractionResultHolder<ItemStack> resultHolder = cir.getReturnValue();

            if (resultHolder == null) {
                return;
            }

            ItemStack returnedStack = resultHolder.getObject();

            // Case 1:
            // Single bucket in hand usually returns the changed bucket directly.
            if (!returnedStack.isEmpty()
                    && projectenervate$isBucketLike(returnedStack)
                    && returnedStack.getItem() != snapshot.sourceItem()) {
                AdaptiveEmcOutputHelper.applyCappedAdaptiveStackEmc(
                        returnedStack,
                        snapshot.sourceSingleEmc()
                );
                return;
            }

            // Case 2:
            // A stack of empty buckets usually keeps the remaining empty buckets in hand
            // and inserts the new filled bucket into the inventory.
            projectenervate$applyToNewBucketOutputInInventory(player.getInventory(), snapshot);
        } finally {
            PROJECTENERVATE_BUCKET_USE.remove();
        }
    }

    @Unique
    private static void projectenervate$applyToNewBucketOutputInInventory(
            Inventory inventory,
            BucketUseSnapshot snapshot
    ) {
        for (int slotIndex = 0; slotIndex < inventory.getContainerSize(); slotIndex++) {
            if (snapshot.existingBucketOutputSlots().contains(slotIndex)) {
                continue;
            }

            ItemStack stack = inventory.getItem(slotIndex);

            if (stack.isEmpty()) {
                continue;
            }

            if (!projectenervate$isBucketLike(stack)) {
                continue;
            }

            if (stack.getItem() == snapshot.sourceItem()) {
                continue;
            }

            AdaptiveEmcOutputHelper.applyCappedAdaptiveStackEmc(
                    stack,
                    snapshot.sourceSingleEmc()
            );

            return;
        }
    }

    @Unique
    private static Set<Integer> projectenervate$getExistingBucketOutputSlots(Inventory inventory) {
        Set<Integer> slots = new HashSet<>();

        for (int slotIndex = 0; slotIndex < inventory.getContainerSize(); slotIndex++) {
            ItemStack stack = inventory.getItem(slotIndex);

            if (!stack.isEmpty() && projectenervate$isBucketLike(stack)) {
                slots.add(slotIndex);
            }
        }

        return slots;
    }

    @Unique
    private static boolean projectenervate$isBucketLike(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return projectenervate$isBucketLike(stack.getItem());
    }

    @Unique
    private static boolean projectenervate$isBucketLike(Item item) {
        return item == Items.BUCKET
                || item == Items.WATER_BUCKET
                || item == Items.LAVA_BUCKET
                || item == Items.POWDER_SNOW_BUCKET;
    }

    private record BucketUseSnapshot(
            Item sourceItem,
            BigDecimal sourceSingleEmc,
            Set<Integer> existingBucketOutputSlots
    ) {
    }
}