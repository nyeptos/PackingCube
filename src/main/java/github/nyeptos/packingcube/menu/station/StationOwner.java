package github.nyeptos.packingcube.menu.station;

import github.nyeptos.packingcube.bag.station.TickingStation;

import net.minecraft.world.item.ItemStack;

/** A menu that drives one carried station, so the player tick leaves that station alone. */
public interface StationOwner {
    TickingStation station();

    boolean ownsStation(ItemStack root, int moduleSlot);
}
