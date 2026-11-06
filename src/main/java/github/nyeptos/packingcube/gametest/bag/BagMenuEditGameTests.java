package github.nyeptos.packingcube.gametest.bag;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.menu.BagMenu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static github.nyeptos.packingcube.gametest.support.BagTestSupport.*;

/** Filter editing through the menu: samples, rules, modes, and stale menus. */
@GameTestHolder(PackingCube.MODID)
@PrefixGameTestTemplate(false)
public final class BagMenuEditGameTests {
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
}
