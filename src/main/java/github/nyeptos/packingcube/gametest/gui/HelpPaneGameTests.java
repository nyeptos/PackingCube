package github.nyeptos.packingcube.gametest.gui;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.client.widget.HelpPane;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Explanations open beside the settings window and never over its controls. */
@GameTestHolder(PackingCube.MODID)
@PrefixGameTestTemplate(false)
public final class HelpPaneGameTests {
    @GameTest(template = "empty")
    public static void placePrefersTheRightSideBesideTheWindow(GameTestHelper h) {
        var p = HelpPane.place(200, 40, 244, 640, 360, 150, 120);
        h.assertTrue(
                p.x() == 448 && p.y() == 40 && p.right() == 598,
                "Right side is used when the pane fits beside the window");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void placeFlipsToTheLeftWhenTheRightDoesNotFit(GameTestHelper h) {
        var p = HelpPane.place(200, 40, 244, 500, 360, 150, 120);
        h.assertTrue(
                p.x() == 46 && p.right() == 196,
                "Narrow screens put the pane on the left instead of off-screen");
        h.assertTrue(
                !p.contains(200, 40) && !p.contains(300, 100), "Pane never covers the window");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void placeStaysInsideASmallScreen(GameTestHelper h) {
        var p = HelpPane.place(0, 0, 300, 200, 100, 150, 80);
        h.assertTrue(
                p.x() == 2 && p.y() == 2 && p.right() <= 198 && p.bottom() <= 98,
                "Pane is clamped into the screen and keeps its top-left margin");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void heightGrowsWithWrappedLines(GameTestHelper h) {
        h.assertTrue(
                HelpPane.height(0) < HelpPane.height(1)
                        && HelpPane.height(1) < HelpPane.height(3),
                "Each wrapped line reserves its own row");
        h.assertTrue(
                HelpPane.height(4) - HelpPane.height(3) == HelpPane.LINE_HEIGHT,
                "Spacing stays constant so rows cannot overlap");
        h.succeed();
    }
}
