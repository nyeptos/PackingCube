package github.nyeptos.packingcube.bag.refill;

import github.nyeptos.packingcube.bag.filter.BagFilter;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Bounded, item-owned settings. Filter rules restrict withdrawal, never storage. */
public final class RefillSettings {
    private static final String KEY = "packingcube_refill";
    public static final int OFFHAND_SLOT = 40;
    public static final int SETTINGS_ROWS = 10;
    public static final java.util.List<String> TIMINGS =
            java.util.List.of("empty", "threshold", "use");

    private RefillSettings() {}

    public static CustomData defaults() {
        var data = new CompoundTag();
        var filter = new CompoundTag();
        filter.putString("mode", "exclude");
        data.put("packingcube_filter", filter);
        // New pouches start in explicit source-rule mode; no implicit hand targets.
        data.put("packingcube_refill_sources", new CompoundTag());
        return CustomData.of(data);
    }

    private static CompoundTag settings(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getCompound(KEY);
    }

    public static boolean enabled(ItemStack stack, boolean trade) {
        var settings = settings(stack);
        String field = trade ? "trade" : "hotbar";
        return !settings.contains(field) || settings.getBoolean(field);
    }

    public static boolean offhandEnabled(ItemStack stack) {
        var settings = settings(stack);
        return !settings.contains("offhand") || settings.getBoolean("offhand");
    }

    public static boolean filterEnabled(ItemStack stack) {
        return settings(stack).getBoolean("filter");
    }

    public static boolean allows(ItemStack stack, ItemStack item) {
        return !item.isEmpty() && (!filterEnabled(stack) || BagFilter.matches(stack, item));
    }

    public static int slots(ItemStack stack) {
        var settings = settings(stack);
        return settings.contains("slots") ? settings.getInt("slots") & 511 : 511;
    }

    public static boolean suppliesSlot(ItemStack stack, int slot) {
        return slot >= 0
                && slot <= OFFHAND_SLOT
                && RefillTargets.selected(stack, RefillTargets.inventory(slot))
                && (slot >= 9 || enabled(stack, false));
    }

    public static String timing(ItemStack stack) {
        String timing = settings(stack).getString("timing");
        return TIMINGS.contains(timing) ? timing : "empty";
    }

    public static int threshold(ItemStack stack) {
        var settings = settings(stack);
        return settings.contains("threshold")
                ? Math.clamp(settings.getInt("threshold"), 0, 63)
                : 16;
    }

    /** Zero means one full stack, clamped to the actual item's stack limit. */
    public static int target(ItemStack stack) {
        return Math.clamp(settings(stack).getInt("target"), 0, 64);
    }

    public static int desired(ItemStack stack, int remaining, int maximum) {
        boolean triggered =
                switch (timing(stack)) {
                    case "threshold" -> remaining <= threshold(stack);
                    case "use" -> true;
                    default -> remaining == 0;
                };
        if (!triggered) return remaining;
        return Math.min(maximum, target(stack) == 0 ? maximum : target(stack));
    }

    public static void setEnabled(ItemStack stack, boolean trade, boolean enabled) {
        set(stack, trade ? "trade" : "hotbar", Boolean.toString(enabled));
    }

    /** Network and UI share the same strict allowlist; invalid input never writes data. */
    public static boolean set(ItemStack stack, String field, String value) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var settings = data.getCompound(KEY);
        switch (field) {
            case "hotbar", "trade", "offhand", "filter" -> {
                if (!value.equals("true") && !value.equals("false")) return false;
                settings.putBoolean(field, Boolean.parseBoolean(value));
            }
            case "slots", "threshold", "target" -> {
                if (!value.matches("[0-9]{1,3}")) return false;
                int number = Integer.parseInt(value);
                int max = field.equals("slots") ? 511 : field.equals("threshold") ? 63 : 64;
                if (number > max) return false;
                settings.putInt(field, number);
            }
            case "timing" -> {
                if (!TIMINGS.contains(value)) return false;
                settings.putString(field, value);
            }
            default -> {
                return false;
            }
        }
        data.put(KEY, settings);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return true;
    }

    /** Sanitize only configuration; never copy storage or identity into radial previews. */
    public static void copySettings(ItemStack source, ItemStack target) {
        setEnabled(target, false, enabled(source, false));
        setEnabled(target, true, enabled(source, true));
        set(target, "offhand", Boolean.toString(offhandEnabled(source)));
        set(target, "filter", Boolean.toString(filterEnabled(source)));
        set(target, "slots", Integer.toString(slots(source)));
        set(target, "timing", timing(source));
        set(target, "threshold", Integer.toString(threshold(source)));
        set(target, "target", Integer.toString(target(source)));
        RefillTargets.copy(source, target);
        RefillSlotSettings.copy(source, target);
    }
}
