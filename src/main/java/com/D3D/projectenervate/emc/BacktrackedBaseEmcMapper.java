package com.D3D.projectenervate.emc;

import com.D3D.projectenervate.ProjectEnervate;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class BacktrackedBaseEmcMapper {
    private static final int MAX_PASSES = 8;
    private static final int LOOT_SAMPLES = 96;
    private static final BigDecimal LONG_MAX = BigDecimal.valueOf(Long.MAX_VALUE);

    private BacktrackedBaseEmcMapper() {
    }

    public static int apply(Object2LongMap<ItemInfo> data, Set<ItemInfo> blockedItems) {
        if (data == null) {
            return 0;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            return 0;
        }

        ServerLevel level = server.overworld();

        if (level == null) {
            return 0;
        }

        HolderLookup.Provider registries = level.registryAccess();
        int totalAdded = 0;

        for (int pass = 0; pass < MAX_PASSES; pass++) {
            Map<ItemInfo, Long> candidates = new HashMap<>();

            collectRecipeCandidates(data, blockedItems, registries, server.getRecipeManager().getRecipes(), candidates);
            collectBlockDropCandidates(data, blockedItems, level, candidates);

            int addedThisPass = applyCandidates(data, blockedItems, candidates);

            if (addedThisPass <= 0) {
                break;
            }

            totalAdded += addedThisPass;
        }

        if (totalAdded > 0) {
            ProjectEnervate.LOGGER.info("ProjectEnervate inferred {} missing base EMC values by reverse mapping recipes and block drops.", totalAdded);
        }

        return totalAdded;
    }

    private static void collectRecipeCandidates(
            Object2LongMap<ItemInfo> data,
            Set<ItemInfo> blockedItems,
            HolderLookup.Provider registries,
            Collection<RecipeHolder<?>> recipes,
            Map<ItemInfo, Long> candidates
    ) {
        if (recipes == null || recipes.isEmpty()) {
            return;
        }

        for (RecipeHolder<?> holder : recipes) {
            if (holder == null) {
                continue;
            }

            Recipe<?> recipe = holder.value();

            if (recipe == null || recipe.isSpecial()) {
                continue;
            }

            ItemStack result;

            try {
                result = recipe.getResultItem(registries);
            } catch (Exception exception) {
                continue;
            }

            if (result.isEmpty()) {
                continue;
            }

            BigDecimal resultEmc = stackEmc(data, result);

            if (resultEmc.signum() <= 0) {
                continue;
            }

            List<IngredientTarget> missingTargets = new ArrayList<>();
            int inputSlotCount = 0;

            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient == null || ingredient.isEmpty()) {
                    continue;
                }

                inputSlotCount++;
                List<ItemInfo> missing = new ArrayList<>();
                Long knownAlternative = null;

                for (ItemStack option : ingredient.getItems()) {
                    ItemInfo optionInfo = itemInfo(option);

                    if (optionInfo == null || blockedItems.contains(optionInfo)) {
                        continue;
                    }

                    long value = currentValue(data, optionInfo);

                    if (value > 0) {
                        knownAlternative = knownAlternative == null ? value : Math.min(knownAlternative, value);
                    } else {
                        missing.add(optionInfo);
                    }
                }

                if (!missing.isEmpty()) {
                    missingTargets.add(new IngredientTarget(missing, knownAlternative));
                }
            }

            if (inputSlotCount <= 0 || missingTargets.isEmpty()) {
                continue;
            }

            BigDecimal perSlot = resultEmc.divide(
                    BigDecimal.valueOf(inputSlotCount),
                    8,
                    RoundingMode.HALF_UP
            );

            for (IngredientTarget target : missingTargets) {
                BigDecimal candidate = perSlot;

                if (target.knownAlternativeValue() != null && target.knownAlternativeValue() > 0) {
                    candidate = candidate.min(BigDecimal.valueOf(target.knownAlternativeValue()));
                }

                long whole = toWholeEmc(candidate);
                addCandidate(candidates, data, blockedItems, target.items(), whole);
            }
        }
    }

    private static void collectBlockDropCandidates(
            Object2LongMap<ItemInfo> data,
            Set<ItemInfo> blockedItems,
            ServerLevel level,
            Map<ItemInfo, Long> candidates
    ) {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block == null || block == Blocks.AIR) {
                continue;
            }

            Item blockItem = block.asItem();

            if (blockItem == Items.AIR) {
                continue;
            }

            ItemInfo blockInfo = ItemInfo.fromItem(blockItem);

            if (blockInfo == null || blockedItems.contains(blockInfo) || currentValue(data, blockInfo) > 0) {
                continue;
            }

            BigDecimal averageDropEmc = averageBlockDropEmc(data, blockedItems, level, block.defaultBlockState(), blockItem);

            if (averageDropEmc.signum() <= 0) {
                continue;
            }

            long whole = toWholeEmc(averageDropEmc);
            addCandidate(candidates, data, blockedItems, blockInfo, whole);
        }
    }

    private static BigDecimal averageBlockDropEmc(
            Object2LongMap<ItemInfo> data,
            Set<ItemInfo> blockedItems,
            ServerLevel level,
            BlockState state,
            Item blockItem
    ) {
        BigDecimal bestAverage = BigDecimal.ZERO;

        for (ItemStack tool : blockDropTools()) {
            BigDecimal total = BigDecimal.ZERO;
            int validSamples = 0;

            for (int sample = 0; sample < LOOT_SAMPLES; sample++) {
                List<ItemStack> drops;

                try {
                    drops = state.getDrops(blockDropParams(level, state, tool.copy()));
                } catch (Exception exception) {
                    return BigDecimal.ZERO;
                }

                BigDecimal sampleValue = BigDecimal.ZERO;
                boolean usefulDrop = false;

                for (ItemStack drop : drops) {
                    if (drop.isEmpty()) {
                        continue;
                    }

                    if (drop.getItem() == blockItem) {
                        continue;
                    }

                    ItemInfo dropInfo = itemInfo(drop);

                    if (dropInfo == null || blockedItems.contains(dropInfo)) {
                        continue;
                    }

                    long singleValue = currentValue(data, dropInfo);

                    if (singleValue <= 0) {
                        continue;
                    }

                    usefulDrop = true;
                    sampleValue = sampleValue.add(BigDecimal.valueOf(singleValue).multiply(BigDecimal.valueOf(drop.getCount())));
                }

                if (usefulDrop) {
                    total = total.add(sampleValue);
                    validSamples++;
                }
            }

            if (validSamples > 0) {
                BigDecimal average = total.divide(BigDecimal.valueOf(validSamples), 8, RoundingMode.HALF_UP);

                if (average.compareTo(bestAverage) > 0) {
                    bestAverage = average;
                }
            }
        }

        return bestAverage;
    }

    private static List<ItemStack> blockDropTools() {
        return List.of(
                ItemStack.EMPTY,
                new ItemStack(Items.DIAMOND_PICKAXE),
                new ItemStack(Items.DIAMOND_AXE),
                new ItemStack(Items.DIAMOND_SHOVEL),
                new ItemStack(Items.DIAMOND_HOE),
                new ItemStack(Items.SHEARS)
        );
    }

    private static LootParams.Builder blockDropParams(ServerLevel level, BlockState state, ItemStack tool) {
        BlockPos pos = BlockPos.ZERO;

        return new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, tool);
    }

    private static int applyCandidates(
            Object2LongMap<ItemInfo> data,
            Set<ItemInfo> blockedItems,
            Map<ItemInfo, Long> candidates
    ) {
        int added = 0;

        for (Map.Entry<ItemInfo, Long> entry : candidates.entrySet()) {
            ItemInfo info = entry.getKey();
            long value = entry.getValue();

            if (info == null || value <= 0 || blockedItems.contains(info) || currentValue(data, info) > 0) {
                continue;
            }

            data.put(info, value);
            added++;
        }

        return added;
    }

    private static void addCandidate(
            Map<ItemInfo, Long> candidates,
            Object2LongMap<ItemInfo> data,
            Set<ItemInfo> blockedItems,
            List<ItemInfo> infos,
            long value
    ) {
        for (ItemInfo info : infos) {
            addCandidate(candidates, data, blockedItems, info, value);
        }
    }

    private static void addCandidate(
            Map<ItemInfo, Long> candidates,
            Object2LongMap<ItemInfo> data,
            Set<ItemInfo> blockedItems,
            ItemInfo info,
            long value
    ) {
        if (info == null || value <= 0 || blockedItems.contains(info) || currentValue(data, info) > 0) {
            return;
        }

        candidates.merge(info, value, Math::min);
    }

    private static BigDecimal stackEmc(Object2LongMap<ItemInfo> data, ItemStack stack) {
        ItemInfo info = itemInfo(stack);

        if (info == null) {
            return BigDecimal.ZERO;
        }

        long singleValue = currentValue(data, info);

        if (singleValue <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(singleValue).multiply(BigDecimal.valueOf(stack.getCount()));
    }

    private static long currentValue(Object2LongMap<ItemInfo> data, ItemInfo info) {
        if (data == null || info == null || !data.containsKey(info)) {
            return 0L;
        }

        long value = data.getLong(info);
        return value > 0 ? value : 0L;
    }

    private static ItemInfo itemInfo(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        return ItemInfo.fromItem(stack.getItem());
    }

    private static long toWholeEmc(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            return 0L;
        }

        BigDecimal rounded = value.setScale(0, RoundingMode.CEILING);

        if (rounded.compareTo(LONG_MAX) > 0) {
            return Long.MAX_VALUE;
        }

        return rounded.longValue();
    }

    public static Set<ItemInfo> collectCustomRemovedItems() {
        Set<ItemInfo> removed = new HashSet<>();

        if (moze_intel.projecte.config.CustomEMCParser.currentEntries == null) {
            return removed;
        }

        for (it.unimi.dsi.fastutil.objects.Object2LongMap.Entry<moze_intel.projecte.api.nss.NSSItem> entry
                : moze_intel.projecte.config.CustomEMCParser.currentEntries.entries().object2LongEntrySet()) {
            if (entry.getLongValue() > 0) {
                continue;
            }

            ItemInfo itemInfo = ItemInfo.fromNSS(entry.getKey());

            if (itemInfo != null) {
                removed.add(itemInfo);
            }
        }

        return removed;
    }

    private record IngredientTarget(List<ItemInfo> items, Long knownAlternativeValue) {
    }
}
