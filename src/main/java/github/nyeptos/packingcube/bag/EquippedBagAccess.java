package github.nyeptos.packingcube.bag;

import github.nyeptos.packingcube.compat.BagEquipment;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.menu.BagMenuOpener;
import github.nyeptos.packingcube.menu.BagRadialMenu;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Neither key request accepts an item or equipment slot supplied by the client. */
public final class EquippedBagAccess {
    private EquippedBagAccess() {}

    private static boolean canOpen(Player player) {
        return !player.level().isClientSide
                && player.isAlive()
                && !player.isSpectator()
                && player.containerMenu == player.inventoryMenu
                && player.containerMenu.getCarried().isEmpty();
    }

    private static ItemStack primaryBag(Player player) {
        var equipped = BagEquipment.activeBackBags(player);
        if (!equipped.isEmpty()) return equipped.getFirst();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (candidate.is(ModItems.LARGE_BAG.get()) && candidate.getCount() == 1)
                return candidate;
        }
        return ItemStack.EMPTY;
    }

    public static boolean open(Player player) {
        if (!canOpen(player)) return false;
        return openRadial(player, primaryBag(player));
    }

    public static boolean openDirect(Player player) {
        if (!canOpen(player)) return false;
        return openManagement(player, primaryBag(player));
    }

    /** B toggles management or returns from a module; C opens the same parent's radial menu. */
    public static boolean navigate(Player player, int containerId, boolean radial) {
        if (player.level().isClientSide
                || !player.isAlive()
                || player.isSpectator()
                || !(player.containerMenu instanceof BagMenu menu)
                || menu.containerId != containerId
                || !menu.getCarried().isEmpty()
                || !menu.stillValid(player)) return false;
        if (!radial && menu.isModules()) {
            player.closeContainer();
            return true;
        }
        // stillValid rebinds a copied/moved root before we read it. Always prefer this parent.
        ItemStack root = menu.rootBag();
        if (!root.is(ModItems.LARGE_BAG.get())) root = primaryBag(player);
        return radial ? openRadial(player, root) : openManagement(player, root);
    }

    private static boolean openRadial(Player player, ItemStack root) {
        if (root.isEmpty()) return false;
        return player.openMenu(
                        new SimpleMenuProvider(
                                (id, inventory, ignored) -> new BagRadialMenu(id, inventory, root),
                                net.minecraft.network.chat.Component.translatable(
                                        "gui.packingcube.radial_title")))
                .isPresent();
    }

    private static boolean openManagement(Player player, ItemStack root) {
        if (root.isEmpty()) return false;
        BagInventory storage = new BagInventory(root, true, player.getInventory()::setChanged);
        storage.recoverLegacyOverflow(player);
        return BagMenuOpener.open(player, storage, root, -1, true, root.getHoverName());
    }
}
