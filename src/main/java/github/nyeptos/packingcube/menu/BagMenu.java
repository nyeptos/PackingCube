package github.nyeptos.packingcube.menu;

import github.nyeptos.packingcube.bag.BagIdentity;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.bag.pickup.BagPickupSettings;
import github.nyeptos.packingcube.bag.pickup.BagRouter;
import github.nyeptos.packingcube.bag.refill.RefillSettings;
import github.nyeptos.packingcube.bag.refill.RefillSlotSettings;
import github.nyeptos.packingcube.bag.refill.RefillTargets;
import github.nyeptos.packingcube.compat.BagEquipment;
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

import java.util.ArrayList;
import java.util.List;
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
    private final boolean refill;
    private final List<BagEquipment.Target> curiosTargets;
    private final List<String> sideIds = new ArrayList<>();
    private int sideStart;
    private static final int[] EQUIPMENT_INDICES = {39, 38, 37, 36, 40};

    public List<BagEquipment.Target> curiosTargets() {
        return curiosTargets;
    }

    public Slot sideSlot(String id) {
        int index = sideIds.indexOf(id);
        return index < 0 ? null : slots.get(sideStart + index);
    }

    public boolean isSideSlot(int index) {
        return index >= sideStart && index < slots.size();
    }

    public static void writeCuriosTargets(
            RegistryFriendlyByteBuf data, List<BagEquipment.Target> targets) {
        data.writeVarInt(targets.size());
        for (var target : targets) {
            data.writeUtf(target.id(), 256);
            data.writeResourceLocation(target.icon());
        }
    }

    private static List<BagEquipment.Target> readCuriosTargets(RegistryFriendlyByteBuf data) {
        int count = data.readVarInt();
        if (count < 0 || count > 4096)
            throw new IllegalArgumentException("Invalid Curios slot count");
        List<BagEquipment.Target> targets = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = data.readUtf(256);
            if (!RefillTargets.valid(id)
                    || !id.startsWith("curios:")
                    || targets.stream().anyMatch(t -> t.id().equals(id)))
                throw new IllegalArgumentException("Invalid Curios slot ID");
            targets.add(new BagEquipment.Target(id, data.readResourceLocation(), ItemStack.EMPTY));
        }
        return List.copyOf(targets);
    }

    // Read-only synchronization slots: settings snapshot and one ghost sample.
    private final SimpleContainer editorState = new SimpleContainer(2);

    public static BagMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        int ownerSlot = data.readVarInt();
        boolean modules = data.readBoolean();
        return new BagMenu(
                id,
                inventory,
                new SimpleContainer(
                        modules ? BagInventory.MODULE_SLOTS : BagInventory.FILTER_SLOTS),
                ItemStack.EMPTY,
                ownerSlot,
                modules,
                data.readBoolean(),
                readCuriosTargets(data),
                true);
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
        this(
                id,
                inventory,
                contents,
                root,
                ownerSlot,
                modules,
                contents instanceof BagInventory storage
                        && storage.bagStack().is(ModItems.REFILL_POUCH.get()));
    }

    private BagMenu(
            int id,
            Inventory inventory,
            Container contents,
            ItemStack root,
            int ownerSlot,
            boolean modules,
            boolean refill) {
        this(
                id,
                inventory,
                contents,
                root,
                ownerSlot,
                modules,
                refill,
                BagEquipment.refillTargets(inventory.player),
                inventory.player.level().isClientSide);
    }

    public BagMenu(
            int id,
            Inventory inventory,
            Container contents,
            ItemStack root,
            int ownerSlot,
            boolean modules,
            boolean refill,
            List<BagEquipment.Target> curiosTargets,
            boolean clientMirror) {
        super(ModMenus.BAG_MENU.get(), id);
        this.curiosTargets = List.copyOf(curiosTargets);
        this.contents = contents;
        this.root = root;
        this.rootId = root.isEmpty() ? null : BagIdentity.renew(root);
        this.rootItem = root.getItem();
        this.rootStorage = contents instanceof BagInventory storage ? storage.rootView() : null;
        this.ownerSlot = ownerSlot;
        this.modules = modules;
        this.refill = refill;
        this.bagSlots = contents.getContainerSize();
        int rows = bagSlots / 9;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(
                        new Slot(contents, column + row * 9, 8 + column * 18, 18 + row * 18) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return BagInventory.accepts(modules, stack)
                                        && (modules
                                                || refill
                                                || BagFilter.matches(filterSnapshot(), stack));
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
        sideStart = slots.size();
        for (int index : EQUIPMENT_INDICES) {
            int vanillaIndex = index < 40 ? 44 - index : 45;
            var vanilla = inventory.player.inventoryMenu.getSlot(vanillaIndex);
            sideIds.add(RefillTargets.inventory(index));
            addSlot(
                    new Slot(inventory, index, -10000, -10000) {
                        @Override
                        public boolean isActive() {
                            return false;
                        }

                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return vanilla.mayPlace(stack);
                        }

                        @Override
                        public boolean mayPickup(Player actor) {
                            return vanilla.mayPickup(actor);
                        }

                        @Override
                        public int getMaxStackSize(ItemStack stack) {
                            return vanilla.getMaxStackSize(stack);
                        }

                        @Override
                        public int getMaxStackSize() {
                            return index < 40 ? 1 : 64;
                        }

                        @Override
                        public void setByPlayer(ItemStack stack, ItemStack oldStack) {
                            vanilla.setByPlayer(stack, oldStack);
                        }
                    });
        }
        for (var target : this.curiosTargets) {
            sideIds.add(target.id());
            Slot slot =
                    clientMirror ? null : BagEquipment.equipmentSlot(inventory.player, target.id());
            if (slot == null)
                slot =
                        new Slot(new SimpleContainer(1), 0, -10000, -10000) {
                            @Override
                            public boolean isActive() {
                                return false;
                            }

                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                var destination =
                                        BagEquipment.refillDestination(
                                                inventory.player, target.id());
                                return destination != null && destination.accepts(stack);
                            }

                            @Override
                            public boolean mayPickup(Player actor) {
                                var destination =
                                        BagEquipment.refillDestination(actor, target.id());
                                return destination != null && destination.removable();
                            }

                            @Override
                            public int getMaxStackSize(ItemStack stack) {
                                var destination =
                                        BagEquipment.refillDestination(
                                                inventory.player, target.id());
                                return destination == null ? 1 : destination.limit(stack);
                            }
                        };
            addSlot(slot);
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

    public boolean isRefill() {
        return refill;
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
                (modules
                                ? ModItems.LARGE_BAG
                                : refill ? ModItems.REFILL_POUCH : ModItems.FILTER_BAG)
                        .get()
                        .getDefaultInstance();
        if (!modules) {
            var data = storage.bagStack().get(DataComponents.CUSTOM_DATA);
            if (data != null) snapshot.set(DataComponents.CUSTOM_DATA, data);
            else snapshot.remove(DataComponents.CUSTOM_DATA);
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
        if (operation == FilterActions.SET_REFILL_SOURCE) {
            if (modules
                    || !refill
                    || sampleSlot < 0
                    || sampleSlot >= 9
                    || (field.equals("destination") && !getCarried().isEmpty())) return false;
            if (field.equals("destination") && !value.isEmpty()) {
                if (!RefillTargets.valid(value)) return false;
                if (value.startsWith("curios:")
                        && BagEquipment.refillTargets(player).stream()
                                .noneMatch(t -> t.id().equals(value))) return false;
            }
            if (!RefillSlotSettings.set(storage.bagStack(), sampleSlot, field, value)) return false;
            contents.setChanged();
            refreshFilterSnapshot();
            broadcastChanges();
            return true;
        }
        if (operation == FilterActions.SET_REFILL_TARGET) {
            if (modules
                    || !refill
                    || sampleSlot != -1
                    || !(field.equals("true") || field.equals("false"))
                    || !getCarried().isEmpty()
                    || !RefillTargets.valid(value)) return false;
            if (value.startsWith("curios:")
                    && BagEquipment.refillTargets(player).stream()
                            .noneMatch(t -> t.id().equals(value))) return false;
            if (!RefillTargets.set(storage.bagStack(), value, Boolean.parseBoolean(field)))
                return false;
            contents.setChanged();
            refreshFilterSnapshot();
            broadcastChanges();
            return true;
        }
        if (operation == FilterActions.SET_REFILL_OPTION) {
            if (modules
                    || !refill
                    || sampleSlot != -1
                    || !RefillSettings.set(storage.bagStack(), field, value)) return false;
            contents.setChanged();
            refreshFilterSnapshot();
            broadcastChanges();
            return true;
        }
        if (operation == FilterActions.SET_HOTBAR_REFILL
                || operation == FilterActions.SET_TRADE_REFILL) {
            boolean trade = operation == FilterActions.SET_TRADE_REFILL;
            if (modules
                    || !refill
                    || sampleSlot != -1
                    || !field.equals(trade ? "trade" : "hotbar")
                    || !(value.equals("true") || value.equals("false"))) return false;
            RefillSettings.setEnabled(storage.bagStack(), trade, Boolean.parseBoolean(value));
            contents.setChanged();
            refreshFilterSnapshot();
            broadcastChanges();
            return true;
        }
        if (operation == FilterActions.SET_PICKUP_ENABLED) {
            if (modules
                    || refill
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

    /**
     * Returns item count moved, or -1 for a rejected/stale request. Never touches hotbar/equipment.
     */
    public int stowAll(Player player, int requestContainerId) {
        if (player.level().isClientSide
                || player.containerMenu != this
                || containerId != requestContainerId
                || !modules
                || player.isSpectator()
                || rootStorage == null
                || contents != rootStorage
                || !root.is(ModItems.LARGE_BAG.get())
                || !getCarried().isEmpty()
                || !stillValid(player)) return -1;
        int moved = 0;
        Inventory inventory = player.getInventory();
        // Reuse the open menu's live parent view, never construct a competing root writer.
        for (int slot = 9; slot < 36; slot++) {
            ItemStack source = inventory.getItem(slot);
            if (source.isEmpty()) continue;
            ItemStack remainder = BagRouter.routeManual(rootStorage, source);
            int amount = source.getCount() - remainder.getCount();
            if (amount <= 0) continue;
            inventory.setItem(slot, remainder);
            moved += amount;
        }
        if (moved > 0) {
            inventory.setChanged();
            broadcastChanges();
        }
        player.displayClientMessage(
                moved == 0
                        ? Component.translatable("message.packingcube.stow_empty")
                        : Component.translatable("message.packingcube.stow_result", moved),
                true);
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().isClientSide
                || (locateRoot(player)
                        && curiosTargets.stream()
                                .map(BagEquipment.Target::id)
                                .toList()
                                .equals(
                                        BagEquipment.refillTargets(player).stream()
                                                .map(BagEquipment.Target::id)
                                                .toList()));
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (!player.level().isClientSide && !stillValid(player)) return;
        if (slotId >= visibleSlotCount() && !isSideSlot(slotId)) return;
        super.clicked(slotId, button, type, player);
        if (!player.level().isClientSide && !locateRoot(player)) {
            // Dropped or otherwise outside inventory/cursor: stop editing immediately.
            player.closeContainer();
        }
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != editorState
                && !isSideSlot(slot.index)
                && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if ((!player.level().isClientSide && !stillValid(player))
                || index < 0
                || (index >= visibleSlotCount() && !isSideSlot(index))) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (isSideSlot(index)) {
            if (!moveItemStackTo(stack, bagSlots, visibleSlotCount(), false))
                return ItemStack.EMPTY;
        } else if (index < bagSlots) {
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
