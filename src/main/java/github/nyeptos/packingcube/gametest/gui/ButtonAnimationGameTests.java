package github.nyeptos.packingcube.gametest.gui;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.gui.animation.ButtonAnimation;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class ButtonAnimationGameTests {
    @GameTest(template = "empty")
    public static void hoverMovesSmoothlyAndReversesWithoutJumping(GameTestHelper h) {
        var motion = new ButtonAnimation();
        h.assertTrue(motion.sample(0, false, true).offsetY() == 0, "Starts at rest");
        var entering = motion.sample(20, true, true);
        h.assertTrue(
                entering.offsetY() < 0 && entering.offsetY() > -1,
                "Hover eases into a one-pixel lift");
        var instant = motion.sample(20, false, true);
        h.assertTrue(instant.equals(entering), "Changing target does not snap current position");
        var leaving = motion.sample(40, false, true);
        h.assertTrue(
                leaving.offsetY() > entering.offsetY() && leaving.offsetY() < 0, "Exit eases back");
        h.assertTrue(
                Math.abs(motion.sample(1000, false, true).offsetY()) < .001, "Returns to rest");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void hoverTimingDoesNotDependOnFrameRate(GameTestHelper h) {
        var slow = new ButtonAnimation();
        var fast = new ButtonAnimation();
        slow.sample(0, false, true);
        fast.sample(0, false, true);
        for (int time = 10; time <= 120; time += 10) fast.sample(time, true, true);
        h.assertTrue(
                Math.abs(
                                slow.sample(120, true, true).offsetY()
                                        - fast.sample(120, true, true).offsetY())
                        < .00001,
                "Same real time gives same hover at different frame rates");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void clickCompressesAndSettlesWithoutActionDelay(GameTestHelper h) {
        var motion = new ButtonAnimation();
        motion.sample(100, false, true);
        motion.press(100);
        h.assertTrue(motion.sample(100, false, true).scale() == 1, "Press begins at current scale");
        var middle = motion.sample(130, false, true);
        h.assertTrue(
                Math.abs(middle.scale() - .935) < .00001 && middle.offsetY() == 2,
                "Peak compression after 30ms");
        var release = motion.sample(190, false, true);
        h.assertTrue(release.scale() > middle.scale() && release.scale() < 1, "Press eases out");
        var end = motion.sample(280, false, true);
        h.assertTrue(end.scale() == 1 && end.offsetY() == 0, "Settles after 180ms");
        motion.press(300);
        for (int time = 300; time <= 500; time++) {
            var frame = motion.sample(time, true, true);
            h.assertTrue(
                    frame.scale() >= .9349
                            && frame.scale() <= 1
                            && frame.offsetY() >= -1
                            && frame.offsetY() <= 2,
                    "Motion never grows beyond content/scissor bounds");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void disabledStateCancelsMotionAndRepeatedPressRestartsPulse(GameTestHelper h) {
        var motion = new ButtonAnimation();
        motion.sample(0, false, true);
        motion.press(0);
        motion.sample(30, true, true);
        var off = motion.sample(40, true, false);
        h.assertTrue(
                off.scale() == 1 && off.offsetY() == 0,
                "Disabled buttons cannot retain click feedback");
        var enabled = motion.sample(40, false, true);
        h.assertTrue(enabled.equals(off), "Re-enable does not revive stale pulse");
        motion.press(50);
        motion.sample(80, false, true);
        motion.press(90);
        h.assertTrue(
                Math.abs(motion.sample(120, false, true).scale() - .935) < .00001,
                "Rapid repeated activation gets fresh feedback");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void heldPressAndTextureWindowStayExplicit(GameTestHelper h) {
        var motion = new ButtonAnimation();
        motion.sample(0, false, true);
        motion.press(10);
        h.assertTrue(
                motion.pressing(10) && motion.pressing(189) && !motion.pressing(190),
                "Pressed texture pulse has a strict 180ms lifetime");
        var held = motion.sample(1000, true, true, true);
        h.assertTrue(
                Math.abs(held.scale() - .935F) < .00001
                        && held.offsetY() >= 1
                        && held.offsetY() <= 2,
                "Long held press keeps compression instead of disappearing");
        motion.press(1000);
        h.assertTrue(motion.pressing(1000), "Release can start visible short feedback tail");
        motion.sample(1010, true, false, true);
        h.assertTrue(!motion.pressing(1010), "Disabled button clears pressed texture state");
        h.succeed();
    }
}
