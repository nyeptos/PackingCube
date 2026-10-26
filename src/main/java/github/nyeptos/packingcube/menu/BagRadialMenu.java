package github.nyeptos.packingcube.menu;

import github.nyeptos.packingcube.bag.BagIdentity;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.bag.refill.RefillSettings;
import github.nyeptos.packingcube.registry.ModItems;
import github.nyeptos.packingcube.registry.ModMenus;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Nine display-only icons with names and filter settings, never real storage or identity. */
public final class BagRadialMenu extends AbstractContainerMenu implements BagRootMenu {
    private final SimpleContainer icons = new SimpleContainer(BagInventory.MODULE_SLOTS);
    private final ItemStack[] expected = new ItemStack[BagInventory.MODULE_SLOTS];
    private final UUID rootId;
    private boolean selected;

    public BagRadialMenu(int id, Inventory inventory) {
        this(id, inventory, ItemStack.EMPTY);
    }

    public BagRadialMenu(int id, Inventory inventory, ItemStack root) {
        super(ModMenus.BAG_RADIAL_MENU.get(), id);
        rootId = root.isEmpty() ? null : BagIdentity.renew(root);
        BagInventory modules = root.isEmpty() ? null : new BagInventory(root, true, () -> {});
        for (int i = 0; i < BagInventory.MODULE_SLOTS; i++) {
            ItemStack actual = modules == null ? ItemStack.EMPTY : modules.getItem(i);
            expected[i] = actual.copy();
            if (BagInventory.isPouch(actual) && actual.getCount() == 1) {
                ItemStack icon = actual.getItem().getDefaultInstance();
                var name = actual.get(DataComponents.CUSTOM_NAME);
                if (name != null) icon.set(DataComponents.CUSTOM_NAME, name);
                BagFilter.copySettings(actual, icon);
                if (actual.is(ModItems.REFILL_POUCH.get()))
                    RefillSettings.copySettings(actual, icon);
                icons.setItem(i, icon);
            }
            addSlot(
                    new Slot(icons, i, -10000, -10000) {
                        @Override
                        public boolean mayPickup(Player player) {
                            return false;
                        }

                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return false;
                        }

                        @Override
                        public boolean isActive() {
                            return false;
                        }
                    });
        }
    }

    public ItemStack icon(int index) {
        return icons.getItem(index);
    }

    @Override
    public boolean ownsRoot(ItemStack stack) {
        return stack.is(ModItems.LARGE_BAG.get()) && BagIdentity.matches(stack, rootId);
    }

    private ItemStack findRoot(Player player) {
        return BagIdentity.findUnique(player, this::ownsRoot, getCarried());
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().isClientSide || !findRoot(player).isEmpty();
    }

    /**
     * Only the live selector's menu ID and a bounded slot index are accepted by the network
     * handler.
     */
    public boolean select(Player player, int moduleSlot) {
        if (player.level().isClientSide
                || player.containerMenu != this
                || selected
                || player.isSpectator()
                || !getCarried().isEmpty()
                || moduleSlot < -1
                || moduleSlot >= BagInventory.MODULE_SLOTS) return false;
        ItemStack root = findRoot(player);
        if (root.isEmpty()) return false;
        BagInventory parent = new BagInventory(root, true, player.getInventory()::setChanged);
        boolean modules =
                moduleSlot == -1; // Explicit management button, not the central dead zone.
        BagInventory target = parent;
        ItemStack targetStack = root;
        if (!modules) {
            ItemStack actual = parent.getItem(moduleSlot);
            if (!BagInventory.isPouch(actual)
                    || actual.getCount() != 1
                    || !ItemStack.matches(actual, expected[moduleSlot])) return false;
            target = new BagInventory(actual, false, parent);
            targetStack = actual;
        }
        selected = true;
        parent.recoverLegacyOverflow(player);
        if (!modules) target.recoverLegacyOverflow(player);
        return BagMenuOpener.open(player, target, root, -1, modules, targetStack.getHoverName());
    }

    // No normal container action may take the display copies or move inventory items through this
    // menu.
    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {}

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return false;
    }
}
