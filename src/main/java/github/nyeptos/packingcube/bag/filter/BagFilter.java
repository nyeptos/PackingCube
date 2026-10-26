package github.nyeptos.packingcube.bag.filter;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** One shared rule list. Include/exclude mode only inverts its matching result. */
public final class BagFilter {
    private static final String KEY = "packingcube_filter";
    public static final int MAX_RULES = 256;
    public static final List<String> FIELDS =
            List.of("tags", "items", "exclude_tags", "exclude_items");

    private BagFilter() {}

    public static List<String> entries(ItemStack bag, String field) {
        if (!FIELDS.contains(field)) return List.of();
        String canonical = field.startsWith("exclude_") ? field.substring(8) : field;
        CompoundTag filter =
                bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag()
                        .getCompound(KEY);
        LinkedHashSet<String> result = new LinkedHashSet<>();
        // Read both old lists without losing entries; writes consolidate them into the shared list.
        for (String key : List.of(canonical, "exclude_" + canonical)) {
            ListTag values = filter.getList(key, Tag.TAG_STRING);
            for (int i = 0; i < values.size(); i++) result.add(values.getString(i));
        }
        return new ArrayList<>(result);
    }

    public static boolean add(ItemStack bag, String field, ResourceLocation id) {
        if (!FIELDS.contains(field))
            throw new IllegalArgumentException("Unknown filter field: " + field);
        List<String> values = entries(bag, field);
        if (values.contains(id.toString())) return true;
        int count =
                List.of("tags", "items").stream().mapToInt(key -> entries(bag, key).size()).sum();
        if (count >= MAX_RULES) return false;
        values.add(id.toString());
        write(bag, field, values);
        return true;
    }

    public static boolean remove(ItemStack bag, String field, ResourceLocation id) {
        if (!FIELDS.contains(field)) return false;
        List<String> values = entries(bag, field);
        if (!values.remove(id.toString())) return false;
        write(bag, field, values);
        return true;
    }

    private static void write(ItemStack bag, String field, List<String> values) {
        CompoundTag data = bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag filter = data.getCompound(KEY);
        ListTag list = new ListTag();
        for (String value : values) list.add(StringTag.valueOf(value));
        String canonical = field.startsWith("exclude_") ? field.substring(8) : field;
        filter.put(canonical, list);
        filter.remove("exclude_" + canonical);
        data.put(KEY, filter);
        bag.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    public static void clear(ItemStack bag) {
        CompoundTag data = bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.remove(KEY);
        if (data.isEmpty()) bag.remove(DataComponents.CUSTOM_DATA);
        else bag.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    public static boolean isExcluding(ItemStack bag) {
        CompoundTag data = bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return data.getCompound(KEY).getString("mode").equals("exclude");
    }

    public static void setExcluding(ItemStack bag, boolean excluding) {
        write(bag, "tags", entries(bag, "tags"));
        write(bag, "items", entries(bag, "items"));
        CompoundTag data = bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag filter = data.getCompound(KEY);
        filter.putString("mode", excluding ? "exclude" : "include");
        data.put(KEY, filter);
        bag.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    /**
     * Copies only bounded, valid filter settings; never storage, identity or unrelated custom data.
     */
    public static void copySettings(ItemStack source, ItemStack target) {
        clear(target);
        setExcluding(target, isExcluding(source));
        for (String field : List.of("tags", "items")) {
            for (String entry : entries(source, field)) {
                ResourceLocation id = ResourceLocation.tryParse(entry);
                if (id != null && !add(target, field, id)) return;
            }
        }
    }

    public static boolean matches(ItemStack bag, ItemStack incoming) {
        if (incoming.isEmpty()) return false;
        String itemId = BuiltInRegistries.ITEM.getKey(incoming.getItem()).toString();
        boolean excluding = isExcluding(bag);
        boolean listed =
                entries(bag, "items").contains(itemId) || matchesTag(bag, "tags", incoming);
        return excluding ? !listed : listed;
    }

    private static boolean matchesTag(ItemStack bag, String field, ItemStack incoming) {
        for (String value : entries(bag, field)) {
            ResourceLocation id = ResourceLocation.tryParse(value);
            if (id != null && incoming.is(TagKey.create(Registries.ITEM, id))) return true;
        }
        return false;
    }
}
