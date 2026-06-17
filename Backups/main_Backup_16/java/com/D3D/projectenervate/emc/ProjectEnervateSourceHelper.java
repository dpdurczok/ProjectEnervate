package com.D3D.projectenervate.emc;

import com.D3D.projectenervate.ProjectEnervateConfig;
import java.math.BigDecimal;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Canonical ProjectEnervate economic-state helper.
 *
 * Stack data is intentionally limited to these economic states:
 * - verified: base ProjectE EMC is trusted.
 * - zero: base ProjectE EMC exists, but this stack is untrusted and sells for 0.
 * - adaptive: this stack sells for a capped per-item EMC below base.
 *
 * The old projectenervate_source key is treated as legacy data only. It is removed
 * whenever a canonical state is written and is ignored for stack identity helpers.
 */
public final class ProjectEnervateSourceHelper {
    public static final String TAG_STATE = "projectenervate_state";
    public static final String TAG_SOURCE = "projectenervate_source";

    public static final String STATE_VERIFIED = "verified";
    public static final String STATE_ZERO = "zero";
    public static final String STATE_ADAPTIVE = "adaptive";

    // Legacy constants kept so older call sites still compile. These values are never written.
    @Deprecated public static final String SOURCE_COMMAND = "command";
    @Deprecated public static final String SOURCE_TRANSMUTATION = "transmutation";
    @Deprecated public static final String SOURCE_TRACKED_CONVERSION = "tracked_conversion";
    @Deprecated public static final String SOURCE_BLOCK_DROP = "block_drop";
    @Deprecated public static final String SOURCE_LIVING_DROP = "living_drop";
    @Deprecated public static final String SOURCE_UNKNOWN_MINIMUM = "unknown_minimum";
    @Deprecated public static final String SOURCE_CRAFTING = "crafting";
    @Deprecated public static final String SOURCE_TRADE = "trade";
    @Deprecated public static final String SOURCE_PLAYER_INVENTORY = "player_inventory";
    @Deprecated public static final String SOURCE_PLAYER_PICKUP = "player_pickup";
    @Deprecated public static final String SOURCE_CREATIVE_OR_COMMAND = "creative_or_command";

    private ProjectEnervateSourceHelper() {
    }

    public static void markKnown(ItemStack stack, String ignoredLegacySource) {
        markVerifiedIfBaseEmcPreservingExisting(stack);
    }

    public static void markKnownIfBaseEmc(ItemStack stack, String ignoredLegacySource) {
        markVerifiedIfBaseEmcPreservingExisting(stack);
    }

    public static boolean markVerifiedIfBaseEmc(ItemStack stack) {
        if (!canCarryProjectEnervateState(stack)) {
            return false;
        }

        if (AdaptiveEmcOutputHelper.getBaseSingleEmc(stack).signum() <= 0) {
            clearProjectEnervateData(stack);
            return false;
        }

        markVerified(stack);
        return true;
    }

    /**
     * Marks a normal base-EMC stack as verified, but never downgrades an existing
     * zero/adaptive/assigned ProjectEnervate state. Use this for generic pickup,
     * GUI, recipe, and output-display paths that only need to trust plain stacks.
     */
    public static boolean markVerifiedIfBaseEmcPreservingExisting(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (hasProjectEnervateData(stack)) {
            return hasBaseEmc(stack) || AdaptiveEmcValues.has(stack);
        }

        return markVerifiedIfBaseEmc(stack);
    }

    public static void markVerified(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        writeState(stack, STATE_VERIFIED, null);
    }

    public static boolean markZeroIfBaseEmc(ItemStack stack) {
        if (!canCarryProjectEnervateState(stack)) {
            return false;
        }

        if (AdaptiveEmcOutputHelper.getBaseSingleEmc(stack).signum() <= 0) {
            clearProjectEnervateData(stack);
            return false;
        }

        markZero(stack);
        return true;
    }

    public static void markZero(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        writeState(stack, STATE_ZERO, BigDecimal.ZERO);
    }

    public static void markAdaptive(ItemStack stack, BigDecimal perItemEmc) {
        if (stack.isEmpty()) {
            return;
        }

        if (!ProjectEnervateConfig.adaptiveEmc()) {
            if (!markVerifiedIfBaseEmc(stack)) {
                clearProjectEnervateData(stack);
            }
            return;
        }

        if (perItemEmc == null || perItemEmc.signum() <= 0) {
            if (!markZeroIfBaseEmc(stack)) {
                clearProjectEnervateData(stack);
            }
            return;
        }

        BigDecimal baseSingle = AdaptiveEmcOutputHelper.getBaseSingleEmc(stack);

        if (baseSingle.signum() <= 0) {
            clearProjectEnervateData(stack);
            return;
        }

        if (perItemEmc.compareTo(baseSingle) >= 0) {
            if (ProjectEnervateConfig.chooseBaseEmcIfLower()
                    || perItemEmc.compareTo(baseSingle) == 0) {
                markVerified(stack);
            } else {
                markAssignedEmc(stack, perItemEmc);
            }
            return;
        }

        writeState(stack, STATE_ADAPTIVE, AdaptiveEmcValues.normalizeInternal(perItemEmc));
    }

    /**
     * Assigns a positive per-item EMC value to a stack that has no ProjectE base EMC.
     * This is intentionally separate from markAdaptive: adaptive conversion outputs
     * are still capped against ProjectE base EMC, while the assignment station is a
     * paid opt-in path for otherwise unvalued items.
     */
    public static void markAssignedEmc(ItemStack stack, BigDecimal perItemEmc) {
        if (stack.isEmpty() || isProtectedProjectEItem(stack)) {
            return;
        }

        if (perItemEmc == null || perItemEmc.signum() <= 0) {
            clearProjectEnervateData(stack);
            return;
        }

        writeState(stack, STATE_ADAPTIVE, AdaptiveEmcValues.normalizeInternal(perItemEmc));
    }

    public static boolean enforceUnknownMinimum(ItemStack stack) {
        if (!ProjectEnervateConfig.voidUnknownSources()) {
            return false;
        }

        if (stack.isEmpty() || hasProjectEnervateState(stack)) {
            return false;
        }

        return markUnknownSource(stack);
    }

    public static boolean markUnknownSource(ItemStack stack) {
        if (!ProjectEnervateConfig.voidUnknownSources()) {
            return false;
        }

        return markZeroIfBaseEmc(stack);
    }

    public static boolean hasKnownSource(ItemStack stack) {
        return hasProjectEnervateState(stack);
    }

    public static boolean hasSourceMarker(ItemStack stack) {
        return hasProjectEnervateState(stack);
    }

    public static boolean hasEconomicState(ItemStack stack) {
        return hasProjectEnervateState(stack);
    }

    public static boolean hasProjectEnervateState(ItemStack stack) {
        return getState(stack) != null;
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

    public static boolean hasBaseEmc(ItemStack stack) {
        return AdaptiveEmcOutputHelper.getBaseSingleEmc(stack).signum() > 0;
    }

    public static String getState(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        if (tag.contains(TAG_STATE)) {
            String state = tag.getString(TAG_STATE);

            if (STATE_VERIFIED.equals(state)
                    || STATE_ZERO.equals(state)
                    || STATE_ADAPTIVE.equals(state)) {
                return state;
            }

            clearProjectEnervateData(stack);
            return null;
        }

        if (tag.contains(TAG_SOURCE)) {
            // One-time migration from old source strings. Preserve the economic meaning
            // but canonicalize to a stack-friendly verified state.
            if (canCarryProjectEnervateState(stack)
                    && AdaptiveEmcOutputHelper.getBaseSingleEmc(stack).signum() > 0) {
                markVerified(stack);
                return STATE_VERIFIED;
            }

            clearProjectEnervateData(stack);
        }

        return null;
    }

    public static boolean isVerified(ItemStack stack) {
        return STATE_VERIFIED.equals(getState(stack));
    }

    public static boolean isZero(ItemStack stack) {
        return ProjectEnervateConfig.voidUnknownSources() && STATE_ZERO.equals(getState(stack));
    }

    public static boolean isAdaptive(ItemStack stack) {
        if (STATE_ADAPTIVE.equals(getState(stack))) {
            return true;
        }

        return hasLegacyAdaptiveValueOnly(stack);
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
        writeRawTag(stack, tag);
    }

    public static ItemStack copyWithoutSourceMarker(ItemStack original) {
        return copyWithoutProjectEnervateData(original);
    }

    public static ItemStack copyWithoutProjectEnervateData(ItemStack original) {
        ItemStack copy = original.copy();
        clearProjectEnervateData(copy);
        return copy;
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

        writeRawTag(stack, tag);
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
                value -> {
                    BigDecimal targetBaseSingle = AdaptiveEmcOutputHelper.getBaseSingleEmc(to);

                    if (targetBaseSingle.signum() > 0 && value.compareTo(targetBaseSingle) <= 0) {
                        markAdaptive(to, value);
                    } else if (targetBaseSingle.signum() > 0 && ProjectEnervateConfig.chooseBaseEmcIfLower()) {
                        markVerified(to);
                    } else {
                        markAssignedEmc(to, value);
                    }
                },
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

    private static boolean canCarryProjectEnervateState(ItemStack stack) {
        return !stack.isEmpty() && !isProtectedProjectEItem(stack);
    }

    private static boolean hasLegacyAdaptiveValueOnly(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        return !tag.contains(TAG_STATE) && tag.contains(AdaptiveEmcValues.TAG_ADAPTIVE_EMC);
    }

    private static void writeState(ItemStack stack, String state, BigDecimal adaptiveValue) {
        if (stack.isEmpty()) {
            return;
        }

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

    private static void writeRawTag(ItemStack stack, CompoundTag tag) {
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static boolean isProtectedProjectEItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY) != null) {
            return true;
        }

        return stack.is(PEItems.TOME_OF_KNOWLEDGE);
    }
}
