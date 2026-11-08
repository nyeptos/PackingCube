package github.nyeptos.packingcube.gametest.refill;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.refill.RefillDispatcher;
import github.nyeptos.packingcube.bag.refill.RefillSlotDestinations;
import github.nyeptos.packingcube.bag.refill.RefillTargets;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;


@GameTestHolder(PackingCube.MODID)
@PrefixGameTestTemplate(false)
public final class RefillScreenGameTests {
    private static BagInventory storage(ItemStack pouch) {
        return new BagInventory(pouch, false, () -> {});
    }

    private static ItemStack pouch(ItemStack... items) {
        var pouch = ModItems.REFILL_POUCH.get().getDefaultInstance();
        var inv = storage(pouch);
        for (int i = 0; i < items.length; i++) inv.setItem(i, items[i]);
        return pouch;
    }

    private static ItemStack backpack(ItemStack module) {
        var root = ModItems.BACKPACK.get().getDefaultInstance();
        if (!module.isEmpty()) new BagInventory(root, true, () -> {}).setItem(0, module);
        return root;
    }

    private static int count(ItemStack root, int slot) {
        return storage(new BagInventory(root, true, () -> {}).getItem(0))
                .getItem(slot)
                .getCount();
    }

    private static BagMenu openScreen(Player player, ItemStack root, int ownerSlot) {
        var modules = new BagInventory(root, true, () -> {});
        var menu =
                new BagMenu(
                        42,
                        player.getInventory(),
                        new BagInventory(modules.getItem(0), false, modules),
                        root,
                        ownerSlot,
                        false);
        player.containerMenu = menu;
        return menu;
    }

    @GameTest(template = "empty")
    public static void ruleRefillKeepsRunningWhileABagScreenIsOpen(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var source = pouch(new ItemStack(Items.STONE, 12));
        RefillSlotDestinations.set(source, 0, "destination", RefillTargets.inventory(9));
        RefillSlotDestinations.set(source, 0, "target", "4");
        var supplyRoot = backpack(source);
        player.getInventory().setItem(8, supplyRoot);
        var openRoot = backpack(ModItems.STORAGE_POUCH.get().getDefaultInstance());
        player.getInventory().setItem(7, openRoot);
        var menu = openScreen(player, openRoot, 7);
        h.assertTrue(
                RefillDispatcher.tick(player) == 4,
                "The settings screen no longer pauses rule-based refill");
        h.assertTrue(
                player.getInventory().getItem(9).getCount() == 4 && count(supplyRoot, 0) == 8,
                "Destination filled and the source pouch keeps the rest");
        h.assertTrue(
                menu.getSlot(9).getItem().getCount() == 4,
                "The open screen shows the refilled stack");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void carriedStackStillPausesTheOpenScreen(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var source = pouch(new ItemStack(Items.STONE, 12));
        RefillSlotDestinations.set(source, 0, "destination", RefillTargets.inventory(9));
        RefillSlotDestinations.set(source, 0, "target", "4");
        var supplyRoot = backpack(source);
        player.getInventory().setItem(8, supplyRoot);
        var openRoot = backpack(ModItems.STORAGE_POUCH.get().getDefaultInstance());
        player.getInventory().setItem(7, openRoot);
        var menu = openScreen(player, openRoot, 7);
        menu.setCarried(new ItemStack(Items.DIRT));
        h.assertTrue(
                RefillDispatcher.tick(player) == 0
                        && player.getInventory().getItem(9).isEmpty()
                        && count(supplyRoot, 0) == 12,
                "An item on the cursor pauses refill even on the settings screen");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void otherContainersStillPauseRefill(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var source = pouch(new ItemStack(Items.STONE, 12));
        RefillSlotDestinations.set(source, 0, "destination", RefillTargets.inventory(9));
        RefillSlotDestinations.set(source, 0, "target", "4");
        var supplyRoot = backpack(source);
        player.getInventory().setItem(8, supplyRoot);
        player.containerMenu =
                new ChestMenu(
                        MenuType.GENERIC_9x1,
                        7,
                        player.getInventory(),
                        new SimpleContainer(9),
                        1);
        h.assertTrue(
                RefillDispatcher.tick(player) == 0
                        && player.getInventory().getItem(9).isEmpty()
                        && count(supplyRoot, 0) == 12,
                "Chests keep automatic refill paused");
        h.succeed();
    }
}
