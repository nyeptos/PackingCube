package github.nyeptos.packingcube.bag.refill;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Stable slot identities. Never store the current visual order of Curios slots. */
public final class RefillTargets {
    private static final String KEY = "packingcube_refill_targets";
    public static final int MAX_OVERRIDES = 512;

    private RefillTargets() {}

    public static String inventory(int index) {
        return "inventory:" + index;
    }

    public static String curios(String type, int index) {
        return "curios:" + type + ":" + index;
    }

    public static boolean valid(String id) {
        if (id.startsWith("inventory:")) {
            try {
                int slot = Integer.parseInt(id.substring(10));
                return slot >= 0 && slot <= 40 && id.equals(inventory(slot));
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        if (id.length() > 240 || !id.matches("curios:[a-z0-9_.-]+:[0-9]{1,5}")) return false;
        String index = id.substring(id.lastIndexOf(':') + 1);
        return index.equals(Integer.toString(Integer.parseInt(index)));
    }

    public static boolean selected(ItemStack pouch, String id) {
        if (!valid(id)) return false;
        var map =
                pouch.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag()
                        .getCompound(KEY);
        if (map.contains(id)) return map.getBoolean(id);
        // Retain old pouch settings. Newly supported inventory, armor and Curios start unselected.
        if (!id.startsWith("inventory:")) return false;
        int slot = Integer.parseInt(id.substring(10));
        if (slot == RefillSettings.OFFHAND_SLOT) return RefillSettings.offhandEnabled(pouch);
        return slot < 9 && (RefillSettings.slots(pouch) & (1 << slot)) != 0;
    }

    public static boolean set(ItemStack pouch, String id, boolean selected) {
        if (!valid(id)) return false;
        var data = pouch.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var map = data.getCompound(KEY);
        if (!map.contains(id) && map.size() >= MAX_OVERRIDES) return false;
        // Keep old consumers in agreement when changing an existing destination.
        if (id.startsWith("inventory:")) {
            int slot = Integer.parseInt(id.substring(10));
            if (slot < 9)
                RefillSettings.set(
                        pouch,
                        "slots",
                        Integer.toString(
                                selected
                                        ? RefillSettings.slots(pouch) | (1 << slot)
                                        : RefillSettings.slots(pouch) & ~(1 << slot)));
            if (slot == RefillSettings.OFFHAND_SLOT)
                RefillSettings.set(pouch, "offhand", Boolean.toString(selected));
            data = pouch.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        }
        map.putBoolean(id, selected);
        data.put(KEY, map);
        pouch.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return true;
    }

    public static void copy(ItemStack source, ItemStack destination) {
        var data = destination.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.remove(KEY);
        var values =
                source.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag()
                        .getCompound(KEY);
        var clean = new CompoundTag();
        for (String key : values.getAllKeys()) {
            if (valid(key) && clean.size() < MAX_OVERRIDES)
                clean.putBoolean(key, values.getBoolean(key));
        }
        if (!clean.isEmpty()) data.put(KEY, clean);
        destination.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }
}
