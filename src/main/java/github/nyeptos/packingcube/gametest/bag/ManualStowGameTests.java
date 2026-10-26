package github.nyeptos.packingcube.gametest.bag;

import com.google.gson.JsonParser;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.bag.pickup.BagPickupSettings;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.network.BagStowPayload;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class ManualStowGameTests {
    private static Player player(GameTestHelper h) {
        return h.makeMockPlayer(GameType.SURVIVAL);
    }

    private static BagInventory modules(ItemStack root) {
        return new BagInventory(root, true, () -> {});
    }

    private static BagInventory contents(ItemStack pouch) {
        return new BagInventory(pouch, false, () -> {});
    }

    private static ItemStack pouch(int priority) {
        var stack = ModItems.FILTER_BAG.get().getDefaultInstance();
        BagFilter.add(stack, "items", ResourceLocation.withDefaultNamespace("stone"));
        BagPickupSettings.setPriority(stack, priority);
        return stack;
    }

    private static ItemStack install(Player player, ItemStack... pouches) {
        var root = ModItems.LARGE_BAG.get().getDefaultInstance();
        var parent = modules(root);
        for (int i = 0; i < pouches.length; i++) parent.setItem(i, pouches[i]);
        player.getInventory().setItem(0, root);
        return root;
    }

    private static BagMenu open(Player player, ItemStack root) {
        var menu = new BagMenu(42, player.getInventory(), modules(root), root, 0, true);
        player.containerMenu = menu;
        return menu;
    }

    private static int count(ItemStack root, int module) {
        var storage = contents(modules(root).getItem(module));
        int total = 0;
        for (int i = 0; i < storage.getContainerSize(); i++) total += storage.getItem(i).getCount();
        return total;
    }

    private static void room(ItemStack root, int module, int room) {
        var parent = modules(root);
        var storage = new BagInventory(parent.getItem(module), false, parent);
        for (int i = 0; i < 9; i++)
            storage.setItem(i, new ItemStack(Items.STONE, i == 8 ? 64 - room : 64));
    }

    @GameTest(template = "empty")
    public static void onlyMainInventoryMovesAndAutoOffStillAllowsManualStow(GameTestHelper h) {
        var player = player(h);
        var module = pouch(0);
        BagPickupSettings.setEnabled(module, false);
        var root = install(player, module);
        var inv = player.getInventory();
        inv.setItem(1, new ItemStack(Items.STONE, 7));
        inv.setItem(9, new ItemStack(Items.STONE, 11));
        inv.setItem(35, new ItemStack(Items.STONE, 3));
        inv.setItem(10, new ItemStack(Items.DIRT, 6));
        inv.setItem(36, new ItemStack(Items.STONE, 4));
        inv.setItem(40, new ItemStack(Items.STONE, 5));
        var menu = open(player, root);
        h.assertTrue(
                menu.stowAll(player, 42) == 14 && count(root, 0) == 14,
                "Matching main stacks stored even with auto pickup OFF");
        h.assertTrue(
                inv.getItem(9).isEmpty()
                        && inv.getItem(35).isEmpty()
                        && inv.getItem(10).getCount() == 6,
                "Main boundary slots move; unmatched stays");
        h.assertTrue(
                inv.getItem(1).getCount() == 7
                        && inv.getItem(36).getCount() == 4
                        && inv.getItem(40).getCount() == 5,
                "Hotbar, armor and offhand untouched");
        h.assertTrue(
                !BagPickupSettings.enabled(modules(root).getItem(0)),
                "Manual action does not change pickup setting");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void highPriorityFillsFirstThenRemainderStaysInOriginalSlot(GameTestHelper h) {
        var player = player(h);
        var root = install(player, pouch(0), pouch(9));
        room(root, 0, 4);
        room(root, 1, 2);
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 10));
        var menu = open(player, root);
        h.assertTrue(menu.stowAll(player, 42) == 6, "Only available capacity moves");
        h.assertTrue(
                count(root, 0) == 576 && count(root, 1) == 576,
                "Both destinations save through the same open parent");
        h.assertTrue(
                player.getInventory().getItem(9).getCount() == 4,
                "No dropping/deletion/normal-slot rearrangement");
        h.assertTrue(
                menu.stowAll(player, 42) == 0 && player.getInventory().getItem(9).getCount() == 4,
                "Repeated request cannot duplicate or discard overflow");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void prioritiesAndStableLeftToRightOrderAreRespected(GameTestHelper h) {
        var player = player(h);
        var root = install(player, pouch(0), ItemStack.EMPTY, pouch(9), pouch(9));
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 8));
        var menu = open(player, root);
        h.assertTrue(
                menu.stowAll(player, 42) == 8
                        && count(root, 0) == 0
                        && count(root, 2) == 8
                        && count(root, 3) == 0,
                "Higher priority wins, equal priority keeps original slot order including gaps");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void onlySelectedBackpackReceivesItemsAndLoosePouchesAreIgnored(
            GameTestHelper h) {
        var player = player(h);
        var target = install(player, pouch(0));
        var other = ModItems.LARGE_BAG.get().getDefaultInstance();
        modules(other).setItem(0, pouch(9));
        player.getInventory().setItem(1, other);
        var loose = pouch(9);
        player.getInventory().setItem(10, loose);
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 12));
        var menu = open(player, target);
        h.assertTrue(
                menu.stowAll(player, 42) == 12 && count(target, 0) == 12 && count(other, 0) == 0,
                "Other backpack's higher priority does not steal manual input");
        h.assertTrue(
                player.getInventory().getItem(10) == loose && contents(loose).getItem(0).isEmpty(),
                "Loose pouch is neither moved nor filled");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tagsExcludeAndNoNestedContainerRulesAreShared(GameTestHelper h) {
        var player = player(h);
        var tagPouch = ModItems.FILTER_BAG.get().getDefaultInstance();
        BagFilter.add(tagPouch, "tags", ResourceLocation.withDefaultNamespace("logs"));
        var excludePouch = pouch(0);
        BagFilter.setExcluding(excludePouch, true);
        var emptyInclude = ModItems.FILTER_BAG.get().getDefaultInstance();
        BagPickupSettings.setPriority(tagPouch, 9);
        var root = install(player, emptyInclude, tagPouch, excludePouch);
        var inv = player.getInventory();
        inv.setItem(9, new ItemStack(Items.OAK_LOG, 3));
        inv.setItem(10, new ItemStack(Items.DIRT, 5));
        inv.setItem(11, new ItemStack(Items.STONE, 7));
        inv.setItem(12, new ItemStack(Items.SHULKER_BOX));
        var menu = open(player, root);
        h.assertTrue(
                menu.stowAll(player, 42) == 8
                        && count(root, 0) == 0
                        && count(root, 1) == 3
                        && count(root, 2) == 5,
                "Shared include/empty/tag/exclude rules apply");
        h.assertTrue(
                inv.getItem(11).getCount() == 7 && inv.getItem(12).is(Items.SHULKER_BOX),
                "Excluded and non-nestable items stay at source");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void itemComponentsSurviveAndDistinctStacksNeverMerge(GameTestHelper h) {
        var player = player(h);
        var module = pouch(0);
        contents(module).setItem(0, new ItemStack(Items.STONE, 60));
        var root = install(player, module);
        var named = new ItemStack(Items.STONE, 10);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Keep this name"));
        player.getInventory().setItem(9, named);
        var menu = open(player, root);
        h.assertTrue(menu.stowAll(player, 42) == 10, "Named stack moves");
        var storage = contents(modules(root).getItem(0));
        h.assertTrue(
                storage.getItem(0).getCount() == 60
                        && storage.getItem(1).getCount() == 10
                        && ItemStack.isSameItemSameComponents(storage.getItem(1), named),
                "Components and stack boundaries preserved");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void movedRootRebindsWithoutOverwritingPreviousManualSaves(GameTestHelper h) {
        var player = player(h);
        var root = install(player, pouch(0), pouch(1));
        var menu = open(player, root);
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 5));
        h.assertTrue(menu.stowAll(player, 42) == 5, "Initial save");
        var moved = root.copy();
        player.getInventory().setItem(0, ItemStack.EMPTY);
        player.getInventory().setItem(20, moved);
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 6));
        h.assertTrue(
                menu.stowAll(player, 42) == 6 && count(moved, 1) == 11,
                "Rebound menu uses same live storage and preserves earlier contents");
        player.containerMenu = player.inventoryMenu;
        var reopened = open(player, moved);
        h.assertTrue(
                reopened.stowAll(player, 42) == 0 && count(moved, 1) == 11,
                "Reopening retains all saved items without duplicate transfer");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void staleWrongMenuAndCarriedRequestsAreRejected(GameTestHelper h) {
        var player = player(h);
        var root = install(player, pouch(0));
        var menu = open(player, root);
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 5));
        h.assertTrue(menu.stowAll(player, 999) == -1, "Wrong container ID rejected");
        menu.setCarried(new ItemStack(Items.DIRT));
        h.assertTrue(
                menu.stowAll(player, 42) == -1 && menu.getCarried().is(Items.DIRT),
                "Cursor held item never consumed or moved");
        menu.setCarried(ItemStack.EMPTY);
        player.containerMenu = player.inventoryMenu;
        h.assertTrue(menu.stowAll(player, 42) == -1, "Old menu requests rejected");
        var parent = modules(root);
        var pouchMenu =
                new BagMenu(
                        43,
                        player.getInventory(),
                        new BagInventory(parent.getItem(0), false, parent),
                        root,
                        0,
                        false);
        player.containerMenu = pouchMenu;
        h.assertTrue(
                pouchMenu.stowAll(player, 43) == -1
                        && count(root, 0) == 0
                        && player.getInventory().getItem(9).getCount() == 5,
                "Pouch menu cannot trigger backpack-wide transfer");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void missingDuplicateDeadAndSpectatorRootsCannotTransfer(GameTestHelper h) {
        var player = player(h);
        var root = install(player, pouch(0));
        var menu = open(player, root);
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 5));
        player.getInventory().setItem(1, root.copy());
        h.assertTrue(menu.stowAll(player, 42) == -1, "Duplicate identity rejected");
        player.getInventory().setItem(0, ItemStack.EMPTY);
        player.getInventory().setItem(1, ItemStack.EMPTY);
        h.assertTrue(
                menu.stowAll(player, 42) == -1 && count(root, 0) == 0,
                "Missing root rejected without touching source");
        player.getInventory().setItem(0, root);
        player.setHealth(0);
        h.assertTrue(menu.stowAll(player, 42) == -1, "Dead owner rejected");
        var spectator = h.makeMockPlayer(GameType.SPECTATOR);
        var specRoot = install(spectator, pouch(0));
        var specMenu = open(spectator, specRoot);
        spectator.getInventory().setItem(9, new ItemStack(Items.STONE, 3));
        h.assertTrue(
                specMenu.stowAll(spectator, 42) == -1 && count(specRoot, 0) == 0,
                "Spectator request rejected");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeOverflowAndEmptyBackpacksNeverDeleteSources(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.CREATIVE);
        var root = install(player, pouch(0));
        room(root, 0, 0);
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 64));
        var menu = open(player, root);
        h.assertTrue(
                menu.stowAll(player, 42) == 0
                        && player.getInventory().getItem(9).getCount() == 64
                        && count(root, 0) == 576,
                "Full destinations never delete creative overflow");
        var emptyRoot = ModItems.LARGE_BAG.get().getDefaultInstance();
        player.getInventory().setItem(0, emptyRoot);
        var emptyMenu = open(player, emptyRoot);
        h.assertTrue(
                emptyMenu.stowAll(player, 42) == 0
                        && player.getInventory().getItem(9).getCount() == 64,
                "No destination leaves source intact");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void requestCodecAndBothLanguagesAreComplete(GameTestHelper h) throws Exception {
        var buf =
                new RegistryFriendlyByteBuf(
                        io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            BagStowPayload.STREAM_CODEC.encode(buf, new BagStowPayload(42));
            h.assertTrue(
                    BagStowPayload.STREAM_CODEC.decode(buf).containerId() == 42
                            && buf.readableBytes() == 0,
                    "Request contains only the menu ID");
        } finally {
            buf.release();
        }
        for (String lang : new String[] {"en_us", "ja_jp"}) {
            try (var reader =
                    new InputStreamReader(
                            Objects.requireNonNull(
                                    ManualStowGameTests.class.getResourceAsStream(
                                            "/assets/packingcube/lang/" + lang + ".json")),
                            StandardCharsets.UTF_8)) {
                var json = JsonParser.parseReader(reader).getAsJsonObject();
                for (String key :
                        new String[] {
                            "gui.packingcube.stow_all",
                            "gui.packingcube.stow_all_help",
                            "message.packingcube.stow_result",
                            "message.packingcube.stow_empty"
                        })
                    h.assertTrue(
                            json.has(key) && !json.get(key).getAsString().isBlank(),
                            "New string generated for " + lang + ": " + key);
            }
        }
        h.succeed();
    }
}
