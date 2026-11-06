package github.nyeptos.packingcube.menu.station;

import github.nyeptos.packingcube.bag.BagIdentity;
import github.nyeptos.packingcube.bag.station.StationBlocks;
import github.nyeptos.packingcube.bag.station.StationFurnace;
import github.nyeptos.packingcube.bag.station.StationFurnaces;
import github.nyeptos.packingcube.registry.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** A carried smoker that keeps heating while its GUI is closed. */
public final class SmokerStationMenu extends SmokerMenu implements StationOwner {
    private final StationFurnace station;
    private final UUID rootId;
    private final int moduleSlot;

    public SmokerStationMenu(
            int id, Inventory inventory, UUID rootId, int moduleSlot, ItemStack block) {
        this(
                id,
                inventory,
                rootId,
                moduleSlot,
                StationFurnaces.of(
                        inventory.player,
                        rootId,
                        moduleSlot,
                        block,
                        StationBlocks.Kind.SMOKER));
    }

    private SmokerStationMenu(
            int id, Inventory inventory, UUID rootId, int moduleSlot, StationFurnace station) {
        super(id, inventory, station, station);
        this.station = station;
        this.rootId = rootId;
        this.moduleSlot = moduleSlot;
    }

    public static SmokerStationMenu fromNetwork(
            int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        return new SmokerStationMenu(
                id, inventory, data.readUUID(), data.readVarInt(), ItemStack.EMPTY);
    }

    @Override
    public MenuType<?> getType() {
        return ModMenus.SMOKER_STATION.get();
    }

    @Override
    public StationFurnace station() {
        return station;
    }

    @Override
    public boolean ownsStation(ItemStack root, int slot) {
        return slot == moduleSlot && BagIdentity.matches(root, rootId);
    }

    @Override
    public void removed(Player player) {
        if (!player.level().isClientSide && !station.flush()) station.spill(player);
        super.removed(player);
    }
}
