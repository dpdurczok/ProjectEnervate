package com.D3D.projectenervate.emc;

import com.D3D.projectenervate.ProjectEnervateConfig;
import com.D3D.projectenervate.world.PlacedBlockAdaptiveEmcData;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Reflection-only Create integration helpers.
 *
 * These methods intentionally do not import Create classes. The mixins that call
 * this helper are optional and only apply when Create is loaded.
 */
public final class CreateEmcIntegrationHelper {
    private static final ThreadLocal<BlockBreakBudgetContext> ACTIVE_BLOCK_BREAK_BUDGET = new ThreadLocal<>();

    private CreateEmcIntegrationHelper() {
    }

    public static BigDecimal getStackInputBudget(ItemStack input) {
        if (input == null || input.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return AdaptiveEmcOutputHelper.getEffectiveStackEmc(input);
    }

    public static void applyRecipeOutputs(ItemStack input, List<ItemStack> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return;
        }

        AdaptiveEmcConversionHelper.applyBudgetToOutputsInPlace(getStackInputBudget(input), outputs);
    }

    public static IItemHandler getItemHandlerField(Object owner, String fieldName) {
        Object value = getFieldValue(owner, fieldName);
        return value instanceof IItemHandler itemHandler ? itemHandler : null;
    }


    public static List<ItemStack> getItemStackListField(Object owner, String fieldName) {
        Object value = getFieldValue(owner, fieldName);

        if (!(value instanceof List<?> list)) {
            return null;
        }

        for (Object entry : list) {
            if (entry != null && !(entry instanceof ItemStack)) {
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        List<ItemStack> itemStacks = (List<ItemStack>) list;
        return itemStacks;
    }

    public static List<ItemStack> snapshotItemStackListField(Object owner, String fieldName) {
        List<ItemStack> list = getItemStackListField(owner, fieldName);
        List<ItemStack> snapshot = new ArrayList<>();

        if (list == null) {
            return snapshot;
        }

        for (ItemStack stack : list) {
            snapshot.add(stack == null ? ItemStack.EMPTY : stack.copy());
        }

        return snapshot;
    }

    public static ItemStack getStackInHandlerSlot(Object owner, String fieldName, int slot) {
        IItemHandler handler = getItemHandlerField(owner, fieldName);

        if (handler == null || slot < 0 || slot >= handler.getSlots()) {
            return ItemStack.EMPTY;
        }

        return handler.getStackInSlot(slot).copy();
    }

    public static void pushBlockBreakBudget(Object blockEntity, BlockState stateToBreak) {
        Level level = getLevel(blockEntity);
        BlockPos breakingPos = getBlockPosField(blockEntity, "breakingPos");

        if (!(level instanceof ServerLevel serverLevel) || breakingPos == null || stateToBreak == null) {
            ACTIVE_BLOCK_BREAK_BUDGET.remove();
            return;
        }

        ACTIVE_BLOCK_BREAK_BUDGET.set(getBlockBudget(serverLevel, breakingPos, stateToBreak));
    }

    public static void popBlockBreakBudget() {
        ACTIVE_BLOCK_BREAK_BUDGET.remove();
    }

    public static void applyActiveBlockBreakBudget(List<ItemStack> drops) {
        BlockBreakBudgetContext context = ACTIVE_BLOCK_BREAK_BUDGET.get();

        if (context == null || drops == null || drops.isEmpty()) {
            return;
        }

        if (!context.naturalBlock()) {
            applyUncappedBlockBudgetToOutputs(context.totalBudget(), drops);
            return;
        }

        if (ProjectEnervateConfig.enableRandomCourses()) {
            List<ItemStack> uncoursedDrops = new ArrayList<>();

            for (ItemStack drop : drops) {
                if (drop == null || drop.isEmpty()) {
                    continue;
                }

                if (!ResourceCourseManager.applyNaturalCourse(context.level(), drop)) {
                    uncoursedDrops.add(drop);
                }
            }

            if (!uncoursedDrops.isEmpty()) {
                AdaptiveEmcConversionHelper.applyBudgetToOutputsInPlace(context.totalBudget(), uncoursedDrops);
            }
            return;
        }

        AdaptiveEmcConversionHelper.applyBudgetToOutputsInPlace(context.totalBudget(), drops);
    }


    private static void applyUncappedBlockBudgetToOutputs(BigDecimal totalBudget, List<ItemStack> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return;
        }

        BigDecimal safeBudget = totalBudget == null ? BigDecimal.ZERO : totalBudget.max(BigDecimal.ZERO);
        BigDecimal totalBaseWeight = BigDecimal.ZERO;

        for (ItemStack output : outputs) {
            if (output == null || output.isEmpty()) {
                continue;
            }

            totalBaseWeight = totalBaseWeight.add(AdaptiveEmcOutputHelper.getBaseStackEmc(output));
        }

        if (totalBaseWeight.signum() <= 0) {
            for (ItemStack output : outputs) {
                if (output != null && !output.isEmpty()) {
                    ProjectEnervateSourceHelper.clearProjectEnervateData(output);
                }
            }
            return;
        }

        for (ItemStack output : outputs) {
            if (output == null || output.isEmpty()) {
                continue;
            }

            BigDecimal baseWeight = AdaptiveEmcOutputHelper.getBaseStackEmc(output);

            if (baseWeight.signum() <= 0) {
                ProjectEnervateSourceHelper.clearProjectEnervateData(output);
                continue;
            }

            BigDecimal outputBudget = safeBudget.multiply(baseWeight).divide(
                    totalBaseWeight,
                    AdaptiveEmcValues.INTERNAL_SCALE,
                    RoundingMode.HALF_UP
            );

            AdaptiveEmcOutputHelper.applyUncappedAdaptiveStackEmc(output, outputBudget);
        }
    }

    private static BlockBreakBudgetContext getBlockBudget(ServerLevel level, BlockPos pos, BlockState stateToBreak) {
        Optional<PlacedBlockAdaptiveEmcData.Entry> storedEntry = PlacedBlockAdaptiveEmcData.get(level).remove(pos);

        if (storedEntry.isPresent()) {
            return new BlockBreakBudgetContext(
                    level,
                    storedEntry.get().emc().max(BigDecimal.ZERO),
                    false
            );
        }

        if (stateToBreak.isAir()) {
            return new BlockBreakBudgetContext(level, BigDecimal.ZERO, true);
        }

        if (stateToBreak.getBlock().asItem() == Items.AIR) {
            return new BlockBreakBudgetContext(level, BigDecimal.ZERO, true);
        }

        ItemStack blockStack = new ItemStack(stateToBreak.getBlock().asItem());
        return new BlockBreakBudgetContext(
                level,
                AdaptiveEmcOutputHelper.getBaseStackEmc(blockStack).max(BigDecimal.ZERO),
                true
        );
    }

    private static Level getLevel(Object owner) {
        Object value = getFieldValue(owner, "level");
        return value instanceof Level level ? level : null;
    }

    private static BlockPos getBlockPosField(Object owner, String fieldName) {
        Object value = getFieldValue(owner, fieldName);
        return value instanceof BlockPos pos ? pos : null;
    }

    private static Object getFieldValue(Object owner, String fieldName) {
        if (owner == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }

        Class<?> type = owner.getClass();

        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return null;
            }
        }

        return null;
    }

    private record BlockBreakBudgetContext(
            ServerLevel level,
            BigDecimal totalBudget,
            boolean naturalBlock
    ) {
    }
}
