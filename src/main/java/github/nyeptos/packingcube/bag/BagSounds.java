package github.nyeptos.packingcube.bag;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/** Vanilla sounds shared by menu opening and optional equipment integration. */
public final class BagSounds {
    public static final float VOLUME = 1.0F;
    public static final float PITCH = 1.0F;

    private BagSounds() {}

    public static SoundEvent leather() {
        return SoundEvents.ARMOR_EQUIP_LEATHER.value();
    }

    /** Notify only the opener, after the server has accepted the menu opening. */
    public static void opened(Player player) {
        player.playNotifySound(leather(), SoundSource.PLAYERS, VOLUME, PITCH);
    }
}
