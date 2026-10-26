package github.nyeptos.packingcube.gametest;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.gui.BagSettingsLayout;
import github.nyeptos.packingcube.gui.SettingsPanelPosition;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class SettingsPanelPositionGameTests {
    private static SettingsPanelPosition position() {
        var state = new SettingsPanelPosition();
        state.configure(400, 60, 200, 300, 800, 600);
        return state;
    }

    @GameTest(template = "empty")
    public static void dragKeepsGrabOffsetAndInventoryFixed(GameTestHelper h) {
        var state = position();
        var inventory = BagSettingsLayout.inventoryAnchor(800, 600);
        state.begin(420, 70);
        state.drag(220, 170);
        h.assertTrue(state.x() == 200 && state.y() == 160, "Grab offset retained");
        h.assertTrue(
                inventory.equals(BagSettingsLayout.inventoryAnchor(800, 600)),
                "Inventory unchanged");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void panelCannotLeaveScreen(GameTestHelper h) {
        var state = position();
        state.begin(400, 60);
        state.drag(-1000, -1000);
        h.assertTrue(state.x() == 0 && state.y() == 0, "Top left clamped");
        state.drag(10000, 10000);
        h.assertTrue(state.x() == 600 && state.y() == 300, "Bottom right clamped");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void releaseAndInvalidPointerNeverMovePanel(GameTestHelper h) {
        var state = position();
        state.drag(0, 0);
        state.begin(400, 60);
        state.drag(Double.NaN, 4);
        state.drag(4, Double.POSITIVE_INFINITY);
        state.stop();
        state.drag(0, 0);
        h.assertTrue(
                !state.dragging() && state.x() == 400 && state.y() == 60,
                "No invalid or released drag");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rebuildRetainsPositionResizeClampsAndStopsDrag(GameTestHelper h) {
        var state = position();
        state.begin(400, 60);
        state.drag(180, 100);
        state.configure(400, 60, 200, 300, 800, 600);
        h.assertTrue(
                state.x() == 180 && state.y() == 100 && !state.dragging(),
                "Reopen retains position");
        state.configure(0, 0, 200, 300, 250, 320);
        h.assertTrue(state.x() == 50 && state.y() == 20, "Resize reclamps");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void dragHeaderNeverScrollsWithContent(GameTestHelper h) {
        var layout = new BagSettingsLayout(420, 240, 2);
        h.assertTrue(
                layout.dragHandle().bottom() <= layout.viewport().y(), "Handle outside viewport");
        h.assertTrue(layout.optionRow(0).y() >= layout.viewport().y(), "Controls below handle");
        h.assertTrue(
                !layout.viewport().contains(layout.dragHandle().centerX(), 10),
                "Drag cannot toggle a setting");
        h.succeed();
    }
}
