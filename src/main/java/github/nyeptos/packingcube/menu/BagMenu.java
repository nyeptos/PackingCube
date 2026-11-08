package github.nyeptos.packingcube.menu;

import github.nyeptos.packingcube.bag.BagIdentity;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.BulkStore;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.bag.pickup.BagRouter;
import github.nyeptos.packingcube.bag.refill.RefillTargets;
import github.nyeptos.packingcube.compat.BagEquipment;
import github.nyeptos.packingcube.network.EquipmentTargetCodec;
import github.nyeptos.packingcube.registry.ModItems;
import github.nyeptos.packingcube.registry.ModMenus;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
    private final SimpleContainer bulkInput = new SimpleContainer(1);
    private int bulkInputSlot = -1;
    private final Inventory playerInventory;
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

    // Read-only synchronization slots: settings snapshot and one ghost sample.
    private final SimpleContainer editorState = new SimpleContainer(2);

    public static BagMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        int ownerSlot = data.readVarInt();
        boolean modules = data.readBoolean();
        int slots = data.readVarInt();
        boolean bulk = data.readBoolean();
        return new BagMenu(
                id,
                inventory,
                BagInventory.mirror(slots, bulk),
                ItemStack.EMPTY,
                ownerSlot,
                modules,
                data.readBoolean(),
                EquipmentTargetCodec.read(data),
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
        this.playerInventory = inventory;
        this.contents = contents;
        this.root = root;
        this.rootId = root.isEmpty() ? null : BagIdentity.renew(root);
        this.rootItem = root.getItem();
        this.rootStorage = contents instanceof BagInventory storage ? storage.rootView() : null;
        this.ownerSlot = ownerSlot;
        this.modules = modules;
        this.refill = refill;
        this.bagSlots = contents.getContainerSize();
        // The pouch frame is 132 tall; the bulk pouch has its own 140-tall window with one more row.
        int top = isBulk() ? 56 : 49;
        addBagSlots();
        addPlayerSlots(inventory, top);
        if (isBulk()) addBulkInputSlot();
        addEditorSlots();
        addEquipmentSlots(inventory);
        addCuriosSlots(inventory, clientMirror);
        refreshFilterSnapshot();
    }

    /** The pouch contents. The bulk pouch has one tall slot that hands out a stack at a time. */
    private void addBagSlots() {
        for (int index = 0; index < bagSlots; index++) {
            int column = index % 9;
            int row = index / 9;
            final int slotIndex = index;
            addSlot(
                    new Slot(
                            contents,
                            slotIndex,
                            isBulk() ? 116 : 8 + column * 18,
                            isBulk() ? 17 : 18 + row * 18) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            if (!BagInventory.accepts(modules, stack)) return false;
                            if (contents instanceof BagInventory storage && storage.bulk())
                                return storage.canPlaceItem(slotIndex, stack);
                            if (!modules
                                    && !refill
                                    && !BagFilter.matches(filterSnapshot(), stack)) return false;
                            return true;
                        }

                        @Override
                        public ItemStack remove(int amount) {
                            if (!isBulk()) return super.remove(amount);
                            // The stored amount can pass a stack; a hand can still hold only one.
                            int cap = Math.max(1, getItem().getMaxStackSize());
                            return super.remove(Math.min(amount, cap));
                        }
                    });
        }
    }

    /** The 27 inventory slots and the hotbar, laid out under the pouch. */
    private void addPlayerSlots(Inventory inventory, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addPlayerSlot(inventory, column + row * 9 + 9, 8 + column * 18, top + row * 18);
            }
        }
        for (int column = 0; column < 9; column++)
            addPlayerSlot(inventory, column, 8 + column * 18, top + 58);
    }

    /** The bulk pouch input slot, which takes a whole stack beyond the vanilla limit. */
    private void addBulkInputSlot() {
        bulkInputSlot = slots.size();
        addSlot(new BulkInputSlot(bulkInput, 0, 44, 17));
    }

    /** Two hidden slots that hold the filter editor sample copy. */
    private void addEditorSlots() {
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
    }

    /** The equipment side slots; placement and pickup follow the vanilla inventory slots. */
    private void addEquipmentSlots(Inventory inventory) {
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
    }

    /** The Curios side slots, or a local stand-in when the player has no such slot. */
    private void addCuriosSlots(Inventory inventory, boolean clientMirror) {
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

    /** A bulk pouch keeps one kind of item in a single slot with a large amount. */
    public boolean isBulk() {
        return contents instanceof BagInventory storage && storage.bulk();
    }

    public int bulkAmount() {
        return isBulk() ? contents.getItem(0).getCount() : 0;
    }

    /** Index of the bulk pouch's intake slot, or -1 for every other bag. */
    public int bulkInputSlot() {
        return bulkInputSlot;
    }

    /**
     * Every slot that reacts to hover. The bulk pouch's intake sits outside the 3x9 grid, so it is
     * not part of {@link #visibleSlotCount()} and has to be listed here by hand.
     */
    public List<Integer> hoverSlotIndices() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < visibleSlotCount(); i++) indices.add(i);
        if (bulkInputSlot >= 0) indices.add(bulkInputSlot);
        return indices;
    }

    /** How many items one hand-out takes, as set in the pouch's settings. */
    /** One hand-out, always a single stack: the store never floods the inventory at once. */
    public int bulkTakeAmount() {
        ItemStack stored = contents.getItem(0);
        return stored.isEmpty() ? 64 : Math.max(1, stored.getMaxStackSize());
    }

    /** Merges a stack into the stored count. Returns how many items went in. */
    public int absorbBulk(ItemStack stack) {
        if (!isBulk() || stack.isEmpty()) return 0;
        ItemStack stored = contents.getItem(0);
        if (!stored.isEmpty() && !ItemStack.isSameItemSameComponents(stored, stack)) return 0;
        int held = stored.isEmpty() ? 0 : stored.getCount();
        int moved = Math.min(stack.getCount(), BulkStore.LIMIT - held);
        if (moved <= 0) return 0;
        contents.setItem(0, stack.copyWithCount(held + moved));
        stack.shrink(moved);
        return moved;
    }

    /** Fills matching stacks first, then empty ones, across the inventory and hotbar. */
    private static int fillInventory(Player player, ItemStack out) {
        Inventory inventory = player.getInventory();
        int max = Math.max(1, out.getMaxStackSize());
        int remaining = out.getCount();
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int i = 0; i < 36 && remaining > 0; i++) {
                ItemStack slot = inventory.getItem(i);
                if (pass == 0) {
                    if (slot.isEmpty() || !ItemStack.isSameItemSameComponents(slot, out)) continue;
                    int room = Math.min(max, slot.getMaxStackSize()) - slot.getCount();
                    if (room <= 0) continue;
                    int moved = Math.min(room, remaining);
                    slot.grow(moved);
                    remaining -= moved;
                } else if (slot.isEmpty()) {
                    int moved = Math.min(max, remaining);
                    inventory.setItem(i, out.copyWithCount(moved));
                    remaining -= moved;
                }
            }
        }
        inventory.setChanged();
        return out.getCount() - remaining;
    }

    /**
     * Hands out the chosen amount: inventory and hotbar first, then the ground. The client only
     * sends a number, so the server decides what is actually available.
     */
    public int takeBulk(Player player, int requestContainerId, int amount) {
        if (player.level().isClientSide
                || player.containerMenu != this
                || containerId != requestContainerId
                || !isBulk()
                || player.isSpectator()
                || !getCarried().isEmpty()
                || !stillValid(player)) return 0;
        ItemStack stored = contents.getItem(0);
        if (stored.isEmpty()) return 0;
        int wanted = Math.max(1, Math.min(amount, stored.getCount()));
        int perStack = Math.max(1, stored.getMaxStackSize());
        int handed = 0;
        while (wanted > 0) {
            int chunk = Math.min(wanted, perStack);
            ItemStack out = stored.copyWithCount(chunk);
            int moved = fillInventory(player, out);
            handed += moved;
            int left = chunk - moved;
            if (left > 0) {
                player.drop(out.copyWithCount(left), false);
                handed += left;
            }
            wanted -= chunk;
        }
        stored.shrink(handed);
        if (stored.isEmpty()) contents.setItem(0, ItemStack.EMPTY);
        else contents.setChanged();
        return handed;
    }

    public int visibleSlotCount() {
        return bagSlots + 36;
    }

    public ItemStack filterSnapshot() {
        return editorState.getItem(0);
    }

    /** The handler sees the same flags the menu validated against. */
    boolean modulesOnly() {
        return modules;
    }

    SimpleContainer editorState() {
        return editorState;
    }

    /** Every accepted edit saves the bag, refreshes the synced view and pushes it to the client. */
    void markEdited() {
        contents.setChanged();
        refreshFilterSnapshot();
        broadcastChanges();
    }

    public ItemStack sample() {
        return editorState.getItem(1);
    }

    private void refreshFilterSnapshot() {
        if (!(contents instanceof BagInventory storage)) return;
        ItemStack snapshot =
                (modules
                                ? ModItems.BACKPACK
                                : refill
                                        ? ModItems.REFILL_POUCH
                                        : storage.bagStack().is(ModItems.BULK_POUCH.get())
                                                ? ModItems.BULK_POUCH
                                                : ModItems.STORAGE_POUCH)
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
        return FilterActionHandler.apply(this, player, storage, operation, sampleSlot, field, value);
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
                || !root.is(ModItems.BACKPACK.get())
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
        if (slotId >= visibleSlotCount() && slotId != bulkInputSlot && !isSideSlot(slotId)) return;
        if (isBulk() && slotId == 0 && bulkHandOut(player, type, button)) {
            // Handled here: a left click hands over one item, a right click one stack.
        } else if (isBulk()
                && slotId == 0
                && type == ClickType.SWAP
                && swapOneStackOut(player, button)) {
            // Handled here so the whole stored amount never leaves in one gesture.
        } else {
            super.clicked(slotId, button, type, player);
        }
        if (!player.level().isClientSide && !locateRoot(player)) {
            // Dropped or otherwise outside inventory/cursor: stop editing immediately.
            player.closeContainer();
        }
    }

    /**
     * The output slot's own gestures: a left click hands over one item and a right click one
     * stack. A double click counts as one more left click, so holding the button keeps the pace of
     * the clicks instead of gathering a stack.
     */
    private boolean bulkHandOut(Player player, ClickType type, int button) {
        if (type == ClickType.PICKUP_ALL) return handOutToCursor(1);
        if (type != ClickType.PICKUP || (button != 0 && button != 1)) return false;
        return handOutToCursor(button == 0 ? 1 : 0);
    }

    /**
     * Moves the requested amount to the cursor; zero means one stack. The client runs this too, so
     * the cursor and the stored count follow the click immediately instead of waiting for the
     * round trip.
     */
    private boolean handOutToCursor(int requested) {
        ItemStack stored = contents.getItem(0);
        if (stored.isEmpty()) return true;
        ItemStack carried = getCarried();
        if (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, stored))
            return false;
        int cap = Math.max(1, stored.getMaxStackSize());
        int room = cap - carried.getCount();
        if (room <= 0) return true;
        int wanted = requested <= 0 ? cap : requested;
        int amount = Math.min(wanted, Math.min(room, stored.getCount()));
        if (amount <= 0) return true;
        setCarried(
                carried.isEmpty()
                        ? stored.copyWithCount(amount)
                        : carried.copyWithCount(carried.getCount() + amount));
        stored.shrink(amount);
        if (stored.isEmpty()) contents.setItem(0, ItemStack.EMPTY);
        else contents.setChanged();
        broadcastChanges();
        return true;
    }

    /**
     * A number key or F on the output hands over one stack into that slot. Vanilla would move the
     * whole stored amount and then clear the pouch, which a bulk pouch must never allow.
     */
    private boolean swapOneStackOut(Player player, int button) {
        if (button != Inventory.SLOT_OFFHAND && (button < 0 || button > 8)) return false;
        if (player.level().isClientSide) return true;
        ItemStack stored = contents.getItem(0);
        if (stored.isEmpty()) return true;
        var inventory = player.getInventory();
        if (!inventory.getItem(button).isEmpty()) return true;
        int amount = Math.min(Math.max(1, stored.getMaxStackSize()), stored.getCount());
        inventory.setItem(button, stored.copyWithCount(amount));
        stored.shrink(amount);
        if (stored.isEmpty()) contents.setItem(0, ItemStack.EMPTY);
        else contents.setChanged();
        inventory.setChanged();
        return true;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != editorState
                && !isSideSlot(slot.index)
                && super.canTakeItemForPickAll(stack, slot);
    }

    /**
     * The bulk pouch's intake. It never keeps anything of its own: whatever lands here is merged
     * into the stored count and the slot reads as empty again, so a stack can be poured in over
     * and over instead of being blocked once the stored amount passes a stack.
     */
    private final class BulkInputSlot extends Slot {
        private BulkInputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!BagInventory.accepts(modules, stack)) return false;
            ItemStack stored = contents.getItem(0);
            if (stored.isEmpty())
                return !(contents instanceof BagInventory storage)
                        || storage.canPlaceItem(0, stack);
            return stored.getCount() < BulkStore.LIMIT
                    && ItemStack.isSameItemSameComponents(stored, stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public ItemStack getItem() {
            return ItemStack.EMPTY;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            ItemStack stored = contents.getItem(0);
            int room = BulkStore.LIMIT - (stored.isEmpty() ? 0 : stored.getCount());
            return Math.max(0, Math.min(stack.getMaxStackSize(), room));
        }

        @Override
        public void set(ItemStack stack) {
            absorb(stack);
        }

        @Override
        public void setByPlayer(ItemStack stack) {
            absorb(stack);
        }

        @Override
        public void setByPlayer(ItemStack stack, ItemStack previous) {
            absorb(stack);
        }

        private void absorb(ItemStack stack) {
            if (stack.isEmpty()) return;
            absorbBulk(stack);
            if (stack.isEmpty()) return;
            // Past the limit: hand the rest back rather than lose it.
            ItemStack left = stack.copy();
            stack.setCount(0);
            if (!playerInventory.player.getInventory().add(left))
                playerInventory.player.drop(left, false);
        }
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
            if (isBulk()) {
                // One stack leaves through the take path, so a bulk amount never floods the
                // inventory in a single shift-click.
                takeBulk(player, containerId, bulkTakeAmount());
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(stack, bagSlots, visibleSlotCount(), true)) return ItemStack.EMPTY;
        } else {
            // An empty matching equipment/Curios slot wins over the bag. An occupied slot is never
            // swapped out, so a full equipment set falls through to the normal bag stow.
            equipIntoEmptySideSlot(stack);
            if (!stack.isEmpty() && isBulk()) absorbBulk(stack);
            // Vanilla's merge-into-existing-stacks path does not call Slot.mayPlace.
            // Check before attempting any merge so old, now-disallowed contents cannot bypass the
            // filter.
            if (!stack.isEmpty() && !isBulk() && slots.get(0).mayPlace(stack))
                moveItemStackTo(stack, 0, bagSlots, false);
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    /**
     * Shift-click fills an empty equipment or Curios slot before the bag takes the stack. Side
     * slots sit off-screen, so {@code isActive} stays false and the vanilla/Curios
     * {@code mayPlace} decides whether the stack belongs there. Existing equipment is never
     * removed or swapped, and the offhand stays a manual choice.
     */
    private void equipIntoEmptySideSlot(ItemStack stack) {
        for (int index = sideStart; index < slots.size(); index++) {
            Slot slot = slots.get(index);
            if (slot.hasItem() || !slot.mayPlace(stack)) continue;
            if (RefillTargets.inventory(40).equals(sideIds.get(index - sideStart))) continue;
            if (moveItemStackTo(stack, index, index + 1, false)) return;
        }
    }
}
