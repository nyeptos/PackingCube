package github.nyeptos.packingcube.bag.refill;

import github.nyeptos.packingcube.bag.BagInventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

/** Server-only supply, never a purchase, sale, inventory-wide refill, or output-slot click. */
public final class RefillService {
    private RefillService() {}

    /** Compatibility entry point for the selected hotbar; offhand uses hand(). */
    public static int hotbar(Player player, int slot, ItemStack before) {
        return hand(player, slot, before);
    }

    public static int hand(Player player, int slot, ItemStack before) {
        boolean offhand = slot == RefillSettings.OFFHAND_SLOT;
        if (player.level().isClientSide
                || !player.isAlive()
                || player.isSpectator()
                || player.isCreative()
                || player.containerMenu != player.inventoryMenu
                || !player.containerMenu.getCarried().isEmpty()
                || (!offhand && (slot < 0 || slot > 8 || player.getInventory().selected != slot))
                || before.isEmpty()
                || BagInventory.isBag(before)) return 0;
        var current = player.getInventory().getItem(slot);
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, before)) return 0;
        int original = current.getCount();
        // Actual consumption must have happened; never undo a plain move or fill from a click
        // alone.
        if (original >= before.getCount()) return 0;
        int maximum = Math.min(before.getMaxStackSize(), player.getInventory().getMaxStackSize());
        var plan = new ArrayList<RefillSupply.Withdrawal>();
        int added = 0, ceiling = maximum;
        boolean policyChosen = false;
        for (var pouch : RefillSupply.collect(player, slot)) {
            int desired =
                    Math.min(ceiling, RefillSettings.desired(pouch.bagStack(), original, maximum));
            int available = RefillSupply.available(java.util.List.of(pouch), before);
            int take = Math.min(available, desired - original - added);
            if (take <= 0) continue;
            // The first eligible stocked pouch sets the ceiling; fallbacks cannot exceed it.
            if (!policyChosen) {
                ceiling = desired;
                policyChosen = true;
            }
            if (RefillSupply.reserve(java.util.List.of(pouch), before, take, plan)) added += take;
            if (original + added >= ceiling) break;
        }
        if (added == 0) return 0;
        RefillSupply.commit(plan);
        player.getInventory().setItem(slot, before.copyWithCount(original + added));
        player.getInventory().setChanged();
        // The client predicts item consumption. If refill restores exactly the last sent count,
        // ordinary delta sync sees no change and leaves the client with its predicted empty/reduced
        // hand. Seed only this slot's remote baseline with the consumed state so vanilla sends an
        // authoritative correction (and manages state IDs), without a full-inventory resend.
        int menuSlot =
                offhand
                        ? net.minecraft.world.inventory.InventoryMenu.SHIELD_SLOT
                        : net.minecraft.world.inventory.InventoryMenu.USE_ROW_SLOT_START + slot;
        player.inventoryMenu.setRemoteSlot(menuSlot, current);
        player.inventoryMenu.broadcastChanges();
        return added;
    }

    private static int needed(ItemStack current, ItemStack cost) {
        if (cost.isEmpty()) return current.isEmpty() ? 0 : -1;
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, cost)) return -1;
        return Math.max(0, cost.getCount() - current.getCount());
    }

    public static int trade(Player player) {
        if (player.level().isClientSide
                || !player.isAlive()
                || player.isSpectator()
                || !(player.containerMenu instanceof MerchantMenu menu)
                || !menu.stillValid(player)) return 0;
        int selected = menu.tradeContainer.selectionHint;
        var offers = menu.getOffers();
        if (selected < 0 || selected >= offers.size()) return 0;
        var offer = offers.get(selected);
        if (offer.isOutOfStock()) return 0;
        var a = offer.getCostA();
        var b = offer.getCostB();
        if (a.isEmpty()
                || a.getCount() > menu.getSlot(0).getMaxStackSize(a)
                || (!b.isEmpty() && b.getCount() > menu.getSlot(1).getMaxStackSize(b))) return 0;
        var currentA = menu.getSlot(0).getItem();
        var currentB = menu.getSlot(1).getItem();
        int needA = needed(currentA, a), needB = needed(currentB, b);
        if (needA < 0 || needB < 0 || needA + needB == 0) return 0;
        var supplies = RefillSupply.collect(player, true);
        var plan = new ArrayList<RefillSupply.Withdrawal>();
        // Both inputs must be available before either is removed from a pouch.
        if (!RefillSupply.reserve(supplies, a, needA, plan)
                || !RefillSupply.reserve(supplies, b, needB, plan)) return 0;
        RefillSupply.commit(plan);
        if (needA > 0) menu.getSlot(0).set(a.copyWithCount(currentA.getCount() + needA));
        if (needB > 0) menu.getSlot(1).set(b.copyWithCount(currentB.getCount() + needB));
        menu.broadcastChanges();
        return needA + needB;
    }
}
