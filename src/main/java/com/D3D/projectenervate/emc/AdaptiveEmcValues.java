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
        write(stack, normalized);
    }

    public static void setExact(ItemStack stack, BigDecimal value) {
        if (stack.isEmpty()) {
            return;
        }

        BigDecimal normalized = normalizeInternal(value);
        write(stack, normalized);
    }

    private static void write(ItemStack stack, BigDecimal value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag ->
                tag.putString(TAG_ADAPTIVE_EMC, value.toPlainString())
        );
    }

    public static Optional<BigDecimal> get(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
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

        if (!tag.contains(TAG_ADAPTIVE_EMC)) {
            return;
        }

        tag.remove(TAG_ADAPTIVE_EMC);

        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static ItemStack copyWithoutAdaptiveEmc(ItemStack original) {
        ItemStack copy = original.copy();

        CustomData customData = copy.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        tag.remove(TAG_ADAPTIVE_EMC);
        tag.remove(ProjectEnervateSourceHelper.TAG_SOURCE);

        if (tag.isEmpty()) {
            copy.remove(DataComponents.CUSTOM_DATA);
        } else {
            copy.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }

        return copy;
    }

    public static String format(BigDecimal value) {
        return normalize(value).stripTrailingZeros().toPlainString();
    }
}