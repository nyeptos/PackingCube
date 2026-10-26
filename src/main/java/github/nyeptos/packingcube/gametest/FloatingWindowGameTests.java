package github.nyeptos.packingcube.gametest;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.gui.BagSettingsLayout;
import github.nyeptos.packingcube.gui.FloatingWindowState;
import github.nyeptos.packingcube.gui.SettingsScrollState;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class FloatingWindowGameTests {
    private static FloatingWindowState window() {
        var s = new FloatingWindowState();
        s.configure(100, 100, 200, 180, 124, 80, 800, 600);
        return s;
    }

    @GameTest(template = "empty")
    public static void headerMovesWindowNotInventory(GameTestHelper h) {
        var s = window();
        var inventory = BagSettingsLayout.inventoryAnchor(800, 600);
        h.assertTrue(s.press(130, 110, 0, true), "Header captures move");
        s.drag(230, 160);
        h.assertTrue(
                s.x() == 200 && s.y() == 150 && s.width() == 200 && s.height() == 180,
                "Move retains size and grab point");
        h.assertTrue(
                inventory.equals(BagSettingsLayout.inventoryAnchor(800, 600)), "Inventory fixed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void allFourEdgesAndFourCornersResize(GameTestHelper h) {
        int[] masks = {1, 2, 4, 8, 5, 6, 9, 10};
        for (int mask : masks) {
            var s = window();
            double x = (mask & 1) != 0 ? 100 : (mask & 2) != 0 ? 299 : 200;
            double y = (mask & 4) != 0 ? 100 : (mask & 8) != 0 ? 279 : 190;
            h.assertTrue(
                    s.edgeAt(x, y) == mask && s.press(x, y, 0, true),
                    "Resize edge/corner captured");
            s.drag(x + ((mask & 1) != 0 ? -30 : 30), y + ((mask & 4) != 0 ? -25 : 25));
            if ((mask & 3) != 0) h.assertTrue(s.width() == 230, "Horizontal resize");
            else h.assertTrue(s.width() == 200 && s.x() == 100, "Unselected horizontal axis fixed");
            if ((mask & 12) != 0) h.assertTrue(s.height() == 205, "Vertical resize");
            else h.assertTrue(s.height() == 180 && s.y() == 100, "Unselected vertical axis fixed");
            h.assertTrue(
                    (mask & 1) == 0 ? s.x() == 100 : s.x() + s.width() == 300,
                    "Opposite X anchor fixed");
            h.assertTrue(
                    (mask & 4) == 0 ? s.y() == 100 : s.y() + s.height() == 280,
                    "Opposite Y anchor fixed");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void resizingClampsMinimumAndScreenEdges(GameTestHelper h) {
        var s = window();
        s.press(299, 279, 0, true);
        s.drag(-10000, -10000);
        h.assertTrue(s.width() == 124 && s.height() == 80, "Minimum readable dimensions");
        s.drag(10000, 10000);
        h.assertTrue(s.x() + s.width() == 800 && s.y() + s.height() == 600, "Screen limits");
        s.release(0);
        s.press(100, 100, 0, true);
        s.drag(-10000, -10000);
        h.assertTrue(
                s.x() == 0 && s.y() == 0 && s.width() == 800 && s.height() == 600,
                "Opposite edges remain anchored");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void smallScreensFitWithoutSidebarRequirement(GameTestHelper h) {
        for (int width : new int[] {96, 320, 400, 438}) {
            var s = new FloatingWindowState();
            s.configure(300, -50, 244, 373, 124, 80, width, 90);
            h.assertTrue(
                    s.x() >= 0
                            && s.y() >= 0
                            && s.x() + s.width() <= width
                            && s.y() + s.height() <= 90,
                    "Floating window shrinks and clamps instead of refusing to open");
            var layout = BagSettingsLayout.floating(s.width(), s.height(), 2);
            var scroll = new SettingsScrollState();
            scroll.configure(layout.viewport().height(), layout.content().height());
            scroll.setOffset(scroll.maxOffset());
            h.assertTrue(
                    scroll.scrollable()
                            && scroll.fullyVisible(layout.pagerY() - 24, layout.pagerY() + 18 - 24),
                    "Last setting remains reachable by scrolling");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rebuildRetainsResizeGestureAndGeometry(GameTestHelper h) {
        var s = window();
        s.press(299, 279, 0, true);
        s.drag(350, 330);
        int w = s.width(), height = s.height();
        s.configure(0, 0, 124, 80, 124, 80, 800, 600);
        h.assertTrue(
                s.interacting() && s.width() == w && s.height() == height,
                "Same-screen widget reflow preserves gesture");
        s.drag(380, 350);
        h.assertTrue(s.width() > w && s.height() > height, "Resizing continues after reflow");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void screenResizeCancelsGestureAndClampsSavedWindow(GameTestHelper h) {
        var s = window();
        s.press(299, 279, 0, true);
        s.drag(700, 500);
        s.configure(0, 0, 124, 80, 124, 80, 320, 240);
        h.assertTrue(
                !s.interacting() && s.x() + s.width() <= 320 && s.y() + s.height() <= 240,
                "Real window resize cancels captured input and fits geometry");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void contentRightClickAndCarriedItemsNeverStartTransform(GameTestHelper h) {
        var s = window();
        h.assertTrue(
                !s.press(150, 150, 0, true) && !s.press(130, 110, 1, true),
                "Content and right click not captured");
        h.assertTrue(
                s.press(130, 110, 0, false) && !s.interacting(),
                "Chrome blocks clicks without moving with carried item");
        s.drag(400, 400);
        h.assertTrue(s.x() == 100 && s.y() == 100, "No accidental move");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void releaseOutsideAndInvalidPointersAreSafe(GameTestHelper h) {
        var s = window();
        s.press(299, 279, 0, true);
        s.drag(Double.NaN, 500);
        s.drag(500, Double.POSITIVE_INFINITY);
        h.assertTrue(s.width() == 200 && s.height() == 180, "Nonfinite coordinates ignored");
        h.assertTrue(!s.release(1) && s.interacting(), "Wrong release does not end left gesture");
        h.assertTrue(
                s.release(0) && !s.interacting(), "Left release ends captured gesture anywhere");
        s.drag(600, 500);
        h.assertTrue(s.width() == 200 && s.height() == 180, "Released pointer no longer resizes");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void independentHostsNeverShareGeometry(GameTestHelper h) {
        var first = window();
        var second = window();
        first.press(299, 279, 0, true);
        first.drag(400, 400);
        h.assertTrue(
                second.width() == 200 && second.height() == 180 && !second.interacting(),
                "State is not static or bag-specific");
        first.release(0);
        first.configure(0, 0, 124, 80, 124, 80, 800, 600);
        h.assertTrue(
                first.width() == 301 && first.height() == 301, "Reopening retains chosen size");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void resizedLayoutKeepsHeaderAndInventoryIndependent(GameTestHelper h) {
        for (int width : new int[] {124, 244, 400})
            for (int height : new int[] {80, 132, 373, 500}) {
                var layout = BagSettingsLayout.floating(width, height, 2);
                h.assertTrue(
                        layout.settingsBackground().width() == width && layout.height() == height,
                        "Layout follows window geometry");
                h.assertTrue(
                        layout.inventory().height() == 132 && layout.inventory().width() == 176,
                        "Inventory dimensions never follow window resize");
                h.assertTrue(
                        layout.viewport().y() >= FloatingWindowState.HEADER_HEIGHT
                                && layout.viewport().bottom() <= height - 4,
                        "Content clear of header and resize rim");
            }
        h.succeed();
    }
}
