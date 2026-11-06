package github.nyeptos.packingcube.gametest.bag;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.bag.pickup.BagPickupHandler;
import github.nyeptos.packingcube.menu.BagMenu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static github.nyeptos.packingcube.gametest.support.BagTestSupport.*;

/** Ground pickup: delay, owner, completion, and overflow. */
@GameTestHolder(PackingCube.MODID)
@PrefixGameTestTemplate(false)
public final class BagPickupGameTests {
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
}
