package github.nyeptos.packingcube.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** Quiet high-pitched UI feedback shared by PackingCube widgets only. */
public final class UiInteractionSounds {
    private UiInteractionSounds() {}

    public static void hover() {
        click(1.6F, .15F);
    }

    public static void grab() {
        click(1.8F, .2F);
    }

    private static void click(float pitch, float volume) {
        Minecraft.getInstance()
                .getSoundManager()
                .play(
                        SimpleSoundInstance.forUI(
                                SoundEvents.UI_BUTTON_CLICK.value(), pitch, volume));
    }
}
