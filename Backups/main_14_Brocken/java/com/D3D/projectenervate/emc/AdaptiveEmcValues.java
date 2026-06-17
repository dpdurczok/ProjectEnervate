package com.D3D.projectenervate.emc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class AdaptiveEmcValues {

    public static final String TAG_ADAPTIVE_EMC = "projectenervate_adaptive_emc";

    public static final int DISPLAY_SCALE = 2;
    public static final int INTERNAL_SCALE = 8;

    private AdaptiveEmcValues() {
    }

    public static BigDecimal normalize(BigDecimal value) {
        return value.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal normalizeInternal(BigDecimal value) {
        return value.setScale(INTERNAL_SCALE, RoundingMode.HALF_UP);
    }

    public static void set(ItemStack stack, BigDecimal value) {
        if (stack.isEmpty()) {
            return;
        }

        BigDecimal normalized = normalize(value);
        setExact(stack, normalized);
    }

    public static void setExact(ItemStack stack, BigDecimal value) {
        if (stack.isEmpty()) {
            return;
        }

        BigDecimal normalized = normalizeInternal(value);
        ProjectEnervateSourceHelper.markAdaptive(stack, normalized);
    }

    public static Optional<BigDecimal> get(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        if (ProjectEnervateSourceHelper.isZero(stack)) {
            return Optional.of(normalizeInternal(BigDecimal.ZERO));
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        if (!tag.contains(TAG_ADAPTIVE_EMC)) {
            return Optional.empty();
        }

        try {
            return Optional.of(normalizeInternal(new BigDecimal(tag.getString(TAG_ADAPTIVE_EMC))));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static boolean has(ItemStack stack) {
        return get(stack).isPresent();
    }

    public static void remove(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        if (!tag.contains(TAG_ADAPTIVE_EMC)
                && !ProjectEnervateSourceHelper.isAdaptive(stack)
                && !ProjectEnervateSourceHelper.isZero(stack)) {
            return;
        }

        tag.remove(TAG_ADAPTIVE_EMC);
        tag.remove(ProjectEnervateSourceHelper.TAG_SOURCE);

        String state = tag.getString(ProjectEnervateSourceHelper.TAG_STATE);

        if (ProjectEnervateSourceHelper.STATE_ADAPTIVE.equals(state)
                || ProjectEnervateSourceHelper.STATE_ZERO.equals(state)) {
            tag.remove(ProjectEnervateSourceHelper.TAG_STATE);
        }

        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static ItemStack copyWithoutAdaptiveEmc(ItemStack original) {
        return ProjectEnervateSourceHelper.copyWithoutProjectEnervateData(original);
    }

    public static String format(BigDecimal value) {
        return normalize(value).stripTrailingZeros().toPlainString();
    }
}
