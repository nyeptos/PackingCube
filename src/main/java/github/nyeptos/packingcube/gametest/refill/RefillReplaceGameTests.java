package github.nyeptos.packingcube.gametest.refill;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.refill.RefillDispatcher;
import github.nyeptos.packingcube.bag.refill.RefillSlotDestinations;
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

/** One switch decides whether an occupied destination may be replaced at all. */
@GameTestHolder(PackingCube.MODID)
@PrefixGameTestTemplate(false)
public final class RefillReplaceGameTests {
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
        var root = ModItems.BACKPACK.get().getDefaultInstance();
        new BagInventory(root, true, () -> {}).setItem(0, pouch);
        player.getInventory().setItem(8, root);
        return root;
    }

    private static ItemStack saved(ItemStack root) {
        return new BagInventory(root, true, () -> {}).getItem(0);
    }

    private static ItemStack pickaxe(int damage) {
        var stack = new ItemStack(Items.DIAMOND_PICKAXE);
        stack.setDamageValue(damage);
        return stack;
    }

    private static void assign(ItemStack pouch, int destination, boolean replace) {
        RefillSlotDestinations.set(pouch, 0, "destination", RefillTargets.inventory(destination));
        RefillSlotDestinations.set(pouch, 0, "target", "1");
        RefillSlotDestinations.set(pouch, 0, "replace", Boolean.toString(replace));
    }

    @GameTest(template = "empty")
    public static void replaceOnSwapsAWornItemForAFreshOne(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(pickaxe(1500));
        assign(pouch, 9, true);
        var root = install(player, pouch);
        player.getInventory().setItem(9, pickaxe(0));
        h.assertTrue(RefillDispatcher.tick(player) == 1, "Switch ON replaces without comparing");
        h.assertTrue(
                player.getInventory().getItem(9).getDamageValue() == 1500
                        && storage(saved(root)).getItem(0).getDamageValue() == 0,
                "The pouch item takes the slot and the hand item is collected");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void replaceOffKeepsTheHandItem(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(pickaxe(1500));
        assign(pouch, 9, false);
        var root = install(player, pouch);
        player.getInventory().setItem(9, pickaxe(0));
        h.assertTrue(
                RefillDispatcher.tick(player) == 0
                        && player.getInventory().getItem(9).getDamageValue() == 0
                        && storage(saved(root)).getItem(0).getDamageValue() == 1500,
                "Switch OFF leaves the occupied slot alone");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void replaceOnAlsoSwapsDifferentItems(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pouch = pouch(new ItemStack(Items.STONE, 12));
        assign(pouch, 9, true);
        var root = install(player, pouch);
        player.getInventory().setItem(9, new ItemStack(Items.DIRT));
        h.assertTrue(
                RefillDispatcher.tick(player) == 1
                        && player.getInventory().getItem(9).is(Items.STONE),
                "A different item is replaced too while the switch is on");
        h.succeed();
    }
}
