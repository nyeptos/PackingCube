package github.nyeptos.packingcube.gametest.refill;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.bag.refill.RefillDispatcher;
import github.nyeptos.packingcube.bag.refill.RefillService;
import github.nyeptos.packingcube.bag.refill.RefillSettings;
import github.nyeptos.packingcube.bag.refill.RefillSlotSettings;
import github.nyeptos.packingcube.bag.refill.RefillSupply;
import github.nyeptos.packingcube.bag.refill.RefillTargets;
import github.nyeptos.packingcube.gametest.support.LegacyRefillFixture;
import github.nyeptos.packingcube.gui.input.RefillSettingsView;
import github.nyeptos.packingcube.gui.input.RefillTargetInteraction;
import github.nyeptos.packingcube.menu.BagMenu;
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

import java.util.Set;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class RefillSourceGameTests {
    private static BagInventory storage(ItemStack p) {
        return new BagInventory(p, false, () -> {});
    }

    private static ItemStack pouch(ItemStack... items) {
        var pouch = ModItems.REFILL_POUCH.get().getDefaultInstance();
        var inv = storage(pouch);
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

    private static int count(ItemStack root, int slot) {
        return storage(saved(root)).getItem(slot).getCount();
    }

    private static void assign(ItemStack pouch, int source, int destination, int target) {
        RefillSlotSettings.set(pouch, source, "destination", RefillTargets.inventory(destination));
        RefillSlotSettings.set(pouch, source, "target", Integer.toString(target));
    }

    @GameTest(template = "empty")
    public static void perSourceRulesAndPagesAreIndependent(GameTestHelper h) {
        var pouch = pouch();
        var view = new RefillSettingsView();
        var original = pouch.copy();
        view.source(8);
        view.page(RefillSettingsView.METHOD);
        h.assertTrue(
                ItemStack.matches(original, pouch) && RefillSlotSettings.mapped(pouch),
                "Browsing does not change new source-rule settings");
        assign(pouch, 0, 9, 3);
        assign(pouch, 1, 9, 7);
        RefillSlotSettings.set(pouch, 1, "replace", "true");
        RefillSlotSettings.set(pouch, 1, "priority", "9");
        view.source(0);
        h.assertTrue(
                view.rule(pouch).target() == 3 && !view.rule(pouch).replace(),
                "First source retains own settings");
        view.source(1);
        h.assertTrue(
                view.rule(pouch).target() == 7
                        && view.rule(pouch).replace()
                        && view.rule(pouch).priority() == 9,
                "Second source independent, duplicate destination allowed");
        var snapshot = ModItems.REFILL_POUCH.get().getDefaultInstance();
        RefillSettings.copySettings(pouch, snapshot);
        h.assertTrue(
                RefillSlotSettings.get(snapshot, 1).equals(RefillSlotSettings.get(pouch, 1)),
                "Radial preview includes source settings");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyTargetsReceiveOnlyTheirAssignedSource(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.TORCH, 12), new ItemStack(Items.TORCH, 20));
        assign(pouch, 0, 9, 4);
        assign(pouch, 1, 40, 3);
        var root = install(p, pouch);
        h.assertTrue(
                RefillDispatcher.tick(p, Set.of()) == 7,
                "Initially empty inventory and offhand fill without use events");
        h.assertTrue(
                p.getInventory().getItem(9).getCount() == 4 && p.getOffhandItem().getCount() == 3,
                "Each destination receives own count");
        h.assertTrue(
                count(root, 0) == 8 && count(root, 1) == 17,
                "Never withdraw from another source slot");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void disabledUnassignedAndGlobalFilterRulesStopSupply(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.STONE, 20), new ItemStack(Items.DIRT, 20));
        assign(pouch, 0, 0, 4);
        RefillSlotSettings.set(pouch, 0, "enabled", "false");
        var root = install(p, pouch);
        h.assertTrue(
                RefillDispatcher.tick(p, Set.of()) == 0 && count(root, 0) == 20,
                "OFF and unassigned sources never supply");
        var parent = new BagInventory(root, true, () -> {});
        var mounted = parent.getItem(0);
        RefillSlotSettings.set(mounted, 0, "enabled", "true");
        RefillSettings.set(mounted, "filter", "true");
        BagFilter.setExcluding(mounted, false);
        parent.setChanged();
        h.assertTrue(
                RefillDispatcher.tick(p, Set.of()) == 0,
                "Shared enabled include filter with no rules blocks supply");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void replacementOffPreservesOtherItem(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.STONE, 10));
        assign(pouch, 0, 0, 4);
        var root = install(p, pouch);
        p.getInventory().setItem(0, new ItemStack(Items.DIRT, 2));
        h.assertTrue(
                RefillDispatcher.tick(p, Set.of()) == 0
                        && p.getInventory().getItem(0).is(Items.DIRT)
                        && count(root, 0) == 10,
                "Other item untouched by default");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void replacementReturnsOldStackWithoutLoss(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.STONE, 10));
        assign(pouch, 0, 0, 4);
        RefillSlotSettings.set(pouch, 0, "replace", "true");
        var root = install(p, pouch);
        p.getInventory().setItem(0, new ItemStack(Items.DIRT, 7));
        h.assertTrue(
                RefillDispatcher.tick(p, Set.of()) == 4
                        && p.getInventory().getItem(0).is(Items.STONE)
                        && count(root, 0) == 6,
                "Source count conserved");
        int dirt = 0;
        for (int i = 0; i < 36; i++)
            if (p.getInventory().getItem(i).is(Items.DIRT))
                dirt += p.getInventory().getItem(i).getCount();
        h.assertTrue(dirt == 7, "Displaced stack fully returned, not deleted or dropped");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void noReturnCapacityLeavesBothSidesUnchanged(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.STONE, 10));
        assign(pouch, 0, 0, 4);
        RefillSlotSettings.set(pouch, 0, "replace", "true");
        var root = install(p, pouch);
        for (int i = 0; i < 36; i++)
            if (i != 8) p.getInventory().setItem(i, new ItemStack(Items.DIAMOND, 64));
        var oldRoot = root.copy();
        h.assertTrue(
                RefillDispatcher.tick(p, Set.of()) == 0
                        && ItemStack.matches(oldRoot, root)
                        && p.getInventory().getItem(0).getCount() == 64,
                "Full inventory aborts before any mutation");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void higherPriorityWinsAndDoesNotPingPong(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.DIRT, 12), new ItemStack(Items.STONE, 12));
        assign(pouch, 0, 0, 4);
        assign(pouch, 1, 0, 4);
        RefillSlotSettings.set(pouch, 0, "replace", "true");
        RefillSlotSettings.set(pouch, 1, "replace", "true");
        RefillSlotSettings.set(pouch, 1, "priority", "9");
        var root = install(p, pouch);
        h.assertTrue(
                RefillDispatcher.tick(p, Set.of()) == 4
                        && p.getInventory().getItem(0).is(Items.STONE),
                "Higher source priority wins regardless of source number");
        h.assertTrue(
                RefillDispatcher.tick(p, Set.of()) == 0
                        && count(root, 0) == 12
                        && count(root, 1) == 8,
                "Satisfied high-priority destination blocks later replacement");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void equalPriorityUsesSourceOrderAndEmptySourceFallsBack(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch =
                pouch(
                        ItemStack.EMPTY,
                        new ItemStack(Items.STONE, 12),
                        new ItemStack(Items.DIRT, 12));
        assign(pouch, 0, 0, 4);
        assign(pouch, 1, 0, 4);
        assign(pouch, 2, 0, 4);
        var root = install(p, pouch);
        h.assertTrue(
                RefillDispatcher.tick(p, Set.of()) == 4
                        && p.getInventory().getItem(0).is(Items.STONE)
                        && count(root, 2) == 12,
                "First stocked source in 1-9 order supplies fallback");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void armorUsesVanillaPlacementAndStackLimit(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.STONE, 12));
        assign(pouch, 0, 39, 64);
        assign(pouch, 1, 38, 64);
        var root = install(p, pouch);
        h.assertTrue(
                RefillDispatcher.tick(p, Set.of()) == 1
                        && p.getInventory().getItem(39).is(Items.DIAMOND_HELMET),
                "Correct armor equips, upper limit one");
        h.assertTrue(
                p.getInventory().getItem(38).isEmpty() && count(root, 1) == 12,
                "Wrong equipment type rejected");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void thresholdUseAndItemStackLimitsAreRespected(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.ENDER_PEARL, 16));
        assign(pouch, 0, 0, 64);
        RefillSlotSettings.set(pouch, 0, "timing", "use");
        var root = install(p, pouch);
        p.getInventory().setItem(0, new ItemStack(Items.ENDER_PEARL, 3));
        h.assertTrue(
                RefillDispatcher.tick(p, Set.of()) == 0,
                "Use mode does not top off an existing stack passively");
        h.assertTrue(
                RefillDispatcher.tick(p, Set.of("inventory:0")) == 13
                        && p.getInventory().getItem(0).getCount() == 16
                        && count(root, 0) == 3,
                "Use witness triggers count clamped to real item maximum");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void openMenusAndCursorStacksPauseAutomaticRefill(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.STONE, 12));
        assign(pouch, 0, 0, 4);
        var root = install(p, pouch);
        p.inventoryMenu.setCarried(new ItemStack(Items.DIRT));
        h.assertTrue(
                RefillDispatcher.tick(p, Set.of()) == 0 && count(root, 0) == 12,
                "Cursor activity pauses supply");
        p.inventoryMenu.setCarried(ItemStack.EMPTY);
        var parent = new BagInventory(root, true, () -> {});
        p.containerMenu =
                new BagMenu(
                        42,
                        p.getInventory(),
                        new BagInventory(parent.getItem(0), false, parent),
                        root,
                        8,
                        false);
        h.assertTrue(
                RefillDispatcher.tick(p, Set.of()) == 0 && count(root, 0) == 12,
                "Editing/selection screen cannot move items automatically");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void inputModesRequireExplicitEntryAndCommitExits(GameTestHelper h) {
        var state = new RefillTargetInteraction();
        var view = new RefillSettingsView();
        view.source(7);
        state.press("inventory:0", 0);
        h.assertTrue(
                !state.selecting() && state.release("inventory:0", 0) == null,
                "Changing source page does not enter selection");
        state.selecting(true);
        state.press("inventory:0", 0);
        h.assertTrue(
                state.release("inventory:1", 0) == null && state.selecting(),
                "Drag away does not change destination");
        state.press("inventory:0", 0);
        h.assertTrue(
                "inventory:0".equals(state.release("inventory:0", 0)) && !state.selecting(),
                "Commit automatically returns to item mode");
        state.selecting(true);
        state.press("inventory:1", 0);
        state.selecting(false);
        h.assertTrue(state.release("inventory:1", 0) == null, "Cancel never commits stale press");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void badSourcePacketsAndMissingCuriosCannotChangePouch(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch();
        p.getInventory().setItem(0, pouch);
        var menu = new BagMenu(42, p.getInventory(), storage(pouch), pouch, 0, false);
        p.containerMenu = menu;
        var original = pouch.copy();
        for (String[] bad :
                new String[][] {
                    {"priority", "10"},
                    {"target", "65"},
                    {"threshold", "64"},
                    {"enabled", "yes"},
                    {"destination", "inventory:41"},
                    {"destination", "curios:back:00"},
                    {"container", "true"}
                })
            h.assertTrue(
                    !menu.editFilter(p, FilterActions.SET_REFILL_SOURCE, 0, bad[0], bad[1]),
                    "Invalid rule rejected");
        h.assertTrue(
                !menu.editFilter(p, FilterActions.SET_REFILL_SOURCE, 9, "enabled", "true")
                        && !menu.editFilter(
                                p,
                                FilterActions.SET_REFILL_SOURCE,
                                0,
                                "destination",
                                "curios:nonexistent:0"),
                "Out-of-range source and nonexistent equipment rejected");
        h.assertTrue(ItemStack.matches(original, pouch), "Rejected edits are no-op");
        h.assertTrue(
                menu.editFilter(p, FilterActions.SET_REFILL_SOURCE, 0, "destination", "inventory:9")
                        && menu.editFilter(
                                p,
                                FilterActions.SET_REFILL_SOURCE,
                                1,
                                "destination",
                                "inventory:9"),
                "Server accepts overlapping destinations");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void backpackNeverDisplacedAndLegacyHandsDoNotBypassMapping(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.STONE, 12));
        assign(pouch, 0, 8, 4);
        RefillSlotSettings.set(pouch, 0, "replace", "true");
        var root = install(p, pouch);
        h.assertTrue(
                RefillDispatcher.tick(p, Set.of()) == 0 && count(root, 0) == 12,
                "Live source root cannot be replaced or copied back with stale contents");
        p.getInventory().selected = 0;
        h.assertTrue(
                RefillService.hand(p, 0, new ItemStack(Items.STONE)) == 0,
                "Legacy hand refill cannot draw from mapped sources");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void partialReturnReservationCannotPartiallyMutateInventory(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.STONE, 10));
        assign(pouch, 0, 0, 4);
        RefillSlotSettings.set(pouch, 0, "replace", "true");
        var root = install(p, pouch);
        for (int j = 0; j < 36; j++)
            if (j != 8) p.getInventory().setItem(j, new ItemStack(Items.DIAMOND, 64));
        p.getInventory().setItem(0, new ItemStack(Items.DIRT, 64));
        p.getInventory().setItem(1, new ItemStack(Items.DIRT, 60));
        var before = root.copy();
        h.assertTrue(
                RefillDispatcher.tick(p, Set.of()) == 0
                        && ItemStack.matches(root, before)
                        && p.getInventory().getItem(1).getCount() == 60,
                "Failed multi-slot return reservation never applies its partial plan");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void perSourceOffAlsoBlocksTradeWithdrawal(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.STONE, 12), new ItemStack(Items.STONE, 12));
        RefillSlotSettings.set(pouch, 0, "enabled", "false");
        RefillSlotSettings.set(pouch, 1, "priority", "9");
        var root = install(p, pouch);
        var supplies = RefillSupply.collect(p, true);
        h.assertTrue(
                RefillSupply.available(supplies, new ItemStack(Items.STONE)) == 12,
                "Trade cannot bypass source OFF");
        var plan = new java.util.ArrayList<RefillSupply.Withdrawal>();
        h.assertTrue(
                RefillSupply.reserve(supplies, new ItemStack(Items.STONE), 5, plan),
                "Enabled source can still supply trades");
        RefillSupply.commit(plan);
        h.assertTrue(
                count(root, 0) == 12 && count(root, 1) == 7, "Withdraw only from enabled source");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void bindingArmorCannotBeReplaced(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.DIAMOND_HELMET));
        assign(pouch, 0, 39, 1);
        RefillSlotSettings.set(pouch, 0, "replace", "true");
        var root = install(p, pouch);
        var old = new ItemStack(Items.IRON_HELMET);
        old.enchant(
                h.getLevel()
                        .registryAccess()
                        .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                        .getOrThrow(
                                net.minecraft.world.item.enchantment.Enchantments.BINDING_CURSE),
                1);
        p.getInventory().setItem(39, old);
        h.assertTrue(
                RefillDispatcher.tick(p, Set.of()) == 0
                        && ItemStack.matches(old, p.getInventory().getItem(39))
                        && count(root, 0) == 1,
                "Vanilla curse and mayPickup constraints remain authoritative");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void newPouchHasNoImplicitDestinationsAndOldPouchStillMigrates(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var fresh = pouch(new ItemStack(Items.STONE, 12));
        h.assertTrue(RefillSlotSettings.mapped(fresh), "New default is explicit source rules");
        install(player, fresh);
        player.getInventory().selected = 0;
        h.assertTrue(
                RefillDispatcher.tick(player, Set.of()) == 0
                        && RefillService.hand(player, 0, new ItemStack(Items.STONE)) == 0,
                "Unassigned new pouch cannot refill arbitrary hand slots");
        var old = LegacyRefillFixture.pouch();
        var before = old.copy();
        var view = new RefillSettingsView();
        view.source(4);
        view.page(RefillSettingsView.METHOD);
        h.assertTrue(
                !RefillSlotSettings.mapped(old) && ItemStack.matches(old, before),
                "Browsing old pouch leaves saved behavior intact");
        RefillSlotSettings.set(old, 4, "destination", "inventory:9");
        h.assertTrue(
                RefillSlotSettings.mapped(old)
                        && RefillSlotSettings.get(old, 4).destination().equals("inventory:9"),
                "Explicit edit switches old pouch to source rules");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void allNinePagesAssignThroughPacketsPersistAndActuallySupply(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch();
        var inventory = storage(pouch);
        for (int i = 0; i < 9; i++) inventory.setItem(i, new ItemStack(Items.TORCH, 20));
        var root = install(player, pouch);
        var parent = new BagInventory(root, true, () -> {});
        var inner = new BagInventory(parent.getItem(0), false, parent);
        var menu = new BagMenu(51, player.getInventory(), inner, root, 8, false);
        player.containerMenu = menu;
        var view = new RefillSettingsView();
        var buffer =
                new net.minecraft.network.RegistryFriendlyByteBuf(
                        io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            for (int i = 0; i < 9; i++) {
                view.source(i);
                var request =
                        new github.nyeptos.packingcube.network.FilterActionPayload(
                                51,
                                FilterActions.SET_REFILL_SOURCE,
                                view.source(),
                                "destination",
                                RefillTargets.inventory(9 + i));
                buffer.clear();
                github.nyeptos.packingcube.network.FilterActionPayload.STREAM_CODEC.encode(
                        buffer, request);
                var decoded =
                        github.nyeptos.packingcube.network.FilterActionPayload.STREAM_CODEC.decode(
                                buffer);
                h.assertTrue(
                        menu.editFilter(
                                player,
                                decoded.operation(),
                                decoded.sampleSlot(),
                                decoded.field(),
                                decoded.value()),
                        "Source " + (i + 1) + " accepts packet assignment");
                h.assertTrue(
                        menu.editFilter(
                                player,
                                FilterActions.SET_REFILL_SOURCE,
                                i,
                                "target",
                                Integer.toString(i + 1)),
                        "Source count accepted");
                h.assertTrue(
                        view.rule(menu.filterSnapshot())
                                .destination()
                                .equals(RefillTargets.inventory(9 + i)),
                        "Source snapshot matches page " + (i + 1));
            }
        } finally {
            buffer.release();
        }
        for (int i = 0; i < 9; i++)
            h.assertTrue(
                    RefillSlotSettings.get(saved(root), i)
                            .destination()
                            .equals(RefillTargets.inventory(9 + i)),
                    "Source " + (i + 1) + " persists independently");
        player.containerMenu = player.inventoryMenu;
        h.assertTrue(
                RefillDispatcher.tick(player, Set.of()) == 45,
                "All nine sources supply their assigned independent targets");
        for (int i = 0; i < 9; i++)
            h.assertTrue(
                    player.getInventory().getItem(9 + i).getCount() == i + 1
                            && count(root, i) == 19 - i,
                    "Source " + (i + 1) + " supplies only own target");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void modalSelectionPinsEverySourceAndClearsStalePresses(GameTestHelper h) {
        var input = new RefillTargetInteraction();
        var page = new RefillSettingsView();
        for (int source = 0; source < 9; source++) {
            page.source(source);
            input.begin(page.source());
            input.press("inventory:9", 0);
            // A queued GUI rebuild/page callback must not change the captured packet source.
            page.source((source + 1) % 9);
            h.assertTrue(input.source() == source, "Captured source remains " + (source + 1));
            h.assertTrue(
                    "inventory:9".equals(input.release("inventory:9", 0)) && !input.selecting(),
                    "All nine sources commit matching release");
            input.begin(source);
            input.press("inventory:10", 0);
            input.begin(source);
            h.assertTrue(
                    input.release("inventory:10", 0) == null && input.selecting(),
                    "Restart clears stale press from old widgets");
            input.press("inventory:10", 0);
            input.selecting(false);
            h.assertTrue(
                    input.release("inventory:10", 0) == null,
                    "Cancel does not send a stale assignment");
        }
        h.succeed();
    }
}
