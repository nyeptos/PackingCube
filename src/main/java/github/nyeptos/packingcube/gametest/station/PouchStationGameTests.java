package github.nyeptos.packingcube.gametest.station;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.craft.PouchRecipe;
import github.nyeptos.packingcube.craft.PouchRecipes;
import github.nyeptos.packingcube.menu.PouchStationMenu;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The station charges the player's own inventory and leaves the pouch in the result slot. */
@GameTestHolder(PackingCube.MODID)
@PrefixGameTestTemplate(false)
public final class PouchStationGameTests {
    private static final int STORAGE = 0;
    private static final int BULK = 2;

    private static void stockStorageBill(Player player) {
        player.getInventory().setItem(0, new ItemStack(Items.LEATHER, 5));
        player.getInventory().setItem(1, new ItemStack(Items.STRING, 2));
        player.getInventory().setItem(2, new ItemStack(Items.CHEST, 1));
    }

    private static int countOf(Player player, net.minecraft.world.item.Item item) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    @GameTest(template = "empty")
    public static void theBillIsPaidFromTheInventory(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var menu = new PouchStationMenu(70, player.getInventory(), BlockPos.ZERO);
        stockStorageBill(player);
        menu.select(STORAGE);
        h.assertTrue(menu.canCraftSelected(), "the inventory covers the bill");
        h.assertTrue(menu.craft(player), "the craft is accepted");
        h.assertTrue(
                menu.slots.get(PouchStationMenu.RESULT_SLOT).getItem().is(ModItems.STORAGE_POUCH.get()),
                "the pouch waits in the result slot");
        h.assertTrue(countOf(player, Items.LEATHER) == 0, "the leather is gone");
        h.assertTrue(countOf(player, Items.STRING) == 0, "the string is gone");
        h.assertTrue(countOf(player, Items.CHEST) == 0, "the chest is gone");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aShortBillCraftsNothing(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var menu = new PouchStationMenu(71, player.getInventory(), BlockPos.ZERO);
        stockStorageBill(player);
        player.getInventory().setItem(0, new ItemStack(Items.LEATHER, 4));
        menu.select(STORAGE);
        h.assertFalse(menu.canCraftSelected(), "one leather short");
        h.assertFalse(menu.craft(player), "nothing is crafted");
        h.assertTrue(
                menu.slots.get(PouchStationMenu.RESULT_SLOT).getItem().isEmpty(),
                "the result slot stays empty");
        h.assertTrue(countOf(player, Items.STRING) == 2, "the rest of the bill is untouched");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theResultSlotTakesNoInput(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var menu = new PouchStationMenu(72, player.getInventory(), BlockPos.ZERO);
        h.assertFalse(
                menu.slots.get(PouchStationMenu.RESULT_SLOT)
                        .mayPlace(new ItemStack(Items.LEATHER)),
                "the result slot refuses items");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theResultSlotMustBeClearedBeforeTheNextCraft(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var menu = new PouchStationMenu(73, player.getInventory(), BlockPos.ZERO);
        stockStorageBill(player);
        player.getInventory().setItem(3, new ItemStack(Items.LEATHER, 5));
        player.getInventory().setItem(4, new ItemStack(Items.STRING, 2));
        player.getInventory().setItem(5, new ItemStack(Items.CHEST, 1));
        menu.select(STORAGE);
        h.assertTrue(menu.craft(player), "the first pouch is made");
        h.assertFalse(menu.canCraftSelected(), "a full result slot blocks the next craft");
        menu.slots.get(PouchStationMenu.RESULT_SLOT).set(ItemStack.EMPTY);
        h.assertTrue(menu.canCraftSelected(), "clearing the slot allows the next craft");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theCraftedPouchCanBeTaken(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var menu = new PouchStationMenu(74, player.getInventory(), BlockPos.ZERO);
        menu.slots.get(PouchStationMenu.RESULT_SLOT).set(new ItemStack(ModItems.BULK_POUCH.get()));
        menu.clicked(PouchStationMenu.RESULT_SLOT, 0, ClickType.PICKUP, player);
        h.assertTrue(
                menu.getCarried().is(ModItems.BULK_POUCH.get()), "the pouch lands in the cursor");
        h.assertTrue(menu.slots.get(0).getItem().isEmpty(), "the result slot is emptied");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closingThePanelKeepsTheCraftedPouch(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var menu = new PouchStationMenu(75, player.getInventory(), BlockPos.ZERO);
        menu.slots.get(PouchStationMenu.RESULT_SLOT).set(new ItemStack(ModItems.BULK_POUCH.get()));
        menu.removed(player);
        h.assertTrue(
                countOf(player, ModItems.BULK_POUCH.get()) == 1, "the pouch went to the player");
        h.assertTrue(menu.slots.get(0).getItem().isEmpty(), "the result slot was cleared");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void everyPouchOnTheListHasABill(GameTestHelper h) {
        var recipes = PouchRecipes.all(h.getLevel());
        h.assertTrue(recipes.size() == 4, "three pouches and the backpack are offered");
        for (PouchRecipe recipe : recipes) {
            h.assertFalse(recipe.materials().isEmpty(), "a bill exists");
            h.assertFalse(recipe.resultStack().isEmpty(), "a result exists");
        }
        h.assertTrue(PouchRecipes.get(h.getLevel(), BULK) != null, "the bulk pouch is on the list");
        h.succeed();
    }

    /** The order field, not the file name, decides where a pouch sits in the list. */
    @GameTest(template = "empty")
    public static void theListFollowsTheDataOrder(GameTestHelper h) {
        var recipes = PouchRecipes.all(h.getLevel());
        h.assertTrue(
                recipes.get(STORAGE).result() == ModItems.STORAGE_POUCH.get(),
                "the storage pouch comes first");
        h.assertTrue(
                recipes.get(BULK).result() == ModItems.BULK_POUCH.get(),
                "the bulk pouch comes third");
        h.assertTrue(
                recipes.get(3).result() == ModItems.BACKPACK.get(),
                "the backpack comes last");
        h.succeed();
    }
}
