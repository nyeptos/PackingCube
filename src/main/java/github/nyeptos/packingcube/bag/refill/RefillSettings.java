package github.nyeptos.packingcube.bag.refill;

import github.nyeptos.packingcube.bag.filter.BagFilter;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Bounded, item-owned settings. Filter rules restrict withdrawal, never storage. */
public final class RefillSettings {
    private static final String KEY = "packingcube_refill";
    private static final String LOCK_KEY = "locked";
    public static final int OFFHAND_SLOT = 40;
    public static final int SETTINGS_ROWS = 10;
    public static final int MAX_TARGET = 64;
    public static final int LOCK_SLOTS = 9;

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

    /** Nine-bit mask of pouch slots holding a returned item; those slots never supply. */
    public static int lockedMask(ItemStack stack) {
        return settings(stack).getInt(LOCK_KEY) & 511;
    }

    public static boolean locked(ItemStack stack, int slot) {
        return slot >= 0 && slot < LOCK_SLOTS && (lockedMask(stack) & (1 << slot)) != 0;
    }

    /** Writes one lock bit. Callers keep the mask clear while the slot is empty. */
    public static void setLocked(ItemStack stack, int slot, boolean locked) {
        if (slot < 0 || slot >= LOCK_SLOTS) return;
        int mask = lockedMask(stack);
        int next = locked ? mask | (1 << slot) : mask & ~(1 << slot);
        if (next == mask) return;
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var settings = data.getCompound(KEY);
        settings.putInt(LOCK_KEY, next);
        data.put(KEY, settings);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    /** Remaining hand count at which a refill starts; 0 waits until the hand is empty. */
    public static int threshold(ItemStack stack) {
        var settings = settings(stack);
        return settings.contains("threshold")
                ? Math.clamp(settings.getInt("threshold"), 0, 63)
                : legacyThreshold(settings, target(stack));
    }

    /** Count the hand is filled to, clamped to the actual item's stack limit when refilling. */
    public static int target(ItemStack stack) {
        int target = Math.clamp(settings(stack).getInt("target"), 0, MAX_TARGET);
        // Older pouches stored 0 for "one full stack".
        return target == 0 ? MAX_TARGET : target;
    }

    /** True when the hand is low enough to top up to {@link #target}. */
    public static int desired(ItemStack stack, int remaining, int maximum) {
        if (remaining > threshold(stack)) return remaining;
        return Math.min(maximum, target(stack));
    }

    /** Legacy three-way timing, kept only to read pouches saved before PC-66. */
    static String legacyTiming(ItemStack stack) {
        return settings(stack).getString("timing");
    }

    /** "use" used to top up after every use; that equals a threshold one below the target. */
    static int legacyThreshold(CompoundTag settings, int target) {
        return switch (settings.getString("timing")) {
            case "threshold" -> 16;
            case "use" -> Math.max(0, target - 1);
            default -> 0;
        };
    }

    /** Keeps threshold below target and writes both, so legacy values are migrated on write. */
    private static void normalize(CompoundTag settings) {
        int target =
                settings.contains("target")
                        ? Math.clamp(settings.getInt("target"), 0, MAX_TARGET)
                        : MAX_TARGET;
        if (target == 0) target = MAX_TARGET;
        int threshold =
                settings.contains("threshold")
                        ? Math.clamp(settings.getInt("threshold"), 0, 63)
                        : legacyThreshold(settings, target);
        settings.putInt("target", target);
        settings.putInt("threshold", Math.min(threshold, target - 1));
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
                int max = field.equals("slots") ? 511 : field.equals("threshold") ? 63 : MAX_TARGET;
                if (number > max) return false;
                if (field.equals("target") && number < 1) return false;
                settings.putInt(field, number);
            }
            default -> {
                return false;
            }
        }
        normalize(settings);
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
        set(target, "target", Integer.toString(target(source)));
        set(target, "threshold", Integer.toString(threshold(source)));
        RefillTargets.copy(source, target);
        RefillSlotSettings.copy(source, target);
    }
}
