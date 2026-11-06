package github.nyeptos.packingcube.menu.station;

import github.nyeptos.packingcube.bag.BagIdentity;
import github.nyeptos.packingcube.bag.station.StationBrewingStand;
import github.nyeptos.packingcube.registry.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** A carried brewing stand that keeps brewing while its GUI is closed. */
public final class BrewingStandStationMenu extends BrewingStandMenu implements StationOwner {
    private final StationBrewingStand station;
    private final UUID rootId;
    private final int moduleSlot;

    public BrewingStandStationMenu(
            int id, Inventory inventory, UUID rootId, int moduleSlot, ItemStack block) {
        this(
                id,
                inventory,
                rootId,
                moduleSlot,
                StationBrewingStand.of(inventory.player, rootId, moduleSlot, block));
    }

    private BrewingStandStationMenu(
            int id, Inventory inventory, UUID rootId, int moduleSlot, StationBrewingStand station) {
        super(id, inventory, station, station);
        this.station = station;
        this.rootId = rootId;
        this.moduleSlot = moduleSlot;
    }

    public static BrewingStandStationMenu fromNetwork(
            int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        return new BrewingStandStationMenu(
                id, inventory, data.readUUID(), data.readVarInt(), ItemStack.EMPTY);
    }

    @Override
    public MenuType<?> getType() {
        return ModMenus.BREWING_STAND_STATION.get();
    }

    @Override
    public StationBrewingStand station() {
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
