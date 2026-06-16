package com.D3D.projectenervate.emc;

import java.math.BigDecimal;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class ProjectEnervateSourceHelper {
    public static final String TAG_SOURCE = "projectenervate_source";

    private static final String KNOWN_SOURCE_VALUE = "known";

    public static final String SOURCE_COMMAND = "command";
    public static final String SOURCE_TRANSMUTATION = "transmutation";
    public static final String SOURCE_TRACKED_CONVERSION = "tracked_conversion";
    public static final String SOURCE_BLOCK_DROP = "block_drop";
    public static final String SOURCE_LIVING_DROP = "living_drop";
    public static final String SOURCE_UNKNOWN_MINIMUM = "unknown_minimum";
    public static final String SOURCE_CRAFTING = "crafting";
    public static final String SOURCE_PLAYER_INVENTORY = "player_inventory";
    public static final String SOURCE_PLAYER_PICKUP = "player_pickup";
    public static final String SOURCE_CREATIVE_OR_COMMAND = "creative_or_command";

    private static final BigDecimal UNKNOWN_SOURCE_SINGLE_ITEM_EMC = BigDecimal.ONE;

    private ProjectEnervateSourceHelper() {
    }

    public static boolean hasKnownSource(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (AdaptiveEmcValues.has(stack)) {
            return true;
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        return tag.contains(TAG_SOURCE);
    }

    public static void markKnown(ItemStack stack, String source) {
        if (stack.isEmpty()) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TAG_SOURCE, KNOWN_SOURCE_VALUE);
        });
    }

    public static void markKnownIfBaseEmc(ItemStack stack, String source) {
        if (stack.isEmpty()) {
            return;
        }

        if (isProtectedProjectEItem(stack)) {
            return;
        }

        BigDecimal baseSingle = AdaptiveEmcOutputHelper.getBaseSingleEmc(stack);

        if (baseSingle.signum() <= 0) {
            return;
        }

        markKnown(stack, source);
    }

    public static void removeSourceMarker(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        if (!tag.contains(TAG_SOURCE)) {
            return;
        }

        tag.remove(TAG_SOURCE);

        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static ItemStack copyWithoutSourceMarker(ItemStack original) {
        ItemStack copy = original.copy();
        removeSourceMarker(copy);
        return copy;
    }

    public static boolean enforceUnknownMinimum(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (hasKnownSource(stack)) {
            return false;
        }

        if (isProtectedProjectEItem(stack)) {
            return false;
        }

        BigDecimal baseSingle = AdaptiveEmcOutputHelper.getBaseSingleEmc(stack);

        if (baseSingle.signum() <= 0) {
            return false;
        }

        BigDecimal proposedStackEmc = UNKNOWN_SOURCE_SINGLE_ITEM_EMC
                .multiply(BigDecimal.valueOf(stack.getCount()));

        AdaptiveEmcOutputHelper.applyCappedAdaptiveStackEmc(stack, proposedStackEmc);
        markKnown(stack, SOURCE_UNKNOWN_MINIMUM);

        return true;
    }

    private static boolean isProtectedProjectEItem(ItemStack stack) {
        if (stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY) != null) {
            return true;
        }

        return stack.is(PEItems.TOME_OF_KNOWLEDGE);
    }
}