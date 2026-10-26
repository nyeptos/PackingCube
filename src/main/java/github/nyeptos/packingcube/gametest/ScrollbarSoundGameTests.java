package github.nyeptos.packingcube.gametest;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.gui.ScrollbarSoundState;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class ScrollbarSoundGameTests {
    @GameTest(template = "empty")
    public static void hoverIsOncePerThumbEntryAndTrackIsSilent(GameTestHelper h) {
        var state = new ScrollbarSoundState();
        h.assertTrue(!state.hover(true, false), "Track/outside is silent");
        h.assertTrue(state.hover(true, true), "Thumb entry plays once");
        for (int i = 0; i < 120; i++)
            h.assertTrue(!state.hover(true, true), "No continuous sound while hovering");
        h.assertTrue(
                !state.hover(true, false) && state.hover(true, true),
                "Leaving and re-entering plays again");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void grabDoesNotDoublePlayOrRepeatDuringDrag(GameTestHelper h) {
        var state = new ScrollbarSoundState();
        h.assertTrue(state.grab(true), "Thumb or track click starts one grab sound");
        h.assertTrue(
                !state.hover(true, true),
                "Track click cannot also play hover on new thumb position");
        for (int i = 0; i < 120; i++) {
            h.assertTrue(!state.grab(true), "No repeated grab in the same gesture");
            h.assertTrue(
                    !state.hover(true, i % 2 == 0), "No hover clicks while dragging in and out");
        }
        state.hover(true, true);
        state.release();
        h.assertTrue(!state.hover(true, true), "Releasing over thumb is not a new hover entry");
        h.assertTrue(state.grab(true), "Next gesture gets its own grab sound");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void hiddenScrollbarStaysSilentAndReappearanceResetsState(GameTestHelper h) {
        var state = new ScrollbarSoundState();
        h.assertTrue(
                !state.grab(false) && !state.hover(false, true), "Absent scrollbar never sounds");
        state.grab(true);
        state.hover(false, false);
        h.assertTrue(state.hover(true, true), "Reappearance starts with a fresh thumb entry");
        state.release();
        state.hover(true, false);
        h.assertTrue(state.hover(true, true), "Released pointer can leave and return normally");
        h.succeed();
    }
}
