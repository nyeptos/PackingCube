package github.nyeptos.packingcube.bag.station;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Builds the state of a carried furnace from the bag that holds it. */
public final class StationFurnaces {
    private StationFurnaces() {}

    public static StationFurnace of(
            Player player,
            UUID rootId,
            int moduleSlot,
            ItemStack block,
            StationBlocks.Kind kind) {
        return StationFurnace.of(
                player,
                () -> StationStorage.root(player, rootId),
                moduleSlot,
                block,
                kind);
    }
}
