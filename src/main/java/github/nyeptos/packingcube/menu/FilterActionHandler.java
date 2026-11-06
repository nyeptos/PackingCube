package github.nyeptos.packingcube.menu;

import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.bag.pickup.BagPickupSettings;
import github.nyeptos.packingcube.bag.refill.RefillPouchSettings;
import github.nyeptos.packingcube.bag.refill.RefillSlotDestinations;
import github.nyeptos.packingcube.bag.refill.RefillTargets;
import github.nyeptos.packingcube.compat.BagEquipment;
import github.nyeptos.packingcube.network.FilterActions;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Applies one filter-action operation to the open bag menu. The menu validates the request, this
 * class decides what the operation changes. Every accepted operation ends in {@link BagMenu#markEdited()}.
 */
final class FilterActionHandler {
    private FilterActionHandler() {}

    static boolean apply(
            BagMenu menu,
            Player player,
            BagInventory storage,
            int operation,
            int sampleSlot,
            String field,
            String value) {
        if (operation == FilterActions.SET_REFILL_SOURCE)
            return setRefillSource(menu, player, storage, sampleSlot, field, value);
        if (operation == FilterActions.SET_REFILL_SLOT_LOCK)
            return setRefillSlotLock(menu, storage, sampleSlot, field, value);
        if (operation == FilterActions.SET_REFILL_TARGET)
            return setRefillTarget(menu, player, storage, sampleSlot, field, value);
        if (operation == FilterActions.SET_REFILL_OPTION)
            return setRefillOption(menu, storage, sampleSlot, field, value);
        if (operation == FilterActions.SET_HOTBAR_REFILL
                || operation == FilterActions.SET_TRADE_REFILL)
            return setRefillEnabled(menu, storage, operation, sampleSlot, field, value);
        if (operation == FilterActions.SET_PICKUP_ENABLED)
            return setPickupEnabled(menu, storage, sampleSlot, field, value);
        if (operation == FilterActions.SET_PICKUP_PRIORITY)
            return setPickupPriority(menu, storage, sampleSlot, field, value);
        if (menu.modulesOnly()) return false;
        if (operation == FilterActions.SAMPLE_CURSOR)
            return sampleFromCursor(menu, field, value);
        if (operation == FilterActions.SAMPLE_SLOT) return sampleFromSlot(menu, sampleSlot);
        if (operation == FilterActions.SET_MODE)
            return setFilterMode(menu, storage, field, value);
        return editRule(menu, player, storage, operation, field, value);
    }

    /** Points one of the nine source slots at a destination. */
    private static boolean setRefillSource(
            BagMenu menu, Player player, BagInventory storage, int sampleSlot,
            String field, String value) {
            if (menu.modulesOnly()
                    || !menu.isRefill()
                    || sampleSlot < 0
                    || sampleSlot >= 9
                    || (field.equals("destination") && !menu.getCarried().isEmpty())) return false;
            if (field.equals("destination") && !value.isEmpty()) {
                if (!RefillTargets.valid(value)) return false;
                if (value.startsWith("curios:")
                        && BagEquipment.refillTargets(player).stream()
                                .noneMatch(t -> t.id().equals(value))) return false;
            }
            if (!RefillSlotDestinations.set(storage.bagStack(), sampleSlot, field, value)) return false;
            menu.markEdited();
            return true;
    }

    /** Locks or unlocks a collected slot so refill leaves it alone. */
    private static boolean setRefillSlotLock(
            BagMenu menu, BagInventory storage, int sampleSlot, String field, String value) {
            if (menu.modulesOnly()
                    || !menu.isRefill()
                    || sampleSlot < 0
                    || sampleSlot >= RefillPouchSettings.LOCK_SLOTS
                    || !field.equals("locked")
                    || !(value.equals("true") || value.equals("false"))
                    || !menu.getCarried().isEmpty()) return false;
            RefillPouchSettings.setLocked(
                    storage.bagStack(), sampleSlot, Boolean.parseBoolean(value));
            menu.markEdited();
            return true;
    }

    /** Turns a refill target (bag or equipment) on or off. */
    private static boolean setRefillTarget(
            BagMenu menu, Player player, BagInventory storage, int sampleSlot,
            String field, String value) {
            if (menu.modulesOnly()
                    || !menu.isRefill()
                    || sampleSlot != -1
                    || !(field.equals("true") || field.equals("false"))
                    || !menu.getCarried().isEmpty()
                    || !RefillTargets.valid(value)) return false;
            if (value.startsWith("curios:")
                    && BagEquipment.refillTargets(player).stream()
                            .noneMatch(t -> t.id().equals(value))) return false;
            if (!RefillTargets.set(storage.bagStack(), value, Boolean.parseBoolean(field)))
                return false;
            menu.markEdited();
            return true;
    }

    /** One pouch-wide refill option such as the exchange limit. */
    private static boolean setRefillOption(
            BagMenu menu, BagInventory storage, int sampleSlot, String field, String value) {
            if (menu.modulesOnly()
                    || !menu.isRefill()
                    || sampleSlot != -1
                    || !RefillPouchSettings.set(storage.bagStack(), field, value)) return false;
            menu.markEdited();
            return true;
    }

    /** The hotbar and trade refill switches. */
    private static boolean setRefillEnabled(
            BagMenu menu, BagInventory storage, int operation, int sampleSlot,
            String field, String value) {
            boolean trade = operation == FilterActions.SET_TRADE_REFILL;
            if (menu.modulesOnly()
                    || !menu.isRefill()
                    || sampleSlot != -1
                    || !field.equals(trade ? "trade" : "hotbar")
                    || !(value.equals("true") || value.equals("false"))) return false;
            RefillPouchSettings.setEnabled(storage.bagStack(), trade, Boolean.parseBoolean(value));
            menu.markEdited();
            return true;
    }

    /** The automatic pickup switch, which only the big bag has. */
    private static boolean setPickupEnabled(
            BagMenu menu, BagInventory storage, int sampleSlot, String field, String value) {
            if (menu.modulesOnly()
                    || menu.isRefill()
                    || sampleSlot != -1
                    || !field.equals("pickup")
                    || !(value.equals("true") || value.equals("false"))) return false;
            BagPickupSettings.setEnabled(storage.bagStack(), Boolean.parseBoolean(value));
            menu.markEdited();
            return true;
    }

    /** The pickup priority, 0 to 9. */
    private static boolean setPickupPriority(
            BagMenu menu, BagInventory storage, int sampleSlot, String field, String value) {
            if (menu.modulesOnly() || sampleSlot != -1 || !field.equals("priority") || !value.matches("[0-9]"))
                return false;
            BagPickupSettings.setPriority(storage.bagStack(), Integer.parseInt(value));
            menu.markEdited();
            return true;
    }

    /** Copies the carried stack into the sample slot, or clears it. */
    private static boolean sampleFromCursor(BagMenu menu, String field, String value) {
            if (!field.isEmpty() || !(value.equals("copy") || value.equals("clear"))) return false;
            ItemStack source = value.equals("clear") ? ItemStack.EMPTY : menu.getCarried();
            if (BagInventory.isBag(source)) return false;
            // A ghost reference only. Never split, shrink, insert, return or drop the source stack.
            menu.editorState().setItem(1, source.isEmpty() ? ItemStack.EMPTY : source.copyWithCount(1));
            menu.broadcastChanges();
            return true;
    }

    /** Copies a visible bag slot into the sample slot. */
    private static boolean sampleFromSlot(BagMenu menu, int sampleSlot) {
            if (sampleSlot < 0 || sampleSlot >= menu.visibleSlotCount()) return false;
            ItemStack actual = menu.slots.get(sampleSlot).getItem();
            if (BagInventory.isBag(actual)) return false;
            menu.editorState().setItem(1, actual.isEmpty() ? ItemStack.EMPTY : actual.copyWithCount(1));
            menu.broadcastChanges();
            return true;
    }

    /** Switches the filter between include and exclude. */
    private static boolean setFilterMode(
            BagMenu menu, BagInventory storage, String field, String value) {
            if (!field.equals("mode") || !(value.equals("include") || value.equals("exclude")))
                return false;
            BagFilter.setExcluding(storage.bagStack(), value.equals("exclude"));
            menu.markEdited();
            return true;
    }

    /** Adds or removes one rule. The field and the value must name a real item or tag. */
    private static boolean editRule(
            BagMenu menu,
            Player player,
            BagInventory storage,
            int operation,
            String field,
            String value) {
        if (!BagFilter.FIELDS.contains(field)) return false;
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) return false;
        boolean changed;
        if (operation == FilterActions.ADD_RULE) {
            ItemStack sample = menu.sample();
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
        if (changed) menu.markEdited(); // Also saves a mounted module back into its parent.
        return changed;
    }
}
