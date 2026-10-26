package github.nyeptos.packingcube.bag.pickup;

import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.compat.BagEquipment;
import github.nyeptos.packingcube.menu.BagRootMenu;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** One coordinator owns the remainder; individual bags do not subscribe to pickup events. */
public final class BagRouter {
    private record Destination(ItemStack pouch, BagInventory parent) {}

    private BagRouter() {}

    public static ItemStack route(Player player, ItemStack incoming) {
        ItemStack remainder = incoming.copy();
        if (remainder.isEmpty() || BagInventory.isBag(remainder)) return remainder;
        List<Destination> destinations = new ArrayList<>();
        // Collection order is the tie-breaker: active equipment, inventory slot, then module slot.
        for (ItemStack carrier : BagEquipment.activeBackBags(player)) {
            collect(player, carrier, remainder, destinations);
        }
        for (int index = 0; index < 36; index++) {
            collect(player, player.getInventory().getItem(index), remainder, destinations);
        }
        return insertOrdered(destinations, remainder);
    }

    /** Explicit manual action: one existing live parent, including auto-pickup-disabled pouches. */
    public static ItemStack routeManual(BagInventory parent, ItemStack incoming) {
        ItemStack remainder = incoming.copy();
        if (!parent.bagStack().is(ModItems.LARGE_BAG.get())
                || remainder.isEmpty()
                || BagInventory.isBag(remainder)) return remainder;
        List<Destination> destinations = new ArrayList<>();
        collectModules(parent, remainder, destinations, false);
        return insertOrdered(destinations, remainder);
    }

    private static ItemStack insertOrdered(List<Destination> destinations, ItemStack remainder) {
        // List.sort is stable: equal priorities retain the visible equipment/slot order.
        destinations.sort(
                Comparator.comparingInt(
                                (Destination destination) ->
                                        BagPickupSettings.priority(destination.pouch()))
                        .reversed());
        for (Destination destination : destinations) {
            remainder =
                    new BagInventory(destination.pouch(), false, destination.parent())
                            .insert(remainder);
            if (remainder.isEmpty()) break;
        }
        return remainder;
    }

    private static void collect(
            Player player, ItemStack carrier, ItemStack incoming, List<Destination> destinations) {
        // Legacy backpack enabled flags are intentionally ignored; only pouches have switches.
        if (!carrier.is(ModItems.LARGE_BAG.get())) return;
        // A screen owns its live parent view. Do not create a second writer, even for an open
        // pouch.
        if (player.containerMenu instanceof BagRootMenu menu && menu.ownsRoot(carrier)) return;
        BagInventory modules = new BagInventory(carrier, true, player.getInventory()::setChanged);
        collectModules(modules, incoming, destinations, true);
    }

    private static void collectModules(
            BagInventory modules,
            ItemStack incoming,
            List<Destination> destinations,
            boolean automatic) {
        for (int slot = 0; slot < modules.getContainerSize(); slot++) {
            ItemStack module = modules.getItem(slot);
            if ((automatic ? module.is(ModItems.FILTER_BAG.get()) : BagInventory.isPouch(module))
                    && (!automatic || BagPickupSettings.enabled(module))
                    && (module.is(ModItems.REFILL_POUCH.get())
                            || BagFilter.matches(module, incoming))) {
                destinations.add(new Destination(module, modules));
            }
        }
    }
}
