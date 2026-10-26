package github.nyeptos.packingcube.menu;

import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.BagSounds;
import github.nyeptos.packingcube.compat.BagEquipment;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Shared transport and open feedback. Callers retain ownership/permission and overflow checks. */
public final class BagMenuOpener {
    private BagMenuOpener() {}

    public static boolean open(
            Player player,
            BagInventory storage,
            ItemStack root,
            int ownerSlot,
            boolean modules,
            Component title) {
        if (player.level().isClientSide) return false;
        var curiosTargets = BagEquipment.refillTargets(player);
        // root is the owning backpack for a nested pouch, not the pouch being opened.
        boolean refill = storage.bagStack().is(ModItems.REFILL_POUCH.get());
        boolean opened =
                player.openMenu(
                                new SimpleMenuProvider(
                                        (id, inventory, ignored) ->
                                                new BagMenu(
                                                        id,
                                                        inventory,
                                                        storage,
                                                        root,
                                                        ownerSlot,
                                                        modules,
                                                        refill,
                                                        curiosTargets,
                                                        false),
                                        title),
                                data -> {
                                    data.writeVarInt(ownerSlot);
                                    data.writeBoolean(modules);
                                    data.writeBoolean(refill);
                                    BagMenu.writeCuriosTargets(data, curiosTargets);
                                })
                        .isPresent();
        if (opened && modules) BagSounds.opened(player);
        return opened;
    }
}
