package github.nyeptos.packingcube.bag.filter;

import github.nyeptos.packingcube.bag.refill.RefillSettings;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Shared, client-independent summary for real stacks and sanitized radial previews. */
public final class BagFilterTooltip {
    public static final int MAX_VISIBLE_RULES = 8;

    private BagFilterTooltip() {}

    public static void append(ItemStack bag, List<Component> lines) {
        if (bag.is(github.nyeptos.packingcube.registry.ModItems.REFILL_POUCH.get())) {
            lines.add(
                    Component.translatable(
                                    "gui.packingcube.refill_filter_"
                                            + (RefillSettings.filterEnabled(bag) ? "on" : "off"))
                            .withStyle(ChatFormatting.GOLD));
            if (!RefillSettings.filterEnabled(bag)) return;
        }
        boolean excluding = BagFilter.isExcluding(bag);
        lines.add(
                Component.translatable(
                                excluding
                                        ? "tooltip.packingcube.filter_exclude"
                                        : "tooltip.packingcube.filter_include")
                        .withStyle(ChatFormatting.GOLD));
        List<String> tags = BagFilter.entries(bag, "tags"), items = BagFilter.entries(bag, "items");
        int total = tags.size() + items.size();
        if (total == 0) {
            lines.add(
                    Component.translatable(
                                    excluding
                                            ? "gui.packingcube.no_exclude_rules"
                                            : "gui.packingcube.no_rules")
                            .withStyle(ChatFormatting.GRAY));
            return;
        }
        int shown = 0;
        for (String tag : tags) {
            if (shown == MAX_VISIBLE_RULES) break;
            lines.add(Component.literal("  #" + tag).withStyle(ChatFormatting.AQUA));
            shown++;
        }
        for (String item : items) {
            if (shown == MAX_VISIBLE_RULES) break;
            ResourceLocation id = ResourceLocation.tryParse(item);
            Component name =
                    id != null && BuiltInRegistries.ITEM.containsKey(id)
                            ? Component.translatable(
                                    BuiltInRegistries.ITEM.get(id).getDescriptionId())
                            : Component.literal(item);
            lines.add(
                    Component.translatable("tooltip.packingcube.filter_item", name, item)
                            .withStyle(ChatFormatting.GRAY));
            shown++;
        }
        if (total > shown)
            lines.add(
                    Component.translatable("tooltip.packingcube.filter_more", total - shown)
                            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
