package github.nyeptos.packingcube.bag;

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
        for (int slot = 0; slot < modules.getContainerSize(); slot++) {
            ItemStack module = modules.getItem(slot);
            if (module.is(ModItems.FILTER_BAG.get())
                    && BagPickupSettings.enabled(module)
                    && BagFilter.matches(module, incoming)) {
                destinations.add(new Destination(module, modules));
            }
        }
    }
}
