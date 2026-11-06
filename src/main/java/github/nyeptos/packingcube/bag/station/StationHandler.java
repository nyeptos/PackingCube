package github.nyeptos.packingcube.bag.station;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Carried furnaces cook on the server thread, whether or not their screen is open. */
public final class StationHandler {
    private StationHandler() {}

    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !player.isAlive() || player.isSpectator()) return;
        StationTicker.tick(player);
    }
}
