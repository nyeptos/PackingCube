package github.nyeptos.packingcube.bag.pickup;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Settings belong to the item, independently of its filter rules and installed position. */
public final class BagPickupSettings {
    private static final String KEY = "packingcube_pickup";
    public static final int MIN_PRIORITY = 0;
    public static final int MAX_PRIORITY = 9;

    private BagPickupSettings() {}

    /**
     * Missing settings preserve automatic pickup for bags created before these controls existed.
     */
    public static boolean enabled(ItemStack bag) {
        var settings =
                bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag()
                        .getCompound(KEY);
        return !settings.contains("enabled") || settings.getBoolean("enabled");
    }

    public static int priority(ItemStack bag) {
        int value =
                bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag()
                        .getCompound(KEY)
                        .getInt("priority");
        return Math.clamp(value, MIN_PRIORITY, MAX_PRIORITY);
    }

    public static void setEnabled(ItemStack bag, boolean enabled) {
        var data = bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var settings = data.getCompound(KEY);
        settings.putBoolean("enabled", enabled);
        data.put(KEY, settings);
        bag.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    public static void setPriority(ItemStack bag, int priority) {
        if (priority < MIN_PRIORITY || priority > MAX_PRIORITY)
            throw new IllegalArgumentException("Pickup priority must be between 0 and 9");
        var data = bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var settings = data.getCompound(KEY);
        settings.putInt("priority", priority);
        data.put(KEY, settings);
        bag.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }
}
