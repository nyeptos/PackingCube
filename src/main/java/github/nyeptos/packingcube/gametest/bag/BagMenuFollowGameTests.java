package github.nyeptos.packingcube.gametest.bag;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.EquippedBagAccess;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.bag.pickup.BagRouter;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static github.nyeptos.packingcube.gametest.support.BagTestSupport.*;

/** Open menus that follow a moved bag, rebind external copies, or expose ghosts. */
@GameTestHolder(PackingCube.MODID)
@PrefixGameTestTemplate(false)
public final class BagMenuFollowGameTests {
    @GameTest(template = "empty")
    public static void openedBagCanMoveToAnotherSlot(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack root = carrier();
        player.getInventory().setItem(0, root);
        BagInventory modules = modules(root);
        ItemStack module = filter();
        modules.setItem(0, module);
        BagMenu menu = new BagMenu(1, player.getInventory(), modules, root, 0, true);
        player.containerMenu = menu;
        helper.assertTrue(
                menu.getSlot(36).mayPickup(player), "Opened bag is no longer slot-locked");
        menu.clicked(36, 0, ClickType.PICKUP, player);
        helper.assertTrue(
                menu.getCarried().is(ModItems.LARGE_BAG.get()), "Bag can be held on cursor");
        helper.assertTrue(menu.stillValid(player), "Cursor is a valid bag location");
        menu.clicked(37, 0, ClickType.PICKUP, player);
        helper.assertTrue(
                menu.getCarried().isEmpty()
                        && player.getInventory().getItem(1).is(ModItems.LARGE_BAG.get()),
                "Bag can be placed in a different inventory slot");
        helper.assertTrue(menu.stillValid(player), "Moving must not close the bag");
        modules.setItem(1, filter());
        BagInventory saved = modules(player.getInventory().getItem(1));
        helper.assertTrue(
                saved.getItem(0).is(ModItems.STORAGE_POUCH.get())
                        && saved.getItem(1).is(ModItems.STORAGE_POUCH.get()),
                "Further writes follow the moved root");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void openBagIsNotRoutedInto(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack root = carrier();
        player.getInventory().setItem(0, root);
        ItemStack module = filter();
        BagFilter.add(module, "items", id("stone"));
        BagInventory modules = modules(root);
        modules.setItem(0, module);
        player.containerMenu = new BagMenu(1, player.getInventory(), modules, root, 0, true);
        helper.assertTrue(
                BagRouter.route(player, new ItemStack(Items.STONE, 5)).getCount() == 5,
                "Open bag must not have a second writer");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void openedFilterFollowsNumberAndOffhandSwap(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack bag = filter();
        player.getInventory().setItem(0, bag);
        player.getInventory().setItem(1, new ItemStack(Items.STONE, 5));
        BagFilter.setExcluding(bag, true);
        BagInventory contents = storage(bag);
        BagMenu menu = new BagMenu(7, player.getInventory(), contents, bag, 0, false);
        player.containerMenu = menu;
        menu.clicked(37, 0, ClickType.SWAP, player);
        helper.assertTrue(
                player.getInventory().getItem(1).is(ModItems.STORAGE_POUCH.get()),
                "Number-key swap moves bag");
        helper.assertTrue(menu.stillValid(player), "Number-key movement keeps menu valid");
        menu.clicked(37, 40, ClickType.SWAP, player);
        helper.assertTrue(
                player.getInventory().getItem(40).is(ModItems.STORAGE_POUCH.get()),
                "Offhand swap moves bag");
        helper.assertTrue(menu.stillValid(player), "Offhand movement keeps menu valid");
        contents.setItem(0, new ItemStack(Items.DIAMOND, 3));
        helper.assertTrue(
                total(storage(player.getInventory().getItem(40))) == 3,
                "Writes follow offhand bag");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mountedModuleWritesFollowMovedRoot(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack root = carrier();
        ItemStack module = filter();
        player.getInventory().setItem(0, root);
        player.getInventory().setItem(1, new ItemStack(Items.DIAMOND));
        BagInventory parent = modules(root);
        parent.setItem(0, module);
        BagInventory contents = new BagInventory(module, false, parent);
        BagMenu menu = new BagMenu(7, player.getInventory(), contents, root, 0, false);
        player.containerMenu = menu;
        menu.clicked(36, 0, ClickType.PICKUP, player);
        helper.assertTrue(
                menu.stillValid(player), "Parent can remain on cursor while module is open");
        menu.clicked(38, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.stillValid(player), "Parent can move while module is open");
        contents.setItem(0, new ItemStack(Items.DIAMOND, 6));
        menu.editFilter(player, 0, 37, "", "");
        helper.assertTrue(
                menu.editFilter(player, 1, -1, "items", "minecraft:diamond"),
                "Moved parent's module remains editable");
        ItemStack savedModule = modules(player.getInventory().getItem(2)).getItem(0);
        helper.assertTrue(
                total(storage(savedModule)) == 6,
                "Nested contents are saved into the moved parent");
        helper.assertTrue(
                BagFilter.matches(savedModule, new ItemStack(Items.DIAMOND)),
                "Nested filter follows moved parent");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void externalCopiesRebindAndAmbiguityStopsEditing(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack root = carrier();
        player.getInventory().setItem(0, root);
        BagInventory contents = modules(root);
        BagMenu menu = new BagMenu(7, player.getInventory(), contents, root, 0, true);
        player.containerMenu = menu;
        ItemStack moved = root.copy();
        player.getInventory().setItem(0, ItemStack.EMPTY);
        player.getInventory().setItem(3, moved);
        helper.assertTrue(menu.stillValid(player), "Copied ItemStack identity is tracked");
        contents.setItem(0, filter());
        helper.assertTrue(
                modules(moved).getItem(0).is(ModItems.STORAGE_POUCH.get()),
                "Writes bind to current ItemStack");
        player.getInventory().setItem(4, moved.copy());
        helper.assertFalse(
                menu.stillValid(player), "Ambiguous duplicate identity must not be followed");
        player.getInventory().setItem(4, ItemStack.EMPTY);
        player.getInventory().setItem(3, ItemStack.EMPTY);
        helper.assertFalse(
                menu.stillValid(player), "No bag in inventory/cursor means editing ends");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void movedOpenCarrierIsStillSkippedByRouter(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack root = carrier();
        ItemStack module = filter();
        BagFilter.add(module, "items", id("stone"));
        player.getInventory().setItem(0, root);
        BagInventory contents = modules(root);
        contents.setItem(0, module);
        BagMenu menu = new BagMenu(7, player.getInventory(), contents, root, 0, true);
        player.containerMenu = menu;
        menu.clicked(36, 0, ClickType.PICKUP, player);
        menu.clicked(40, 0, ClickType.PICKUP, player);
        helper.assertTrue(
                BagRouter.route(player, new ItemStack(Items.STONE, 4)).getCount() == 4,
                "Moving the open bag must not create a second routing writer");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ghostSlotCopiesCursorWithoutConsuming(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack bag = filter();
        player.getInventory().setItem(0, bag);
        player.getInventory().setItem(1, new ItemStack(Items.OAK_LOG, 32));
        BagMenu menu = new BagMenu(7, player.getInventory(), storage(bag), bag, 0, false);
        player.containerMenu = menu;
        menu.clicked(37, 0, ClickType.PICKUP, player);
        ItemStack cursor = menu.getCarried();
        helper.assertTrue(cursor.getCount() == 32, "Normal inventory pickup puts source on cursor");
        helper.assertTrue(
                menu.editFilter(player, 4, -1, "", "copy"), "Ghost accepts cursor reference");
        helper.assertTrue(
                menu.getCarried() == cursor && cursor.getCount() == 32,
                "Ghost must not consume or replace cursor stack");
        helper.assertTrue(
                menu.sample().is(Items.OAK_LOG) && menu.sample().getCount() == 1,
                "Ghost stores only a reference copy");
        helper.assertTrue(
                menu.editFilter(player, 1, -1, "tags", "minecraft:logs"),
                "Can register while source remains on cursor");
        menu.clicked(37, 0, ClickType.PICKUP, player);
        helper.assertTrue(
                menu.getCarried().isEmpty() && player.getInventory().getItem(1).getCount() == 32,
                "Source can be returned without loss");
        helper.assertTrue(
                BagFilter.matches(bag, new ItemStack(Items.BIRCH_LOG)), "Registered tag works");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ghostReplaceAndClearDoNotReturnCopies(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack bag = filter();
        player.getInventory().setItem(0, bag);
        BagMenu menu = new BagMenu(7, player.getInventory(), storage(bag), bag, 0, false);
        player.containerMenu = menu;
        ItemStack first = new ItemStack(Items.STONE, 64);
        menu.setCarried(first);
        menu.editFilter(player, 4, -1, "", "copy");
        menu.editFilter(player, 4, -1, "", "copy");
        helper.assertTrue(first.getCount() == 64, "Repeated copying consumes nothing");
        ItemStack second = new ItemStack(Items.DIAMOND, 12);
        menu.setCarried(second);
        menu.editFilter(player, 4, -1, "", "copy");
        helper.assertTrue(menu.sample().is(Items.DIAMOND), "New source replaces ghost reference");
        helper.assertTrue(
                menu.getCarried() == second && second.getCount() == 12,
                "Old reference is not returned as an item");
        menu.editFilter(player, 4, -1, "", "clear");
        helper.assertTrue(
                menu.sample().isEmpty() && second.getCount() == 12, "Clear consumes nothing");
        helper.assertTrue(total(storage(bag)) == 0, "Ghost references never enter storage");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ghostSlotRejectsInvalidActionsAndBagSamples(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack bag = filter();
        player.getInventory().setItem(0, bag);
        BagMenu menu = new BagMenu(7, player.getInventory(), storage(bag), bag, 0, false);
        player.containerMenu = menu;
        menu.setCarried(new ItemStack(Items.STONE, 3));
        menu.editFilter(player, 4, -1, "", "copy");
        helper.assertFalse(
                menu.editFilter(player, 4, -1, "", "invalid"), "Invalid ghost command is rejected");
        helper.assertFalse(
                menu.editFilter(player, 4, -1, "items", "copy"),
                "Ghost action takes no client field");
        menu.setCarried(carrier());
        helper.assertFalse(
                menu.editFilter(player, 4, -1, "", "copy"), "Bags remain invalid samples");
        helper.assertTrue(
                menu.sample().is(Items.STONE), "Rejected sample leaves reference unchanged");
        menu.setCarried(ItemStack.EMPTY);
        menu.editFilter(player, 4, -1, "", "copy");
        helper.assertTrue(menu.sample().isEmpty(), "Clicking with empty cursor clears reference");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void openEquipmentRequestWithoutBagIsHarmless(GameTestHelper helper) {
        Player player = player(helper);
        helper.assertFalse(
                EquippedBagAccess.open(player),
                "No equipped bag means no menu, even if Curios is absent");
        helper.assertFalse(
                EquippedBagAccess.openDirect(player),
                "Direct key also refuses to open without a Big Bag");
        helper.assertTrue(
                player.containerMenu == player.inventoryMenu, "Existing menu stays unchanged");
        helper.succeed();
    }
}
