package com.D3D.projectenervate.client;

import com.D3D.projectenervate.emc.AdaptiveEmcValues;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
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

        AdaptiveEmcValues.get(stack).ifPresent(singleValue -> {
            BigDecimal stackValue = singleValue
                    .multiply(BigDecimal.valueOf(stack.getCount()))
                    .setScale(2, RoundingMode.HALF_UP);

            Component emcLine = Component.literal("EMC: " + formatNumber(singleValue))
                    .withStyle(ChatFormatting.YELLOW);

            Component stackEmcLine = Component.literal("Stack EMC: " + formatNumber(stackValue))
                    .withStyle(ChatFormatting.YELLOW);

            List<Component> tooltip = event.getToolTip();

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
        });
    }

    private static String formatNumber(BigDecimal value) {
        BigDecimal normalized = value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return EMC_FORMAT.format(normalized);
    }
}