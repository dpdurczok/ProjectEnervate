package com.D3D.projectenervate.emc;

import java.math.BigDecimal;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

public final class TradeAdaptiveEmcHelper {

    private static final int PAYMENT_SLOT_A = 0;
    private static final int PAYMENT_SLOT_B = 1;

    private TradeAdaptiveEmcHelper() {
    }

    public static void applyToTradeOutput(MerchantContainer container, ItemStack outputStack) {
        if (container == null || outputStack.isEmpty()) {
            return;
        }

        MerchantOffer offer = container.getActiveOffer();

        if (offer == null) {
            AdaptiveEmcValues.remove(outputStack);
            return;
        }

        applyToTradeOutput(container, offer, outputStack);
    }

    public static void applyToTradeOutput(
            MerchantContainer container,
            MerchantOffer offer,
            ItemStack outputStack
    ) {
        if (container == null || offer == null || outputStack.isEmpty()) {
            return;
        }

        BigDecimal paidEmc = projectenervate$getPaidEmc(container, offer);

        AdaptiveEmcOutputHelper.applyCappedAdaptiveStackEmc(outputStack, paidEmc);

        if (paidEmc.signum() > 0) {
            ProjectEnervateSourceHelper.markKnown(
                    outputStack,
                    ProjectEnervateSourceHelper.SOURCE_TRADE
            );
        }
    }

    private static BigDecimal projectenervate$getPaidEmc(
            MerchantContainer container,
            MerchantOffer offer
    ) {
        ItemStack slotA = container.getItem(PAYMENT_SLOT_A);
        ItemStack slotB = container.getItem(PAYMENT_SLOT_B);

        if (offer.satisfiedBy(slotA, slotB)) {
            return projectenervate$getPaymentOrderValue(
                    slotA,
                    slotB,
                    offer.getCostA(),
                    offer.getCostB()
            );
        }

        if (offer.satisfiedBy(slotB, slotA)) {
            return projectenervate$getPaymentOrderValue(
                    slotB,
                    slotA,
                    offer.getCostA(),
                    offer.getCostB()
            );
        }

        return BigDecimal.ZERO;
    }

    private static BigDecimal projectenervate$getPaymentOrderValue(
            ItemStack actualPaymentA,
            ItemStack actualPaymentB,
            ItemStack requiredCostA,
            ItemStack requiredCostB
    ) {
        return projectenervate$getPaymentValue(actualPaymentA, requiredCostA)
                .add(projectenervate$getPaymentValue(actualPaymentB, requiredCostB));
    }

    private static BigDecimal projectenervate$getPaymentValue(
            ItemStack actualPaymentStack,
            ItemStack requiredCostStack
    ) {
        if (actualPaymentStack.isEmpty() || requiredCostStack.isEmpty()) {
            return BigDecimal.ZERO;
        }

        int consumedCount = Math.min(
                actualPaymentStack.getCount(),
                requiredCostStack.getCount()
        );

        if (consumedCount <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal singlePaymentValue = AdaptiveEmcHelper.getSingleMergeValueDecimal(actualPaymentStack);

        if (singlePaymentValue.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        return singlePaymentValue.multiply(BigDecimal.valueOf(consumedCount));
    }
}
