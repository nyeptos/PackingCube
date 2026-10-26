package github.nyeptos.packingcube.gametest.pickup;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.bag.pickup.BagPickupSettings;
import github.nyeptos.packingcube.bag.pickup.BagRouter;
import github.nyeptos.packingcube.gui.input.FilterEditorState;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.network.FilterActions;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class PickupSettingsGameTests {
    private static Player player(GameTestHelper h) {
        return h.makeMockPlayer(GameType.SURVIVAL);
    }

    private static ItemStack backpack() {
        return ModItems.LARGE_BAG.get().getDefaultInstance();
    }

    private static ItemStack pouch(int priority) {
        ItemStack pouch = ModItems.FILTER_BAG.get().getDefaultInstance();
        BagFilter.add(pouch, "items", ResourceLocation.withDefaultNamespace("stone"));
        BagPickupSettings.setPriority(pouch, priority);
        return pouch;
    }

    private static BagInventory modules(ItemStack root) {
        return new BagInventory(root, true, () -> {});
    }

    private static BagInventory storage(ItemStack pouch) {
        return new BagInventory(pouch, false, () -> {});
    }

    private static int count(ItemStack root, int slot) {
        var storage = storage(modules(root).getItem(slot));
        int count = 0;
        for (int i = 0; i < storage.getContainerSize(); i++) count += storage.getItem(i).getCount();
        return count;
    }

    private static ItemStack install(Player player, int inventorySlot, ItemStack... pouches) {
        ItemStack root = backpack();
        var parent = modules(root);
        for (int i = 0; i < pouches.length; i++) parent.setItem(i, pouches[i]);
        player.getInventory().setItem(inventorySlot, root);
        return root;
    }

    private static void fillExcept(ItemStack root, int slot, int room) {
        var parent = modules(root);
        var storage = new BagInventory(parent.getItem(slot), false, parent);
        for (int i = 0; i < 9; i++)
            storage.setItem(i, new ItemStack(Items.STONE, i == 8 ? 64 - room : 64));
    }

    private static ItemEntity pickup(Player player, GameTestHelper h, int amount) {
        var entity = new ItemEntity(h.getLevel(), 0, 0, 0, new ItemStack(Items.STONE, amount));
        entity.setNoPickUpDelay();
        entity.playerTouch(player);
        return entity;
    }

    private static BagMenu edit(Player player, ItemStack root, boolean backpack) {
        var parent = modules(root);
        var contents = backpack ? parent : new BagInventory(parent.getItem(0), false, parent);
        var menu = new BagMenu(1, player.getInventory(), contents, root, 0, backpack);
        player.containerMenu = menu;
        return menu;
    }

    @GameTest(template = "empty")
    public static void missingSettingsPreservePreviousDefaults(GameTestHelper h) {
        ItemStack pouch = ModItems.FILTER_BAG.get().getDefaultInstance();
        h.assertTrue(
                BagPickupSettings.enabled(pouch) && BagPickupSettings.priority(pouch) == 0,
                "Old pouches remain enabled at priority zero");
        h.assertTrue(BagPickupSettings.enabled(backpack()), "Old backpacks remain enabled");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void priorityOverridesModulePosition(GameTestHelper h) {
        var player = player(h);
        ItemStack root = install(player, 0, pouch(0), pouch(9));
        ItemStack source = new ItemStack(Items.STONE, 11);
        h.assertTrue(BagRouter.route(player, source).isEmpty(), "Matching high priority accepts");
        h.assertTrue(
                source.getCount() == 11 && count(root, 0) == 0 && count(root, 1) == 11,
                "Last module wins by priority without changing caller stack");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void priorityIsGlobalAndTiesKeepInventoryOrder(GameTestHelper h) {
        var player = player(h);
        ItemStack first = install(player, 0, pouch(0));
        ItemStack second = install(player, 8, pouch(9));
        BagRouter.route(player, new ItemStack(Items.STONE, 5));
        h.assertTrue(
                count(first, 0) == 0 && count(second, 0) == 5,
                "Higher priority in a later backpack wins");
        var secondParent = modules(second);
        BagPickupSettings.setPriority(secondParent.getItem(0), 0);
        secondParent.setChanged();
        BagRouter.route(player, new ItemStack(Items.STONE, 3));
        h.assertTrue(
                count(first, 0) == 3 && count(second, 0) == 5,
                "Equal priority restores inventory order");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fullHighPriorityPassesExactRemainderToNextPouch(GameTestHelper h) {
        var player = player(h);
        ItemStack root = install(player, 0, pouch(0), pouch(9));
        fillExcept(root, 1, 4);
        h.assertTrue(
                BagRouter.route(player, new ItemStack(Items.STONE, 16)).isEmpty(),
                "Both modules together fit");
        h.assertTrue(
                count(root, 1) == 576 && count(root, 0) == 12,
                "High fills before low and one parent view preserves both saves");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void disablingPouchSkipsItButDoesNotDisableManualInsertion(GameTestHelper h) {
        var player = player(h);
        ItemStack disabled = pouch(9);
        BagPickupSettings.setEnabled(disabled, false);
        ItemStack root = install(player, 0, disabled, pouch(0));
        BagRouter.route(player, new ItemStack(Items.STONE, 6));
        h.assertTrue(
                count(root, 0) == 0 && count(root, 1) == 6, "Disabled priority nine is skipped");
        var parent = modules(root);
        var manual = new BagInventory(parent.getItem(0), false, parent);
        h.assertTrue(
                manual.insert(new ItemStack(Items.STONE, 2)).isEmpty(),
                "Manual matching insert works");
        h.assertTrue(
                manual.insert(new ItemStack(Items.DIAMOND)).getCount() == 1,
                "Manual nonmatching insert still rejected");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyBackpackOffDoesNotBlockEnabledPouches(GameTestHelper h) {
        var player = player(h);
        ItemStack first = install(player, 0, pouch(9));
        ItemStack second = install(player, 1, pouch(0));
        BagPickupSettings.setEnabled(first, false);
        BagRouter.route(player, new ItemStack(Items.STONE, 4));
        h.assertTrue(
                count(first, 0) == 4 && count(second, 0) == 0,
                "Legacy backpack OFF cannot block a higher-priority enabled pouch");
        h.assertFalse(BagPickupSettings.enabled(first), "Legacy data is ignored, not rewritten");
        var parent = modules(first);
        BagPickupSettings.setEnabled(parent.getItem(0), false);
        parent.setChanged();
        BagRouter.route(player, new ItemStack(Items.STONE, 2));
        h.assertTrue(
                count(first, 0) == 4 && count(second, 0) == 2,
                "Only the pouch switch disables a destination");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void automaticPickupUsesIncludeAndExcludeRules(GameTestHelper h) {
        var player = player(h);
        ItemStack excluding = pouch(9);
        BagFilter.setExcluding(excluding, true);
        ItemStack root = install(player, 0, excluding, pouch(0));
        BagRouter.route(player, new ItemStack(Items.STONE, 4));
        BagRouter.route(player, new ItemStack(Items.DIAMOND, 3));
        h.assertTrue(
                count(root, 0) == 3 && count(root, 1) == 4,
                "Excluding high priority refuses listed stone; include accepts only stone");
        h.assertTrue(
                storage(modules(root).getItem(0)).getItem(0).is(Items.DIAMOND),
                "Excluded item never entered high priority pouch");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void automaticPickupUsesTagRules(GameTestHelper h) {
        var player = player(h);
        ItemStack logs = ModItems.FILTER_BAG.get().getDefaultInstance();
        BagFilter.add(logs, "tags", ResourceLocation.withDefaultNamespace("logs"));
        install(player, 0, logs);
        h.assertTrue(
                BagRouter.route(player, new ItemStack(Items.OAK_LOG, 3)).isEmpty(),
                "Tag-matched logs enter installed pouch");
        h.assertTrue(
                BagRouter.route(player, new ItemStack(Items.STONE, 3)).getCount() == 3,
                "Unmatched stone remains for inventory");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void groundPickupStoresBeforeInventoryAndConservesRemainder(GameTestHelper h) {
        var player = player(h);
        ItemStack root = install(player, 0, pouch(9), pouch(0));
        fillExcept(root, 0, 2);
        fillExcept(root, 1, 3);
        var entity = pickup(player, h, 12);
        h.assertTrue(entity.isRemoved(), "Rest enters ordinary inventory through vanilla pickup");
        h.assertTrue(
                count(root, 0) == 576 && count(root, 1) == 576,
                "Exactly five items entered pouches");
        h.assertTrue(
                player.getInventory().countItem(Items.STONE) == 7,
                "Only seven remaining items enter inventory");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void disabledPickupUsesVanillaInventory(GameTestHelper h) {
        var player = player(h);
        ItemStack root = install(player, 0, pouch(9));
        var parent = modules(root);
        BagPickupSettings.setEnabled(parent.getItem(0), false);
        parent.setChanged();
        var entity = pickup(player, h, 13);
        h.assertTrue(
                entity.isRemoved()
                        && count(root, 0) == 0
                        && player.getInventory().countItem(Items.STONE) == 13,
                "Off leaves the entire pickup for vanilla without loss or duplication");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void loosePouchesAndManualInventoryChangesAreNotRouted(GameTestHelper h) {
        var player = player(h);
        player.getInventory().setItem(0, pouch(9));
        h.assertTrue(
                BagRouter.route(player, new ItemStack(Items.STONE, 5)).getCount() == 5,
                "Loose pouch never automatically stores");
        ItemStack root = install(player, 1, pouch(9));
        player.getInventory().setItem(2, new ItemStack(Items.STONE, 8));
        h.assertTrue(
                count(root, 0) == 0 && player.getInventory().getItem(2).getCount() == 8,
                "Manual inventory insertion is not intercepted");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void mountedEditorSavesSettingsAndKeepsFilterContents(GameTestHelper h) {
        var player = player(h);
        ItemStack root = install(player, 0, pouch(0));
        var parent = modules(root);
        new BagInventory(parent.getItem(0), false, parent).insert(new ItemStack(Items.STONE, 7));
        var menu = edit(player, root, false);
        h.assertTrue(
                menu.editFilter(player, FilterActions.SET_PICKUP_ENABLED, -1, "pickup", "false"),
                "Mounted pouch switch is accepted");
        h.assertTrue(
                menu.editFilter(player, FilterActions.SET_PICKUP_PRIORITY, -1, "priority", "8"),
                "Mounted pouch priority is accepted");
        ItemStack saved = modules(root).getItem(0);
        h.assertTrue(
                !BagPickupSettings.enabled(saved)
                        && BagPickupSettings.priority(saved) == 8
                        && count(root, 0) == 7
                        && BagFilter.matches(saved, new ItemStack(Items.STONE)),
                "Settings reach parent without altering contents or rules");
        h.assertTrue(
                !BagPickupSettings.enabled(menu.filterSnapshot())
                        && BagPickupSettings.priority(menu.filterSnapshot()) == 8,
                "Settings synchronize");
        ItemStack restored =
                ItemStack.parse(
                                h.getLevel().registryAccess(),
                                root.save(h.getLevel().registryAccess()))
                        .orElseThrow();
        ItemStack restoredPouch = modules(restored).getItem(0);
        h.assertTrue(
                !BagPickupSettings.enabled(restoredPouch)
                        && BagPickupSettings.priority(restoredPouch) == 8
                        && count(restored, 0) == 7,
                "NBT round-trip preserves settings and contents");
        BagFilter.clear(restoredPouch);
        h.assertTrue(
                !BagPickupSettings.enabled(restoredPouch)
                        && BagPickupSettings.priority(restoredPouch) == 8,
                "Resetting filter rules does not reset pickup preferences");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void settingsRequestsRejectInvalidValuesAndStaleMenus(GameTestHelper h) {
        var player = player(h);
        ItemStack root = install(player, 0, pouch(0));
        var menu = edit(player, root, false);
        for (String value : java.util.List.of("-1", "10", "999999999999999999999", "x", "", "2.0"))
            h.assertFalse(
                    menu.editFilter(
                            player, FilterActions.SET_PICKUP_PRIORITY, -1, "priority", value),
                    "Invalid numeric input rejected");
        h.assertFalse(
                menu.editFilter(player, FilterActions.SET_PICKUP_ENABLED, -1, "pickup", "yes"),
                "Strict boolean");
        h.assertFalse(
                menu.editFilter(player, FilterActions.SET_PICKUP_ENABLED, 0, "pickup", "false"),
                "Unexpected slot rejected");
        h.assertFalse(
                menu.editFilter(player, FilterActions.SET_PICKUP_ENABLED, -1, "mode", "false"),
                "Unexpected field rejected");
        player.containerMenu = player.inventoryMenu;
        h.assertFalse(
                menu.editFilter(player, FilterActions.SET_PICKUP_PRIORITY, -1, "priority", "9"),
                "Stale menu rejected");
        h.assertTrue(
                BagPickupSettings.enabled(modules(root).getItem(0))
                        && BagPickupSettings.priority(modules(root).getItem(0)) == 0,
                "No settings changed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void retiredMasterRequestsAreRejectedAndPouchPriorityStillWorks(
            GameTestHelper h) {
        var player = player(h);
        ItemStack root = install(player, 0, pouch(0));
        BagPickupSettings.setEnabled(root, false);
        var menu = edit(player, root, true);
        h.assertFalse(
                menu.editFilter(player, FilterActions.SET_PICKUP_ENABLED, -1, "pickup", "true"),
                "Retired backpack switch cannot be changed by an old or forged request");
        h.assertFalse(BagPickupSettings.enabled(root), "Legacy preference is left untouched");
        h.assertFalse(
                menu.filterSnapshot().has(DataComponents.CUSTOM_DATA),
                "Backpack synchronization no longer exposes retired preferences");
        h.assertFalse(
                menu.filterSnapshot().has(DataComponents.CONTAINER),
                "Snapshot exposes no real storage");
        h.assertFalse(
                menu.editFilter(player, FilterActions.SET_PICKUP_PRIORITY, -1, "priority", "9"),
                "No backpack priority setting");
        h.assertFalse(
                menu.editFilter(player, FilterActions.SET_MODE, -1, "mode", "exclude"),
                "Backpack cannot edit pouch filters");
        menu = edit(player, root, false);
        for (String value : java.util.List.of("0", "9"))
            h.assertTrue(
                    menu.editFilter(
                            player, FilterActions.SET_PICKUP_PRIORITY, -1, "priority", value),
                    "Pouch priority boundaries accepted");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void openRootNeverCompetesWithAutomaticWriter(GameTestHelper h) {
        var player = player(h);
        ItemStack open = install(player, 0, pouch(9));
        ItemStack other = install(player, 1, pouch(0));
        edit(player, open, false);
        BagRouter.route(player, new ItemStack(Items.STONE, 4));
        h.assertTrue(
                count(open, 0) == 0 && count(other, 0) == 4,
                "An open high-priority parent is skipped; unrelated parent can store safely");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyPouchSwitchControlsPickupRegardlessOfLegacyBackpackFlag(
            GameTestHelper h) {
        for (boolean master : new boolean[] {false, true}) {
            for (boolean local : new boolean[] {false, true}) {
                Player player = player(h);
                ItemStack pouch = pouch(9);
                BagPickupSettings.setEnabled(pouch, local);
                ItemStack root = install(player, 0, pouch);
                BagPickupSettings.setEnabled(root, master);
                ItemStack remainder = BagRouter.route(player, new ItemStack(Items.STONE, 5));
                boolean effective = local;
                h.assertTrue(
                        count(root, 0) == (effective ? 5 : 0)
                                && remainder.getCount() == (effective ? 0 : 5),
                        "Only the pouch switch controls pickup; legacy backpack flag is ignored");
                h.assertTrue(
                        BagPickupSettings.enabled(modules(root).getItem(0)) == local,
                        "Routing never rewrites the pouch preference");
                h.assertTrue(
                        BagPickupSettings.enabled(root) == master,
                        "Retired backpack data is not destructively migrated");
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void modeToggleKeepsRulesQueryAndPage(GameTestHelper h) {
        Player player = player(h);
        ItemStack pouch = pouch(0);
        for (int i = 0; i < 12; i++)
            BagFilter.add(
                    pouch, "items", ResourceLocation.fromNamespaceAndPath("test", "entry_" + i));
        player.getInventory().setItem(0, pouch);
        var menu = new BagMenu(2, player.getInventory(), storage(pouch), pouch, 0, false);
        player.containerMenu = menu;
        var original = BagFilter.entries(pouch, "items");
        var state = new FilterEditorState();
        state.setRegistered(true);
        state.setQuery("test:entry");
        state.setPageSize(2);
        state.refresh(menu.filterSnapshot(), ItemStack.EMPTY);
        state.next();
        state.next();
        for (boolean expected : new boolean[] {true, false}) {
            boolean next = !BagFilter.isExcluding(menu.filterSnapshot());
            h.assertTrue(
                    menu.editFilter(
                            player,
                            FilterActions.SET_MODE,
                            -1,
                            "mode",
                            next ? "exclude" : "include"),
                    "Mode toggle is accepted by existing server handler");
            state.refresh(menu.filterSnapshot(), ItemStack.EMPTY);
            h.assertTrue(
                    state.excluding() == expected
                            && state.page() == 2
                            && state.registered()
                            && state.query().equals("test:entry"),
                    "Synced mode changes without resetting the editor view");
            h.assertTrue(
                    BagFilter.entries(pouch, "items").equals(original),
                    "Switching modes never clears rules");
        }
        h.succeed();
    }
}
