package github.nyeptos.packingcube.gametest.gui;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.gui.input.SettingsScrollState;
import github.nyeptos.packingcube.gui.layout.BagSettingsLayout;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class SettingsScrollGameTests {
    @GameTest(template = "empty")
    public static void fittingContentNeverScrolls(GameTestHelper h) {
        var state = new SettingsScrollState();
        state.configure(200, 100);
        state.wheel(-100);
        state.dragThumb(1000);
        h.assertTrue(
                !state.scrollable() && state.offset() == 0 && state.thumbHeight() == 200,
                "No scroll or scrollbar movement when everything fits");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void wheelClampsAndRejectsNonFiniteOffsets(GameTestHelper h) {
        var state = new SettingsScrollState();
        state.configure(100, 400);
        state.wheel(-1);
        h.assertTrue(state.offset() == 24, "Wheel moves settings content only");
        state.wheel(-1000);
        h.assertTrue(state.offset() == 300, "Bottom clamped");
        state.setOffset(Double.NaN);
        state.setOffset(Double.POSITIVE_INFINITY);
        h.assertTrue(state.offset() == 300, "Invalid pointer values ignored");
        state.wheel(1000);
        h.assertTrue(state.offset() == 0, "Top clamped");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void scrollbarThumbMapsBothEndsAndMiddle(GameTestHelper h) {
        var state = new SettingsScrollState();
        state.configure(100, 400);
        h.assertTrue(state.thumbHeight() == 25 && state.thumbTop() == 0, "Proportional thumb");
        state.dragThumb(75);
        h.assertTrue(state.offset() == 300 && state.thumbTop() == 75, "Dragging reaches last item");
        state.dragThumb(37.5);
        h.assertTrue(state.offset() == 150, "Middle maps to middle");
        state.dragThumb(-100);
        h.assertTrue(state.offset() == 0, "Dragging beyond track safely clamps");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void resizePreservesOrClampsScrollPosition(GameTestHelper h) {
        var state = new SettingsScrollState();
        state.configure(100, 400);
        state.setOffset(300);
        state.configure(150, 400);
        h.assertTrue(state.offset() == 250, "Viewport resize clamps old bottom");
        state.configure(150, 600);
        h.assertTrue(state.offset() == 250, "Adding rows preserves position");
        state.configure(600, 600);
        h.assertTrue(state.offset() == 0 && !state.scrollable(), "Large window fits form again");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void focusVisibilityDoesNotExposeHiddenControls(GameTestHelper h) {
        var state = new SettingsScrollState();
        state.configure(100, 400);
        state.ensureVisible(350, 368);
        h.assertTrue(
                state.offset() == 268 && state.fullyVisible(350, 368),
                "Focus can reveal a control");
        state.ensureVisible(5, 23);
        h.assertTrue(state.offset() == 5 && state.fullyVisible(5, 23), "Focus can return to top");
        state.setOffset(50);
        h.assertTrue(
                !state.intersects(0, 20) && state.intersects(45, 60) && !state.fullyVisible(45, 60),
                "Hidden and partial controls distinguished");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void scrolledSampleUsesInverseTransformAndViewportGate(GameTestHelper h) {
        var layout = new BagSettingsLayout(420, 232, 2);
        var state = new SettingsScrollState();
        state.configure(layout.viewport().height(), layout.content().height());
        state.setOffset(50);
        int left = 23, top = 10;
        var slot = layout.sampleSlot();
        var localViewport = layout.viewport();
        var visible =
                new BagSettingsLayout.Rect(
                        left + localViewport.x(),
                        top + localViewport.y(),
                        localViewport.width(),
                        localViewport.height());
        double rawX = left + slot.x() + 4, rawY = top + slot.y() + 4 - state.offset();
        h.assertTrue(
                visible.contains(rawX, rawY)
                        && slot.contains(rawX - left, state.unscrolledY(rawY) - top),
                "Displayed sample and pointer hit use opposite matching transforms");
        h.assertFalse(
                visible.contains(left + 8, rawY),
                "Left inventory never belongs to settings viewport");
        state.setOffset(state.maxOffset());
        rawY = top + slot.y() + 4 - state.offset();
        h.assertFalse(visible.contains(rawX, rawY), "Offscreen sample must not be clickable");
        h.succeed();
    }
}
