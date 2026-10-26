package github.nyeptos.packingcube.gametest.refill;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.refill.RefillSettings;
import github.nyeptos.packingcube.bag.refill.RefillTargets;
import github.nyeptos.packingcube.gametest.support.LegacyRefillFixture;
import github.nyeptos.packingcube.gui.input.RefillTargetInteraction;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.network.FilterActions;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class RefillTargetGameTests {
    @GameTest(template = "empty")
    public static void selectionRequiresMatchingPressAndRelease(GameTestHelper h) {
        var state = new RefillTargetInteraction();
        state.press("inventory:0", 0);
        h.assertTrue(state.release("inventory:0", 0) == null, "Ordinary mode does not select");
        state.selecting(true);
        state.press("inventory:0", 0);
        h.assertTrue(
                state.pressed("inventory:0") && state.release("inventory:1", 0) == null,
                "Drag away cancels");
        state.press("inventory:0", 1);
        h.assertTrue(state.release("inventory:0", 1) == null, "Only left click selects");
        state.press("inventory:0", 0);
        h.assertTrue(
                "inventory:0".equals(state.release("inventory:0", 0)), "Matching release selects");
        state.press("inventory:0", 0);
        state.selecting(false);
        state.selecting(true);
        h.assertTrue(state.release("inventory:0", 0) == null, "Mode switch clears capture");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldHotbarSettingsMigrateWithoutNewTargetsSelected(GameTestHelper h) {
        var pouch = LegacyRefillFixture.pouch();
        RefillSettings.set(pouch, "slots", "5");
        RefillSettings.set(pouch, "offhand", "false");
        h.assertTrue(
                RefillTargets.selected(pouch, "inventory:0")
                        && !RefillTargets.selected(pouch, "inventory:1")
                        && RefillTargets.selected(pouch, "inventory:2"),
                "Legacy masks retained");
        for (int i = 9; i <= 40; i++)
            h.assertTrue(
                    !RefillTargets.selected(pouch, RefillTargets.inventory(i)), "New slots opt in");
        h.assertTrue(!RefillTargets.selected(pouch, "curios:ring:0"), "New Curios targets opt in");
        RefillTargets.set(pouch, "inventory:0", false);
        RefillTargets.set(pouch, "inventory:40", true);
        h.assertTrue(
                !RefillSettings.suppliesSlot(pouch, 0) && RefillSettings.suppliesSlot(pouch, 40),
                "Existing hand refill follows slot choices");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void stableTargetsPersistAndCopyWithoutPouchStorage(GameTestHelper h) {
        var pouch = ModItems.REFILL_POUCH.get().getDefaultInstance();
        new BagInventory(pouch, false, () -> {}).setItem(0, new ItemStack(Items.DIAMOND, 8));
        for (String id : new String[] {"inventory:9", "inventory:39", "curios:ring:2"})
            h.assertTrue(RefillTargets.set(pouch, id, true), "Valid target accepted");
        var copy = ModItems.REFILL_POUCH.get().getDefaultInstance();
        RefillSettings.copySettings(pouch, copy);
        h.assertTrue(
                RefillTargets.selected(copy, "curios:ring:2")
                        && !RefillTargets.selected(copy, "curios:ring:0"),
                "Identity is type + index, never display order");
        h.assertTrue(
                RefillTargets.selected(copy, "inventory:39")
                        && new BagInventory(copy, false, () -> {}).isEmpty(),
                "Preview copies selection but not stored items");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidTargetIdsCannotWriteSettings(GameTestHelper h) {
        var pouch = ModItems.REFILL_POUCH.get().getDefaultInstance();
        var original = pouch.copy();
        for (String id :
                new String[] {
                    "inventory:-1",
                    "inventory:41",
                    "inventory:01",
                    "inventory:999999999999",
                    "curios:ring:-1",
                    "curios::0",
                    "other:0"
                })
            h.assertTrue(!RefillTargets.set(pouch, id, true), "Malformed destination rejected");
        h.assertTrue(ItemStack.matches(original, pouch), "Rejected writes are no-op");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void serverValidatesOpenPouchAndCurrentCuriosSlots(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = ModItems.REFILL_POUCH.get().getDefaultInstance();
        player.getInventory().setItem(0, pouch);
        var menu =
                new BagMenu(
                        42,
                        player.getInventory(),
                        new BagInventory(pouch, false, () -> {}),
                        pouch,
                        0,
                        false);
        player.containerMenu = menu;
        h.assertTrue(
                menu.editFilter(player, FilterActions.SET_REFILL_TARGET, -1, "true", "inventory:9"),
                "Real open refill pouch accepts selection");
        h.assertTrue(
                RefillTargets.selected(menu.filterSnapshot(), "inventory:9"), "Snapshot synced");
        h.assertTrue(
                !menu.editFilter(
                        player,
                        FilterActions.SET_REFILL_TARGET,
                        -1,
                        "true",
                        "curios:nonexistent:0"),
                "Missing Curios slot rejected");
        menu.setCarried(new ItemStack(Items.STONE));
        h.assertTrue(
                !menu.editFilter(
                        player, FilterActions.SET_REFILL_TARGET, -1, "true", "inventory:10"),
                "Cursor stack prevents unsafe mode edits");
        h.assertTrue(
                menu.getCarried().is(Items.STONE) && player.getInventory().getItem(0) == pouch,
                "No item movement or cursor consumption");
        menu.setCarried(ItemStack.EMPTY);
        player.containerMenu = player.inventoryMenu;
        h.assertTrue(
                !menu.editFilter(
                        player, FilterActions.SET_REFILL_TARGET, -1, "true", "inventory:10"),
                "Stale menu rejected");
        h.succeed();
    }
}
