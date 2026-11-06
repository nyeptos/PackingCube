package github.nyeptos.packingcube.menu.station;

import github.nyeptos.packingcube.bag.station.StationAccess;
import github.nyeptos.packingcube.bag.station.StationBlocks;
import github.nyeptos.packingcube.bag.station.StationState;
import github.nyeptos.packingcube.registry.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** A carried stonecutter that keeps its GUI contents inside the block stack. */
public final class StonecutterStationMenu extends StonecutterMenu implements CarriedStationMenu {
    private final StationState state;

    public StonecutterStationMenu(
            int id, Inventory inventory, UUID rootId, int moduleSlot, ItemStack block) {
        super(id, inventory, StationAccess.at(inventory.player));
        this.state =
                new StationState(
                        inventory.player,
                        rootId,
                        moduleSlot,
                        block,
                        StationBlocks.Kind.STONECUTTER);
        state.restore(this);
    }

    public static StonecutterStationMenu fromNetwork(
            int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        return new StonecutterStationMenu(
                id, inventory, data.readUUID(), data.readVarInt(), ItemStack.EMPTY);
    }

    @Override
    public MenuType<?> getType() {
        return ModMenus.STONECUTTER_STATION.get();
    }

    /** No world block exists to stand next to, so only the player's life is checked. */
    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    /** Vanilla would spill the inputs back; the block keeps them instead. */
    @Override
    public void removed(Player player) {
        state.closed(this, player);
    }
}
