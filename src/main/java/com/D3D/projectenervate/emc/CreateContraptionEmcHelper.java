package com.D3D.projectenervate.emc;

import com.D3D.projectenervate.api.ProjectEnervateCreateContraptionEmcAccess;
import com.D3D.projectenervate.world.PlacedBlockAdaptiveEmcData;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Create contraption support for placed-block EMC.
 *
 * Create turns world blocks into local StructureBlockInfo entries while moving.
 * ProjectEnervate's placed-block EMC is stored externally by world position, so it
 * must be copied into the contraption while assembled and restored to the final
 * world positions when the contraption disassembles.
 */
public final class CreateContraptionEmcHelper {
    private static final String TAG_ROOT = "ProjectEnervatePlacedBlockEmc";
    private static final String TAG_POS = "Pos";
    private static final String TAG_EMC = "Emc";
    private static final String TAG_ALWAYS_APPLY = "AlwaysApply";

    private CreateContraptionEmcHelper() {
    }

    public static void captureFromWorld(Object contraption, Level level, BlockPos sourcePos) {
        if (!(level instanceof ServerLevel serverLevel) || sourcePos == null) {
            return;
        }

        BlockPos anchor = getAnchor(contraption);

        if (anchor == null) {
            return;
        }

        Optional<PlacedBlockAdaptiveEmcData.Entry> storedEntry = PlacedBlockAdaptiveEmcData
                .get(serverLevel)
                .get(sourcePos);

        if (storedEntry.isEmpty()) {
            return;
        }

        Map<Long, PlacedBlockAdaptiveEmcData.Entry> contraptionData = getContraptionData(contraption);

        if (contraptionData == null) {
            return;
        }

        BlockPos localPos = sourcePos.subtract(anchor);
        contraptionData.put(localPos.asLong(), storedEntry.get());
    }

    public static void consumeCapturedSourcesFromWorld(Object contraption, Level level, BlockPos offset) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Map<Long, PlacedBlockAdaptiveEmcData.Entry> contraptionData = getContraptionData(contraption);

        if (contraptionData == null || contraptionData.isEmpty()) {
            return;
        }

        BlockPos anchor = getAnchor(contraption);

        if (anchor == null) {
            return;
        }

        BlockPos safeOffset = offset == null ? BlockPos.ZERO : offset;
        PlacedBlockAdaptiveEmcData worldData = PlacedBlockAdaptiveEmcData.get(serverLevel);

        for (Long localPosLong : contraptionData.keySet()) {
            BlockPos localPos = BlockPos.of(localPosLong);
            BlockPos sourcePos = localPos.offset(anchor).offset(safeOffset);
            worldData.remove(sourcePos);
        }
    }

    public static void restoreToWorld(Object contraption, Level level, Object transform) {
        if (!(level instanceof ServerLevel serverLevel) || transform == null) {
            return;
        }

        Map<Long, PlacedBlockAdaptiveEmcData.Entry> contraptionData = getContraptionData(contraption);

        if (contraptionData == null || contraptionData.isEmpty()) {
            return;
        }

        Map<BlockPos, StructureTemplate.StructureBlockInfo> blocks = getBlocks(contraption);

        if (blocks == null || blocks.isEmpty()) {
            contraptionData.clear();
            return;
        }

        PlacedBlockAdaptiveEmcData worldData = PlacedBlockAdaptiveEmcData.get(serverLevel);
        Iterator<Map.Entry<Long, PlacedBlockAdaptiveEmcData.Entry>> iterator = contraptionData.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Long, PlacedBlockAdaptiveEmcData.Entry> mapEntry = iterator.next();
            BlockPos localPos = BlockPos.of(mapEntry.getKey());
            StructureTemplate.StructureBlockInfo blockInfo = blocks.get(localPos);

            if (blockInfo == null) {
                iterator.remove();
                continue;
            }

            BlockPos targetPos = applyTransform(transform, localPos);

            if (targetPos == null) {
                continue;
            }

            if (!isExpectedBlockAt(serverLevel, targetPos, blockInfo.state())) {
                iterator.remove();
                continue;
            }

            PlacedBlockAdaptiveEmcData.Entry storedEntry = mapEntry.getValue();
            worldData.put(targetPos, storedEntry.emc(), storedEntry.alwaysApply());
            iterator.remove();
        }
    }

    public static void writeToNbt(Object contraption, CompoundTag nbt) {
        if (nbt == null) {
            return;
        }

        Map<Long, PlacedBlockAdaptiveEmcData.Entry> contraptionData = getContraptionData(contraption);

        if (contraptionData == null || contraptionData.isEmpty()) {
            nbt.remove(TAG_ROOT);
            return;
        }

        ListTag entriesTag = new ListTag();

        for (Map.Entry<Long, PlacedBlockAdaptiveEmcData.Entry> mapEntry : contraptionData.entrySet()) {
            PlacedBlockAdaptiveEmcData.Entry storedEntry = mapEntry.getValue();

            if (storedEntry == null || storedEntry.emc() == null || storedEntry.emc().signum() < 0) {
                continue;
            }

            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong(TAG_POS, mapEntry.getKey());
            entryTag.putString(TAG_EMC, storedEntry.emc().toPlainString());
            entryTag.putBoolean(TAG_ALWAYS_APPLY, storedEntry.alwaysApply());
            entriesTag.add(entryTag);
        }

        if (entriesTag.isEmpty()) {
            nbt.remove(TAG_ROOT);
            return;
        }

        nbt.put(TAG_ROOT, entriesTag);
    }

    public static void readFromNbt(Object contraption, CompoundTag nbt) {
        Map<Long, PlacedBlockAdaptiveEmcData.Entry> contraptionData = getContraptionData(contraption);

        if (contraptionData == null) {
            return;
        }

        contraptionData.clear();

        if (nbt == null || !nbt.contains(TAG_ROOT, Tag.TAG_LIST)) {
            return;
        }

        ListTag entriesTag = nbt.getList(TAG_ROOT, Tag.TAG_COMPOUND);

        for (int i = 0; i < entriesTag.size(); i++) {
            CompoundTag entryTag = entriesTag.getCompound(i);

            try {
                long pos = entryTag.getLong(TAG_POS);
                BigDecimal emc = new BigDecimal(entryTag.getString(TAG_EMC));
                boolean alwaysApply = entryTag.getBoolean(TAG_ALWAYS_APPLY);

                if (emc.signum() >= 0) {
                    contraptionData.put(pos, new PlacedBlockAdaptiveEmcData.Entry(emc, alwaysApply));
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private static Map<Long, PlacedBlockAdaptiveEmcData.Entry> getContraptionData(Object contraption) {
        if (contraption instanceof ProjectEnervateCreateContraptionEmcAccess access) {
            return access.projectenervate$getCreateContraptionPlacedBlockEmc();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<BlockPos, StructureTemplate.StructureBlockInfo> getBlocks(Object contraption) {
        Object value = getFieldValue(contraption, "blocks");

        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof BlockPos)) {
                return null;
            }

            if (!(entry.getValue() instanceof StructureTemplate.StructureBlockInfo)) {
                return null;
            }
        }

        return (Map<BlockPos, StructureTemplate.StructureBlockInfo>) map;
    }

    private static BlockPos getAnchor(Object contraption) {
        Object value = getFieldValue(contraption, "anchor");
        return value instanceof BlockPos pos ? pos : null;
    }

    private static BlockPos applyTransform(Object transform, BlockPos localPos) {
        try {
            Method method = transform.getClass().getMethod("apply", BlockPos.class);
            Object result = method.invoke(transform, localPos);
            return result instanceof BlockPos pos ? pos : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static boolean isExpectedBlockAt(ServerLevel level, BlockPos targetPos, BlockState movedState) {
        if (targetPos == null || movedState == null) {
            return false;
        }

        Block placedBlock = level.getBlockState(targetPos).getBlock();
        return placedBlock == movedState.getBlock();
    }

    private static Object getFieldValue(Object owner, String fieldName) {
        if (owner == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }

        Class<?> type = owner.getClass();

        while (type != null) {
            try {
                java.lang.reflect.Field field = type.getDeclaredField(fieldName);
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
}
