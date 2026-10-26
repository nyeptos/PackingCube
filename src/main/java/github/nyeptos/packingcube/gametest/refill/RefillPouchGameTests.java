package github.nyeptos.packingcube.gametest.refill;

import com.google.gson.JsonParser;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.bag.pickup.BagPickupSettings;
import github.nyeptos.packingcube.bag.pickup.BagRouter;
import github.nyeptos.packingcube.bag.refill.RefillService;
import github.nyeptos.packingcube.bag.refill.RefillSettings;
import github.nyeptos.packingcube.event.RefillHandler;
import github.nyeptos.packingcube.gametest.support.LegacyRefillFixture;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.menu.BagRadialMenu;
import github.nyeptos.packingcube.network.FilterActions;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.ClientSideMerchant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class RefillPouchGameTests {
    private static Player player(GameTestHelper h) {
        return h.makeMockPlayer(GameType.SURVIVAL);
    }

    private static BagInventory modules(ItemStack root) {
        return new BagInventory(root, true, () -> {});
    }

    private static BagInventory contents(ItemStack pouch) {
        return new BagInventory(pouch, false, () -> {});
    }

    private static ItemStack refill(ItemStack... stacks) {
        var pouch = LegacyRefillFixture.pouch();
        // Keep the old scenario's 3-item ceiling explicit under the new user-configurable policy.
        RefillSettings.set(pouch, "timing", "use");
        RefillSettings.set(pouch, "target", "3");
        var inventory = contents(pouch);
        for (int i = 0; i < stacks.length; i++) inventory.setItem(i, stacks[i]);
        return pouch;
    }

    private static ItemStack install(Player p, ItemStack... pouches) {
        var root = ModItems.LARGE_BAG.get().getDefaultInstance();
        var modules = modules(root);
        for (int i = 0; i < pouches.length; i++) modules.setItem(i, pouches[i]);
        p.getInventory().setItem(8, root);
        return root;
    }

    private static int count(ItemStack root, int module, int slot) {
        return contents(modules(root).getItem(module)).getItem(slot).getCount();
    }

    private static MerchantOffer offer(int a, boolean second) {
        return new MerchantOffer(
                new ItemCost(Items.EMERALD, a),
                second ? Optional.of(new ItemCost(Items.PAPER, 2)) : Optional.empty(),
                new ItemStack(Items.BREAD),
                10,
                1,
                0);
    }

    private static MerchantMenu merchant(Player p, MerchantOffer... offers) {
        var trader = new ClientSideMerchant(p);
        var list = new MerchantOffers();
        for (var offer : offers) list.add(offer);
        trader.overrideOffers(list);
        var menu = new MerchantMenu(37, p.getInventory(), trader);
        p.containerMenu = menu;
        return menu;
    }

    @GameTest(template = "empty")
    public static void defaultsModuleAndRadialUseTheNewPouch(GameTestHelper h) {
        var p = player(h);
        var pouch = refill();
        h.assertTrue(
                RefillSettings.enabled(pouch, false) && RefillSettings.enabled(pouch, true),
                "Independent refill switches default ON");
        h.assertTrue(
                BagFilter.matches(pouch, new ItemStack(Items.STONE))
                        && !BagFilter.matches(
                                ModItems.FILTER_BAG.get().getDefaultInstance(),
                                new ItemStack(Items.STONE)),
                "Refill initially accepts items; normal pouch defaults unchanged");
        h.assertTrue(
                BagInventory.accepts(true, pouch)
                        && !BagInventory.accepts(false, pouch)
                        && BagInventory.isBag(pouch),
                "Only proper module nesting accepted");
        var root = install(p, pouch);
        var radial = new BagRadialMenu(5, p.getInventory(), root);
        h.assertTrue(
                radial.icon(0).is(ModItems.REFILL_POUCH.get())
                        && radial.icon(0).get(DataComponents.CONTAINER) == null,
                "Radial icon preserves type but strips contents");
        p.containerMenu = radial;
        h.assertTrue(!radial.select(p, 1), "Empty module cannot open");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void switchesAreIndependentValidatedAndSavedInParent(GameTestHelper h) {
        var p = player(h);
        var root = install(p, refill());
        var parent = modules(root);
        var menu =
                new BagMenu(
                        5,
                        p.getInventory(),
                        new BagInventory(parent.getItem(0), false, parent),
                        root,
                        -1,
                        false);
        p.containerMenu = menu;
        h.assertTrue(
                menu.isRefill() && menu.filterSnapshot().is(ModItems.REFILL_POUCH.get()),
                "Menu and synchronized settings identify refill pouch");
        h.assertTrue(
                menu.editFilter(p, FilterActions.SET_HOTBAR_REFILL, -1, "hotbar", "false"),
                "Can disable hotbar");
        h.assertTrue(
                !RefillSettings.enabled(modules(root).getItem(0), false)
                        && RefillSettings.enabled(modules(root).getItem(0), true),
                "Trade remains ON and parent saved");
        h.assertTrue(
                !menu.editFilter(p, FilterActions.SET_TRADE_REFILL, -1, "hotbar", "false")
                        && !menu.editFilter(p, FilterActions.SET_TRADE_REFILL, -1, "trade", "yes"),
                "Malformed switch requests rejected");
        h.assertTrue(
                menu.editFilter(p, FilterActions.SET_TRADE_REFILL, -1, "trade", "false"),
                "Can disable trade independently");
        h.assertTrue(
                !RefillSettings.enabled(modules(root).getItem(0), true), "Trade setting persists");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void clientMenuTransportPreservesRefillKind(GameTestHelper h) {
        var p = player(h);
        var buffer =
                new RegistryFriendlyByteBuf(
                        io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            buffer.writeVarInt(-1);
            buffer.writeBoolean(false);
            buffer.writeBoolean(true);
            BagMenu.writeCuriosTargets(buffer, java.util.List.of());
            var menu = BagMenu.fromNetwork(5, p.getInventory(), buffer);
            h.assertTrue(
                    menu.isRefill() && !menu.isModules() && buffer.readableBytes() == 0,
                    "Client knows which settings rows to build before slot synchronization");
        } finally {
            buffer.release();
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void actualUseRefillsTheSelectedStackAndExhaustedStacks(GameTestHelper h) {
        var p = player(h);
        var root = install(p, refill(new ItemStack(Items.STONE, 10)));
        p.getInventory().selected = 0;
        p.getInventory().setItem(0, new ItemStack(Items.STONE, 3));
        RefillHandler.onItem(new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND));
        p.getInventory().getItem(0).shrink(1);
        RefillHandler.onTick(new PlayerTickEvent.Post(p));
        h.assertTrue(
                p.getInventory().getItem(0).getCount() == 3 && count(root, 0, 0) == 9,
                "Only used quantity refilled after actual use event");
        p.getInventory().setItem(0, new ItemStack(Items.STONE));
        RefillHandler.onItem(new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND));
        p.getInventory().setItem(0, ItemStack.EMPTY);
        RefillHandler.onTick(new PlayerTickEvent.Post(p));
        h.assertTrue(
                p.getInventory().getItem(0).getCount() == 3 && count(root, 0, 0) == 6,
                "Last-item exhaustion can refill exact same data");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void movingAndDroppingDoNotTriggerRefill(GameTestHelper h) {
        var p = player(h);
        var root = install(p, refill(new ItemStack(Items.STONE, 10)));
        p.getInventory().selected = 0;
        p.getInventory().setItem(0, ItemStack.EMPTY);
        RefillHandler.onTick(new PlayerTickEvent.Post(p));
        h.assertTrue(
                p.getInventory().getItem(0).isEmpty() && count(root, 0, 0) == 10,
                "Manual empty slot without use event stays empty");
        p.getInventory().setItem(0, new ItemStack(Items.STONE));
        RefillHandler.onItem(new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND));
        p.getInventory().setItem(0, ItemStack.EMPTY);
        RefillHandler.onToss(
                new ItemTossEvent(
                        new ItemEntity(h.getLevel(), 0, 0, 0, new ItemStack(Items.STONE)), p));
        RefillHandler.onTick(new PlayerTickEvent.Post(p));
        h.assertTrue(
                p.getInventory().getItem(0).isEmpty() && count(root, 0, 0) == 10,
                "Toss invalidates pending use sample");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void hotbarProtectsDifferentComponentsSlotsMenusAndSwitches(GameTestHelper h) {
        var p = player(h);
        var named = new ItemStack(Items.STONE, 10);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Protected"));
        var pouch = refill(named);
        var root = install(p, pouch);
        p.getInventory().selected = 0;
        var before = new ItemStack(Items.STONE, 2);
        p.getInventory().setItem(0, new ItemStack(Items.STONE));
        h.assertTrue(
                RefillService.hotbar(p, 0, before) == 0 && count(root, 0, 0) == 10,
                "Plain template cannot consume named reserves");
        h.assertTrue(RefillService.hotbar(p, 1, before) == 0, "Unselected slot rejected");
        p.getInventory().setItem(0, new ItemStack(Items.DIRT));
        h.assertTrue(
                RefillService.hotbar(p, 0, before) == 0
                        && p.getInventory().getItem(0).is(Items.DIRT),
                "Replacement item never overwritten");
        var disabled = refill(new ItemStack(Items.STONE, 10));
        RefillSettings.setEnabled(disabled, false, false);
        install(p, disabled);
        p.getInventory().setItem(0, new ItemStack(Items.STONE));
        h.assertTrue(RefillService.hotbar(p, 0, before) == 0, "Hotbar switch enforced");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void refillPrioritiesPassRemaindersAndIgnoreNormalPouches(GameTestHelper h) {
        var p = player(h);
        var low = refill(new ItemStack(Items.STONE, 5));
        var high = refill(new ItemStack(Items.STONE, 1));
        BagPickupSettings.setPriority(high, 9);
        var ordinary = ModItems.FILTER_BAG.get().getDefaultInstance();
        contents(ordinary).setItem(0, new ItemStack(Items.STONE, 20));
        var root = install(p, low, high, ordinary);
        p.getInventory().selected = 0;
        p.getInventory().setItem(0, ItemStack.EMPTY);
        h.assertTrue(
                RefillService.hotbar(p, 0, new ItemStack(Items.STONE, 3)) == 3,
                "Can refill across supplies");
        h.assertTrue(
                count(root, 1, 0) == 0 && count(root, 0, 0) == 3 && count(root, 2, 0) == 20,
                "High first, low remainder, normal reserves untouched");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tradeFillsOnlyCostsWithoutBuyingAnything(GameTestHelper h) {
        var p = player(h);
        var root =
                install(
                        p,
                        refill(new ItemStack(Items.EMERALD, 20), new ItemStack(Items.PAPER, 10)));
        var offer = offer(3, true);
        var menu = merchant(p, offer);
        h.assertTrue(RefillService.trade(p) == 5, "Both exact costs supplied");
        h.assertTrue(
                menu.getSlot(0).getItem().getCount() == 3
                        && menu.getSlot(1).getItem().getCount() == 2
                        && count(root, 0, 0) == 17
                        && count(root, 0, 1) == 8,
                "Only one trade's costs moved");
        h.assertTrue(
                offer.getUses() == 0 && p.getInventory().countItem(Items.BREAD) == 0,
                "Never takes result or increments trade uses");
        h.assertTrue(
                RefillService.trade(p) == 0 && count(root, 0, 0) == 17,
                "Full payment not repeatedly drained");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tradePreflightRequiresBothInputsAndHandlesIdenticalCosts(GameTestHelper h) {
        var p = player(h);
        var root = install(p, refill(new ItemStack(Items.EMERALD, 4)));
        var menu = merchant(p, offer(3, true));
        h.assertTrue(
                RefillService.trade(p) == 0
                        && menu.getSlot(0).getItem().isEmpty()
                        && count(root, 0, 0) == 4,
                "No partial deduction when second material missing");
        var sameCost =
                new MerchantOffer(
                        new ItemCost(Items.EMERALD, 3),
                        Optional.of(new ItemCost(Items.EMERALD, 2)),
                        new ItemStack(Items.BREAD),
                        10,
                        1,
                        0);
        menu = merchant(p, sameCost);
        h.assertTrue(
                RefillService.trade(p) == 0 && count(root, 0, 0) == 4,
                "Same item in both costs cannot reserve the same units twice");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void selectedOfferPriceStockAndTradeSwitchAreRespected(GameTestHelper h) {
        var p = player(h);
        var root = install(p, refill(new ItemStack(Items.EMERALD, 20)));
        var first = offer(2, false);
        var second = offer(5, false);
        second.addToSpecialPriceDiff(-1);
        var menu = merchant(p, first, second);
        menu.setSelectionHint(1);
        h.assertTrue(
                RefillService.trade(p) == 4 && count(root, 0, 0) == 16,
                "Selected offer's modified price, not first/base price");
        menu.getSlot(0).set(ItemStack.EMPTY);
        second.setToOutOfStock();
        h.assertTrue(RefillService.trade(p) == 0, "Out-of-stock trade never supplied");
        var disabled = refill(new ItemStack(Items.EMERALD, 8));
        RefillSettings.setEnabled(disabled, true, false);
        install(p, disabled);
        merchant(p, offer(2, false));
        h.assertTrue(RefillService.trade(p) == 0, "Trade switch enforced independently");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void wrongManualPaymentAndNamedSuppliesAreProtected(GameTestHelper h) {
        var p = player(h);
        var root = install(p, refill(new ItemStack(Items.EMERALD, 10)));
        var menu = merchant(p, offer(3, false));
        menu.getSlot(0).set(new ItemStack(Items.DIRT));
        h.assertTrue(
                RefillService.trade(p) == 0
                        && menu.getSlot(0).getItem().is(Items.DIRT)
                        && count(root, 0, 0) == 10,
                "Existing wrong input never replaced or returned secretly");
        var named = new ItemStack(Items.EMERALD, 10);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Savings"));
        root = install(p, refill(named));
        merchant(p, offer(3, false));
        h.assertTrue(
                RefillService.trade(p) == 0 && count(root, 0, 0) == 10,
                "Ordinary costs cannot consume named supplies");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void refillPouchesDoNotAutoPickupButAllowManualStow(GameTestHelper h) {
        var p = player(h);
        var root = install(p, refill());
        h.assertTrue(
                BagRouter.route(p, new ItemStack(Items.STONE, 4)).getCount() == 4,
                "Supply pouch doesn't silently collect loot");
        var menu = new BagMenu(8, p.getInventory(), modules(root), root, 8, true);
        p.containerMenu = menu;
        p.getInventory().setItem(9, new ItemStack(Items.STONE, 4));
        h.assertTrue(
                menu.stowAll(p, 8) == 4 && count(root, 0, 0) == 4,
                "Explicit manual stow can stock refill pouches without a storage filter");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void originalTextureAndGeneratedModelAreUsed(GameTestHelper h) throws Exception {
        byte[] png;
        try (var input =
                RefillPouchGameTests.class.getResourceAsStream(
                        "/assets/packingcube/textures/item/refill_pouch.png")) {
            png = input.readAllBytes();
        }
        h.assertTrue(
                HexFormat.of()
                        .formatHex(MessageDigest.getInstance("SHA-256").digest(png))
                        .equals("96fa232da8220812d82bde3c69d9d85695cb25a7e23f52b932bf36962576cb3b"),
                "Supplied 16x16 PNG bytes preserved");
        try (var reader =
                new InputStreamReader(
                        RefillPouchGameTests.class.getResourceAsStream(
                                "/assets/packingcube/models/item/refill_pouch.json"))) {
            var model = JsonParser.parseReader(reader).getAsJsonObject();
            h.assertTrue(
                    model.getAsJsonObject("textures")
                            .get("layer0")
                            .getAsString()
                            .equals("packingcube:item/refill_pouch"),
                    "Item model uses artist texture");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedFilterDataDoesNotReappearInSettingsSnapshot(GameTestHelper h) {
        var p = player(h);
        var pouch = refill();
        pouch.remove(DataComponents.CUSTOM_DATA);
        var root = install(p, pouch);
        var parent = modules(root);
        var menu =
                new BagMenu(
                        5,
                        p.getInventory(),
                        new BagInventory(parent.getItem(0), false, parent),
                        root,
                        -1,
                        false);
        h.assertTrue(
                menu.isRefill()
                        && !BagFilter.isExcluding(menu.filterSnapshot())
                        && menu.filterSnapshot().get(DataComponents.CUSTOM_DATA) == null,
                "Snapshot reflects actual removed data instead of silently reinstating default"
                        + " exclude mode");
        h.succeed();
    }
}
