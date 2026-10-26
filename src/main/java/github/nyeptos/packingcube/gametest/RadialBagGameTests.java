package github.nyeptos.packingcube.gametest;

import com.mojang.authlib.GameProfile;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.bag.BagCursorPosition;
import github.nyeptos.packingcube.bag.BagCursorRelease;
import github.nyeptos.packingcube.bag.BagFilter;
import github.nyeptos.packingcube.bag.BagFilterTooltip;
import github.nyeptos.packingcube.bag.BagIdentity;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.BagKeyActions;
import github.nyeptos.packingcube.bag.BagKeyLatch;
import github.nyeptos.packingcube.bag.BagRouter;
import github.nyeptos.packingcube.bag.EquippedBagAccess;
import github.nyeptos.packingcube.bag.RadialGeometry;
import github.nyeptos.packingcube.bag.RadialKeyState;
import github.nyeptos.packingcube.bag.RadialSelectionFeedback;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.menu.BagRadialMenu;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class RadialBagGameTests {
    private static ItemStack bag() {
        return ModItems.LARGE_BAG.get().getDefaultInstance();
    }

    private static ItemStack module() {
        return ModItems.FILTER_BAG.get().getDefaultInstance();
    }

    private static BagInventory modules(ItemStack root) {
        return new BagInventory(root, true, () -> {});
    }

    private static BagInventory storage(ItemStack stack) {
        return new BagInventory(stack, false, () -> {});
    }

    /** Mock transport only; selector validation, menu creation, and saved data are real. */
    private static Player player(GameTestHelper helper) {
        return new Player(
                helper.getLevel(),
                BlockPos.ZERO,
                0,
                new GameProfile(UUID.randomUUID(), "radial-test")) {
            private int nextMenuId = 30;

            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }

            @Override
            public OptionalInt openMenu(MenuProvider provider) {
                containerMenu = provider.createMenu(++nextMenuId, getInventory(), this);
                return OptionalInt.of(containerMenu.containerId);
            }

            @Override
            public OptionalInt openMenu(
                    MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extra) {
                var buf =
                        new RegistryFriendlyByteBuf(
                                io.netty.buffer.Unpooled.buffer(), level().registryAccess());
                try {
                    extra.accept(buf);
                } finally {
                    buf.release();
                }
                containerMenu = provider.createMenu(++nextMenuId, getInventory(), this);
                return OptionalInt.of(containerMenu.containerId);
            }
        };
    }

    private static BagRadialMenu open(Player player, ItemStack root) {
        player.getInventory().setItem(0, root);
        BagRadialMenu menu = new BagRadialMenu(20, player.getInventory(), root);
        player.containerMenu = menu;
        return menu;
    }

    @GameTest(template = "empty")
    public static void radialGeometryHasStableNineSectorsAndDeadZone(GameTestHelper h) {
        h.assertTrue(
                RadialGeometry.hit(0, 0) == -1 && RadialGeometry.hit(0, 29) == -1,
                "Center cancels");
        for (int i = 0; i < 9; i++)
            h.assertTrue(
                    RadialGeometry.hit(RadialGeometry.x(0, 72, i), RadialGeometry.y(0, 72, i)) == i,
                    "Every displayed slot is its own sector");
        h.assertTrue(RadialGeometry.hit(0, -80) == 0, "Slot one starts at twelve o'clock");
        h.assertTrue(
                RadialGeometry.hit(Double.NaN, 0) == -1,
                "Invalid coordinates cannot select a slot");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lowerDiagonalFramesHaveMoreSpaceAndKeepTheirSectors(GameTestHelper h) {
        for (int radius : new int[] {63, 67, 72}) {
            int rightGap = RadialGeometry.x(0, radius, 3) - RadialGeometry.x(0, radius, 4) - 36;
            int leftGap = RadialGeometry.x(0, radius, 5) - RadialGeometry.x(0, radius, 6) - 36;
            int oldGap =
                    (int) Math.round(Math.cos(RadialGeometry.angle(3)) * radius)
                            - RadialGeometry.x(0, radius, 4)
                            - 36;
            h.assertTrue(
                    rightGap == leftGap && rightGap > oldGap && rightGap >= 3,
                    "Lower pairs have symmetric, wider gaps between their 36px frames");
            for (int i = 0; i < 9; i++) {
                h.assertTrue(
                        RadialGeometry.hit(
                                        RadialGeometry.x(0, radius, i),
                                        RadialGeometry.y(0, radius, i))
                                == i,
                        "Shifted frame center still selects its own numbered slot");
                h.assertTrue(
                        Math.abs(RadialGeometry.x(0, radius, i)) + 18 < 100,
                        "All frames remain inside the smallest standard backdrop");
                if (i != 3 && i != 6)
                    h.assertTrue(
                            RadialGeometry.x(0, radius, i)
                                    == (int) Math.round(Math.cos(RadialGeometry.angle(i)) * radius),
                            "Unrelated slot positions stay unchanged");
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialPreviewsKeepNamesAndRulesButNotContents(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag(), filter = module();
        filter.set(DataComponents.CUSTOM_NAME, Component.literal("Mining"));
        BagFilter.add(filter, "items", ResourceLocation.withDefaultNamespace("diamond"));
        storage(filter).setItem(0, new ItemStack(Items.DIAMOND, 15));
        modules(root).setItem(5, filter);
        BagRadialMenu menu = open(player, root);
        h.assertTrue(
                menu.icon(0).isEmpty() && !menu.icon(5).isEmpty(),
                "Empty positions are not compacted");
        h.assertTrue(
                menu.icon(5).getHoverName().getString().equals("Mining"),
                "Custom name is preserved");
        h.assertTrue(
                menu.icon(5).get(DataComponents.CONTAINER) == null,
                "No nested contents are sent as icons");
        h.assertTrue(
                BagFilter.entries(menu.icon(5), "items")
                        .equals(java.util.List.of("minecraft:diamond")),
                "Only filter settings accompany the preview name");
        h.assertTrue(
                storage(modules(root).getItem(5)).getItem(0).getCount() == 15,
                "Preview does not alter real contents");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialGhostSlotsRefuseEveryContainerAction(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        modules(root).setItem(0, module());
        BagRadialMenu menu = open(player, root);
        for (ClickType type : ClickType.values()) {
            menu.clicked(0, 0, type, player);
            menu.clicked(0, 1, type, player);
            menu.clicked(-999, 0, type, player);
        }
        h.assertTrue(
                menu.getCarried().isEmpty() && !menu.icon(0).isEmpty(),
                "Icons cannot be taken or thrown");
        h.assertTrue(
                menu.quickMoveStack(player, 0).isEmpty(), "Shift transfer cannot retrieve an icon");
        h.assertFalse(menu.getSlot(0).mayPickup(player), "Pickup denied");
        h.assertFalse(menu.getSlot(0).mayPlace(new ItemStack(Items.DIAMOND)), "Insertion denied");
        h.assertTrue(
                modules(root).getItem(0).is(ModItems.FILTER_BAG.get()),
                "Real module stays installed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialSelectionOpensAndSavesInstalledModule(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        modules(root).setItem(6, module());
        BagRadialMenu menu = open(player, root);
        h.assertTrue(menu.select(player, 6), "Valid installed module opens");
        h.assertTrue(
                player.containerMenu instanceof BagMenu bag && !bag.isModules(),
                "Opens storage, not management");
        BagMenu bag = (BagMenu) player.containerMenu;
        h.assertTrue(bag.stillValid(player), "Parent identity follows through menu transition");
        h.assertTrue(
                bag.editFilter(player, 3, -1, "mode", "exclude"), "Installed rules can be edited");
        bag.getSlot(0).setByPlayer(new ItemStack(Items.DIAMOND, 7));
        ItemStack saved = modules(root).getItem(6);
        h.assertTrue(
                BagFilter.isExcluding(saved) && storage(saved).getItem(0).getCount() == 7,
                "Rules and contents persist into original installed module");
        h.assertFalse(menu.select(player, 6), "Old selector cannot act after transition");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialRejectsEmptyAndInvalidSlotsAndForeignMenus(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        BagRadialMenu menu = open(player, root);
        h.assertFalse(
                menu.select(player, -2) || menu.select(player, 9) || menu.select(player, 0),
                "Invalid and empty slots cannot open bags");
        player.containerMenu = player.inventoryMenu;
        h.assertFalse(menu.select(player, -1), "Not the player's active selector");
        player.containerMenu = menu;
        menu.setCarried(new ItemStack(Items.DIAMOND));
        h.assertFalse(menu.select(player, -1), "Carried items cannot be lost by changing menu");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialRejectsChangedOrNewlyInsertedModules(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        modules(root).setItem(0, module());
        BagRadialMenu menu = open(player, root);
        ItemStack changed = module();
        BagFilter.add(changed, "items", ResourceLocation.withDefaultNamespace("stone"));
        modules(root).setItem(0, changed);
        modules(root).setItem(1, module());
        h.assertFalse(
                menu.select(player, 0), "Changed module cannot be mistaken for previewed module");
        h.assertFalse(menu.select(player, 1), "New module must be previewed by reopening first");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialTracksMovedParentAndRejectsDuplicates(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        modules(root).setItem(2, module());
        BagRadialMenu menu = open(player, root);
        ItemStack moved = root.copy();
        player.getInventory().setItem(0, ItemStack.EMPTY);
        player.getInventory().setItem(8, moved);
        h.assertTrue(menu.stillValid(player), "Selector follows copied parent in another slot");
        player.getInventory().setItem(9, moved.copy());
        h.assertFalse(
                menu.stillValid(player) || menu.select(player, 2),
                "Duplicate identity stops selection");
        player.getInventory().setItem(9, ItemStack.EMPTY);
        h.assertTrue(menu.select(player, 2), "Unique moved parent can open its module");
        BagMenu target = (BagMenu) player.containerMenu;
        target.getSlot(0).setByPlayer(new ItemStack(Items.DIAMOND, 4));
        h.assertTrue(
                storage(modules(moved).getItem(2)).getItem(0).getCount() == 4,
                "Save uses moved parent");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialMissingParentCannotOpenManagement(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        BagRadialMenu menu = open(player, root);
        player.getInventory().setItem(0, ItemStack.EMPTY);
        h.assertFalse(
                menu.stillValid(player) || menu.select(player, -1),
                "Dropped parent invalidates selector");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialExcludesParentFromAutomaticStorage(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag(), filter = module();
        BagFilter.add(filter, "items", ResourceLocation.withDefaultNamespace("stone"));
        modules(root).setItem(0, filter);
        open(player, root);
        h.assertTrue(
                BagRouter.route(player, new ItemStack(Items.STONE, 12)).getCount() == 12,
                "Previewed root is protected from another live writer");
        player.containerMenu = player.inventoryMenu;
        h.assertTrue(
                BagRouter.route(player, new ItemStack(Items.STONE, 12)).isEmpty(),
                "Closing selector resumes pickup");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldRightClickDoesNotOpenModule(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        player.getInventory().setItem(0, root);
        BagInventory modules = modules(root);
        modules.setItem(0, module());
        BagMenu menu = new BagMenu(4, player.getInventory(), modules, root, 0, true);
        player.containerMenu = menu;
        menu.clicked(0, 1, ClickType.PICKUP, player);
        h.assertTrue(
                player.containerMenu == menu && menu.isModules(),
                "Right click never navigates to Filter Bag");
        h.assertTrue(
                menu.getCarried().is(ModItems.FILTER_BAG.get()),
                "Right click is normal item handling instead");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void inventoryFallbackAndManagementButtonWorkWithoutEquipment(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        player.getInventory().setItem(4, root);
        h.assertTrue(
                EquippedBagAccess.open(player),
                "Inventory bag supports radial without Curios equipment");
        h.assertTrue(
                player.containerMenu instanceof BagRadialMenu radial && radial.ownsRoot(root),
                "Opens correct inventory root");
        BagRadialMenu menu = (BagRadialMenu) player.containerMenu;
        h.assertTrue(menu.select(player, -1), "Explicit management action remains available");
        h.assertTrue(
                player.containerMenu instanceof BagMenu bag && bag.isModules(),
                "Management keeps installation/removal UI");
        h.succeed();
    }

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

    private static String translationKey(Component component) {
        return component.getContents()
                        instanceof net.minecraft.network.chat.contents.TranslatableContents value
                ? value.getKey()
                : "";
    }

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

    @GameTest(template = "empty")
    public static void radialHoverFeedbackOnlyFiresWhenValidTargetChanges(GameTestHelper h) {
        var feedback = new RadialSelectionFeedback();
        h.assertFalse(feedback.moveTo(-1), "Opening at center is silent");
        h.assertTrue(feedback.moveTo(0), "Entering a bag clicks");
        h.assertFalse(feedback.moveTo(0), "Repeated frames and move events do not repeat sound");
        h.assertTrue(feedback.moveTo(4), "Changing bag clicks");
        h.assertFalse(feedback.moveTo(-1), "Entering center or an empty slot is silent");
        h.assertTrue(feedback.moveTo(4), "Returning to a bag clicks again");
        h.assertTrue(feedback.moveTo(9), "Management button is also a valid target");
        h.assertFalse(feedback.moveTo(9), "Stationary management target stays silent");
        h.assertFalse(feedback.moveTo(99), "Invalid target is silent");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void bagCursorPreservesPositionAcrossScreensAndResize(GameTestHelper h) {
        var cursor = new BagCursorPosition();
        h.assertTrue(cursor.remember(384, 324, 1920, 1080), "Visible pointer captured");
        var same = cursor.resolve(1920, 1080).orElseThrow();
        h.assertTrue(
                Math.abs(same.x() - 384) < 0.0001 && Math.abs(same.y() - 324) < 0.0001,
                "Opening another bag does not center the pointer");
        var smaller = cursor.resolve(960, 540).orElseThrow();
        h.assertTrue(
                Math.abs(smaller.x() - 192) < 0.0001 && Math.abs(smaller.y() - 162) < 0.0001,
                "Window resize keeps relative position, independent of GUI scale");
        cursor.remember(1290, 860, 1920, 1080);
        var next = cursor.resolve(1920, 1080).orElseThrow();
        h.assertTrue(
                Math.abs(next.x() - 1290) < 0.0001 && Math.abs(next.y() - 860) < 0.0001,
                "Latest visible pointer replaces old bag position");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void bagCursorRejectsInvalidCoordinatesAndClampsEdges(GameTestHelper h) {
        var cursor = new BagCursorPosition();
        cursor.remember(40, 50, 800, 600);
        h.assertFalse(
                cursor.remember(Double.NaN, 1, 800, 600)
                        || cursor.remember(1, Double.POSITIVE_INFINITY, 800, 600)
                        || cursor.remember(1, 1, 0, 600),
                "Invalid positions do not replace saved pointer");
        var same = cursor.resolve(800, 600).orElseThrow();
        h.assertTrue(
                Math.abs(same.x() - 40) < 0.0001 && Math.abs(same.y() - 50) < 0.0001,
                "Valid position remains");
        cursor.remember(-30, 999, 800, 600);
        var clamped = cursor.resolve(800, 600).orElseThrow();
        h.assertTrue(
                clamped.x() == 0 && clamped.y() == 599, "Pointer remains within window bounds");
        h.assertTrue(
                cursor.resolve(0, 600).isEmpty(),
                "Minimized/invalid window does not produce a warp");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void bagCursorHasNoForcedCenterOrCrossWorldDefault(GameTestHelper h) {
        var cursor = new BagCursorPosition();
        h.assertTrue(
                cursor.resolve(1920, 1080).isEmpty(),
                "No invented center when nothing has been remembered");
        cursor.remember(300, 200, 800, 600);
        cursor.clear();
        h.assertTrue(
                cursor.resolve(1920, 1080).isEmpty(), "World change discards stale saved pointer");
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

    @GameTest(template = "empty")
    public static void cursorStateIsReadyBeforeNativePointerIsShown(GameTestHelper h) {
        var position = new BagCursorPosition();
        position.remember(320, 210, 1280, 720);
        var calls = new java.util.ArrayList<String>();
        var logical = new BagCursorPosition.Point[1];
        var nativePoint = new BagCursorPosition.Point[1];
        boolean released =
                BagCursorRelease.restore(
                        position,
                        true,
                        1280,
                        720,
                        new BagCursorRelease.Backend() {
                            @Override
                            public void hide() {
                                calls.add("hide");
                                nativePoint[0] =
                                        new BagCursorPosition.Point(
                                                640, 360); // Platform restores disabled-mode center
                                // while hidden.
                            }

                            @Override
                            public void synchronize(BagCursorPosition.Point point) {
                                h.assertTrue(
                                        calls.equals(java.util.List.of("hide")),
                                        "Leaving disabled mode is hidden first");
                                calls.add("synchronize");
                                logical[0] = point;
                            }

                            @Override
                            public void position(BagCursorPosition.Point point) {
                                nativePoint[0] = point;
                                calls.add("position");
                            }

                            @Override
                            public void show() {
                                h.assertTrue(
                                        nativePoint[0].equals(logical[0]),
                                        "Logical hover position matches native position before"
                                                + " showing");
                                h.assertTrue(
                                        nativePoint[0].x() == 320 && nativePoint[0].y() == 210,
                                        "Center is never visible");
                                calls.add("show");
                            }
                        });
        h.assertTrue(
                released
                        && calls.equals(
                                java.util.List.of("hide", "synchronize", "position", "show")),
                "One correctly ordered visible release");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void cursorReleaseLeavesVisibleGuiAndUnknownPositionUntouched(GameTestHelper h) {
        var position = new BagCursorPosition();
        var calls = new java.util.ArrayList<String>();
        var backend =
                new BagCursorRelease.Backend() {
                    @Override
                    public void hide() {
                        calls.add("hide");
                    }

                    @Override
                    public void synchronize(BagCursorPosition.Point point) {
                        calls.add("synchronize");
                    }

                    @Override
                    public void position(BagCursorPosition.Point point) {
                        calls.add("position");
                    }

                    @Override
                    public void show() {
                        calls.add("show");
                    }
                };
        h.assertFalse(
                BagCursorRelease.restore(position, true, 1280, 720, backend),
                "Unknown first position delegates to vanilla");
        position.remember(320, 210, 1280, 720);
        h.assertFalse(
                BagCursorRelease.restore(position, false, 1280, 720, backend),
                "Already-visible GUI pointer is not moved");
        h.assertFalse(
                BagCursorRelease.restore(position, true, 0, 720, backend),
                "Invalid window does not fabricate a position");
        h.assertTrue(calls.isEmpty(), "No backend writes on skipped restoration");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void cursorReleaseUsesSameResizedPointForLogicAndNativeCursor(GameTestHelper h) {
        var position = new BagCursorPosition();
        position.remember(320, 210, 1280, 720);
        var points = new java.util.ArrayList<BagCursorPosition.Point>();
        var backend =
                new BagCursorRelease.Backend() {
                    @Override
                    public void hide() {}

                    @Override
                    public void synchronize(BagCursorPosition.Point point) {
                        points.add(point);
                    }

                    @Override
                    public void position(BagCursorPosition.Point point) {
                        points.add(point);
                    }

                    @Override
                    public void show() {}
                };
        h.assertTrue(
                BagCursorRelease.restore(position, true, 640, 360, backend),
                "Resized release succeeds");
        h.assertTrue(
                points.size() == 2
                        && points.get(0).equals(points.get(1))
                        && points.get(0).x() == 160
                        && points.get(0).y() == 105,
                "Logical and native targets use the same resized point");
        h.succeed();
    }
}
