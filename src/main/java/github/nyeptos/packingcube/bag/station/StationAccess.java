package github.nyeptos.packingcube.bag.station;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;

/**
 * Vanilla station menus ask for a world position to read the level from. A carried station has no
 * block, so the player's own position stands in: recipe lookups, sounds and experience all land
 * where the player is, and no block is ever read or damaged.
 */
public final class StationAccess {
    private StationAccess() {}

    public static ContainerLevelAccess at(Player player) {
        return ContainerLevelAccess.create(player.level(), player.blockPosition());
    }
}
