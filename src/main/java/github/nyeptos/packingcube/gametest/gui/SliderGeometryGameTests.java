package github.nyeptos.packingcube.gametest.gui;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.gui.input.SliderGeometry;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Slider drag math: the knob and the reported value must agree at every position. */
@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class SliderGeometryGameTests {
    @GameTest(template = "empty")
    public static void knobAndValueRoundTripAcrossTheTrack(GameTestHelper h) {
        int x = 10, width = 100;
        h.assertTrue(
                SliderGeometry.valueAt(x + SliderGeometry.TRACK_INSET, 0, 64, x, width) == 0,
                "The left end is the minimum");
        h.assertTrue(
                SliderGeometry.valueAt(x + width - SliderGeometry.TRACK_INSET, 0, 64, x, width)
                        == 64,
                "The right end is the maximum");
        for (int value = 0; value <= 64; value++) {
            int knob = SliderGeometry.knobX(value, 0, 64, x, width);
            h.assertTrue(
                    knob >= x + SliderGeometry.TRACK_INSET
                            && knob + SliderGeometry.KNOB_WIDTH
                                    <= x + width - SliderGeometry.TRACK_INSET,
                    "The knob stays inside the track at " + value);
            h.assertTrue(
                    Math.abs(SliderGeometry.valueAt(knob, 0, 64, x, width) - value) <= 1,
                    "Dragging the knob reports the same value at " + value);
        }
        h.assertTrue(
                SliderGeometry.knobX(0, 0, 64, x, width) == SliderGeometry.trackX(x)
                        && SliderGeometry.knobX(64, 0, 64, x, width)
                                        + SliderGeometry.KNOB_WIDTH
                                == SliderGeometry.trackX(x)
                                        + SliderGeometry.trackWidth(width),
                "Groove ends sit exactly where the knob stops at both limits");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void draggingOutsideTheTrackClamps(GameTestHelper h) {
        int x = 10, width = 60;
        h.assertTrue(
                SliderGeometry.valueAt(x - 40, 1, 16, x, width) == 1
                        && SliderGeometry.valueAt(x + width + 40, 1, 16, x, width) == 16,
                "Releases outside the track clamp to the ends");
        h.assertTrue(
                SliderGeometry.knobX(99, 1, 16, x, width)
                                == x + width - SliderGeometry.TRACK_INSET
                                        - SliderGeometry.KNOB_WIDTH
                        && SliderGeometry.knobX(-5, 1, 16, x, width)
                                == x + SliderGeometry.TRACK_INSET,
                "Out-of-range values draw at the ends");
        h.assertTrue(
                SliderGeometry.travel(4) == 0
                        && SliderGeometry.valueAt(x + 20, 3, 3, x, 4) == 3
                        && SliderGeometry.knobX(3, 3, 3, x, 4) == x + SliderGeometry.TRACK_INSET,
                "A track too narrow for travel still reports the only value");
        h.succeed();
    }
}
