package github.nyeptos.packingcube.bag.station;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;

/**
 * A station that keeps working on its own stack, GUI open or closed. The slots are the container
 * and the progress counters are the container data, so a menu can drive it directly.
 */
public interface TickingStation extends Container, ContainerData {
    /** Advances one tick and reports whether anything changed. */
    boolean tick(Level level);

    /** Writes the slots and the counters back into the block that carries them. */
    boolean flush();

    /** Hands the contents back when the block itself left the bag. */
    void spill(Player player);
}
