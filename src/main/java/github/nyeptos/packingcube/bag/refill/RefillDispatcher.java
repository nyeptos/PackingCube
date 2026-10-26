package github.nyeptos.packingcube.bag.refill;

import github.nyeptos.packingcube.bag.BagInventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Server-thread, deterministic per-source dispatch with a single winner per destination. */
public final class RefillDispatcher {
    private record Candidate(BagInventory pouch, int source, RefillSlotSettings.Rule rule) {}

    private record ReturnSlot(int index, ItemStack stack) {}

    private RefillDispatcher() {}

    public static int tick(Player player, Set<String> consumed) {
        if (player.level().isClientSide
                || !player.isAlive()
                || player.isCreative()
                || player.isSpectator()
                || player.containerMenu != player.inventoryMenu
                || !player.containerMenu.getCarried().isEmpty()) return 0;
        List<Candidate> candidates = new ArrayList<>();
        for (var pouch : RefillSupply.collect(player, -2)) {
            if (!RefillSlotSettings.mapped(pouch.bagStack())) continue;
            for (int i = 0; i < RefillSlotSettings.COUNT; i++) {
                var rule = RefillSlotSettings.get(pouch.bagStack(), i);
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
            String id = c.rule().destination();
            if (claimed.contains(id)) continue;
            ItemStack source = c.pouch().getItem(c.source()).copy();
            if (source.isEmpty()
                    || !BagInventory.accepts(false, source)
                    || !RefillSettings.allows(c.pouch().bagStack(), source)) continue;
            var destination = RefillDestination.resolve(player, id);
            if (destination == null || !destination.accepts(source)) continue;
            ItemStack old = destination.read().copy();
            // Never displace a backpack/pouch (which may be the live source root).
            if (BagInventory.isBag(old)) continue;
            boolean same = !old.isEmpty() && ItemStack.isSameItemSameComponents(source, old);
            boolean replacing = !old.isEmpty() && !same;
            if (replacing && (!c.rule().replace() || !destination.removable())) continue;
            int existing = same ? old.getCount() : 0;
            int maximum = Math.min(source.getMaxStackSize(), destination.limit(source));
            int desired = c.rule().desired(existing, maximum, consumed.contains(id));
            // A stocked higher-priority rule also owns an already-satisfied destination.
            // Lower-priority replacement must not ping-pong it on the next tick.
            if (desired <= existing) {
                claimed.add(id);
                continue;
            }
            int added = Math.min(source.getCount(), desired - existing);
            if (added <= 0) continue;
            var returns =
                    replacing ? planReturn(player, old, protectedSlots) : List.<ReturnSlot>of();
            if (returns == null)
                continue; // No capacity: neither side changes; never drop anything.
            if (!ItemStack.matches(source, c.pouch().getItem(c.source()))) continue;
            if (!destination.write(old, source.copyWithCount(existing + added))) continue;
            c.pouch().removeItem(c.source(), added);
            for (var back : returns) player.getInventory().setItem(back.index(), back.stack());
            player.getInventory().setChanged();
            claimed.add(id);
            total += added;
        }
        if (total > 0) player.inventoryMenu.broadcastChanges();
        return total;
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
