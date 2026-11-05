package github.nyeptos.packingcube.gametest.refill;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.refill.RefillDispatcher;
import github.nyeptos.packingcube.bag.refill.RefillSettings;
import github.nyeptos.packingcube.bag.refill.RefillSlotSettings;
import github.nyeptos.packingcube.bag.refill.RefillTargets;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.function.Consumer;

/** Replacement returns the old item to the pouch and locks that slot against re-supply. */
@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class RefillReturnGameTests {
    private static BagInventory storage(ItemStack pouch) {
        return new BagInventory(pouch, false, () -> {});
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

    private static void edit(ItemStack root, Consumer<ItemStack> change) {
        var parent = new BagInventory(root, true, () -> {});
        var inner = new BagInventory(parent.getItem(0), false, parent);
        change.accept(inner.bagStack());
        inner.setChanged();
    }

    /** One stone per tick into a dirt-filled hand slot, so every tick replaces exactly one. */
    private static void assign(ItemStack pouch, int source, int destination) {
        RefillSlotSettings.set(pouch, source, "destination", RefillTargets.inventory(destination));
        RefillSlotSettings.set(pouch, source, "target", "1");
        RefillSlotSettings.set(pouch, source, "replace", "true");
    }

    @GameTest(template = "empty")
    public static void replacedItemReturnsToThePouchAndLocksThatSlot(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.STONE, 12));
        assign(pouch, 0, 9);
        var root = install(player, pouch);
        player.getInventory().setItem(9, new ItemStack(Items.DIRT));
        h.assertTrue(RefillDispatcher.tick(player) == 1, "One stone replaces the dirt");
        var stored = saved(root);
        h.assertTrue(
                player.getInventory().getItem(9).is(Items.STONE)
                        && count(root, 0) == 11
                        && storage(stored).getItem(1).is(Items.DIRT),
                "Old item moves into the pouch instead of the inventory");
        h.assertTrue(
                RefillSettings.locked(stored, 1) && !RefillSettings.locked(stored, 0),
                "Only the slot that took the returned item is locked");
        h.assertTrue(
                RefillDispatcher.tick(player) == 0 && RefillSettings.locked(saved(root), 1),
                "A satisfied destination leaves the lock and the pouch untouched");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lockedSlotIsNeverUsedAsSupply(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.STONE, 12));
        assign(pouch, 0, 9);
        var root = install(player, pouch);
        player.getInventory().setItem(9, new ItemStack(Items.DIRT));
        RefillDispatcher.tick(player);
        edit(
                root,
                p -> {
                    RefillSlotSettings.set(p, 1, "destination", RefillTargets.inventory(10));
                    RefillSlotSettings.set(p, 1, "target", "1");
                });
        h.assertTrue(
                RefillDispatcher.tick(player) == 0
                        && player.getInventory().getItem(10).isEmpty()
                        && count(root, 1) == 1,
                "Returned item stays in its locked slot");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyingTheSlotReleasesTheLock(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.STONE, 12));
        assign(pouch, 0, 9);
        var root = install(player, pouch);
        player.getInventory().setItem(9, new ItemStack(Items.DIRT));
        RefillDispatcher.tick(player);
        var parent = new BagInventory(root, true, () -> {});
        var inner = new BagInventory(parent.getItem(0), false, parent);
        inner.removeItem(1, 1);
        inner.setItem(1, new ItemStack(Items.STONE, 5));
        inner.setChanged();
        h.assertTrue(!RefillSettings.locked(saved(root), 1), "Emptying the slot clears its lock");
        edit(
                root,
                p -> {
                    RefillSlotSettings.set(p, 1, "destination", RefillTargets.inventory(10));
                    RefillSlotSettings.set(p, 1, "target", "1");
                });
        h.assertTrue(
                RefillDispatcher.tick(player) == 1
                        && player.getInventory().getItem(10).is(Items.STONE)
                        && count(root, 1) == 4,
                "A refilled slot supplies again once the lock is gone");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void collectionOffReturnsToTheInventory(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.STONE, 12));
        assign(pouch, 0, 9);
        RefillSlotSettings.set(pouch, 0, "collect", "false");
        var root = install(player, pouch);
        player.getInventory().setItem(9, new ItemStack(Items.DIRT));
        h.assertTrue(RefillDispatcher.tick(player) == 1, "Replacement still happens");
        h.assertTrue(
                player.getInventory().getItem(0).is(Items.DIRT)
                        && storage(saved(root)).getItem(1).isEmpty()
                        && RefillSettings.lockedMask(saved(root)) == 0,
                "Old item goes to the inventory and nothing is locked");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lockOffCollectsWithoutBlockingSupply(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.STONE, 12));
        assign(pouch, 0, 9);
        RefillSlotSettings.set(pouch, 0, "lock", "false");
        var root = install(player, pouch);
        player.getInventory().setItem(9, new ItemStack(Items.DIRT));
        RefillDispatcher.tick(player);
        h.assertTrue(
                storage(saved(root)).getItem(1).is(Items.DIRT)
                        && RefillSettings.lockedMask(saved(root)) == 0,
                "Collected item is stored without a lock");
        edit(
                root,
                p -> {
                    RefillSlotSettings.set(p, 1, "destination", RefillTargets.inventory(10));
                    RefillSlotSettings.set(p, 1, "target", "1");
                });
        h.assertTrue(
                RefillDispatcher.tick(player) == 1
                        && player.getInventory().getItem(10).is(Items.DIRT),
                "Unlocked collected item can be supplied again");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fullPouchAndInventoryAbortReplacementWithoutLoss(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var items = new ItemStack[9];
        items[0] = new ItemStack(Items.STONE, 12);
        for (int i = 1; i < 9; i++) items[i] = new ItemStack(Items.GRANITE, 64);
        var pouch = pouch(items);
        assign(pouch, 0, 9);
        var root = install(player, pouch);
        for (int i = 0; i < 36; i++)
            if (i != 8 && i != 9)
                player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        player.getInventory().setItem(9, new ItemStack(Items.DIRT));
        var before = root.copy();
        h.assertTrue(
                RefillDispatcher.tick(player) == 0
                        && ItemStack.matches(root, before)
                        && player.getInventory().getItem(9).is(Items.DIRT),
                "No room for the old item means no replacement and no loss");
        int stone = 0;
        for (int i = 0; i < 36; i++)
            if (player.getInventory().getItem(i).is(Items.STONE)) stone++;
        h.assertTrue(stone == 0, "Nothing is dropped or duplicated into the inventory");
        h.succeed();
    }
}
