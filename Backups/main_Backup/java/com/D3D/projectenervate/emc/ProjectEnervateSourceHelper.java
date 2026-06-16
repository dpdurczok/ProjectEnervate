package com.D3D.projectenervate.emc;

import java.math.BigDecimal;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class ProjectEnervateSourceHelper {
    public static final String TAG_STATE = "projectenervate_state";

    public static final String STATE_VERIFIED = "verified";
    public static final String STATE_ZERO = "zero";
    public static final String STATE_ADAPTIVE = "adaptive";

    public static final String TAG_SOURCE = "projectenervate_source";

    public static final String SOURCE_COMMAND = "command";
    public static final String SOURCE_TRANSMUTATION = "transmutation";
    public static final String SOURCE_TRACKED_CONVERSION = "tracked_conversion";
    public static final String SOURCE_BLOCK_DROP = "block_drop";
    public static final String SOURCE_LIVING_DROP = "living_drop";
    public static final String SOURCE_UNKNOWN_MINIMUM = "unknown_minimum";
    public static final String SOURCE_CRAFTING = "crafting";
    public static final String SOURCE_TRADE = "trade";
    public static final String SOURCE_PLAYER_INVENTORY = "player_inventory";
    public static final String SOURCE_PLAYER_PICKUP = "player_pickup";
    public static final String SOURCE_CREATIVE_OR_COMMAND = "creative_or_command";

    private ProjectEnervateSourceHelper() {
    }

    public static boolean hasKnownSource(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return hasEconomicState(stack) || AdaptiveEmcValues.has(stack);
    }

    public static void markKnown(ItemStack stack, String source) {
        markVerifiedIfBaseEmc(stack);
    }

    public static void markKnownIfBaseEmc(ItemStack stack, String source) {
        if (stack.isEmpty()) {
            return;
        }

        if (hasKnownSource(stack)) {
            return;
        }

        markVerifiedIfBaseEmc(stack);
    }

    public static boolean markVerifiedIfBaseEmc(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (isProtectedProjectEItem(stack)) {
            return false;
        }

        BigDecimal baseSingle = AdaptiveEmcOutputHelper.getBaseSingleEmc(stack);

        if (baseSingle.signum() <= 0) {
            return false;
        }

        writeState(stack, STATE_VERIFIED, null);
        return true;
    }

    public static boolean markZeroIfBaseEmc(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (isProtectedProjectEItem(stack)) {
            return false;
        }

        BigDecimal baseSingle = AdaptiveEmcOutputHelper.getBaseSingleEmc(stack);

        if (baseSingle.signum() <= 0) {
            return false;
        }

        writeState(stack, STATE_ZERO, BigDecimal.ZERO);
        return true;
    }

    public static void markAdaptive(ItemStack stack, BigDecimal perItemEmc) {
        if (stack.isEmpty()) {
            return;
        }

        if (perItemEmc == null || perItemEmc.signum() <= 0) {
            if (!markZeroIfBaseEmc(stack)) {
                clearProjectEnervateData(stack);
            }
            return;
        }

        BigDecimal baseSingle = AdaptiveEmcOutputHelper.getBaseSingleEmc(stack);

        if (baseSingle.signum() > 0 && perItemEmc.compareTo(baseSingle) >= 0) {
            markVerifiedIfBaseEmc(stack);
            return;
        }

        writeState(stack, STATE_ADAPTIVE, perItemEmc);
    }

    public static void removeSourceMarker(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        if (!tag.contains(TAG_SOURCE) && !tag.contains(TAG_STATE)) {
            return;
        }

        tag.remove(TAG_SOURCE);
        tag.remove(TAG_STATE);

        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static ItemStack copyWithoutSourceMarker(ItemStack original) {
        return copyWithoutProjectEnervateData(original);
    }

    public static boolean enforceUnknownMinimum(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (hasKnownSource(stack)) {
            return false;
        }
        return markUnknownSource(stack);
    }

    public static boolean markUnknownSource(ItemStack stack) {
        return markZeroIfBaseEmc(stack);
    }

    public static boolean hasBaseEmc(ItemStack stack) {
        return AdaptiveEmcOutputHelper.getBaseSingleEmc(stack).signum() > 0;
    }

    public static boolean hasSourceMarker(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        return tag.contains(TAG_STATE);
    }

    public static boolean hasEconomicState(ItemStack stack) {
        return hasSourceMarker(stack);
    }

    public static boolean hasProjectEnervateData(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        return tag.contains(TAG_STATE)
                || tag.contains(TAG_SOURCE)
                || tag.contains(AdaptiveEmcValues.TAG_ADAPTIVE_EMC);
    }

    public static boolean isVerified(ItemStack stack) {
        return STATE_VERIFIED.equals(getState(stack));
    }

    public static boolean isZero(ItemStack stack) {
        return STATE_ZERO.equals(getState(stack));
    }

    public static boolean isAdaptive(ItemStack stack) {
        String state = getState(stack);

        if (STATE_ADAPTIVE.equals(state)) {
            return true;
        }

        return state == null && AdaptiveEmcValues.has(stack);
    }

    public static void clearProjectEnervateData(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        tag.remove(TAG_STATE);
        tag.remove(TAG_SOURCE);
        tag.remove(AdaptiveEmcValues.TAG_ADAPTIVE_EMC);

        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static ItemStack copyWithoutProjectEnervateData(ItemStack original) {
        ItemStack copy = original.copy();
        clearProjectEnervateData(copy);
        return copy;
    }

    public static void copyEconomicState(ItemStack from, ItemStack to) {
        if (from.isEmpty() || to.isEmpty()) {
            return;
        }

        if (isZero(from)) {
            if (!markZeroIfBaseEmc(to)) {
                clearProjectEnervateData(to);
            }
            return;
        }

        AdaptiveEmcValues.get(from).ifPresentOrElse(
                value -> markAdaptive(to, value),
                () -> {
                    if (isVerified(from)) {
                        if (!markVerifiedIfBaseEmc(to)) {
                            clearProjectEnervateData(to);
                        }
                    } else {
                        clearProjectEnervateData(to);
                    }
                }
        );
    }

    private static String getState(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        if (!tag.contains(TAG_STATE)) {
            return null;
        }

        return tag.getString(TAG_STATE);
    }

    private static void writeState(ItemStack stack, String state, BigDecimal adaptiveValue) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(TAG_SOURCE);
            tag.putString(TAG_STATE, state);

            if (adaptiveValue == null) {
                tag.remove(AdaptiveEmcValues.TAG_ADAPTIVE_EMC);
            } else {
                tag.putString(
                        AdaptiveEmcValues.TAG_ADAPTIVE_EMC,
                        AdaptiveEmcValues.normalizeInternal(adaptiveValue).toPlainString()
                );
            }
        });
    }

    private static boolean isProtectedProjectEItem(ItemStack stack) {
        if (stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY) != null) {
            return true;
        }

        return stack.is(PEItems.TOME_OF_KNOWLEDGE);
    }
}
