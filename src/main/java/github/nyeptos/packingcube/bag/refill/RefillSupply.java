package github.nyeptos.packingcube.bag.refill;

import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.pickup.BagPickupSettings;
import github.nyeptos.packingcube.compat.BagEquipment;
import github.nyeptos.packingcube.menu.BagRootMenu;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;

/** Short-lived server transaction: one parent view per backpack, exact component matching. */
public final class RefillSupply {
    public record Withdrawal(BagInventory pouch, int slot, int count) {}

    private RefillSupply() {}

    public static List<BagInventory> collect(Player player, boolean trade) {
        return collect(player, trade ? -1 : player.getInventory().selected);
    }

    public static List<BagInventory> collect(Player player, int targetSlot) {
        List<BagInventory> result = new ArrayList<>();
        var seen = Collections.newSetFromMap(new IdentityHashMap<ItemStack, Boolean>());
        List<ItemStack> roots = new ArrayList<>(BagEquipment.activeBackBags(player));
        for (int i = 0; i < 36; i++) roots.add(player.getInventory().getItem(i));
        for (var root : roots) {
            if (!root.is(ModItems.LARGE_BAG.get()) || root.getCount() != 1 || !seen.add(root))
                continue;
            if (player.containerMenu instanceof BagRootMenu menu && menu.ownsRoot(root)) continue;
            var parent = new BagInventory(root, true, player.getInventory()::setChanged);
            for (int slot = 0; slot < parent.getContainerSize(); slot++) {
                var pouch = parent.getItem(slot);
                if (pouch.is(ModItems.REFILL_POUCH.get())
                        && pouch.getCount() == 1
                        && (targetSlot == -2
                                || (targetSlot == -1
                                        ? RefillSettings.enabled(pouch, true)
                                        : !RefillSlotSettings.mapped(pouch)
                                                && RefillSettings.suppliesSlot(pouch, targetSlot))))
                    result.add(new BagInventory(pouch, false, parent));
            }
        }
        result.sort(
                Comparator.comparingInt(
                                (BagInventory pouch) ->
                                        BagPickupSettings.priority(pouch.bagStack()))
                        .reversed());
        return result;
    }

    public static int available(List<BagInventory> supplies, ItemStack template) {
        int total = 0;
        for (var pouch : supplies)
            if (RefillSettings.allows(pouch.bagStack(), template))
                for (int slot : sourceOrder(pouch)) {
                    if (RefillSlotSettings.mapped(pouch.bagStack())
                            && !RefillSlotSettings.get(pouch.bagStack(), slot).enabled()) continue;
                    var stack = pouch.getItem(slot);
                    if (ItemStack.isSameItemSameComponents(stack, template))
                        total += stack.getCount();
                }
        return total;
    }

    /**
     * Reservation does not mutate anything. Shared plan prevents double-reserving identical costs.
     */
    public static boolean reserve(
            List<BagInventory> supplies, ItemStack template, int amount, List<Withdrawal> plan) {
        int remaining = amount;
        for (var pouch : supplies)
            if (RefillSettings.allows(pouch.bagStack(), template))
                for (int slot : sourceOrder(pouch)) {
                    if (remaining <= 0) break;
                    if (RefillSlotSettings.mapped(pouch.bagStack())
                            && !RefillSlotSettings.get(pouch.bagStack(), slot).enabled()) continue;
                    var stack = pouch.getItem(slot);
                    if (!ItemStack.isSameItemSameComponents(stack, template)) continue;
                    int reserved = 0;
                    for (var prior : plan)
                        if (prior.pouch() == pouch && prior.slot() == slot)
                            reserved += prior.count();
                    int taken = Math.min(remaining, stack.getCount() - reserved);
                    if (taken > 0) {
                        plan.add(new Withdrawal(pouch, slot, taken));
                        remaining -= taken;
                    }
                }
        return remaining == 0;
    }

    private static List<Integer> sourceOrder(BagInventory pouch) {
        var slots = new ArrayList<Integer>();
        for (int i = 0; i < pouch.getContainerSize(); i++) slots.add(i);
        if (RefillSlotSettings.mapped(pouch.bagStack()))
            slots.sort(
                    Comparator.comparingInt(
                                    (Integer i) ->
                                            RefillSlotSettings.get(pouch.bagStack(), i).priority())
                            .reversed());
        return slots;
    }

    public static void commit(List<Withdrawal> plan) {
        for (var entry : plan) entry.pouch().removeItem(entry.slot(), entry.count());
    }
}
