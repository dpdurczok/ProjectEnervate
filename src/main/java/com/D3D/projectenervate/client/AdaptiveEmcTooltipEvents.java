package com.D3D.projectenervate.client;

import com.D3D.projectenervate.emc.AdaptiveEmcOutputHelper;
import com.D3D.projectenervate.emc.AdaptiveEmcValues;
import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class AdaptiveEmcTooltipEvents {

    private static final DecimalFormat EMC_FORMAT = new DecimalFormat(
            "#,##0.##",
            DecimalFormatSymbols.getInstance(Locale.US)
    );

    private AdaptiveEmcTooltipEvents() {
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (stack.isEmpty()) {
            return;
        }

        if (ProjectEnervateSourceHelper.isProtectedProjectEItem(stack)) {
            return;
        }

        BigDecimal baseSingle = AdaptiveEmcOutputHelper.getBaseSingleEmc(stack);

        if (baseSingle.signum() <= 0) {
            return;
        }

        if (ProjectEnervateSourceHelper.isZero(stack)) {
            applyCorruptedTooltip(event.getToolTip());
            return;
        }

        Optional<BigDecimal> adaptiveValue = AdaptiveEmcValues.get(stack);

        if (adaptiveValue.isPresent()) {
            BigDecimal singleValue = adaptiveValue.get();

            if (singleValue.signum() <= 0) {
                applyCorruptedTooltip(event.getToolTip());
                return;
            }

            applyAdaptiveTooltip(event.getToolTip(), stack, singleValue);
            return;
        }

        if (ProjectEnervateSourceHelper.isVerified(stack)) {
            return;
        }

        // ProjectE's own tooltip runs before this and only knows the base EMC map.
        // For unverified stacks, keep the normal-looking EMC numbers visible, but
        // color the value blue so they are visually distinct from verified EMC.
        applyUnverifiedTooltip(event.getToolTip(), stack, baseSingle);
    }

    private static void applyAdaptiveTooltip(List<Component> tooltip, ItemStack stack, BigDecimal singleValue) {
        BigDecimal stackValue = singleValue
                .multiply(BigDecimal.valueOf(stack.getCount()))
                .setScale(2, RoundingMode.HALF_UP);

        Component emcLine = Component.literal("EMC: " + formatNumber(singleValue))
                .withStyle(ChatFormatting.YELLOW);

        Component stackEmcLine = Component.literal("Stack EMC: " + formatNumber(stackValue))
                .withStyle(ChatFormatting.YELLOW);

        boolean replacedEmc = false;
        boolean replacedStackEmc = false;

        for (int i = 0; i < tooltip.size(); i++) {
            String plainText = tooltip.get(i).getString();

            if (plainText.startsWith("ProjectEnervate Adaptive EMC:")) {
                tooltip.remove(i);
                i--;
                continue;
            }

            if (plainText.startsWith("Stack EMC:")) {
                tooltip.set(i, stackEmcLine);
                replacedStackEmc = true;
                continue;
            }

            if (plainText.startsWith("EMC:")) {
                tooltip.set(i, emcLine);
                replacedEmc = true;
            }
        }

        if (!replacedEmc) {
            tooltip.add(emcLine);
        }

        if (!replacedStackEmc && stack.getCount() > 1) {
            tooltip.add(stackEmcLine);
        }
    }

    private static void applyCorruptedTooltip(List<Component> tooltip) {
        replaceWithSingleEmcLine(
                tooltip,
                Component.literal("EMC: CORRUPTED").withStyle(ChatFormatting.RED)
        );
    }

    private static void applyUnverifiedTooltip(List<Component> tooltip, ItemStack stack, BigDecimal baseSingle) {
        BigDecimal stackValue = baseSingle
                .multiply(BigDecimal.valueOf(stack.getCount()))
                .setScale(2, RoundingMode.HALF_UP);

        Component emcLine = Component.literal("EMC: ")
                .append(Component.literal(formatNumber(baseSingle)).withStyle(ChatFormatting.BLUE));

        Component stackEmcLine = Component.literal("Stack EMC: ")
                .append(Component.literal(formatNumber(stackValue)).withStyle(ChatFormatting.BLUE));

        boolean replacedEmc = false;
        boolean replacedStackEmc = false;

        for (int i = 0; i < tooltip.size(); i++) {
            String plainText = tooltip.get(i).getString();

            if (plainText.startsWith("ProjectEnervate Adaptive EMC:")) {
                tooltip.remove(i);
                i--;
                continue;
            }

            if (plainText.startsWith("Stack EMC:")) {
                if (stack.getCount() > 1) {
                    tooltip.set(i, stackEmcLine);
                    replacedStackEmc = true;
                } else {
                    tooltip.remove(i);
                    i--;
                }
                continue;
            }

            if (plainText.startsWith("EMC:")) {
                tooltip.set(i, emcLine);
                replacedEmc = true;
            }
        }

        if (!replacedEmc) {
            tooltip.add(emcLine);
        }

        if (!replacedStackEmc && stack.getCount() > 1) {
            tooltip.add(stackEmcLine);
        }
    }

    private static void replaceWithSingleEmcLine(List<Component> tooltip, Component replacementLine) {
        boolean replacedEmc = false;

        for (int i = 0; i < tooltip.size(); i++) {
            String plainText = tooltip.get(i).getString();

            if (plainText.startsWith("ProjectEnervate Adaptive EMC:")
                    || plainText.startsWith("Stack EMC:")) {
                tooltip.remove(i);
                i--;
                continue;
            }

            if (plainText.startsWith("EMC:")) {
                tooltip.set(i, replacementLine);
                replacedEmc = true;
            }
        }

        if (!replacedEmc) {
            tooltip.add(replacementLine);
        }
    }

    private static String formatNumber(BigDecimal value) {
        BigDecimal normalized = value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return EMC_FORMAT.format(normalized);
    }
}
