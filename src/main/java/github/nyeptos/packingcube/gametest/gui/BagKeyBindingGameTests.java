package github.nyeptos.packingcube.gametest.gui;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.EquippedBagAccess;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.gui.input.BagKeyActions;
import github.nyeptos.packingcube.gui.input.BagKeyLatch;
import github.nyeptos.packingcube.gui.radial.RadialKeyState;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.menu.BagRadialMenu;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static github.nyeptos.packingcube.gametest.support.RadialTestSupport.*;

/** Direct and radial key bindings, and navigation between open menus. */
@GameTestHolder(PackingCube.MODID)
@PrefixGameTestTemplate(false)
public final class BagKeyBindingGameTests {
    @GameTest(template = "empty")
    public static void keyActionsAreIndependentAndDuplicateBindingsSendOneAction(GameTestHelper h) {
        var none = BagKeyActions.Action.NONE;
        var radial = BagKeyActions.Action.RADIAL;
        var direct = BagKeyActions.Action.DIRECT;
        h.assertTrue(BagKeyActions.choose(false, false, false) == none, "No click does nothing");
        h.assertTrue(
                BagKeyActions.choose(true, false, false) == radial,
                "Radial key has its own action");
        h.assertTrue(
                BagKeyActions.choose(false, true, false) == direct, "Direct key bypasses radial");
        h.assertTrue(
                BagKeyActions.choose(true, true, false) == radial,
                "Simultaneous clicks send only radial");
        h.assertTrue(
                BagKeyActions.choose(false, true, true) == radial,
                "A duplicated binding is deterministic even if Minecraft dispatches only to direct"
                        + " mapping");
        h.assertTrue(
                BagKeyActions.choose(false, false, true) == none, "Duplicates do not auto-open");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void directKeyOpensManagementAndProtectsExistingMenus(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        player.getInventory().setItem(4, root);
        player.inventoryMenu.setCarried(new ItemStack(Items.DIAMOND, 3));
        h.assertFalse(
                EquippedBagAccess.openDirect(player), "Direct key never discards carried stack");
        player.inventoryMenu.setCarried(ItemStack.EMPTY);
        h.assertTrue(
                EquippedBagAccess.openDirect(player),
                "Direct key opens inventory Big Bag without Curios");
        h.assertTrue(
                player.containerMenu instanceof BagMenu menu
                        && menu.isModules()
                        && menu.rootBag() == root,
                "Direct key opens management, not radial");
        var current = player.containerMenu;
        h.assertFalse(
                EquippedBagAccess.open(player) || EquippedBagAccess.openDirect(player),
                "Neither key replaces another active GUI");
        h.assertTrue(player.containerMenu == current, "Existing menu remains unchanged");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void directKeyClosesAlreadyOpenBigBag(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        player.getInventory().setItem(0, root);
        h.assertTrue(EquippedBagAccess.openDirect(player), "Open management first");
        int id = player.containerMenu.containerId;
        h.assertTrue(EquippedBagAccess.navigate(player, id, false), "B closes management");
        h.assertTrue(player.containerMenu == player.inventoryMenu, "Returns to gameplay");
        h.assertFalse(
                EquippedBagAccess.navigate(player, id, false),
                "Repeated old B request cannot reopen");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void filterDirectKeyReturnsToItsParentNotFirstInventoryBag(GameTestHelper h) {
        Player player = player(h);
        ItemStack other = bag(), root = bag();
        player.getInventory().setItem(0, other);
        player.getInventory().setItem(8, root);
        BagInventory parent = modules(root);
        parent.setItem(3, module());
        BagInventory inner = new BagInventory(parent.getItem(3), false, parent);
        BagMenu menu = new BagMenu(7, player.getInventory(), inner, root, 8, false);
        player.containerMenu = menu;
        menu.getSlot(0).setByPlayer(new ItemStack(Items.DIAMOND, 11));
        h.assertTrue(
                EquippedBagAccess.navigate(player, 7, false), "B returns to parent management");
        h.assertTrue(
                player.containerMenu instanceof BagMenu next
                        && next.isModules()
                        && next.rootBag() == root,
                "Uses original parent instead of another bag in slot zero");
        h.assertTrue(
                storage(modules(root).getItem(3)).getItem(0).getCount() == 11,
                "Contents saved across transition");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialKeyWorksFromBigAndFilterScreens(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        modules(root).setItem(4, module());
        player.getInventory().setItem(0, root);
        h.assertTrue(EquippedBagAccess.openDirect(player), "Management opens");
        int originalId = player.containerMenu.containerId;
        h.assertTrue(
                EquippedBagAccess.navigate(player, originalId, true),
                "C from Big Bag opens radial");
        h.assertTrue(
                player.containerMenu instanceof BagRadialMenu first && first.ownsRoot(root),
                "Same parent radial");
        h.assertTrue(
                ((BagRadialMenu) player.containerMenu).select(player, 4),
                "Choose installed Filter Bag");
        BagMenu filter = (BagMenu) player.containerMenu;
        h.assertTrue(
                filter.editFilter(player, 3, -1, "mode", "exclude"),
                "Change settings before switching");
        h.assertTrue(
                EquippedBagAccess.navigate(player, filter.containerId, true),
                "C from Filter Bag opens radial");
        h.assertTrue(
                player.containerMenu instanceof BagRadialMenu second && second.ownsRoot(root),
                "Filter keeps its parent");
        h.assertTrue(
                BagFilter.isExcluding(modules(root).getItem(4)),
                "Filter settings survive navigation");
        h.assertFalse(
                EquippedBagAccess.navigate(player, originalId, true),
                "Stale ID cannot affect replacement screen");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void navigationRejectsCursorStaleIdAndMissingRoot(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        player.getInventory().setItem(0, root);
        EquippedBagAccess.openDirect(player);
        BagMenu menu = (BagMenu) player.containerMenu;
        menu.setCarried(new ItemStack(Items.DIAMOND, 13));
        h.assertFalse(
                EquippedBagAccess.navigate(player, menu.containerId, false)
                        || EquippedBagAccess.navigate(player, menu.containerId, true),
                "Both operations guard cursor");
        h.assertTrue(
                menu.getCarried().getCount() == 13 && player.containerMenu == menu,
                "Cursor and menu preserved");
        menu.setCarried(ItemStack.EMPTY);
        h.assertFalse(
                EquippedBagAccess.navigate(player, menu.containerId + 1, true), "Wrong ID ignored");
        player.getInventory().setItem(0, ItemStack.EMPTY);
        h.assertFalse(
                EquippedBagAccess.navigate(player, menu.containerId, true),
                "Missing root rejected");
        player.containerMenu = player.inventoryMenu;
        h.assertFalse(
                EquippedBagAccess.navigate(player, player.inventoryMenu.containerId, true),
                "Only BagMenu can navigate");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void standaloneFilterUsesPrimaryBigBagOnlyWhenPresent(GameTestHelper h) {
        Player player = player(h);
        ItemStack filter = module();
        player.getInventory().setItem(3, filter);
        BagMenu menu = new BagMenu(8, player.getInventory(), storage(filter), filter, 3, false);
        player.containerMenu = menu;
        h.assertFalse(
                EquippedBagAccess.navigate(player, 8, false)
                        || EquippedBagAccess.navigate(player, 8, true),
                "Without a Big Bag no destination is fabricated");
        ItemStack root = bag();
        player.getInventory().setItem(7, root);
        h.assertTrue(
                EquippedBagAccess.navigate(player, 8, false),
                "Standalone filter can return to carried Big Bag");
        h.assertTrue(
                player.containerMenu instanceof BagMenu next
                        && next.isModules()
                        && next.rootBag() == root,
                "Fallback selects actual Big Bag");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialToggleAndHeldInputDoNotDoubleFire(GameTestHelper h) {
        var state = new RadialKeyState(true);
        var none = RadialKeyState.Action.NONE;
        h.assertTrue(state.press() == none, "Opening key repeat is not a second press");
        state.release();
        h.assertFalse(state.held(), "Release rearms the toggle without selecting");
        h.assertTrue(state.press() == RadialKeyState.Action.CLOSE, "Fresh C closes");
        var hold = new RadialKeyState(true);
        hold.release();
        hold.release();
        h.assertFalse(hold.held(), "Releasing over any direction never selects or closes");
        h.assertTrue(hold.press() == RadialKeyState.Action.CLOSE, "Next fresh press closes");
        var sticky = new RadialKeyState(false);
        h.assertTrue(
                sticky.press() == RadialKeyState.Action.CLOSE,
                "Late-open sticky menu closes on next C");
        var latch = new BagKeyLatch();
        var direct = BagKeyActions.Action.DIRECT;
        h.assertTrue(latch.accept(direct, false), "First B accepted");
        latch.refresh(false, true);
        h.assertFalse(latch.accept(direct, false), "Holding close key cannot reopen GUI");
        latch.refresh(false, false);
        h.assertTrue(latch.accept(direct, false), "Release rearms key");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialKeyReleaseNeverChoosesBag(GameTestHelper h) {
        var state = new RadialKeyState(true);
        for (int repeat = 0; repeat < 10; repeat++) {
            h.assertTrue(
                    state.press() == RadialKeyState.Action.NONE,
                    "Opening hold/repeat does not close");
        }
        state.release();
        state.release();
        h.assertFalse(state.held(), "Release is idempotent and only rearms the key");
        h.assertTrue(state.press() == RadialKeyState.Action.CLOSE, "Only a fresh C press closes");
        var lateOpen = new RadialKeyState(false);
        lateOpen.release();
        h.assertTrue(
                lateOpen.press() == RadialKeyState.Action.CLOSE,
                "Already released before open still toggles normally");
        h.assertTrue(
                RadialKeyState.Action.values().length == 2,
                "Trigger state has no selection action");
        h.succeed();
    }
}
