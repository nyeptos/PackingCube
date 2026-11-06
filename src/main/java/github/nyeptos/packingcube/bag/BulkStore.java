package github.nyeptos.packingcube.bag;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * The count of a bulk pouch. The item itself stays in the pouch's container slot as a one-item
 * template, because a saved stack may not carry more than 99.
 */
public final class BulkStore {
    public static final int LIMIT = 4096;
    private static final String KEY = "packingcube_bulk";
    private static final String COUNT = "count";

    private BulkStore() {}

    private static CompoundTag state(ItemStack pouch) {
        CompoundTag tag =
                pouch.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(KEY) ? tag.getCompound(KEY).copy() : new CompoundTag();
    }

    private static void save(ItemStack pouch, CompoundTag state) {
        CompoundTag tag =
                pouch.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (state.isEmpty()) tag.remove(KEY);
        else tag.put(KEY, state);
        if (tag.isEmpty()) pouch.remove(DataComponents.CUSTOM_DATA);
        else pouch.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int count(ItemStack pouch) {
        return clamp(state(pouch).getInt(COUNT), 0, LIMIT);
    }

    public static void setCount(ItemStack pouch, int count) {
        CompoundTag state = state(pouch);
        int clamped = clamp(count, 0, LIMIT);
        if (clamped == 0) state.remove(COUNT);
        else state.putInt(COUNT, clamped);
        save(pouch, state);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
