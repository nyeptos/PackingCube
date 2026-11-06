package github.nyeptos.packingcube.gametest.gui;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.bag.BagIdentity;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.item.BagFilterTooltip;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static github.nyeptos.packingcube.gametest.support.RadialTestSupport.*;

/** Filter tooltips and the radial filter preview payload. */
@GameTestHolder(PackingCube.MODID)
@PrefixGameTestTemplate(false)
public final class FilterPreviewGameTests {
    @GameTest(template = "empty")
    public static void filterItemTooltipsExposeModesAndEmptyBehavior(GameTestHelper h) {
        ItemStack filter = module();
        var lines = new java.util.ArrayList<Component>();
        var context = net.minecraft.world.item.Item.TooltipContext.of(h.getLevel());
        filter.getItem()
                .appendHoverText(
                        filter, context, lines, net.minecraft.world.item.TooltipFlag.NORMAL);
        h.assertTrue(
                lines.size() == 2
                        && translationKey(lines.get(0)).equals("tooltip.packingcube.filter_include")
                        && translationKey(lines.get(1)).equals("gui.packingcube.no_rules"),
                "Empty include explains rejection");
        BagFilter.setExcluding(filter, true);
        lines.clear();
        filter.getItem()
                .appendHoverText(
                        filter, context, lines, net.minecraft.world.item.TooltipFlag.NORMAL);
        h.assertTrue(
                translationKey(lines.get(0)).equals("tooltip.packingcube.filter_exclude")
                        && translationKey(lines.get(1)).equals("gui.packingcube.no_exclude_rules"),
                "Empty exclude explains acceptance");
        ItemStack root = bag();
        lines.clear();
        root.getItem()
                .appendHoverText(root, context, lines, net.minecraft.world.item.TooltipFlag.NORMAL);
        h.assertTrue(lines.isEmpty(), "Big Bag does not claim its own filter settings");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void filterTooltipListsTagsItemsAndBoundsLongLists(GameTestHelper h) {
        ItemStack filter = module();
        BagFilter.add(filter, "tags", ResourceLocation.parse("c:ores"));
        BagFilter.add(filter, "items", ResourceLocation.withDefaultNamespace("diamond"));
        BagFilter.add(filter, "items", ResourceLocation.parse("missing_mod:unknown"));
        ItemStack before = filter.copy();
        var lines = new java.util.ArrayList<Component>();
        BagFilterTooltip.append(filter, lines);
        h.assertTrue(lines.get(1).getString().equals("  #c:ores"), "Tag IDs keep their # prefix");
        h.assertTrue(
                lines.get(2).getString().contains("minecraft:diamond")
                        && lines.get(3).getString().contains("missing_mod:unknown"),
                "Items include IDs and unknown registry entries remain safe");
        h.assertTrue(ItemStack.matches(filter, before), "Tooltip is read-only");
        for (int i = 0; i < 16; i++)
            BagFilter.add(filter, "items", ResourceLocation.parse("test:item_" + i));
        lines.clear();
        BagFilterTooltip.append(filter, lines);
        h.assertTrue(
                lines.size() == 10
                        && translationKey(lines.getLast())
                                .equals("tooltip.packingcube.filter_more"),
                "Only eight rules plus header and remaining count are displayed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialFilterPreviewRoundTripsWithoutPrivateStorage(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag(), filter = module();
        BagFilter.setExcluding(filter, true);
        BagFilter.add(filter, "tags", ResourceLocation.parse("c:ores"));
        BagFilter.add(filter, "items", ResourceLocation.withDefaultNamespace("diamond"));
        filter.set(DataComponents.CUSTOM_NAME, Component.literal("Ore rules"));
        BagIdentity.renew(filter);
        var data = filter.get(DataComponents.CUSTOM_DATA).copyTag();
        data.putString("private_note", "never_sync");
        filter.set(
                DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
        storage(filter).setItem(0, new ItemStack(Items.DIAMOND, 15));
        modules(root).setItem(2, filter);
        ItemStack preview = open(player, root).icon(2), decoded;
        var buf =
                new RegistryFriendlyByteBuf(
                        io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, preview);
            decoded = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        } finally {
            buf.release();
        }
        h.assertTrue(
                decoded.getHoverName().getString().equals("Ore rules")
                        && BagFilter.isExcluding(decoded)
                        && BagFilter.entries(decoded, "tags").equals(java.util.List.of("c:ores"))
                        && BagFilter.entries(decoded, "items")
                                .equals(java.util.List.of("minecraft:diamond")),
                "Preview survives normal slot synchronization codec");
        h.assertTrue(
                decoded.get(DataComponents.CONTAINER) == null
                        && decoded.get(DataComponents.CUSTOM_DATA)
                                .copyTag()
                                .getAllKeys()
                                .equals(java.util.Set.of("packingcube_filter")),
                "Only filter subtree is sent, never root identity, contents or other metadata");
        var actualLines = new java.util.ArrayList<Component>();
        var previewLines = new java.util.ArrayList<Component>();
        BagFilterTooltip.append(filter, actualLines);
        BagFilterTooltip.append(decoded, previewLines);
        h.assertTrue(
                actualLines.equals(previewLines), "Radial and real bag show identical summaries");
        h.assertTrue(
                storage(modules(root).getItem(2)).getItem(0).getCount() == 15,
                "Preview leaves storage untouched");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void filterPreviewMergesLegacyAndBoundsRules(GameTestHelper h) {
        ItemStack filter = module();
        var filterData = new net.minecraft.nbt.CompoundTag();
        var old = new net.minecraft.nbt.ListTag();
        old.add(net.minecraft.nbt.StringTag.valueOf("c:ores"));
        old.add(net.minecraft.nbt.StringTag.valueOf("Not a valid ID"));
        filterData.put("exclude_tags", old);
        var items = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < 300; i++)
            items.add(net.minecraft.nbt.StringTag.valueOf("test:item_" + i));
        filterData.put("items", items);
        filterData.putString("mode", "exclude");
        var rootData = new net.minecraft.nbt.CompoundTag();
        rootData.put("packingcube_filter", filterData);
        filter.set(
                DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(rootData));
        ItemStack before = filter.copy(), preview = module();
        BagFilter.copySettings(filter, preview);
        h.assertTrue(
                BagFilter.isExcluding(preview)
                        && BagFilter.entries(preview, "tags").equals(java.util.List.of("c:ores")),
                "Legacy list retained, malformed IDs skipped");
        h.assertTrue(
                BagFilter.entries(preview, "items").size() == BagFilter.MAX_RULES - 1,
                "Preview remains bounded to 256 rules");
        h.assertTrue(
                ItemStack.matches(before, filter),
                "Preview normalization never rewrites original data");
        h.succeed();
    }
}
