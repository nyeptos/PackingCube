package github.nyeptos.packingcube.bag.refill;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
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
            int threshold,
            int target,
            boolean collect,
            boolean lock,
            String durabilityMode,
            int durability) {
        /** Off keeps the plain rule; the other modes only narrow same-item swaps. */
        public boolean durabilityOff() {
            return durabilityMode.equals("off");
        }
        /** Refills once the destination is at or below the threshold, up to the target. */
        public int desired(int remaining, int maximum) {
            if (remaining > threshold) return remaining;
            return Math.min(maximum, target);
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
        String dest = tag.getString("destination");
        int target =
                tag.contains("target")
                        ? Math.clamp(tag.getInt("target"), 0, RefillSettings.MAX_TARGET)
                        : RefillSettings.target(pouch);
        if (target == 0) target = RefillSettings.MAX_TARGET;
        int threshold =
                tag.contains("threshold")
                        ? Math.clamp(tag.getInt("threshold"), 0, 63)
                        : legacyThreshold(pouch, tag, target);
        return new Rule(
                !tag.contains("enabled") || tag.getBoolean("enabled"),
                RefillTargets.valid(dest) ? dest : "",
                tag.getBoolean("replace"),
                Math.clamp(tag.getInt("priority"), 0, 9),
                Math.min(threshold, target - 1),
                target,
                !tag.contains("collect") || tag.getBoolean("collect"),
                !tag.contains("lock") || tag.getBoolean("lock"),
                mode(tag.getString("durabilityMode")),
                Math.clamp(tag.contains("durability") ? tag.getInt("durability") : 5, 1, 100));
    }

    private static String mode(String value) {
        return value.equals("percent") || value.equals("count") ? value : "off";
    }

    /** Slots saved before PC-66 inherit the pouch-wide three-way timing when they have none. */
    private static int legacyThreshold(ItemStack pouch, CompoundTag tag, int target) {
        String timing =
                tag.contains("timing")
                        ? tag.getString("timing")
                        : RefillSettings.legacyTiming(pouch);
        return switch (timing) {
            case "threshold" -> RefillSettings.threshold(pouch);
            case "use" -> Math.max(0, target - 1);
            default -> 0;
        };
    }

    public static boolean set(ItemStack pouch, int source, String field, String value) {
        if (source < 0 || source >= COUNT) return false;
        var data = pouch.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var sources = data.getCompound(KEY);
        var tag = sources.getCompound(Integer.toString(source));
        switch (field) {
            case "enabled", "replace", "collect", "lock" -> {
                if (!value.equals("true") && !value.equals("false")) return false;
                tag.putBoolean(field, Boolean.parseBoolean(value));
            }
            case "destination" -> {
                if (!value.isEmpty() && !RefillTargets.valid(value)) return false;
                tag.putString(field, value);
            }
            case "durabilityMode" -> {
                if (!value.equals("off") && !value.equals("percent") && !value.equals("count"))
                    return false;
                tag.putString(field, value);
            }
            case "durability" -> {
                if (!value.matches("[0-9]{1,3}")) return false;
                int number = Integer.parseInt(value);
                if (number < 1 || number > 100) return false;
                tag.putInt(field, number);
            }
            case "priority", "threshold", "target" -> {
                if (!value.matches("[0-9]{1,2}")) return false;
                int number = Integer.parseInt(value);
                int max =
                        field.equals("priority")
                                ? 9
                                : field.equals("threshold") ? 63 : RefillSettings.MAX_TARGET;
                if (number > max) return false;
                if (field.equals("target") && number < 1) return false;
                tag.putInt(field, number);
            }
            default -> {
                return false;
            }
        }
        if (field.equals("priority")) {
            sources.put(Integer.toString(source), tag);
            data.put(KEY, sources);
            pouch.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            return true;
        }
        int target =
                tag.contains("target") ? tag.getInt("target") : RefillSettings.target(pouch);
        if (target < 1) target = 1;
        int threshold =
                tag.contains("threshold")
                        ? tag.getInt("threshold")
                        : get(pouch, source).threshold();
        tag.putInt("threshold", Math.min(Math.clamp(threshold, 0, 63), target - 1));
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
            set(to, i, "target", Integer.toString(rule.target()));
            set(to, i, "threshold", Integer.toString(rule.threshold()));
            set(to, i, "collect", Boolean.toString(rule.collect()));
            set(to, i, "lock", Boolean.toString(rule.lock()));
            set(to, i, "durabilityMode", rule.durabilityMode());
            set(to, i, "durability", Integer.toString(rule.durability()));
        }
    }
}
