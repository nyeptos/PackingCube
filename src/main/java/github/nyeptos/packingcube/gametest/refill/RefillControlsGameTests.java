package github.nyeptos.packingcube.gametest.refill;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.bag.filter.BagFilterTooltip;
import github.nyeptos.packingcube.bag.pickup.BagPickupSettings;
import github.nyeptos.packingcube.bag.refill.RefillService;
import github.nyeptos.packingcube.bag.refill.RefillSettings;
import github.nyeptos.packingcube.event.RefillHandler;
import github.nyeptos.packingcube.gametest.support.LegacyRefillFixture;
import github.nyeptos.packingcube.gui.layout.BagSettingsLayout;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.menu.BagRadialMenu;
import github.nyeptos.packingcube.network.FilterActions;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.ClientSideMerchant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class RefillControlsGameTests {
    private static Player player(GameTestHelper h) {
        return h.makeMockPlayer(GameType.SURVIVAL);
    }

    private static BagInventory modules(ItemStack root) {
        return new BagInventory(root, true, () -> {});
    }

    private static BagInventory contents(ItemStack pouch) {
        return new BagInventory(pouch, false, () -> {});
    }

    private static ItemStack pouch(ItemStack... items) {
        var pouch = LegacyRefillFixture.pouch();
        var contents = contents(pouch);
        for (int i = 0; i < items.length; i++) contents.setItem(i, items[i]);
        return pouch;
    }

    private static ItemStack install(Player p, ItemStack... pouches) {
        var root = ModItems.LARGE_BAG.get().getDefaultInstance();
        var modules = modules(root);
        for (int i = 0; i < pouches.length; i++) modules.setItem(i, pouches[i]);
        p.getInventory().setItem(8, root);
        p.getInventory().selected = 0;
        return root;
    }

    private static int count(ItemStack root, int module, int slot) {
        return contents(modules(root).getItem(module)).getItem(slot).getCount();
    }

    private static void rule(ItemStack p, String field, String id) {
        BagFilter.add(p, field, ResourceLocation.parse(id));
    }

    private static void use(Player p, InteractionHand hand, int remaining) {
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(p, hand));
        var stack = p.getItemInHand(hand);
        p.setItemInHand(hand, remaining == 0 ? ItemStack.EMPTY : stack.copyWithCount(remaining));
        RefillHandler.onTick(new PlayerTickEvent.Post(p));
    }

    private static MerchantMenu trade(Player p) {
        var trader = new ClientSideMerchant(p);
        var offers = new MerchantOffers();
        offers.add(
                new MerchantOffer(
                        new ItemCost(Items.EMERALD, 3), new ItemStack(Items.BREAD), 10, 1, 0));
        trader.overrideOffers(offers);
        var menu = new MerchantMenu(20, p.getInventory(), trader);
        p.containerMenu = menu;
        return menu;
    }

    @GameTest(template = "empty")
    public static void defaultsAndStrictOptionBounds(GameTestHelper h) {
        var p = pouch();
        h.assertTrue(
                RefillSettings.slots(p) == 511
                        && RefillSettings.offhandEnabled(p)
                        && !RefillSettings.filterEnabled(p)
                        && RefillSettings.timing(p).equals("empty")
                        && RefillSettings.target(p) == 0,
                "Defaults: all hand slots, empty trigger, full stack, filter OFF");
        var original = p.copy();
        for (String[] invalid :
                new String[][] {
                    {"slots", "512"},
                    {"slots", "-1"},
                    {"target", "65"},
                    {"threshold", "64"},
                    {"offhand", "yes"},
                    {"timing", "other"},
                    {"filter", "1"},
                    {"container", "true"},
                    {"target", "999999999999"}
                })
            h.assertTrue(!RefillSettings.set(p, invalid[0], invalid[1]), "Invalid option rejected");
        h.assertTrue(ItemStack.matches(original, p), "Rejected writes do not mutate data");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void filtersDoNotRestrictStorageOrManualStow(GameTestHelper h) {
        var p = player(h);
        var pouch = pouch();
        BagFilter.setExcluding(pouch, false);
        rule(pouch, "items", "minecraft:stone");
        RefillSettings.set(pouch, "filter", "true");
        h.assertTrue(
                contents(pouch).canPlaceItem(0, new ItemStack(Items.DIRT)),
                "Insertion ignores output filter");
        h.assertTrue(
                contents(pouch).insert(new ItemStack(Items.DIRT, 2)).isEmpty(),
                "Manual insertion accepts excluded supply");
        h.assertTrue(
                !contents(pouch).canPlaceItem(0, new ItemStack(Items.SHULKER_BOX)),
                "Container nesting still forbidden");
        var root = install(p, pouch);
        var parent = modules(root);
        var screen =
                new BagMenu(
                        6,
                        p.getInventory(),
                        new BagInventory(parent.getItem(0), false, parent),
                        root,
                        -1,
                        false);
        p.containerMenu = screen;
        h.assertTrue(
                screen.getSlot(0).mayPlace(new ItemStack(Items.DIRT)),
                "Menu slot agrees before any server correction");
        var menu = new BagMenu(7, p.getInventory(), modules(root), root, 8, true);
        p.containerMenu = menu;
        p.getInventory().setItem(9, new ItemStack(Items.DIRT, 4));
        h.assertTrue(
                menu.stowAll(p, 7) == 4 && count(root, 0, 0) == 6,
                "Explicit stow ignores supply-only rule");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void itemFilterIncludeExcludeAndDisabledPreserveRules(GameTestHelper h) {
        var pouch = pouch(new ItemStack(Items.STONE, 8), new ItemStack(Items.DIRT, 8));
        rule(pouch, "items", "minecraft:stone");
        BagFilter.setExcluding(pouch, false);
        RefillSettings.set(pouch, "filter", "true");
        h.assertTrue(
                RefillSettings.allows(pouch, new ItemStack(Items.STONE))
                        && !RefillSettings.allows(pouch, new ItemStack(Items.DIRT)),
                "Include permits only listed supplies");
        BagFilter.setExcluding(pouch, true);
        h.assertTrue(
                !RefillSettings.allows(pouch, new ItemStack(Items.STONE))
                        && RefillSettings.allows(pouch, new ItemStack(Items.DIRT)),
                "Exclude inverts output matching");
        RefillSettings.set(pouch, "filter", "false");
        h.assertTrue(
                RefillSettings.allows(pouch, new ItemStack(Items.STONE))
                        && BagFilter.entries(pouch, "items").size() == 1,
                "Disabling filter retains rules");
        RefillSettings.set(pouch, "filter", "true");
        h.assertTrue(
                !RefillSettings.allows(pouch, new ItemStack(Items.STONE)),
                "Reenabling restores exclusion");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tagAndEmptyOutputRules(GameTestHelper h) {
        var pouch = pouch();
        BagFilter.setExcluding(pouch, false);
        RefillSettings.set(pouch, "filter", "true");
        h.assertTrue(
                !RefillSettings.allows(pouch, new ItemStack(Items.OAK_LOG)),
                "Enabled empty include supplies nothing");
        rule(pouch, "tags", "minecraft:logs");
        h.assertTrue(
                RefillSettings.allows(pouch, new ItemStack(Items.OAK_LOG))
                        && !RefillSettings.allows(pouch, new ItemStack(Items.STONE)),
                "Vanilla tags match supply contents");
        BagFilter.setExcluding(pouch, true);
        h.assertTrue(
                !RefillSettings.allows(pouch, new ItemStack(Items.OAK_LOG)),
                "Tag exclusion also enforced");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void hotbarMaskDoesNotRetargetAnotherSlot(GameTestHelper h) {
        var p = player(h);
        var pouch = pouch(new ItemStack(Items.STONE, 20));
        RefillSettings.set(pouch, "slots", "2");
        var root = install(p, pouch);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        use(p, InteractionHand.MAIN_HAND, 0);
        h.assertTrue(
                p.getMainHandItem().isEmpty() && count(root, 0, 0) == 20,
                "Disabled slot stays empty");
        p.getInventory().selected = 1;
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        use(p, InteractionHand.MAIN_HAND, 0);
        h.assertTrue(
                p.getMainHandItem().getCount() == 20 && p.getInventory().getItem(0).isEmpty(),
                "Only used enabled slot filled, not other enabled empty slots");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void offhandWorksWhileHotbarDisabledAndCanBeDisabled(GameTestHelper h) {
        var p = player(h);
        var pouch = pouch(new ItemStack(Items.TORCH, 10));
        RefillSettings.setEnabled(pouch, false, false);
        RefillSettings.set(pouch, "target", "4");
        var root = install(p, pouch);
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
        use(p, InteractionHand.OFF_HAND, 0);
        h.assertTrue(
                p.getOffhandItem().getCount() == 4 && count(root, 0, 0) == 6,
                "Offhand independent of hotbar switch");
        var parent = modules(root);
        RefillSettings.set(parent.getItem(0), "offhand", "false");
        parent.setChanged();
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
        use(p, InteractionHand.OFF_HAND, 0);
        h.assertTrue(
                p.getOffhandItem().isEmpty() && count(root, 0, 0) == 6, "Offhand switch enforced");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void timingThresholdAndTargetAreIndependent(GameTestHelper h) {
        var p = player(h);
        var pouch = pouch(new ItemStack(Items.STONE, 40));
        RefillSettings.set(pouch, "target", "16");
        var root = install(p, pouch);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 5));
        use(p, InteractionHand.MAIN_HAND, 4);
        h.assertTrue(
                p.getMainHandItem().getCount() == 4 && count(root, 0, 0) == 40,
                "Empty mode waits for exhaustion");
        var parent = modules(root);
        RefillSettings.set(parent.getItem(0), "timing", "threshold");
        RefillSettings.set(parent.getItem(0), "threshold", "2");
        parent.setChanged();
        use(p, InteractionHand.MAIN_HAND, 3);
        h.assertTrue(p.getMainHandItem().getCount() == 3, "Above threshold untouched");
        use(p, InteractionHand.MAIN_HAND, 2);
        h.assertTrue(
                p.getMainHandItem().getCount() == 16 && count(root, 0, 0) == 26,
                "Threshold supplies up to chosen target");
        parent = modules(root);
        RefillSettings.set(parent.getItem(0), "timing", "use");
        parent.setChanged();
        use(p, InteractionHand.MAIN_HAND, 15);
        h.assertTrue(
                p.getMainHandItem().getCount() == 16 && count(root, 0, 0) == 25,
                "Each-use mode fills after actual consumption");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void stackLimitsPartialSupplyAndNoConsumptionAreSafe(GameTestHelper h) {
        var p = player(h);
        var root = install(p, pouch(new ItemStack(Items.ENDER_PEARL, 16)));
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.assertTrue(
                RefillService.hand(p, 0, new ItemStack(Items.ENDER_PEARL)) == 16
                        && p.getMainHandItem().getCount() == 16,
                "Full stack clamps to item maximum 16");
        h.assertTrue(
                RefillService.hand(p, 0, new ItemStack(Items.ENDER_PEARL, 16)) == 0,
                "No actual decrease cannot refill");
        root = install(p, pouch(new ItemStack(Items.TORCH, 3)));
        p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        h.assertTrue(
                RefillService.hand(p, 40, new ItemStack(Items.TORCH)) == 3
                        && count(root, 0, 0) == 0,
                "Shortage supplies only available units");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void filteredHighPriorityFallsBackWithoutExceedingPolicy(GameTestHelper h) {
        var p = player(h);
        var high = pouch(new ItemStack(Items.STONE, 2));
        BagPickupSettings.setPriority(high, 9);
        RefillSettings.set(high, "target", "8");
        var low = pouch(new ItemStack(Items.STONE, 30));
        RefillSettings.set(low, "target", "32");
        var root = install(p, high, low);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.assertTrue(
                RefillService.hand(p, 0, new ItemStack(Items.STONE)) == 8
                        && count(root, 0, 0) == 0
                        && count(root, 1, 0) == 24,
                "Fallback fills first eligible pouch ceiling, never lower priority's larger"
                        + " target");
        high = pouch(new ItemStack(Items.STONE, 8));
        BagPickupSettings.setPriority(high, 9);
        RefillSettings.set(high, "filter", "true");
        rule(high, "items", "minecraft:stone");
        root = install(p, high, low);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.assertTrue(
                RefillService.hand(p, 0, new ItemStack(Items.STONE)) == 30
                        && count(root, 0, 0) == 8,
                "Excluded high-priority supply remains intact");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tradeUsesOutputFilterAndDoesNotPartiallyWithdraw(GameTestHelper h) {
        var p = player(h);
        var pouch = pouch(new ItemStack(Items.EMERALD, 10));
        RefillSettings.set(pouch, "filter", "true");
        rule(pouch, "items", "minecraft:emerald");
        var root = install(p, pouch);
        var menu = trade(p);
        h.assertTrue(
                RefillService.trade(p) == 0
                        && count(root, 0, 0) == 10
                        && menu.getSlot(0).getItem().isEmpty(),
                "Excluded payment not supplied");
        var parent = modules(root);
        RefillSettings.set(parent.getItem(0), "filter", "false");
        parent.setChanged();
        h.assertTrue(
                RefillService.trade(p) == 3
                        && count(root, 0, 0) == 7
                        && menu.getOffers().get(0).getUses() == 0,
                "Disabled filter allows one payment only without executing trade");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void consumedTotemRefillsAndCanBeExcluded(GameTestHelper h) {
        var p = player(h);
        var root = install(p, pouch(new ItemStack(Items.TOTEM_OF_UNDYING)));
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        // The real damage path posts LivingUseTotemEvent before shrink and resurrects before Post.
        p.hurt(p.damageSources().generic(), 1000);
        h.assertTrue(
                p.isAlive() && p.getOffhandItem().isEmpty(),
                "Vanilla consumed totem to prevent death");
        RefillHandler.onTick(new PlayerTickEvent.Post(p));
        h.assertTrue(
                p.getOffhandItem().is(Items.TOTEM_OF_UNDYING) && count(root, 0, 0) == 0,
                "Real registered totem event restores offhand after consumption");
        var excluded = pouch(new ItemStack(Items.TOTEM_OF_UNDYING));
        RefillSettings.set(excluded, "filter", "true");
        rule(excluded, "items", "minecraft:totem_of_undying");
        root = install(p, excluded);
        p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        h.assertTrue(
                RefillService.hand(p, 40, new ItemStack(Items.TOTEM_OF_UNDYING)) == 0
                        && count(root, 0, 0) == 1,
                "Generic exclusion prevents totem refill");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void cancelledTotemAndManualOffhandMovesDoNotRefill(GameTestHelper h) {
        var p = player(h);
        var root = install(p, pouch(new ItemStack(Items.TOTEM_OF_UNDYING)));
        var before = new ItemStack(Items.TOTEM_OF_UNDYING);
        p.setItemInHand(InteractionHand.OFF_HAND, before.copy());
        var event =
                new LivingUseTotemEvent(
                        p, p.damageSources().generic(), before, InteractionHand.OFF_HAND);
        event.setCanceled(true);
        RefillHandler.onTotem(event);
        p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        RefillHandler.onTick(new PlayerTickEvent.Post(p));
        h.assertTrue(
                p.getOffhandItem().isEmpty() && count(root, 0, 0) == 1,
                "Canceled event plus manual empty has no pending consumption");
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
        NeoForge.EVENT_BUS.post(
                new PlayerInteractEvent.RightClickItem(p, InteractionHand.OFF_HAND));
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLASS_BOTTLE));
        RefillHandler.onTick(new PlayerTickEvent.Post(p));
        h.assertTrue(p.getOffhandItem().is(Items.GLASS_BOTTLE), "Replacement never overwritten");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void newSettingsAreValidatedSavedAndPreservedInRadial(GameTestHelper h) {
        var p = player(h);
        var root = install(p, pouch());
        var parent = modules(root);
        var menu =
                new BagMenu(
                        10,
                        p.getInventory(),
                        new BagInventory(parent.getItem(0), false, parent),
                        root,
                        -1,
                        false);
        p.containerMenu = menu;
        h.assertTrue(
                menu.editFilter(p, FilterActions.SET_REFILL_OPTION, -1, "slots", "5")
                        && menu.editFilter(
                                p, FilterActions.SET_REFILL_OPTION, -1, "offhand", "false")
                        && menu.editFilter(p, FilterActions.SET_REFILL_OPTION, -1, "target", "16")
                        && menu.editFilter(
                                p, FilterActions.SET_REFILL_OPTION, -1, "filter", "true"),
                "Valid writes accepted");
        h.assertTrue(
                !menu.editFilter(p, FilterActions.SET_REFILL_OPTION, 0, "slots", "1")
                        && !menu.editFilter(p, FilterActions.SET_REFILL_OPTION, -1, "target", "65"),
                "Untrusted malformed writes rejected");
        var snapshot = menu.filterSnapshot();
        h.assertTrue(
                RefillSettings.slots(snapshot) == 5
                        && !RefillSettings.offhandEnabled(snapshot)
                        && RefillSettings.target(snapshot) == 16,
                "Synchronized snapshot contains item settings");
        var radial = new BagRadialMenu(11, p.getInventory(), root);
        var icon = radial.icon(0);
        h.assertTrue(
                RefillSettings.slots(icon) == 5
                        && !RefillSettings.offhandEnabled(icon)
                        && RefillSettings.filterEnabled(icon)
                        && icon.get(DataComponents.CONTAINER) == null,
                "Radial sanitized copy retains configuration, not contents");
        var normal = ModItems.FILTER_BAG.get().getDefaultInstance();
        var normalMenu = new BagMenu(12, p.getInventory(), contents(normal), normal, -1, false);
        p.containerMenu = normalMenu;
        h.assertTrue(
                !normalMenu.editFilter(p, FilterActions.SET_REFILL_OPTION, -1, "target", "16"),
                "Ordinary pouch cannot accept refill-specific options");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void screenLayoutAndOutputTooltipsRemainBounded(GameTestHelper h) {
        var layout = BagSettingsLayout.floating(124, 80, RefillSettings.SETTINGS_ROWS);
        h.assertTrue(
                layout.optionRow(9).bottom() < layout.modeY()
                        && layout.contentBottom() > layout.viewport().bottom(),
                "Extra controls scroll above unchanged reusable editor in small windows");
        var p = pouch();
        var lines = new ArrayList<Component>();
        BagFilterTooltip.append(p, lines);
        h.assertTrue(
                lines.get(0).getContents()
                                instanceof
                                net.minecraft.network.chat.contents.TranslatableContents text
                        && text.getKey().equals("gui.packingcube.refill_filter_off"),
                "Disabled output filter clearly labeled");
        RefillSettings.set(p, "filter", "true");
        BagFilterTooltip.append(p, new ArrayList<>());
        var regular = ModItems.FILTER_BAG.get().getDefaultInstance();
        h.assertTrue(
                !contents(regular).canPlaceItem(0, new ItemStack(Items.DIRT)),
                "Normal storage pouch still obeys its filter");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void bothHandsInOneTickHaveIndependentSnapshots(GameTestHelper h) {
        var p = player(h);
        var supply = pouch(new ItemStack(Items.TORCH, 10));
        RefillSettings.set(supply, "target", "2");
        var root = install(p, supply);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH));
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
        NeoForge.EVENT_BUS.post(
                new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND));
        NeoForge.EVENT_BUS.post(
                new PlayerInteractEvent.RightClickItem(p, InteractionHand.OFF_HAND));
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        RefillHandler.onTick(new PlayerTickEvent.Post(p));
        h.assertTrue(
                p.getMainHandItem().getCount() == 2
                        && p.getOffhandItem().getCount() == 2
                        && count(root, 0, 0) == 6,
                "Both hands persist their own consumption witnesses and withdrawals");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tradePreflightIncludesBothOutputFilters(GameTestHelper h) {
        var p = player(h);
        var supply = pouch(new ItemStack(Items.EMERALD, 10), new ItemStack(Items.PAPER, 10));
        RefillSettings.set(supply, "filter", "true");
        rule(supply, "items", "minecraft:paper");
        var root = install(p, supply);
        var trader = new ClientSideMerchant(p);
        var offers = new MerchantOffers();
        offers.add(
                new MerchantOffer(
                        new ItemCost(Items.EMERALD, 3),
                        java.util.Optional.of(new ItemCost(Items.PAPER, 2)),
                        new ItemStack(Items.BREAD),
                        10,
                        1,
                        0));
        trader.overrideOffers(offers);
        var menu = new MerchantMenu(22, p.getInventory(), trader);
        p.containerMenu = menu;
        h.assertTrue(
                RefillService.trade(p) == 0
                        && menu.getSlot(0).getItem().isEmpty()
                        && menu.getSlot(1).getItem().isEmpty()
                        && count(root, 0, 0) == 10
                        && count(root, 0, 1) == 10,
                "Filtered second cost prevents any partial withdrawal of first cost");
        var parent = modules(root);
        BagFilter.setExcluding(parent.getItem(0), false);
        rule(parent.getItem(0), "items", "minecraft:emerald");
        parent.setChanged();
        h.assertTrue(
                RefillService.trade(p) == 5 && count(root, 0, 0) == 7 && count(root, 0, 1) == 8,
                "Include mode allows both registered payments with no lost rule");
        h.succeed();
    }
}
