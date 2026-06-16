package com.D3D.projectenervate.world;

import com.D3D.projectenervate.ProjectEnervate;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class PlacedBlockAdaptiveEmcData extends SavedData {

    private static final String DATA_NAME = ProjectEnervate.MOD_ID + "_placed_block_adaptive_emc";

    private final Map<Long, Entry> valuesByPos = new HashMap<>();

    public static PlacedBlockAdaptiveEmcData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        PlacedBlockAdaptiveEmcData::new,
                        PlacedBlockAdaptiveEmcData::load,
                        null
                ),
                DATA_NAME
        );
    }

    public static PlacedBlockAdaptiveEmcData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        PlacedBlockAdaptiveEmcData data = new PlacedBlockAdaptiveEmcData();

        CompoundTag valuesTag = tag.getCompound("values");

        for (String key : valuesTag.getAllKeys()) {
            try {
                long posLong = Long.parseLong(key);
                Tag rawTag = valuesTag.get(key);

                if (rawTag instanceof CompoundTag entryTag) {
                    BigDecimal value = new BigDecimal(entryTag.getString("emc"));
                    boolean alwaysApply = entryTag.getBoolean("alwaysApply");

                    if (value.signum() >= 0) {
                        data.valuesByPos.put(posLong, new Entry(value, alwaysApply));
                    }
                } else {
                    // Backwards compatibility with the older format:
                    // values[pos] = "12.5"
                    BigDecimal value = new BigDecimal(valuesTag.getString(key));

                    if (value.signum() >= 0) {
                        data.valuesByPos.put(posLong, new Entry(value, true));
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag valuesTag = new CompoundTag();

        for (Map.Entry<Long, Entry> mapEntry : valuesByPos.entrySet()) {
            Entry entry = mapEntry.getValue();

            if (entry.emc().signum() < 0) {
                continue;
            }

            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("emc", entry.emc().toPlainString());
            entryTag.putBoolean("alwaysApply", entry.alwaysApply());

            valuesTag.put(Long.toString(mapEntry.getKey()), entryTag);
        }

        tag.put("values", valuesTag);
        return tag;
    }

    public void put(BlockPos pos, BigDecimal value, boolean alwaysApply) {
        if (value == null || value.signum() < 0) {
            remove(pos);
            return;
        }

        valuesByPos.put(pos.asLong(), new Entry(value, alwaysApply));
        setDirty();
    }

    public Optional<Entry> get(BlockPos pos) {
        return Optional.ofNullable(valuesByPos.get(pos.asLong()));
    }

    public Optional<Entry> remove(BlockPos pos) {
        Entry removed = valuesByPos.remove(pos.asLong());

        if (removed != null) {
            setDirty();
            return Optional.of(removed);
        }

        return Optional.empty();
    }

    public record Entry(BigDecimal emc, boolean alwaysApply) {
    }
}
