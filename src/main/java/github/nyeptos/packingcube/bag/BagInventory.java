package github.nyeptos.packingcube.bag;

import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/** A short-lived view. Do not keep two live views of the same bag. */
public final class BagInventory extends SimpleContainer {
    public static final int MODULE_SLOTS = 9;
    public static final int FILTER_SLOTS = 9;
    private ItemStack bag;
    private BagInventory parent;
    private final boolean modules;
    private final Runnable onSave;
    private boolean ready;
    private final List<ItemStack> legacyOverflow = new ArrayList<>();

    public BagInventory(ItemStack bag, boolean modules, Runnable onSave) {
        super(modules ? MODULE_SLOTS : FILTER_SLOTS);
        this.bag = bag;
        this.modules = modules;
        this.onSave = onSave;
        ItemContainerContents contents =
                bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        List<ItemStack> saved = contents.stream().toList();
        for (int i = 0; i < Math.min(saved.size(), getContainerSize()); i++) {
            super.setItem(i, saved.get(i).copy());
        }
        // Retain slots from the old 27-slot version until the player can recover them.
        for (int i = getContainerSize(); i < saved.size(); i++)
            legacyOverflow.add(saved.get(i).copy());
        ready = true;
    }

    public BagInventory(ItemStack bag, boolean modules, BagInventory parent) {
        this(bag, modules, parent::setChanged);
        this.parent = parent;
    }

    public BagInventory rootView() {
        return parent == null ? this : parent.rootView();
    }

    /** Rebind the live view after vanilla moves the bag by copying its ItemStack. */
    public void rebind(ItemStack movedBag) {
        bag = movedBag;
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return accepts(modules, stack)
                && (modules
                        || bag.is(ModItems.REFILL_POUCH.get())
                        || BagFilter.matches(bag, stack));
    }

    /** Shared by the server inventory and both client/server menu slots. */
    public static boolean accepts(boolean modules, ItemStack stack) {
        if (modules) return isPouch(stack);
        // No bags, shulker boxes, bundles or other non-nestable containers.
        return !isBag(stack) && stack.getItem().canFitInsideContainerItems();
    }

    public ItemStack bagStack() {
        return bag;
    }

    public static boolean isPouch(ItemStack stack) {
        return stack.is(ModItems.FILTER_BAG.get()) || stack.is(ModItems.REFILL_POUCH.get());
    }

    public static boolean isBag(ItemStack stack) {
        return stack.is(ModItems.LARGE_BAG.get()) || isPouch(stack);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!ready) return;
        List<ItemStack> stacks = new ArrayList<>(getContainerSize());
        for (int i = 0; i < getContainerSize(); i++) stacks.add(getItem(i).copy());
        for (ItemStack extra : legacyOverflow) stacks.add(extra.copy());
        bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks));
        onSave.run();
    }

    /**
     * Recover old slots into player inventory. If full, preserve the remainder for the next
     * opening.
     */
    public void recoverLegacyOverflow(net.minecraft.world.entity.player.Player player) {
        if (legacyOverflow.isEmpty()) return;
        int recovered = 0;
        int retained = 0;
        for (ItemStack extra : legacyOverflow) {
            int before = extra.getCount();
            moveIntoPlayerInventory(player, extra);
            recovered += before - extra.getCount();
            retained += extra.getCount();
        }
        if (retained == 0) legacyOverflow.clear();
        setChanged();
        if (recovered > 0 || retained > 0) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.packingcube.legacy_overflow", recovered, retained),
                    false);
        }
    }

    private static void moveIntoPlayerInventory(
            net.minecraft.world.entity.player.Player player, ItemStack extra) {
        // Inventory.add intentionally deletes overflow in creative mode; migration must never do
        // that.
        var inventory = player.getInventory();
        for (int pass = 0; pass < 2 && !extra.isEmpty(); pass++) {
            for (int i = 0; i < 36 && !extra.isEmpty(); i++) {
                ItemStack existing = inventory.getItem(i);
                if (pass == 0
                        && !existing.isEmpty()
                        && ItemStack.isSameItemSameComponents(existing, extra)) {
                    int moved =
                            Math.min(
                                    extra.getCount(),
                                    Math.min(
                                                    inventory.getMaxStackSize(),
                                                    existing.getMaxStackSize())
                                            - existing.getCount());
                    if (moved > 0) {
                        existing.grow(moved);
                        extra.shrink(moved);
                    }
                } else if (pass == 1 && existing.isEmpty()) {
                    int moved =
                            Math.min(
                                    extra.getCount(),
                                    Math.min(inventory.getMaxStackSize(), extra.getMaxStackSize()));
                    inventory.setItem(i, extra.copyWithCount(moved));
                    extra.shrink(moved);
                }
            }
        }
        inventory.setChanged();
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = super.removeItemNoUpdate(slot);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    /** Returns a copy of the remainder; never mutates the supplied stack. */
    public ItemStack insert(ItemStack incoming) {
        ItemStack remainder = incoming.copy();
        if (remainder.isEmpty() || !canPlaceItem(0, remainder)) return remainder;
        // Fill existing stacks first, then empty slots.
        for (int pass = 0; pass < 2 && !remainder.isEmpty(); pass++) {
            for (int i = 0; i < getContainerSize() && !remainder.isEmpty(); i++) {
                ItemStack existing = getItem(i);
                if (pass == 0
                        && !existing.isEmpty()
                        && ItemStack.isSameItemSameComponents(existing, remainder)) {
                    int moved =
                            Math.min(
                                    remainder.getCount(),
                                    Math.min(getMaxStackSize(), existing.getMaxStackSize())
                                            - existing.getCount());
                    if (moved > 0) {
                        existing.grow(moved);
                        remainder.shrink(moved);
                        setChanged();
                    }
                } else if (pass == 1 && existing.isEmpty()) {
                    int moved =
                            Math.min(
                                    remainder.getCount(),
                                    Math.min(getMaxStackSize(), remainder.getMaxStackSize()));
                    setItem(i, remainder.copyWithCount(moved));
                    remainder.shrink(moved);
                }
            }
        }
        return remainder;
    }
}
