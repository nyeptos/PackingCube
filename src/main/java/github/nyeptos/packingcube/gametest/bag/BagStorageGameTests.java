package github.nyeptos.packingcube.gametest.bag;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.bag.pickup.BagRouter;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static github.nyeptos.packingcube.gametest.support.BagTestSupport.*;

/** Storage rules: filters, capacity, nesting, serialization, and routing. */
@GameTestHolder(PackingCube.MODID)
@PrefixGameTestTemplate(false)
public final class BagStorageGameTests {
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
}
