package github.nyeptos.packingcube.gametest.bag;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.EquippedBagAccess;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.bag.pickup.BagRouter;
import github.nyeptos.packingcube.event.BagPickupHandler;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Development-only tests; Gradle's runGameTestServer discovers these in the main source set. */
@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class BagGameTests {
    private static ItemStack filter() {
        return ModItems.FILTER_BAG.get().getDefaultInstance();
    }

    private static ItemStack carrier() {
        return ModItems.LARGE_BAG.get().getDefaultInstance();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private static BagInventory storage(ItemStack bag) {
        return new BagInventory(bag, false, () -> {});
    }

    private static BagInventory modules(ItemStack bag) {
        return new BagInventory(bag, true, () -> {});
    }

    private static Player player(GameTestHelper helper) {
        return helper.makeMockPlayer(GameType.SURVIVAL);
    }

    private static int total(BagInventory inv) {
        int result = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) result += inv.getItem(i).getCount();
        return result;
    }

    @GameTest(template = "empty")
    public static void emptyFilterRejects(GameTestHelper helper) {
        helper.assertFalse(
                BagFilter.matches(filter(), new ItemStack(Items.STONE)),
                "An empty filter must not match");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tagsAndExclusions(GameTestHelper helper) {
        ItemStack bag = filter();
        BagFilter.add(bag, "tags", id("logs"));
        helper.assertTrue(
                BagFilter.matches(bag, new ItemStack(Items.OAK_LOG)), "Log tag must match");
        BagFilter.add(bag, "exclude_items", id("oak_log"));
        helper.assertTrue(
                BagFilter.matches(bag, new ItemStack(Items.OAK_LOG)),
                "Old exclusion entries join the shared list");
        BagFilter.setExcluding(bag, true);
        helper.assertFalse(
                BagFilter.matches(bag, new ItemStack(Items.OAK_LOG)),
                "Exclude mode must reject listed items");
        helper.assertFalse(
                BagFilter.matches(bag, new ItemStack(Items.BIRCH_LOG)),
                "The same log tag is excluded after switching");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void partialInsertPreservesCounts(GameTestHelper helper) {
        ItemStack bag = filter();
        BagFilter.add(bag, "items", id("stone"));
        BagInventory inv = storage(bag);
        for (int i = 0; i < 8; i++) inv.setItem(i, new ItemStack(Items.STONE, 64));
        inv.setItem(8, new ItemStack(Items.STONE, 60));
        ItemStack source = new ItemStack(Items.STONE, 16);
        ItemStack remainder = inv.insert(source);
        helper.assertTrue(source.getCount() == 16, "Caller stack must not change");
        helper.assertTrue(remainder.getCount() == 12, "Only four items should fit");
        helper.assertTrue(total(storage(bag)) == 9 * 64, "Saved contents must be full");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sparseSlotsSurviveReload(GameTestHelper helper) {
        ItemStack bag = filter();
        storage(bag).setItem(8, new ItemStack(Items.DIAMOND, 3));
        BagInventory loaded = storage(bag.copy());
        helper.assertTrue(loaded.getItem(0).isEmpty(), "Empty slots must retain their positions");
        helper.assertTrue(loaded.getItem(8).getCount() == 3, "Last slot must survive copying");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nestedContentsSurviveSerialization(GameTestHelper helper) {
        ItemStack root = carrier();
        BagInventory modules = modules(root);
        ItemStack module = filter();
        BagFilter.add(module, "items", id("diamond"));
        modules.setItem(4, module);
        BagInventory inner = new BagInventory(module, false, modules);
        inner.setItem(8, new ItemStack(Items.DIAMOND, 7));
        ItemStack restored =
                ItemStack.parse(
                                helper.getLevel().registryAccess(),
                                root.save(helper.getLevel().registryAccess()))
                        .orElseThrow();
        ItemStack restoredModule = modules(restored).getItem(4);
        helper.assertTrue(
                storage(restoredModule).getItem(8).getCount() == 7,
                "Nested contents must serialize");
        helper.assertTrue(
                BagFilter.matches(restoredModule, new ItemStack(Items.DIAMOND)),
                "Filter must serialize");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void filterClearKeepsContents(GameTestHelper helper) {
        ItemStack bag = filter();
        BagFilter.add(bag, "items", id("stone"));
        storage(bag).setItem(0, new ItemStack(Items.STONE, 9));
        BagFilter.clear(bag);
        helper.assertFalse(
                BagFilter.matches(bag, new ItemStack(Items.STONE)), "Cleared filter must reject");
        helper.assertTrue(total(storage(bag)) == 9, "Clearing must not delete items");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bagNestingIsRejected(GameTestHelper helper) {
        helper.assertFalse(
                storage(filter()).canPlaceItem(0, carrier()),
                "Large bag must not fit inside filter bag");
        helper.assertFalse(
                storage(filter()).canPlaceItem(0, filter()),
                "Filter bag must not fit inside itself");
        helper.assertFalse(
                modules(carrier()).canPlaceItem(0, carrier()),
                "Large bag must not fit inside large bag");
        helper.assertTrue(
                modules(carrier()).canPlaceItem(0, filter()), "Module installation must work");
        helper.assertFalse(
                storage(filter()).canPlaceItem(0, new ItemStack(Items.SHULKER_BOX)),
                "No shulker nesting");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void routingFollowsModuleOrder(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack root = carrier();
        player.getInventory().setItem(0, root);
        BagInventory modules = modules(root);
        for (int i = 0; i < 2; i++) {
            ItemStack module = filter();
            BagFilter.add(module, "items", id("stone"));
            modules.setItem(i, module);
        }
        BagInventory first = new BagInventory(modules.getItem(0), false, modules);
        for (int i = 0; i < 9; i++) first.setItem(i, new ItemStack(Items.STONE, i == 8 ? 60 : 64));
        ItemStack remainder = BagRouter.route(player, new ItemStack(Items.STONE, 16));
        BagInventory reloaded = modules(root);
        helper.assertTrue(remainder.isEmpty(), "Both bags should accept the stack");
        helper.assertTrue(
                total(storage(reloaded.getItem(0))) == 9 * 64, "First module fills first");
        helper.assertTrue(
                total(storage(reloaded.getItem(1))) == 12, "Second module gets remainder");
        helper.succeed();
    }

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
                saved.getItem(0).is(ModItems.FILTER_BAG.get())
                        && saved.getItem(1).is(ModItems.FILTER_BAG.get()),
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
    public static void pickupDelayIsRespected(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack root = carrier();
        player.getInventory().setItem(0, root);
        ItemStack module = filter();
        BagFilter.add(module, "items", id("stone"));
        modules(root).setItem(0, module);
        ItemEntity entity =
                new ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(Items.STONE, 5));
        entity.setDefaultPickUpDelay();
        BagPickupHandler.onPickup(new ItemEntityPickupEvent.Pre(player, entity));
        helper.assertTrue(entity.getItem().getCount() == 5, "Delayed items must remain untouched");
        helper.assertTrue(total(storage(modules(root).getItem(0))) == 0, "No premature pickup");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pickupOwnerIsRespected(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack root = carrier();
        player.getInventory().setItem(0, root);
        ItemStack module = filter();
        BagFilter.add(module, "items", id("stone"));
        modules(root).setItem(0, module);
        ItemEntity entity =
                new ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(Items.STONE, 5));
        entity.setNoPickUpDelay();
        entity.setTarget(java.util.UUID.randomUUID());
        BagPickupHandler.onPickup(new ItemEntityPickupEvent.Pre(player, entity));
        helper.assertTrue(
                entity.getItem().getCount() == 5, "Another owner's item must not be taken");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void completePickupSavesAndRemovesEntity(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack root = carrier();
        player.getInventory().setItem(0, root);
        ItemStack module = filter();
        BagFilter.add(module, "items", id("stone"));
        modules(root).setItem(0, module);
        ItemEntity entity =
                new ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(Items.STONE, 5));
        entity.setNoPickUpDelay();
        entity.playerTouch(player);
        helper.assertTrue(entity.isRemoved(), "Fully stored item entity must be discarded");
        helper.assertTrue(
                total(storage(modules(root).getItem(0))) == 5,
                "Pickup must save exactly five items");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void partialPickupLeavesRemainderOnGround(GameTestHelper helper) {
        Player player = player(helper);
        for (int i = 1; i < 36; i++)
            player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        ItemStack root = carrier();
        player.getInventory().setItem(0, root);
        ItemStack module = filter();
        BagFilter.add(module, "items", id("stone"));
        BagInventory modules = modules(root);
        modules.setItem(0, module);
        BagInventory inner = new BagInventory(module, false, modules);
        for (int i = 0; i < 9; i++) inner.setItem(i, new ItemStack(Items.STONE, i == 8 ? 60 : 64));
        ItemEntity entity =
                new ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(Items.STONE, 16));
        entity.setNoPickUpDelay();
        entity.playerTouch(player);
        helper.assertFalse(entity.isRemoved(), "Overflow must remain on the ground");
        helper.assertTrue(entity.getItem().getCount() == 12, "Ground remainder must be twelve");
        helper.assertTrue(
                total(storage(modules(root).getItem(0))) == 9 * 64,
                "Only four should enter the bag");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void clientSlotsRejectInvalidBags(GameTestHelper helper) {
        Player player = player(helper);
        BagMenu clientLarge = BagMenu.clientMenu(1, player.getInventory(), 0, true);
        BagMenu clientFilter = BagMenu.clientMenu(2, player.getInventory(), 0, false);
        helper.assertTrue(
                clientLarge.visibleSlotCount() == 45 && clientFilter.visibleSlotCount() == 45,
                "Both client menus must have nine bag slots and 36 inventory slots");
        helper.assertFalse(
                clientLarge.getSlot(0).mayPlace(carrier()),
                "Client must reject a large bag immediately");
        helper.assertFalse(
                clientLarge.getSlot(0).mayPlace(new ItemStack(Items.STONE)),
                "Carrier only accepts modules");
        helper.assertTrue(
                clientLarge.getSlot(0).mayPlace(filter()), "Carrier accepts filter modules");
        helper.assertFalse(
                clientFilter.getSlot(0).mayPlace(carrier()), "No large bag in client filter menu");
        helper.assertFalse(
                clientFilter.getSlot(0).mayPlace(filter()), "No filter bag nesting on client");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyOverflowIsRetainedAndRecovered(GameTestHelper helper) {
        ItemStack bag = filter();
        java.util.List<ItemStack> oldSlots = new java.util.ArrayList<>();
        for (int i = 0; i < 27; i++) oldSlots.add(ItemStack.EMPTY);
        oldSlots.set(26, new ItemStack(Items.DIAMOND, 7));
        bag.set(
                DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.fromItems(oldSlots));
        BagInventory shrunk = storage(bag);
        helper.assertTrue(shrunk.getContainerSize() == 9, "New capacity is nine");
        shrunk.setItem(0, new ItemStack(Items.STONE));
        helper.assertTrue(
                bag.get(DataComponents.CONTAINER).stream()
                                .filter(s -> s.is(Items.DIAMOND))
                                .mapToInt(ItemStack::getCount)
                                .sum()
                        == 7,
                "Saving must not truncate old contents");
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        for (int i = 0; i < 36; i++)
            player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        shrunk.recoverLegacyOverflow(player);
        helper.assertTrue(
                bag.get(DataComponents.CONTAINER).stream()
                                .filter(s -> s.is(Items.DIAMOND))
                                .mapToInt(ItemStack::getCount)
                                .sum()
                        == 7,
                "Full inventory must retain overflow");
        player.getInventory().setItem(0, ItemStack.EMPTY);
        storage(bag).recoverLegacyOverflow(player);
        helper.assertTrue(
                player.getInventory().getItem(0).is(Items.DIAMOND)
                        && player.getInventory().getItem(0).getCount() == 7,
                "Old contents must be recoverable");
        helper.assertTrue(total(storage(bag)) == 1, "Visible contents remain intact");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void guiSampleDoesNotConsumeItems(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack bag = filter();
        player.getInventory().setItem(0, bag);
        player.getInventory().setItem(1, new ItemStack(Items.OAK_LOG, 32));
        BagMenu menu = new BagMenu(7, player.getInventory(), storage(bag), bag, 0, false);
        player.containerMenu = menu;
        helper.assertTrue(
                menu.editFilter(player, 0, 37, "", ""),
                "Selecting inventory slot one must succeed");
        helper.assertTrue(
                menu.sample().is(Items.OAK_LOG) && menu.sample().getCount() == 1,
                "Sample is one reference copy");
        helper.assertTrue(
                player.getInventory().getItem(1).getCount() == 32,
                "Sample must not consume inventory items");
        helper.assertTrue(menu.getCarried().isEmpty(), "Sample must not occupy the cursor");
        helper.assertTrue(
                menu.editFilter(player, 1, -1, "tags", "minecraft:logs"),
                "Sample tag can be registered");
        helper.assertTrue(
                BagFilter.matches(bag, new ItemStack(Items.BIRCH_LOG)), "Registered tag must work");
        helper.assertTrue(
                player.getInventory().getItem(1).getCount() == 32,
                "Registering must not consume items");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void guiRejectsForgedRules(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack bag = filter();
        player.getInventory().setItem(0, bag);
        player.getInventory().setItem(1, new ItemStack(Items.OAK_LOG));
        BagMenu menu = new BagMenu(7, player.getInventory(), storage(bag), bag, 0, false);
        player.containerMenu = menu;
        helper.assertFalse(
                menu.editFilter(player, 1, -1, "items", "minecraft:diamond"),
                "No additions without sample");
        menu.editFilter(player, 0, 37, "", "");
        helper.assertFalse(
                menu.editFilter(player, 1, -1, "items", "minecraft:diamond"),
                "Cannot register a different item");
        helper.assertFalse(
                menu.editFilter(player, 1, -1, "tags", "minecraft:planks"),
                "Cannot register a tag absent from sample");
        helper.assertFalse(
                menu.editFilter(player, 1, -1, "unknown", "minecraft:oak_log"),
                "Invalid fields must be rejected");
        helper.assertFalse(
                menu.editFilter(player, 1, -1, "items", "BAD ID"),
                "Malformed IDs must be rejected");
        helper.assertFalse(
                menu.editFilter(player, 0, 45, "", ""), "Metadata is never a sample source");
        helper.assertFalse(
                menu.editFilter(player, 0, 36, "", ""), "The bag itself cannot be a sample");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void guiAddExcludeAndRemove(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack bag = filter();
        player.getInventory().setItem(0, bag);
        player.getInventory().setItem(1, new ItemStack(Items.OAK_LOG));
        BagInventory contents = storage(bag);
        contents.setItem(0, new ItemStack(Items.DIAMOND, 3));
        BagMenu menu = new BagMenu(7, player.getInventory(), contents, bag, 0, false);
        player.containerMenu = menu;
        menu.editFilter(player, 0, 37, "", "");
        menu.editFilter(player, 1, -1, "tags", "minecraft:logs");
        menu.editFilter(player, 1, -1, "exclude_items", "minecraft:oak_log");
        helper.assertTrue(
                menu.editFilter(player, 3, -1, "mode", "exclude"), "Switching GUI mode succeeds");
        helper.assertFalse(
                BagFilter.matches(bag, new ItemStack(Items.OAK_LOG)),
                "Active exclude mode rejects listed items");
        helper.assertTrue(
                menu.editFilter(player, 2, -1, "exclude_items", "minecraft:oak_log"),
                "Rule removal succeeds");
        helper.assertFalse(
                BagFilter.matches(bag, new ItemStack(Items.OAK_LOG)),
                "Remaining log tag must still exclude oak");
        menu.editFilter(player, 2, -1, "tags", "minecraft:logs");
        helper.assertTrue(
                BagFilter.matches(bag, new ItemStack(Items.OAK_LOG)),
                "After removing all shared rules, exclude accepts oak");
        helper.assertTrue(
                BagFilter.entries(menu.filterSnapshot(), "exclude_items").isEmpty(),
                "Sync snapshot must update");
        helper.assertTrue(total(storage(bag)) == 3, "Editing must not modify contents");
        menu.editFilter(player, 0, 38, "", "");
        helper.assertTrue(menu.sample().isEmpty(), "Selecting an empty slot clears the sample");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mountedModuleCanBeEdited(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack root = carrier();
        ItemStack module = filter();
        player.getInventory().setItem(0, root);
        player.getInventory().setItem(1, new ItemStack(Items.DIAMOND, 4));
        BagInventory modules = modules(root);
        modules.setItem(2, module);
        BagInventory contents = new BagInventory(module, false, modules);
        BagMenu menu = new BagMenu(7, player.getInventory(), contents, root, 0, false);
        player.containerMenu = menu;
        menu.editFilter(player, 0, 37, "", "");
        helper.assertTrue(
                menu.editFilter(player, 1, -1, "items", "minecraft:diamond"),
                "Mounted filter can be edited");
        helper.assertTrue(
                BagFilter.matches(modules(root).getItem(2), new ItemStack(Items.DIAMOND)),
                "Parent must save edited module");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void editorMetadataCannotBeTaken(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack bag = filter();
        player.getInventory().setItem(0, bag);
        player.getInventory().setItem(1, new ItemStack(Items.DIAMOND, 4));
        BagMenu menu = new BagMenu(7, player.getInventory(), storage(bag), bag, 0, false);
        player.containerMenu = menu;
        menu.editFilter(player, 0, 37, "", "");
        for (int i = 45; i < 47; i++) {
            helper.assertFalse(menu.getSlot(i).mayPickup(player), "Metadata must not be pickable");
            helper.assertFalse(
                    menu.getSlot(i).isActive(), "Metadata must not render as a real slot");
            menu.clicked(i, 0, ClickType.PICKUP, player);
            menu.clicked(i, 0, ClickType.SWAP, player);
            menu.clicked(i, 0, ClickType.CLONE, player);
            helper.assertTrue(
                    menu.quickMoveStack(player, i).isEmpty(), "Metadata cannot be shift-moved");
        }
        helper.assertTrue(menu.getCarried().isEmpty(), "No ghost reference may reach the cursor");
        helper.assertTrue(
                player.getInventory().getItem(1).getCount() == 4, "No item creation or loss");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void staleAndWrongMenusCannotEdit(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack bag = filter();
        player.getInventory().setItem(0, bag);
        BagMenu menu = new BagMenu(7, player.getInventory(), storage(bag), bag, 0, false);
        helper.assertFalse(menu.editFilter(player, 0, 0, "", ""), "Inactive menus cannot edit");
        player.containerMenu = menu;
        player.getInventory().setItem(0, ItemStack.EMPTY);
        helper.assertFalse(menu.editFilter(player, 0, 0, "", ""), "Detached bags cannot edit");
        ItemStack root = carrier();
        player.getInventory().setItem(0, root);
        BagMenu carrierMenu = new BagMenu(8, player.getInventory(), modules(root), root, 0, true);
        player.containerMenu = carrierMenu;
        helper.assertFalse(
                carrierMenu.editFilter(player, 0, 0, "", ""), "Carrier has no filter editor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void modesAreMutuallyExclusive(GameTestHelper helper) {
        ItemStack bag = filter();
        BagFilter.add(bag, "tags", id("logs"));
        BagFilter.add(bag, "exclude_tags", id("logs"));
        helper.assertTrue(
                BagFilter.matches(bag, new ItemStack(Items.OAK_LOG)),
                "Include mode uses shared rules");
        helper.assertFalse(
                BagFilter.matches(bag, new ItemStack(Items.STONE)),
                "Include mode rejects non-matches");
        BagFilter.setExcluding(bag, true);
        helper.assertFalse(
                BagFilter.matches(bag, new ItemStack(Items.OAK_LOG)),
                "Exclude mode rejects matches");
        helper.assertTrue(
                BagFilter.matches(bag, new ItemStack(Items.STONE)),
                "Exclude mode accepts non-matches");
        helper.assertFalse(
                storage(bag).canPlaceItem(0, new ItemStack(Items.OAK_LOG)),
                "Manual rejection follows exclude mode");
        helper.assertTrue(
                storage(bag).canPlaceItem(0, new ItemStack(Items.STONE)),
                "Manual acceptance follows exclude mode");
        BagFilter.setExcluding(bag, false);
        helper.assertTrue(
                BagFilter.entries(bag, "exclude_tags").size() == 1,
                "Switching preserves the shared list");
        helper.assertTrue(
                storage(bag).canPlaceItem(0, new ItemStack(Items.OAK_LOG)),
                "Manual acceptance follows include mode");
        helper.assertFalse(
                storage(bag).canPlaceItem(0, new ItemStack(Items.STONE)),
                "Manual rejection follows include mode");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyListsFollowTheirMode(GameTestHelper helper) {
        ItemStack bag = filter();
        helper.assertFalse(
                BagFilter.matches(bag, new ItemStack(Items.STONE)),
                "Empty include accepts nothing");
        BagFilter.setExcluding(bag, true);
        helper.assertTrue(
                BagFilter.matches(bag, new ItemStack(Items.STONE)),
                "Empty exclude accepts all eligible items");
        helper.assertFalse(
                storage(bag).canPlaceItem(0, carrier()),
                "Exclude mode cannot bypass nesting restrictions");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void manualClicksAndShiftRespectFilter(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack bag = filter();
        BagFilter.add(bag, "tags", id("logs"));
        player.getInventory().setItem(0, bag);
        player.getInventory().setItem(1, new ItemStack(Items.STONE, 10));
        BagInventory contents = storage(bag);
        contents.setItem(
                0,
                new ItemStack(Items.STONE, 4)); // Contents retained from a previous configuration.
        BagMenu menu = new BagMenu(7, player.getInventory(), contents, bag, 0, false);
        player.containerMenu = menu;
        helper.assertFalse(
                menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)),
                "Server slot rejects non-matches");
        helper.assertTrue(
                menu.quickMoveStack(player, 37).isEmpty(),
                "Shift cannot merge into disallowed existing stacks");
        helper.assertTrue(
                contents.getItem(0).getCount() == 4,
                "Existing disallowed contents must not be increased");
        menu.setCarried(new ItemStack(Items.STONE, 2));
        menu.clicked(1, 0, ClickType.PICKUP, player);
        helper.assertTrue(
                contents.getItem(1).isEmpty(), "Normal click cannot place non-matching items");
        helper.assertTrue(menu.getCarried().getCount() == 2, "Rejected items stay on cursor");
        menu.setCarried(ItemStack.EMPTY);
        helper.assertFalse(
                menu.quickMoveStack(player, 0).isEmpty(),
                "Existing disallowed contents can still be taken out");
        helper.assertTrue(contents.getItem(0).isEmpty(), "Old contents are not trapped");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void clientPredictionUsesSyncedMode(GameTestHelper helper) {
        Player player = player(helper);
        BagMenu menu = BagMenu.clientMenu(7, player.getInventory(), 0, false);
        ItemStack snapshot = filter();
        BagFilter.add(snapshot, "tags", id("logs"));
        menu.getSlot(45).set(snapshot);
        helper.assertTrue(
                menu.getSlot(0).mayPlace(new ItemStack(Items.OAK_LOG)),
                "Client include must accept matches");
        helper.assertFalse(
                menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)),
                "Client include must reject non-matches");
        BagFilter.add(snapshot, "exclude_tags", id("logs"));
        BagFilter.setExcluding(snapshot, true);
        menu.getSlot(45).set(snapshot);
        helper.assertFalse(
                menu.getSlot(0).mayPlace(new ItemStack(Items.OAK_LOG)),
                "Client exclude rejects matches");
        helper.assertTrue(
                menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)),
                "Client exclude accepts others");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void switchingModesKeepsSharedRules(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack bag = filter();
        player.getInventory().setItem(0, bag);
        player.getInventory().setItem(1, new ItemStack(Items.OAK_LOG, 16));
        BagMenu menu = new BagMenu(7, player.getInventory(), storage(bag), bag, 0, false);
        player.containerMenu = menu;
        menu.editFilter(player, 0, 37, "", "");
        menu.editFilter(player, 1, -1, "items", "minecraft:oak_log");
        menu.editFilter(player, 1, -1, "tags", "minecraft:logs");
        var tags = BagFilter.entries(menu.filterSnapshot(), "tags");
        var items = BagFilter.entries(menu.filterSnapshot(), "items");
        helper.assertTrue(menu.editFilter(player, 3, -1, "mode", "exclude"), "Switch to exclude");
        helper.assertTrue(
                tags.equals(BagFilter.entries(menu.filterSnapshot(), "tags")),
                "Switch must retain displayed tags");
        helper.assertTrue(
                items.equals(BagFilter.entries(menu.filterSnapshot(), "items")),
                "Switch must retain displayed items");
        helper.assertFalse(
                BagFilter.matches(bag, new ItemStack(Items.OAK_LOG)),
                "Same shared rule now rejects oak");
        menu.editFilter(player, 3, -1, "mode", "include");
        helper.assertTrue(
                tags.equals(BagFilter.entries(menu.filterSnapshot(), "tags")),
                "Switching back also retains tags");
        helper.assertTrue(
                items.equals(BagFilter.entries(menu.filterSnapshot(), "items")),
                "Switching back also retains items");
        helper.assertTrue(
                BagFilter.matches(bag, new ItemStack(Items.OAK_LOG)),
                "Same shared rule accepts oak again");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyListsMergeWithoutLoss(GameTestHelper helper) {
        ItemStack bag = filter();
        var filter = new net.minecraft.nbt.CompoundTag();
        var include = new net.minecraft.nbt.ListTag();
        include.add(net.minecraft.nbt.StringTag.valueOf("minecraft:logs"));
        var exclude = new net.minecraft.nbt.ListTag();
        exclude.add(net.minecraft.nbt.StringTag.valueOf("minecraft:logs"));
        exclude.add(net.minecraft.nbt.StringTag.valueOf("minecraft:planks"));
        filter.put("tags", include);
        filter.put("exclude_tags", exclude);
        var data = new net.minecraft.nbt.CompoundTag();
        data.put("packingcube_filter", filter);
        bag.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
        helper.assertTrue(
                BagFilter.entries(bag, "tags").size() == 2,
                "Old lists must merge with duplicates removed");
        BagFilter.setExcluding(bag, true);
        helper.assertTrue(
                BagFilter.entries(bag, "tags").size() == 2,
                "Persisting the mode must retain merged rules");
        helper.assertFalse(
                BagFilter.matches(bag, new ItemStack(Items.OAK_PLANKS)),
                "Old exclude-only entries still participate");
        BagFilter.remove(bag, "tags", id("planks"));
        helper.assertTrue(
                BagFilter.entries(bag, "tags").size() == 1, "Removing a merged entry is permanent");
        helper.assertTrue(
                BagFilter.matches(bag, new ItemStack(Items.OAK_PLANKS)),
                "Removed entries do not reappear from legacy data");
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
                player.getInventory().getItem(1).is(ModItems.FILTER_BAG.get()),
                "Number-key swap moves bag");
        helper.assertTrue(menu.stillValid(player), "Number-key movement keeps menu valid");
        menu.clicked(37, 40, ClickType.SWAP, player);
        helper.assertTrue(
                player.getInventory().getItem(40).is(ModItems.FILTER_BAG.get()),
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
                modules(moved).getItem(0).is(ModItems.FILTER_BAG.get()),
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
