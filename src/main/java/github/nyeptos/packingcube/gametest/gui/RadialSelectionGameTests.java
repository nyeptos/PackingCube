package github.nyeptos.packingcube.gametest.gui;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.EquippedBagAccess;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.bag.pickup.BagRouter;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.menu.BagRadialMenu;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static github.nyeptos.packingcube.gametest.support.RadialTestSupport.*;

/** Which module a radial selection opens, and when it refuses. */
@GameTestHolder(PackingCube.MODID)
@PrefixGameTestTemplate(false)
public final class RadialSelectionGameTests {
    @GameTest(template = "empty")
    public static void radialSelectionOpensAndSavesInstalledModule(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        modules(root).setItem(6, module());
        BagRadialMenu menu = open(player, root);
        h.assertTrue(menu.select(player, 6), "Valid installed module opens");
        h.assertTrue(
                player.containerMenu instanceof BagMenu bag && !bag.isModules(),
                "Opens storage, not management");
        BagMenu bag = (BagMenu) player.containerMenu;
        h.assertTrue(bag.stillValid(player), "Parent identity follows through menu transition");
        h.assertTrue(
                bag.editFilter(player, 3, -1, "mode", "exclude"), "Installed rules can be edited");
        bag.getSlot(0).setByPlayer(new ItemStack(Items.DIAMOND, 7));
        ItemStack saved = modules(root).getItem(6);
        h.assertTrue(
                BagFilter.isExcluding(saved) && storage(saved).getItem(0).getCount() == 7,
                "Rules and contents persist into original installed module");
        h.assertFalse(menu.select(player, 6), "Old selector cannot act after transition");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialRejectsEmptyAndInvalidSlotsAndForeignMenus(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        BagRadialMenu menu = open(player, root);
        h.assertFalse(
                menu.select(player, -2) || menu.select(player, 9) || menu.select(player, 0),
                "Invalid and empty slots cannot open bags");
        player.containerMenu = player.inventoryMenu;
        h.assertFalse(menu.select(player, -1), "Not the player's active selector");
        player.containerMenu = menu;
        menu.setCarried(new ItemStack(Items.DIAMOND));
        h.assertFalse(menu.select(player, -1), "Carried items cannot be lost by changing menu");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialRejectsChangedOrNewlyInsertedModules(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        modules(root).setItem(0, module());
        BagRadialMenu menu = open(player, root);
        ItemStack changed = module();
        BagFilter.add(changed, "items", ResourceLocation.withDefaultNamespace("stone"));
        modules(root).setItem(0, changed);
        modules(root).setItem(1, module());
        h.assertFalse(
                menu.select(player, 0), "Changed module cannot be mistaken for previewed module");
        h.assertFalse(menu.select(player, 1), "New module must be previewed by reopening first");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialTracksMovedParentAndRejectsDuplicates(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        modules(root).setItem(2, module());
        BagRadialMenu menu = open(player, root);
        ItemStack moved = root.copy();
        player.getInventory().setItem(0, ItemStack.EMPTY);
        player.getInventory().setItem(8, moved);
        h.assertTrue(menu.stillValid(player), "Selector follows copied parent in another slot");
        player.getInventory().setItem(9, moved.copy());
        h.assertFalse(
                menu.stillValid(player) || menu.select(player, 2),
                "Duplicate identity stops selection");
        player.getInventory().setItem(9, ItemStack.EMPTY);
        h.assertTrue(menu.select(player, 2), "Unique moved parent can open its module");
        BagMenu target = (BagMenu) player.containerMenu;
        target.getSlot(0).setByPlayer(new ItemStack(Items.DIAMOND, 4));
        h.assertTrue(
                storage(modules(moved).getItem(2)).getItem(0).getCount() == 4,
                "Save uses moved parent");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialMissingParentCannotOpenManagement(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        BagRadialMenu menu = open(player, root);
        player.getInventory().setItem(0, ItemStack.EMPTY);
        h.assertFalse(
                menu.stillValid(player) || menu.select(player, -1),
                "Dropped parent invalidates selector");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialExcludesParentFromAutomaticStorage(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag(), filter = module();
        BagFilter.add(filter, "items", ResourceLocation.withDefaultNamespace("stone"));
        modules(root).setItem(0, filter);
        open(player, root);
        h.assertTrue(
                BagRouter.route(player, new ItemStack(Items.STONE, 12)).getCount() == 12,
                "Previewed root is protected from another live writer");
        player.containerMenu = player.inventoryMenu;
        h.assertTrue(
                BagRouter.route(player, new ItemStack(Items.STONE, 12)).isEmpty(),
                "Closing selector resumes pickup");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldRightClickDoesNotOpenModule(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        player.getInventory().setItem(0, root);
        BagInventory modules = modules(root);
        modules.setItem(0, module());
        BagMenu menu = new BagMenu(4, player.getInventory(), modules, root, 0, true);
        player.containerMenu = menu;
        menu.clicked(0, 1, ClickType.PICKUP, player);
        h.assertTrue(
                player.containerMenu == menu && menu.isModules(),
                "Right click never navigates to Filter Bag");
        h.assertTrue(
                menu.getCarried().is(ModItems.STORAGE_POUCH.get()),
                "Right click is normal item handling instead");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void inventoryFallbackAndManagementButtonWorkWithoutEquipment(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        player.getInventory().setItem(4, root);
        h.assertTrue(
                EquippedBagAccess.open(player),
                "Inventory bag supports radial without Curios equipment");
        h.assertTrue(
                player.containerMenu instanceof BagRadialMenu radial && radial.ownsRoot(root),
                "Opens correct inventory root");
        BagRadialMenu menu = (BagRadialMenu) player.containerMenu;
        h.assertTrue(menu.select(player, -1), "Explicit management action remains available");
        h.assertTrue(
                player.containerMenu instanceof BagMenu bag && bag.isModules(),
                "Management keeps installation/removal UI");
        h.succeed();
    }
}
