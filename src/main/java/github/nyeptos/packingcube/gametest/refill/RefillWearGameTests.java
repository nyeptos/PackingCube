package github.nyeptos.packingcube.gametest.refill;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.refill.RefillDiagnostics;
import github.nyeptos.packingcube.bag.refill.RefillDispatcher;
import github.nyeptos.packingcube.bag.refill.RefillSlotSettings;
import github.nyeptos.packingcube.bag.refill.RefillTargets;
import github.nyeptos.packingcube.gui.input.RefillSettingsView;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.menu.BagRadialMenu;
import github.nyeptos.packingcube.network.FilterActions;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** A same-item swap can wait until the item in place is nearly spent. */
@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class RefillWearGameTests {
    private static ItemStack pouch(ItemStack... items) {
        var pouch = ModItems.REFILL_POUCH.get().getDefaultInstance();
        var inv = new BagInventory(pouch, false, () -> {});
        for (int i = 0; i < items.length; i++) inv.setItem(i, items[i]);
        return pouch;
    }

    private static ItemStack install(Player player, ItemStack pouch) {
        var root = ModItems.LARGE_BAG.get().getDefaultInstance();
        new BagInventory(root, true, () -> {}).setItem(0, pouch);
        player.getInventory().setItem(8, root);
        return root;
    }

    private static ItemStack saved(ItemStack root) {
        return new BagInventory(root, true, () -> {}).getItem(0);
    }

    private static ItemStack pickaxe(int damage) {
        var stack = new ItemStack(Items.DIAMOND_PICKAXE);
        stack.setDamageValue(damage);
        return stack;
    }

    private static void assign(ItemStack pouch, int destination, String mode, int value) {
        assign(pouch, 0, destination, mode, value);
    }

    private static void assign(
            ItemStack pouch, int source, int destination, String mode, int value) {
        RefillSlotSettings.set(
                pouch, source, "destination", RefillTargets.inventory(destination));
        RefillSlotSettings.set(pouch, source, "target", "1");
        RefillSlotSettings.set(pouch, source, "replace", "true");
        RefillSlotSettings.set(pouch, source, "durability", Integer.toString(value));
        RefillSlotSettings.set(pouch, source, "durabilityMode", mode);
    }

    @GameTest(template = "empty")
    public static void percentWaitsForTheLimit(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(pickaxe(0));
        assign(pouch, 9, "percent", 5);
        install(player, pouch);
        player.getInventory().setItem(9, pickaxe(1000));
        h.assertTrue(
                RefillDispatcher.tick(player) == 0
                        && player.getInventory().getItem(9).getDamageValue() == 1000,
                "A partly worn item keeps its slot");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void percentSwapsANearlyBrokenItem(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(pickaxe(0));
        assign(pouch, 9, "percent", 5);
        var root = install(player, pouch);
        player.getInventory().setItem(9, pickaxe(1550));
        h.assertTrue(RefillDispatcher.tick(player) == 1, "A nearly broken item is replaced");
        h.assertTrue(
                player.getInventory().getItem(9).getDamageValue() == 0
                        && storage(saved(root)).getItem(0).getDamageValue() == 1550,
                "The fresh item takes the slot and the spent one is collected");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void countUsesRemainingPoints(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(pickaxe(0));
        assign(pouch, 9, "count", 20);
        install(player, pouch);
        player.getInventory().setItem(9, pickaxe(1545));
        h.assertTrue(
                RefillDispatcher.tick(player) == 1
                        && player.getInventory().getItem(9).getDamageValue() == 0,
                "Sixteen points left is inside a limit of twenty");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void offSwapsImmediately(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(pickaxe(0));
        assign(pouch, 9, "off", 5);
        install(player, pouch);
        player.getInventory().setItem(9, pickaxe(1000));
        h.assertTrue(
                RefillDispatcher.tick(player) == 1,
                "The limit off keeps the plain replacement behaviour");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void differentItemsIgnoreTheLimit(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.STONE, 12));
        assign(pouch, 9, "percent", 1);
        install(player, pouch);
        player.getInventory().setItem(9, new ItemStack(Items.DIRT));
        h.assertTrue(
                RefillDispatcher.tick(player) == 1
                        && player.getInventory().getItem(9).is(Items.STONE),
                "Another item is replaced without waiting");
        h.succeed();
    }

    /** Dragging the limit must not rebuild the panel, or the grabbed knob is replaced. */
    @GameTest(template = "empty")
    public static void changingTheLimitKeepsThePanel(GameTestHelper h) {
        var pouch = pouch(pickaxe(0));
        RefillSlotSettings.set(pouch, 0, "durabilityMode", "percent");
        RefillSlotSettings.set(pouch, 0, "durability", "5");
        var view = new RefillSettingsView();
        view.page(RefillSettingsView.METHOD);
        var before = view.shape(pouch);
        RefillSlotSettings.set(pouch, 0, "durability", "40");
        h.assertTrue(view.shape(pouch).equals(before), "A new limit value never forces a rebuild");
        RefillSlotSettings.set(pouch, 0, "durabilityMode", "count");
        h.assertTrue(
                !view.shape(pouch).equals(before),
                "Switching the mode still rebuilds to show or hide the slider");
        h.succeed();
    }

    /** The mode chosen in the settings panel has to survive the packet and reach the dispatcher. */
    @GameTest(template = "empty")
    public static void theModeFromThePanelWaitsForTheLimit(GameTestHelper h) {
        var player = new RefillOpeningGameTests.OpeningPlayer(h);
        var pouch = pouch(pickaxe(0));
        var root = ModItems.LARGE_BAG.get().getDefaultInstance();
        new BagInventory(root, true, () -> {}).setItem(0, pouch);
        player.getInventory().setItem(0, root);
        var radial = new BagRadialMenu(70, player.getInventory(), root);
        player.containerMenu = radial;
        h.assertTrue(radial.select(player, 0), "The pouch menu opens from the radial");
        var menu = (BagMenu) player.containerMenu;
        for (var edit :
                new String[][] {
                    {"destination", RefillTargets.inventory(9)},
                    {"target", "1"},
                    {"replace", "true"},
                    {"durabilityMode", "percent"},
                    {"durability", "5"}
                })
            h.assertTrue(
                    menu.editFilter(player, FilterActions.SET_REFILL_SOURCE, 0, edit[0], edit[1]),
                    "Panel edit accepted: " + edit[0]);
        var saved = RefillSlotSettings.get(menu.filterSnapshot(), 0);
        h.assertTrue(
                saved.durabilityMode().equals("percent") && saved.durability() == 5,
                "The snapshot shows the chosen limit");
        player.containerMenu = player.inventoryMenu;
        player.getInventory().setItem(9, pickaxe(1000));
        h.assertTrue(
                RefillDispatcher.tick(player) == 0
                        && player.getInventory().getItem(9).getDamageValue() == 1000,
                "A mode set from the panel still waits for the limit");
        h.succeed();
    }

    /** A full item must not take a worn supply, even though it carries no damage component. */
    @GameTest(template = "empty")
    public static void aFreshDestinationKeepsTheSupplyWaiting(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(pickaxe(800));
        assign(pouch, 9, "percent", 5);
        install(player, pouch);
        player.getInventory().setItem(9, new ItemStack(Items.DIAMOND_PICKAXE));
        h.assertTrue(
                RefillDispatcher.tick(player) == 0
                        && player.getInventory().getItem(9).getDamageValue() == 0,
                "A full item never takes the worn supply");
        h.succeed();
    }

    /** The panel stays open while the player watches, so the limit has to hold there too. */
    @GameTest(template = "empty")
    public static void theLimitStillWaitsWithThePanelOpen(GameTestHelper h) {
        var player = new RefillOpeningGameTests.OpeningPlayer(h);
        var pouch = pouch(pickaxe(0));
        var root = ModItems.LARGE_BAG.get().getDefaultInstance();
        new BagInventory(root, true, () -> {}).setItem(0, pouch);
        player.getInventory().setItem(0, root);
        var radial = new BagRadialMenu(70, player.getInventory(), root);
        player.containerMenu = radial;
        h.assertTrue(radial.select(player, 0), "The pouch menu opens from the radial");
        var menu = (BagMenu) player.containerMenu;
        for (var edit :
                new String[][] {
                    {"destination", RefillTargets.inventory(9)},
                    {"target", "1"},
                    {"replace", "true"},
                    {"durabilityMode", "percent"},
                    {"durability", "5"}
                })
            h.assertTrue(
                    menu.editFilter(player, FilterActions.SET_REFILL_SOURCE, 0, edit[0], edit[1]),
                    "Panel edit accepted: " + edit[0]);
        player.getInventory().setItem(9, pickaxe(1000));
        h.assertTrue(
                RefillDispatcher.tick(player) == 0
                        && player.getInventory().getItem(9).getDamageValue() == 1000,
                "The limit still waits while the pouch screen is open");
        h.succeed();
    }

    /** A plain rule must not swap in ahead of the rule that asks to wait on the same slot. */
    @GameTest(template = "empty")
    public static void aPlainRuleNeverSwapsAheadOfTheLimit(GameTestHelper h) {
        for (boolean limitFirst : new boolean[] {true, false}) {
            var player = h.makeMockPlayer(GameType.SURVIVAL);
            var pouch = pouch(pickaxe(0), pickaxe(0));
            assign(pouch, limitFirst ? 0 : 1, 9, "percent", 5);
            assign(pouch, limitFirst ? 1 : 0, 9, "off", 5);
            install(player, pouch);
            player.getInventory().setItem(9, pickaxe(1000));
            h.assertTrue(
                    RefillDispatcher.tick(player) == 0
                            && player.getInventory().getItem(9).getDamageValue() == 1000,
                    "The strictest limit holds the destination (limit first: " + limitFirst + ")");
        }
        h.succeed();
    }

    /** Without any limit the plain swap still happens, so the hold above is the only change. */
    @GameTest(template = "empty")
    public static void twoPlainRulesStillSwap(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(pickaxe(0), pickaxe(0));
        assign(pouch, 0, 9, "off", 5);
        assign(pouch, 1, 9, "off", 5);
        install(player, pouch);
        player.getInventory().setItem(9, pickaxe(1000));
        h.assertTrue(RefillDispatcher.tick(player) == 1, "Two plain rules swap as before");
        h.succeed();
    }

    /** The read-only dump has to say why the dispatcher keeps the slot. */
    @GameTest(template = "empty")
    public static void theRuleDumpExplainsAWait(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(pickaxe(0));
        assign(pouch, 9, "percent", 5);
        install(player, pouch);
        var view = new BagInventory(pouch, false, () -> {});
        player.getInventory().setItem(9, pickaxe(1000));
        var waiting = RefillDiagnostics.rule(player, view, 0);
        h.assertTrue(
                waiting.contains("wear=percent/5") && waiting.endsWith("-> wait"),
                "A partly worn item reports a wait: " + waiting);
        player.getInventory().setItem(9, pickaxe(1550));
        var swapping = RefillDiagnostics.rule(player, view, 0);
        h.assertTrue(
                swapping.endsWith("-> swap"),
                "A nearly broken item reports the swap: " + swapping);
        view.setItem(0, ItemStack.EMPTY);
        var empty = RefillDiagnostics.rule(player, view, 0);
        h.assertTrue(
                empty.contains("-> swap") && empty.contains("(source empty)"),
                "An empty source still reports what the limit decides: " + empty);
        h.assertTrue(
                waiting.contains("left=35%"),
                "Percent mode reports the share left: " + waiting);
        h.succeed();
    }

    /** The reported case: a sword at 78% must not be swapped by a ten percent limit. */
    @GameTest(template = "empty")
    public static void aPartlyWornSwordKeepsItsSlot(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.IRON_SWORD));
        assign(pouch, 0, 0, "percent", 10);
        install(player, pouch);
        var inPlace = new ItemStack(Items.IRON_SWORD);
        inPlace.setDamageValue(53);
        player.getInventory().setItem(0, inPlace);
        h.assertTrue(
                RefillDispatcher.tick(player) == 0
                        && player.getInventory().getItem(0).getDamageValue() == 53,
                "A sword at 78% stays until the limit is reached");
        h.succeed();
    }

    /** The limit belongs to the slot, so another tool in place waits for it as well. */
    @GameTest(template = "empty")
    public static void aDifferentToolInPlaceStillWaits(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.IRON_SWORD));
        assign(pouch, 9, "percent", 10);
        install(player, pouch);
        var inPlace = new ItemStack(Items.DIAMOND_SWORD);
        inPlace.setDamageValue(500);
        player.getInventory().setItem(9, inPlace);
        h.assertTrue(
                RefillDispatcher.tick(player) == 0
                        && player.getInventory().getItem(9).is(Items.DIAMOND_SWORD),
                "A tool that is still usable keeps the slot");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aSpentToolInPlaceIsReplaced(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.IRON_SWORD));
        assign(pouch, 9, "percent", 10);
        install(player, pouch);
        var inPlace = new ItemStack(Items.DIAMOND_SWORD);
        inPlace.setDamageValue(1550);
        player.getInventory().setItem(9, inPlace);
        h.assertTrue(
                RefillDispatcher.tick(player) == 1
                        && player.getInventory().getItem(9).is(Items.IRON_SWORD),
                "A spent tool is replaced once the limit is reached");
        h.succeed();
    }

    private static BagInventory storage(ItemStack pouch) {
        return new BagInventory(pouch, false, () -> {});
    }
}
