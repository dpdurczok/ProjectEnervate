package com.D3D.projectenervate.station;

import com.D3D.projectenervate.emc.AdaptiveEmcOutputHelper;
import com.D3D.projectenervate.emc.AdaptiveEmcValues;
import com.D3D.projectenervate.emc.ProjectEBaseEmcAssignmentHelper;
import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Optional;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage.EmcAction;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class EmcAssignmentHelper {
    public static final BigDecimal COST_MULTIPLIER = BigDecimal.valueOf(5);
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    private EmcAssignmentHelper() {
    }

    public static boolean isEmcHolder(ItemStack stack) {
        return getEmcHolder(stack) != null;
    }

    public static IItemEmcHolder getEmcHolder(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        return stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY);
    }

    public static Optional<BigDecimal> parseValue(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            BigDecimal value = new BigDecimal(rawValue.trim());

            if (value.signum() <= 0) {
                return Optional.empty();
            }

            return Optional.of(AdaptiveEmcValues.normalizeInternal(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static BigInteger getCost(BigDecimal perItemEmc, int ignoredItemCount) {
        if (perItemEmc == null || perItemEmc.signum() <= 0) {
            return BigInteger.ZERO;
        }

        return perItemEmc
                .multiply(COST_MULTIPLIER)
                .setScale(0, RoundingMode.CEILING)
                .toBigInteger();
    }

    public static long getStoredEmc(ItemStack starStack) {
        IItemEmcHolder emcHolder = getEmcHolder(starStack);
        return emcHolder == null ? 0L : emcHolder.getStoredEmc(starStack);
    }

    public static boolean itemAlreadyHasEmc(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (ProjectEnervateSourceHelper.hasProjectEnervateData(stack)) {
            return true;
        }

        return AdaptiveEmcOutputHelper.getBaseSingleEmc(stack).signum() > 0;
    }

    public static String getFailureReason(ItemStack starStack, ItemStack targetStack, String rawValue) {
        if (targetStack.isEmpty()) {
            return "place an item first";
        }

        if (ProjectEnervateSourceHelper.isProtectedProjectEItem(targetStack)) {
            return "item already has emc";
        }

        if (itemAlreadyHasEmc(targetStack)) {
            return "item already has emc";
        }

        Optional<BigDecimal> parsedValue = parseValue(rawValue);

        if (parsedValue.isEmpty()) {
            return "input emc amount";
        }

        IItemEmcHolder emcHolder = getEmcHolder(starStack);

        if (emcHolder == null) {
            return "insert a Klein star";
        }

        BigInteger cost = getCost(parsedValue.get(), 1);

        if (cost.signum() <= 0 || cost.compareTo(LONG_MAX) > 0) {
            return "input emc amount";
        }

        if (BigInteger.valueOf(emcHolder.getStoredEmc(starStack)).compareTo(cost) < 0) {
            return "not enough emc in the Klein star";
        }

        return "";
    }

    public static boolean canApply(ItemStack starStack, ItemStack targetStack, String rawValue) {
        return getFailureReason(starStack, targetStack, rawValue).isEmpty();
    }

    public static boolean apply(ServerPlayer player, ItemStack starStack, ItemStack targetStack, String rawValue) {
        Optional<BigDecimal> parsedValue = parseValue(rawValue);

        if (parsedValue.isEmpty() || !canApply(starStack, targetStack, rawValue)) {
            return false;
        }

        BigDecimal perItemEmc = parsedValue.get();
        BigInteger cost = getCost(perItemEmc, 1);

        if (cost.compareTo(LONG_MAX) > 0) {
            return false;
        }

        IItemEmcHolder emcHolder = getEmcHolder(starStack);

        if (emcHolder == null) {
            return false;
        }

        long wholeBaseEmc = ProjectEBaseEmcAssignmentHelper.toWholeEmc(perItemEmc);

        if (wholeBaseEmc <= 0) {
            return false;
        }

        long extracted = emcHolder.extractEmc(starStack, cost.longValue(), EmcAction.EXECUTE);

        if (extracted != cost.longValue()) {
            return false;
        }

        if (!ProjectEBaseEmcAssignmentHelper.assignBaseEmc(player, targetStack, wholeBaseEmc)) {
            emcHolder.insertEmc(starStack, extracted, EmcAction.EXECUTE);
            return false;
        }

        return true;
    }
}
