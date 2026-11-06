package github.nyeptos.packingcube.gametest.bag;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.compat.BagEquipment;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Shift-click prefers an empty equipment slot over the bag. */
@GameTestHolder(PackingCube.MODID)
@PrefixGameTestTemplate(false)
public final class ShiftEquipGameTests {
    private static Player player(GameTestHelper h) {
        return h.makeMockPlayer(GameType.SURVIVAL);
    }

    private static BagInventory storage(ItemStack pouch) {
        return new BagInventory(pouch, false, () -> {});
    }

    private static ItemStack pouch(String... items) {
        var stack = ModItems.STORAGE_POUCH.get().getDefaultInstance();
        for (var item : items)
            BagFilter.add(stack, "items", ResourceLocation.withDefaultNamespace(item));
        return stack;
    }

    private static BagMenu open(Player player, ItemStack pouch) {
        var menu = new BagMenu(42, player.getInventory(), storage(pouch), pouch, 0, false);
        player.containerMenu = menu;
        return menu;
    }

    @GameTest(template = "empty")
    public static void emptyArmorSlotWinsOverAnAcceptingBag(GameTestHelper h) {
        var player = player(h);
        var bag = pouch("iron_helmet");
        player.getInventory().setItem(0, bag);
        player.getInventory().setItem(9, new ItemStack(Items.IRON_HELMET));
        var menu = open(player, bag);
        h.assertFalse(menu.quickMoveStack(player, 9).isEmpty(), "Shift transfer moves the helmet");
        h.assertTrue(
                player.getInventory().getItem(39).is(Items.IRON_HELMET),
                "An empty head slot takes the helmet before the bag");
        h.assertTrue(
                player.getInventory().getItem(9).isEmpty() && storage(bag).getItem(0).isEmpty(),
                "The bag stays empty even though its filter accepts the helmet");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void occupiedArmorSlotFallsThroughToTheBag(GameTestHelper h) {
        var player = player(h);
        var bag = pouch("iron_helmet");
        player.getInventory().setItem(0, bag);
        player.getInventory().setItem(39, new ItemStack(Items.LEATHER_HELMET));
        player.getInventory().setItem(9, new ItemStack(Items.IRON_HELMET));
        var menu = open(player, bag);
        h.assertFalse(menu.quickMoveStack(player, 9).isEmpty(), "Shift transfer still stores");
        h.assertTrue(
                player.getInventory().getItem(39).is(Items.LEATHER_HELMET),
                "Existing equipment is never removed or replaced");
        h.assertTrue(
                storage(bag).getItem(0).is(Items.IRON_HELMET)
                        && player.getInventory().getItem(9).isEmpty(),
                "The helmet goes to the bag instead of swapping the worn one");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fullBagStillEquipsAndTheOverflowStaysInTheInventory(GameTestHelper h) {
        var player = player(h);
        var bag = pouch("iron_helmet");
        player.getInventory().setItem(0, bag);
        var contents = storage(bag);
        for (int i = 0; i < contents.getContainerSize(); i++)
            contents.setItem(i, new ItemStack(Items.LEATHER_HELMET));
        player.getInventory().setItem(9, new ItemStack(Items.IRON_HELMET));
        player.getInventory().setItem(10, new ItemStack(Items.IRON_HELMET));
        var menu = open(player, bag);
        h.assertFalse(menu.quickMoveStack(player, 9).isEmpty(), "The first helmet equips");
        h.assertTrue(
                player.getInventory().getItem(39).is(Items.IRON_HELMET),
                "A full bag does not stop the empty equipment slot");
        h.assertTrue(
                menu.quickMoveStack(player, 10).isEmpty(),
                "The second helmet cannot move once the slot and the bag are full");
        h.assertTrue(
                player.getInventory().getItem(10).is(Items.IRON_HELMET)
                        && player.getInventory().getItem(10).getCount() == 1,
                "The leftover stays in its original slot without duplication");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rejectedByTheBagAndAnOccupiedSlotKeepsTheStack(GameTestHelper h) {
        var player = player(h);
        var bag = pouch();
        player.getInventory().setItem(0, bag);
        player.getInventory().setItem(39, new ItemStack(Items.LEATHER_HELMET));
        player.getInventory().setItem(9, new ItemStack(Items.IRON_HELMET));
        var menu = open(player, bag);
        h.assertTrue(menu.quickMoveStack(player, 9).isEmpty(), "Nothing moves");
        h.assertTrue(
                player.getInventory().getItem(9).is(Items.IRON_HELMET)
                        && player.getInventory().getItem(9).getCount() == 1,
                "No item is lost or duplicated");
        h.assertTrue(
                player.getInventory().getItem(39).is(Items.LEATHER_HELMET),
                "The worn helmet is untouched");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void offhandIsNotFilledByShiftClick(GameTestHelper h) {
        var player = player(h);
        var bag = pouch("shield");
        player.getInventory().setItem(0, bag);
        player.getInventory().setItem(9, new ItemStack(Items.SHIELD));
        var menu = open(player, bag);
        h.assertFalse(menu.quickMoveStack(player, 9).isEmpty(), "The shield still moves");
        h.assertTrue(
                player.getInventory().getItem(40).isEmpty(),
                "Shift-click never fills the offhand");
        h.assertTrue(
                storage(bag).getItem(0).is(Items.SHIELD),
                "The shield goes to the bag as before");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void curiosSlotsOnlyReceiveStacksTheyAccept(GameTestHelper h) {
        var player = player(h);
        var bag = pouch();
        player.getInventory().setItem(0, bag);
        player.getInventory().setItem(9, new ItemStack(Items.IRON_HELMET));
        var menu = open(player, bag);
        h.assertFalse(menu.quickMoveStack(player, 9).isEmpty(), "The helmet still equips");
        h.assertTrue(
                player.getInventory().getItem(39).is(Items.IRON_HELMET),
                "Curios targets never shadow the vanilla armor slot");
        for (var target : BagEquipment.refillTargets(player)) {
            if (!target.id().startsWith("curios:")) continue;
            var slot = BagEquipment.equipmentSlot(player, target.id());
            h.assertTrue(
                    slot == null || !slot.getItem().is(Items.IRON_HELMET),
                    "A Curios slot that rejects helmets stays empty: " + target.id());
        }
        h.succeed();
    }
}
