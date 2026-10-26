package github.nyeptos.packingcube.bag.refill;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Nine independent source-slot rules. Browsing pages never migrates or writes an item. */
public final class RefillSlotSettings {
    private static final String KEY = "packingcube_refill_sources";
    public static final int COUNT = 9;

    public record Rule(
            boolean enabled,
            String destination,
            boolean replace,
            int priority,
            String timing,
            int threshold,
            int target) {
        public int desired(int remaining, int maximum, boolean consumed) {
            if (remaining > 0
                    && !(timing.equals("threshold")
                            ? remaining <= threshold
                            : timing.equals("use") && consumed)) return remaining;
            return Math.min(maximum, target == 0 ? maximum : target);
        }
    }

    private RefillSlotSettings() {}

    public static boolean mapped(ItemStack pouch) {
        return pouch.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .contains(KEY);
    }

    public static Rule get(ItemStack pouch, int source) {
        if (source < 0 || source >= COUNT)
            throw new IllegalArgumentException("Invalid source slot");
        var tag =
                pouch.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag()
                        .getCompound(KEY)
                        .getCompound(Integer.toString(source));
        String dest = tag.getString("destination"), timing = tag.getString("timing");
        return new Rule(
                !tag.contains("enabled") || tag.getBoolean("enabled"),
                RefillTargets.valid(dest) ? dest : "",
                tag.getBoolean("replace"),
                Math.clamp(tag.getInt("priority"), 0, 9),
                RefillSettings.TIMINGS.contains(timing) ? timing : RefillSettings.timing(pouch),
                tag.contains("threshold")
                        ? Math.clamp(tag.getInt("threshold"), 0, 63)
                        : RefillSettings.threshold(pouch),
                tag.contains("target")
                        ? Math.clamp(tag.getInt("target"), 0, 64)
                        : RefillSettings.target(pouch));
    }

    public static boolean set(ItemStack pouch, int source, String field, String value) {
        if (source < 0 || source >= COUNT) return false;
        var data = pouch.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var sources = data.getCompound(KEY);
        var tag = sources.getCompound(Integer.toString(source));
        switch (field) {
            case "enabled", "replace" -> {
                if (!value.equals("true") && !value.equals("false")) return false;
                tag.putBoolean(field, Boolean.parseBoolean(value));
            }
            case "destination" -> {
                if (!value.isEmpty() && !RefillTargets.valid(value)) return false;
                tag.putString(field, value);
            }
            case "timing" -> {
                if (!RefillSettings.TIMINGS.contains(value)) return false;
                tag.putString(field, value);
            }
            case "priority", "threshold", "target" -> {
                if (!value.matches("[0-9]{1,2}")) return false;
                int number = Integer.parseInt(value);
                if (number > (field.equals("priority") ? 9 : field.equals("threshold") ? 63 : 64))
                    return false;
                tag.putInt(field, number);
            }
            default -> {
                return false;
            }
        }
        sources.put(Integer.toString(source), tag);
        data.put(KEY, sources);
        pouch.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return true;
    }

    public static void copy(ItemStack from, ItemStack to) {
        var data = to.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.remove(KEY);
        to.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        if (!mapped(from)) return;
        for (int i = 0; i < COUNT; i++) {
            var rule = get(from, i);
            set(to, i, "enabled", Boolean.toString(rule.enabled()));
            set(to, i, "destination", rule.destination());
            set(to, i, "replace", Boolean.toString(rule.replace()));
            set(to, i, "priority", Integer.toString(rule.priority()));
            set(to, i, "timing", rule.timing());
            set(to, i, "threshold", Integer.toString(rule.threshold()));
            set(to, i, "target", Integer.toString(rule.target()));
        }
    }
}
