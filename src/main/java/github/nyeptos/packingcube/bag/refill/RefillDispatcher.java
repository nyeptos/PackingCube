package github.nyeptos.packingcube.bag.refill;

import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.menu.BagMenu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Server-thread, deterministic per-source dispatch with a single winner per destination. */
public final class RefillDispatcher {
    private record Candidate(BagInventory pouch, int source, RefillSlotDestinations.Rule rule) {}

    private record ReturnSlot(int index, ItemStack stack) {}

    private RefillDispatcher() {}

    public static int tick(Player player) {
        if (player.level().isClientSide
                || !player.isAlive()
                || player.isCreative()
                || player.isSpectator()
                || !menuAllowsRefill(player)
                || !player.containerMenu.getCarried().isEmpty()) return 0;
        List<Candidate> candidates = new ArrayList<>();
        for (var pouch : RefillSupply.collect(player, -2)) {
            if (!RefillSlotDestinations.mapped(pouch.bagStack())) continue;
            for (int i = 0; i < RefillSlotDestinations.COUNT; i++) {
                var rule = RefillSlotDestinations.get(pouch.bagStack(), i);
                if (rule.enabled() && !rule.destination().isEmpty())
                    candidates.add(new Candidate(pouch, i, rule));
            }
        }
        // List.sort is stable: after source priority, existing pouch priority/order, then source
        // 1..9.
        candidates.sort(Comparator.comparingInt((Candidate c) -> c.rule().priority()).reversed());
        Set<String> protectedSlots = new HashSet<>(), claimed = new HashSet<>();
        for (var c : candidates) protectedSlots.add(c.rule().destination());
        int total = 0;
        for (var c : candidates) {
            total += supplyOne(player, c, candidates, protectedSlots, claimed);
        }
        if (total > 0) {
            player.getInventory().setChanged();
            if (player.containerMenu == player.inventoryMenu)
                player.containerMenu.broadcastChanges();
            // The bag screen shows the same slots, so resend it whole instead of relying on the
            // inventory menu's delta. The open backpack is never a source (see RefillSupply).
            else player.containerMenu.broadcastFullState();
        }
        return total;
    }

    /**
     * Supplies one destination from one pouch slot and returns how many items moved. A
     * destination that is already claimed, or that another rule holds back, is claimed here so a
     * lower-priority rule cannot ping-pong it on the next tick.
     */
    private static int supplyOne(
            Player player,
            Candidate candidate,
            List<Candidate> candidates,
            Set<String> protectedSlots,
            Set<String> claimed) {
            String id = candidate.rule().destination();
        if (claimed.contains(id)) return 0;
        // A returned item keeps its slot out of supply until the player empties that slot.
        if (RefillPouchSettings.locked(candidate.pouch().bagStack(), candidate.source())) return 0;
        ItemStack source = candidate.pouch().getItem(candidate.source()).copy();
        if (source.isEmpty()
                || !BagInventory.accepts(false, source)
                || !RefillPouchSettings.allows(candidate.pouch().bagStack(), source)) return 0;
        var destination = RefillDestination.resolve(player, id);
        if (destination == null || !destination.accepts(source)) return 0;
        ItemStack old = destination.read().copy();
        // Never displace a backpack/pouch (which may be the live source root).
        if (BagInventory.isBag(old)) return 0;
        boolean same = !old.isEmpty() && ItemStack.isSameItemSameComponents(source, old);
        boolean replacing = !old.isEmpty() && !same;
        if (replacing && (!candidate.rule().replace() || !destination.removable())) return 0;
        // Several rules can share one destination, so the strictest wear limit wins: a rule
        // that asks to wait holds the destination instead of letting another rule swap in.
        if (replacing && waitsForWear(candidates, id, old)) {
            claimed.add(id);
            return 0;
        }
        int existing = same ? old.getCount() : 0;
        int maximum = Math.min(source.getMaxStackSize(), destination.limit(source));
        int desired = candidate.rule().desired(existing, maximum);
        // A stocked higher-priority rule also owns an already-satisfied destination.
        // Lower-priority replacement must not ping-pong it on the next tick.
        if (desired <= existing) {
            claimed.add(id);
            return 0;
        }
        int added = Math.min(source.getCount(), desired - existing);
        if (added <= 0) return 0;
        // Collect into the pouch when allowed, otherwise fall back to the inventory.
        ReturnSlot collected =
                replacing && candidate.rule().collect()
                        ? planCollect(candidate.pouch(), candidate.source(), old, added)
                        : null;
        var returns =
                replacing && collected == null
                        ? planReturn(player, old, protectedSlots)
                        : List.<ReturnSlot>of();
        if (replacing && collected == null && returns == null)
            return 0; // No capacity: neither side changes; never drop anything.
        if (!ItemStack.matches(source, candidate.pouch().getItem(candidate.source()))) return 0;
        if (!destination.write(old, source.copyWithCount(existing + added))) return 0;
        candidate.pouch().removeItem(candidate.source(), added);
        if (collected != null) {
            // Lock first: the slot write also saves the pouch, so both land in one write.
            if (candidate.rule().lock())
                RefillPouchSettings.setLocked(
                        candidate.pouch().bagStack(), collected.index(), true);
            candidate.pouch().setItem(collected.index(), collected.stack());
        }
        for (var back : returns) player.getInventory().setItem(back.index(), back.stack());
        player.getInventory().setChanged();
        claimed.add(id);
        return added;
    }

    /**
     * The pouch settings screen is part of the mod's own bag menu, so rule-based refill keeps
     * running there. Chests, trading and every other container still pause it.
     */
    private static boolean menuAllowsRefill(Player player) {
        return player.containerMenu == player.inventoryMenu
                || player.containerMenu instanceof BagMenu;
    }

    /**
     * True when any rule aimed at this destination wants to keep the item in place. Rules that
     * supply another item, or that do not replace at all, never hold a destination back.
     */
    private static boolean waitsForWear(
            List<Candidate> candidates, String destination, ItemStack old) {
        for (var c : candidates) {
            if (!c.rule().destination().equals(destination) || !c.rule().replace()) continue;
            var source = c.pouch().getItem(c.source());
            if (!source.isEmpty() && !wornEnough(source, old, c.rule())) return true;
        }
        return false;
    }

    /**
     * Same-item swaps can wait until the item in place is nearly spent. Other items are never
     * delayed, so a plain replacement keeps its old behaviour.
     */
    static boolean wornEnough(
            ItemStack source, ItemStack old, RefillSlotDestinations.Rule rule) {
        if (rule.durabilityOff() || !ItemStack.isSameItem(source, old)) return true;
        int max = old.getMaxDamage();
        if (!old.isDamageableItem() || max <= 0) return true;
        int remaining = max - old.getDamageValue();
        int measured = rule.durabilityMode().equals("percent")
                ? (int) ((long) remaining * 100 / max)
                : remaining;
        return measured <= rule.durability();
    }

    /**
     * Slot the replaced item returns to, computed on the pouch contents after this removal. The
     * whole stack must fit one slot, so a returned item is never split or duplicated.
     */
    private static ReturnSlot planCollect(
            BagInventory pouch, int source, ItemStack old, int removed) {
        ItemStack left = pouch.getItem(source);
        ItemStack freed = left.copyWithCount(left.getCount() - removed);
        for (int pass = 0; pass < 2; pass++)
            for (int i = 0; i < pouch.getContainerSize(); i++) {
                ItemStack current = i == source ? freed : pouch.getItem(i);
                if (pass == 0
                        ? current.isEmpty() || !ItemStack.isSameItemSameComponents(current, old)
                        : !current.isEmpty()) continue;
                if (old.getCount() > current.getMaxStackSize() - current.getCount()) continue;
                return new ReturnSlot(i, old.copyWithCount(current.getCount() + old.getCount()));
            }
        return null;
    }

    /** Reserve return capacity outside every active destination, using copies only. */
    private static List<ReturnSlot> planReturn(
            Player player, ItemStack old, Set<String> protectedSlots) {
        ItemStack remaining = old.copy();
        List<ReturnSlot> plan = new ArrayList<>();
        for (int pass = 0; pass < 2; pass++)
            for (int i = 0; i < 36 && !remaining.isEmpty(); i++) {
                if (protectedSlots.contains(RefillTargets.inventory(i))) continue;
                ItemStack current = player.getInventory().getItem(i);
                if (pass == 0
                        ? current.isEmpty()
                                || !ItemStack.isSameItemSameComponents(current, remaining)
                        : !current.isEmpty()) continue;
                int maximum =
                        Math.min(
                                remaining.getMaxStackSize(),
                                player.getInventory().getMaxStackSize());
                int take = Math.min(remaining.getCount(), maximum - current.getCount());
                if (take <= 0) continue;
                plan.add(new ReturnSlot(i, remaining.copyWithCount(current.getCount() + take)));
                remaining.shrink(take);
            }
        return remaining.isEmpty() ? plan : null;
    }
}
