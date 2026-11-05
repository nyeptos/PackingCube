package github.nyeptos.packingcube.gametest.gui;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.gui.animation.SliderSoundState;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Slider ticks are throttled so a drag never plays a sound every frame. */
@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class SliderSoundGameTests {
    @GameTest(template = "empty")
    public static void stepsAreThrottledAndRecoverAfterTheInterval(GameTestHelper h) {
        var sounds = new SliderSoundState();
        h.assertTrue(sounds.step(1000), "The first step is audible");
        h.assertTrue(
                !sounds.step(1010) && !sounds.step(1059),
                "Repeats inside the interval stay silent");
        h.assertTrue(sounds.step(1060), "A later step plays again");
        sounds.reset();
        h.assertTrue(sounds.step(1061), "A new drag starts audible immediately");
        h.succeed();
    }
}
