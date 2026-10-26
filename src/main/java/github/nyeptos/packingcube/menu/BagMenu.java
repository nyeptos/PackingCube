package github.nyeptos.packingcube.menu;

import github.nyeptos.packingcube.bag.BagFilter;
import github.nyeptos.packingcube.bag.BagIdentity;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.BagPickupSettings;
import github.nyeptos.packingcube.network.FilterActions;
import github.nyeptos.packingcube.registry.ModItems;
import github.nyeptos.packingcube.registry.ModMenus;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Identical slot rules on client and server; persistence and menu opening are server-only. */
public final class BagMenu extends AbstractContainerMenu implements BagRootMenu {
    private final Container contents;
    private ItemStack root;
    private final UUID rootId;
    private final Item rootItem;
    private final BagInventory rootStorage;
    private final int ownerSlot;
    private final int bagSlots;
    private final boolean modules;
    // Read-only synchronization slots: settings snapshot and one ghost sample.
    private final SimpleContainer editorState = new SimpleContainer(2);

    public static BagMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        int ownerSlot = data.readVarInt();
        boolean modules = data.readBoolean();
        return clientMenu(id, inventory, ownerSlot, modules);
    }

    /** Client menu uses temporary storage, never the held item's persistent data. */
    public static BagMenu clientMenu(int id, Inventory inventory, int ownerSlot, boolean modules) {
        return new BagMenu(
                id,
                inventory,
                new SimpleContainer(
                        modules ? BagInventory.MODULE_SLOTS : BagInventory.FILTER_SLOTS),
                ItemStack.EMPTY,
                ownerSlot,
                modules);
    }

    public BagMenu(
            int id,
            Inventory inventory,
            Container contents,
            ItemStack root,
            int ownerSlot,
            boolean modules) {
        super(ModMenus.BAG_MENU.get(), id);
        this.contents = contents;
        this.root = root;
        this.rootId = root.isEmpty() ? null : BagIdentity.renew(root);
        this.rootItem = root.getItem();
        this.rootStorage = contents instanceof BagInventory storage ? storage.rootView() : null;
        this.ownerSlot = ownerSlot;
        this.modules = modules;
        this.bagSlots = contents.getContainerSize();
        int rows = bagSlots / 9;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(
                        new Slot(contents, column + row * 9, 8 + column * 18, 18 + row * 18) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return BagInventory.accepts(modules, stack)
                                        && (modules || BagFilter.matches(filterSnapshot(), stack));
                            }
                        });
            }
        }
        int offset = (rows - 4) * 18;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addPlayerSlot(
                        inventory, column + row * 9 + 9, 8 + column * 18, 103 + row * 18 + offset);
            }
        }
        for (int column = 0; column < 9; column++)
            addPlayerSlot(inventory, column, 8 + column * 18, 161 + offset);
        for (int i = 0; i < 2; i++) {
            addSlot(
                    new Slot(editorState, i, -10000, -10000) {
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
        refreshFilterSnapshot();
    }

    private void addPlayerSlot(Inventory inventory, int index, int x, int y) {
        addSlot(new Slot(inventory, index, x, y));
    }

    public ItemStack rootBag() {
        return root;
    }

    public boolean ownsRoot(ItemStack bag) {
        return bag.is(rootItem) && BagIdentity.matches(bag, rootId);
    }

    /** Locate by identity, not by slot or Java reference. Never write to an ambiguous duplicate. */
    private boolean locateRoot(Player player) {
        if (!player.isAlive() || rootStorage == null) return false;
        ItemStack found = BagIdentity.findUnique(player, this::ownsRoot, getCarried());
        if (found.isEmpty()) return false;
        if (found != root) {
            root = found;
            rootStorage.rebind(found);
        }
        return true;
    }

    public boolean isModules() {
        return modules;
    }

    public int visibleSlotCount() {
        return bagSlots + 36;
    }

    public ItemStack filterSnapshot() {
        return editorState.getItem(0);
    }

    public ItemStack sample() {
        return editorState.getItem(1);
    }

    private void refreshFilterSnapshot() {
        if (!(contents instanceof BagInventory storage)) return;
        ItemStack snapshot =
                (modules ? ModItems.LARGE_BAG : ModItems.FILTER_BAG).get().getDefaultInstance();
        if (!modules) {
            var data = storage.bagStack().get(DataComponents.CUSTOM_DATA);
            if (data != null) snapshot.set(DataComponents.CUSTOM_DATA, data);
        }
        editorState.setItem(0, snapshot);
    }

    /**
     * Packet entry point. Never accepts an ItemStack or arbitrary sample supplied by the client.
     */
    public boolean editFilter(
            Player player, int operation, int sampleSlot, String field, String value) {
        if (player.containerMenu != this
                || player.level().isClientSide
                || player.isSpectator()
                || !stillValid(player)
                || !(contents instanceof BagInventory storage)) return false;
        if (operation == FilterActions.SET_PICKUP_ENABLED) {
            if (modules
                    || sampleSlot != -1
                    || !field.equals("pickup")
                    || !(value.equals("true") || value.equals("false"))) return false;
            BagPickupSettings.setEnabled(storage.bagStack(), Boolean.parseBoolean(value));
            contents.setChanged();
            refreshFilterSnapshot();
            broadcastChanges();
            return true;
        }
        if (operation == FilterActions.SET_PICKUP_PRIORITY) {
            if (modules || sampleSlot != -1 || !field.equals("priority") || !value.matches("[0-9]"))
                return false;
            BagPickupSettings.setPriority(storage.bagStack(), Integer.parseInt(value));
            contents.setChanged();
            refreshFilterSnapshot();
            broadcastChanges();
            return true;
        }
        if (modules) return false;
        if (operation == FilterActions.SAMPLE_CURSOR) {
            if (!field.isEmpty() || !(value.equals("copy") || value.equals("clear"))) return false;
            ItemStack source = value.equals("clear") ? ItemStack.EMPTY : getCarried();
            if (BagInventory.isBag(source)) return false;
            // A ghost reference only. Never split, shrink, insert, return or drop the source stack.
            editorState.setItem(1, source.isEmpty() ? ItemStack.EMPTY : source.copyWithCount(1));
            broadcastChanges();
            return true;
        }
        if (operation == FilterActions.SAMPLE_SLOT) {
            if (sampleSlot < 0 || sampleSlot >= visibleSlotCount()) return false;
            ItemStack actual = slots.get(sampleSlot).getItem();
            if (BagInventory.isBag(actual)) return false;
            editorState.setItem(1, actual.isEmpty() ? ItemStack.EMPTY : actual.copyWithCount(1));
            broadcastChanges();
            return true;
        }
        if (operation == FilterActions.SET_MODE) {
            if (!field.equals("mode") || !(value.equals("include") || value.equals("exclude")))
                return false;
            BagFilter.setExcluding(storage.bagStack(), value.equals("exclude"));
            contents.setChanged();
            refreshFilterSnapshot();
            broadcastChanges();
            return true;
        }
        if (!BagFilter.FIELDS.contains(field)) return false;
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) return false;
        boolean changed;
        if (operation == FilterActions.ADD_RULE) {
            ItemStack sample = sample();
            if (sample.isEmpty()) return false;
            if (field.equals("items") || field.equals("exclude_items")) {
                if (!BuiltInRegistries.ITEM.getKey(sample.getItem()).equals(id)) return false;
            } else if (sample.getTags().noneMatch(tag -> tag.location().equals(id))) return false;
            changed = BagFilter.add(storage.bagStack(), field, id);
            if (!changed)
                player.displayClientMessage(
                        Component.translatable(
                                "message.packingcube.filter_limit", BagFilter.MAX_RULES),
                        false);
        } else if (operation == FilterActions.REMOVE_RULE) {
            changed = BagFilter.remove(storage.bagStack(), field, id);
        } else return false;
        if (changed) {
            contents.setChanged(); // Also saves a mounted module back into its parent.
            refreshFilterSnapshot();
            broadcastChanges();
        }
        return changed;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().isClientSide || locateRoot(player);
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (!player.level().isClientSide && !stillValid(player)) return;
        if (slotId >= visibleSlotCount()) return;
        super.clicked(slotId, button, type, player);
        if (!player.level().isClientSide && !locateRoot(player)) {
            // Dropped or otherwise outside inventory/cursor: stop editing immediately.
            player.closeContainer();
        }
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != editorState && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if ((!player.level().isClientSide && !stillValid(player))
                || index < 0
                || index >= visibleSlotCount()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < bagSlots) {
            if (!moveItemStackTo(stack, bagSlots, visibleSlotCount(), true)) return ItemStack.EMPTY;
        } else {
            // Vanilla's merge-into-existing-stacks path does not call Slot.mayPlace.
            // Check before attempting any merge so old, now-disallowed contents cannot bypass the
            // filter.
            if (!slots.get(0).mayPlace(stack) || !moveItemStackTo(stack, 0, bagSlots, false))
                return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
