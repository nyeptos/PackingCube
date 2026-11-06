package github.nyeptos.packingcube.gametest.bag;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.BulkStore;
import github.nyeptos.packingcube.registry.ModItems;

import github.nyeptos.packingcube.menu.BagMenu;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** A bulk pouch keeps one kind of item and nothing else, up to the hard limit. */
@GameTestHolder(PackingCube.MODID)
@PrefixGameTestTemplate(false)
public final class BulkPouchGameTests {
    /** A bulk pouch carries no filter; the kind comes from what the player puts in. */
    private static ItemStack pouch() {
        return ModItems.BULK_POUCH.get().getDefaultInstance();
    }

    @GameTest(template = "empty")
    public static void aBulkPouchHasOneSlot(GameTestHelper h) {
        ItemStack stack = pouch();
        BagInventory pouch = new BagInventory(stack, false, () -> {});
        h.assertTrue(pouch.getContainerSize() == 1, "The bulk pouch exposes a single slot");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aBulkPouchHoldsUpToTheLimit(GameTestHelper h) {
        ItemStack stack = pouch();
        BagInventory pouch = new BagInventory(stack, false, () -> {});
        pouch.insert(new ItemStack(Items.COBBLESTONE, 64));
        for (int i = 0; i < 100; i++) pouch.insert(new ItemStack(Items.COBBLESTONE, 64));
        h.assertTrue(pouch.getItem(0).getCount() == BulkStore.LIMIT, "The slot fills to 4096");
        ItemStack remainder = pouch.insert(new ItemStack(Items.COBBLESTONE, 64));
        h.assertTrue(remainder.getCount() == 64, "Nothing fits past the limit");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aBulkPouchKeepsOneKind(GameTestHelper h) {
        ItemStack stack = pouch();
        BagInventory pouch = new BagInventory(stack, false, () -> {});
        pouch.insert(new ItemStack(Items.COBBLESTONE, 10));
        h.assertFalse(
                pouch.canPlaceItem(0, new ItemStack(Items.DIRT)),
                "Another kind is refused while the slot is taken");
        h.assertTrue(
                pouch.canPlaceItem(0, new ItemStack(Items.COBBLESTONE)),
                "More of the same kind is accepted");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theChosenAmountComesOut(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = pouch();
        BagInventory pouch = new BagInventory(stack, false, () -> {});
        pouch.insert(new ItemStack(Items.COBBLESTONE, 64));
        pouch.insert(new ItemStack(Items.COBBLESTONE, 64));
        player.getInventory().setItem(0, stack);
        var menu = new BagMenu(60, player.getInventory(), pouch, stack, 0, false);
        player.containerMenu = menu;
        int taken = menu.takeBulk(player, menu.containerId, 100);
        h.assertTrue(taken == 100, "handed out " + taken);
        h.assertTrue(pouch.getItem(0).getCount() == 28, "left " + pouch.getItem(0).getCount());
        int inInventory = 0;
        for (int i = 1; i < 36; i++)
            if (player.getInventory().getItem(i).is(Items.COBBLESTONE))
                inInventory += player.getInventory().getItem(i).getCount();
        h.assertTrue(inInventory == 100, "inventory holds " + inInventory);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theAmountSurvivesASave(GameTestHelper h) {
        ItemStack stack = pouch();
        BagInventory pouch = new BagInventory(stack, false, () -> {});
        pouch.setItem(0, new ItemStack(Items.COBBLESTONE, 300));
        h.assertTrue(
                pouch.getItem(0).getCount() == 300,
                "direct set kept " + pouch.getItem(0).getCount());
        pouch.setItem(0, ItemStack.EMPTY);
        ItemStack incoming = new ItemStack(Items.COBBLESTONE, 300);
        pouch.insert(incoming);
        h.assertTrue(
                pouch.getItem(0).getCount() == 300,
                "insert kept " + pouch.getItem(0).getCount());
        h.assertTrue(BulkStore.count(stack) == 300, "recorded " + BulkStore.count(stack));
        BagInventory again = new BagInventory(stack.copy(), false, () -> {});
        h.assertTrue(again.getItem(0).is(Items.COBBLESTONE), "The kind came back");
        h.assertTrue(again.getItem(0).getCount() == 300, "The amount came back");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theIntakeSwallowsAStack(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = pouch();
        BagInventory pouch = new BagInventory(stack, false, () -> {});
        pouch.insert(new ItemStack(Items.COBBLESTONE, 64));
        player.getInventory().setItem(0, stack);
        var menu = new BagMenu(61, player.getInventory(), pouch, stack, 0, false);
        player.containerMenu = menu;
        Slot intake = menu.getSlot(menu.bulkInputSlot());
        h.assertTrue(intake.getItem().isEmpty(), "The intake starts empty");
        intake.set(new ItemStack(Items.COBBLESTONE, 64));
        h.assertTrue(intake.getItem().isEmpty(), "The intake is empty again");
        h.assertTrue(
                pouch.getItem(0).getCount() == 128,
                "stored " + pouch.getItem(0).getCount());
        intake.set(new ItemStack(Items.COBBLESTONE, 64));
        h.assertTrue(
                pouch.getItem(0).getCount() == 192,
                "a second stack went in, stored " + pouch.getItem(0).getCount());
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theIntakeHandsBackWhatDoesNotFit(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = pouch();
        BagInventory pouch = new BagInventory(stack, false, () -> {});
        pouch.insert(new ItemStack(Items.COBBLESTONE, 4090));
        player.getInventory().setItem(0, stack);
        var menu = new BagMenu(62, player.getInventory(), pouch, stack, 0, false);
        player.containerMenu = menu;
        menu.getSlot(menu.bulkInputSlot()).set(new ItemStack(Items.COBBLESTONE, 64));
        h.assertTrue(
                pouch.getItem(0).getCount() == BulkStore.LIMIT,
                "filled to " + pouch.getItem(0).getCount());
        int back = 0;
        for (int i = 1; i < 36; i++)
            if (player.getInventory().getItem(i).is(Items.COBBLESTONE))
                back += player.getInventory().getItem(i).getCount();
        h.assertTrue(back == 58, "handed back " + back);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theIntakeRefusesAnotherKind(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = pouch();
        BagInventory pouch = new BagInventory(stack, false, () -> {});
        pouch.insert(new ItemStack(Items.COBBLESTONE, 64));
        player.getInventory().setItem(0, stack);
        var menu = new BagMenu(63, player.getInventory(), pouch, stack, 0, false);
        player.containerMenu = menu;
        Slot intake = menu.getSlot(menu.bulkInputSlot());
        h.assertFalse(intake.mayPlace(new ItemStack(Items.DIRT)), "Another kind is refused");
        intake.set(new ItemStack(Items.DIRT, 64));
        h.assertTrue(
                pouch.getItem(0).getCount() == 64,
                "the store is untouched, held " + pouch.getItem(0).getCount());
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theIntakeTakesPartInHoverFeedback(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = pouch();
        BagInventory pouch = new BagInventory(stack, false, () -> {});
        player.getInventory().setItem(0, stack);
        var menu = new BagMenu(67, player.getInventory(), pouch, stack, 0, false);
        player.containerMenu = menu;
        h.assertTrue(menu.hoverSlotIndices().contains(0), "the output slot reacts to hover");
        h.assertTrue(
                menu.hoverSlotIndices().contains(menu.bulkInputSlot()),
                "the intake reacts to hover as well");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theOutputHandsOutOneStackAtMost(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = pouch();
        BagInventory pouch = new BagInventory(stack, false, () -> {});
        pouch.insert(new ItemStack(Items.COBBLESTONE, 300));
        player.getInventory().setItem(0, stack);
        var menu = new BagMenu(64, player.getInventory(), pouch, stack, 0, false);
        player.containerMenu = menu;
        ItemStack taken = menu.getSlot(0).remove(300);
        h.assertTrue(taken.getCount() == 64, "handed out " + taken.getCount());
        h.assertTrue(
                pouch.getItem(0).getCount() == 236,
                "left " + pouch.getItem(0).getCount());
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aNumberKeyHandsOutOneStack(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = pouch();
        BagInventory pouch = new BagInventory(stack, false, () -> {});
        pouch.insert(new ItemStack(Items.COBBLESTONE, 300));
        player.getInventory().setItem(0, stack);
        var menu = new BagMenu(68, player.getInventory(), pouch, stack, 0, false);
        player.containerMenu = menu;
        player.getInventory().setItem(5, ItemStack.EMPTY);
        menu.clicked(0, 5, ClickType.SWAP, player);
        ItemStack moved = player.getInventory().getItem(5);
        h.assertTrue(moved.getCount() == 64, "handed out " + moved.getCount());
        h.assertTrue(
                pouch.getItem(0).getCount() == 236,
                "left " + pouch.getItem(0).getCount());
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aShiftClickHandsOutOneStack(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = pouch();
        BagInventory pouch = new BagInventory(stack, false, () -> {});
        pouch.insert(new ItemStack(Items.COBBLESTONE, 300));
        player.getInventory().setItem(0, stack);
        var menu = new BagMenu(66, player.getInventory(), pouch, stack, 0, false);
        player.containerMenu = menu;
        h.assertTrue(menu.bulkTakeAmount() == 64, "one stack is " + menu.bulkTakeAmount());
        int taken = menu.takeBulk(player, menu.containerId, menu.bulkTakeAmount());
        h.assertTrue(taken == 64, "handed " + taken);
        h.succeed();
    }
}
