package github.nyeptos.packingcube.gametest;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.gui.ButtonAnimation;
import github.nyeptos.packingcube.gui.ButtonHoverState;
import github.nyeptos.packingcube.gui.SettingsScrollState;
import github.nyeptos.packingcube.gui.WidgetSkin;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class ButtonHoverGameTests {
    @GameTest(template = "empty")
    public static void mouseClickFocusCannotStickHoverOnEitherButton(GameTestHelper h) {
        var left = new ButtonHoverState();
        var right = new ButtonHoverState();
        h.assertTrue(left.update(true, true, false, false).highlighted(), "Left enters");
        h.assertTrue(
                !left.update(true, false, true, false).highlighted(),
                "Mouse focus is not hover after leaving");
        h.assertTrue(
                right.update(true, true, false, false).highlighted(), "Right enters independently");
        h.assertTrue(
                !right.update(true, false, true, false).highlighted(),
                "Right also exits while focused");
        h.assertTrue(
                left.update(true, true, true, false).playSound(),
                "Returning to focused left is a fresh entry");
        h.assertTrue(
                left.update(true, false, true, true).highlighted(),
                "Keyboard navigation still highlights focused widget");
        h.assertTrue(
                !left.update(true, false, true, false).highlighted(),
                "Returning to mouse input clears keyboard-only highlight");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void soundOnlyPlaysOnEnabledPointerEntryAndCanResetAfterClipping(
            GameTestHelper h) {
        var state = new ButtonHoverState();
        h.assertTrue(!state.update(true, false, false, false).playSound(), "Outside is silent");
        h.assertTrue(state.update(true, true, false, false).playSound(), "Entry clicks once");
        for (int frame = 0; frame < 120; frame++)
            h.assertTrue(
                    !state.update(true, true, true, false).playSound(),
                    "No per-frame or focus sound repetition");
        var disabled = state.update(false, true, true, true);
        h.assertTrue(
                !disabled.highlighted() && !disabled.playSound(),
                "Disabled priority endpoints stay quiet");
        h.assertTrue(
                !state.update(true, false, true, true).playSound(),
                "Keyboard focus does not click");
        h.assertTrue(
                state.update(true, true, false, false).playSound(),
                "Enabled pointer entry works again");
        state.reset();
        h.assertTrue(
                state.update(true, true, false, false).playSound(),
                "Scrolled-out/reappearing widget has fresh pointer state");
        h.assertTrue(
                WidgetSkin.button(true, false, true) == WidgetSkin.BUTTON_SELECTED_HOVERED,
                "Priority and mode ordinary buttons use supplied hover art");
        h.assertTrue(
                WidgetSkin.button(false, false, true) == WidgetSkin.BUTTON_NORMAL,
                "Disabled priority button does not borrow enabled highlight");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void scrollEndOverscanDoesNotChangeOffsetsOrInputVisibility(GameTestHelper h) {
        var scroll = new SettingsScrollState();
        scroll.configure(100, 300);
        h.assertTrue(
                scroll.visualClipTop(24, 2) == 22 && scroll.visualClipBottom(124, 2) == 124,
                "Topmost hover has room above the viewport");
        h.assertTrue(
                scroll.offset() == 0 && scroll.maxOffset() == 200 && !scroll.intersects(-5, -1),
                "Drawing allowance does not expand logical content or input");
        scroll.setOffset(50);
        h.assertTrue(
                scroll.visualClipTop(24, 2) == 24 && scroll.visualClipBottom(124, 2) == 124,
                "Interior scrolling stays strictly clipped");
        scroll.setOffset(200);
        h.assertTrue(
                scroll.visualClipTop(24, 2) == 24 && scroll.visualClipBottom(124, 2) == 126,
                "Last row press may draw into bottom margin only at the end");
        scroll.configure(100, 80);
        h.assertTrue(
                scroll.visualClipTop(24, 2) == 22 && scroll.visualClipBottom(124, 2) == 126,
                "Short forms have both end margins without scrollbar");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void visualMarginCoversFullButtonMotionWithoutChangingButtonBounds(
            GameTestHelper h) {
        for (int height : new int[] {14, 18, 20}) {
            var motion = new ButtonAnimation();
            motion.sample(0, false, true);
            motion.press(0);
            for (int time = 0; time <= 500; time++) {
                var frame = motion.sample(time, true, true);
                float top = frame.offsetY() + (1 - frame.scale()) * height / 2;
                float bottom = frame.offsetY() - (1 - frame.scale()) * height / 2;
                h.assertTrue(
                        top >= -ButtonAnimation.VISUAL_MARGIN
                                && bottom <= ButtonAnimation.VISUAL_MARGIN,
                        "Shared clip margin covers entire hover/press excursion");
            }
        }
        h.succeed();
    }
}
