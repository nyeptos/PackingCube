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

    /** Softer, higher tick for one slider step; the widget throttles repeats. */
    public static void slide() {
        click(1.4F, .12F);
    }

    /** A slot lock snapping shut: the shared click, low and solid. */
    public static void lock() {
        click(.7F, .3F);
    }

    /** The same lock letting go: higher and lighter than the snap shut. */
    public static void unlock() {
        click(1.1F, .22F);
    }

    private static void click(float pitch, float volume) {
        Minecraft.getInstance()
                .getSoundManager()
                .play(
                        SimpleSoundInstance.forUI(
                                SoundEvents.UI_BUTTON_CLICK.value(), pitch, volume));
    }
}
