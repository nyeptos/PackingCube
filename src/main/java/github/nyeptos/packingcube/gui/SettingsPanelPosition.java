package github.nyeptos.packingcube.gui;

/** Compatibility adapter for existing move-only hosts. New screens use FloatingWindowState. */
@Deprecated
public final class SettingsPanelPosition {
    private final FloatingWindowState window = new FloatingWindowState();

    public void configure(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        window.configure(x, y, width, height, width, height, screenWidth, screenHeight);
        window.stop();
    }

    public void begin(double x, double y) {
        window.beginMove(x, y);
    }

    public void drag(double x, double y) {
        window.drag(x, y);
    }

    public void stop() {
        window.stop();
    }

    public boolean dragging() {
        return window.interacting();
    }

    public int x() {
        return window.x();
    }

    public int y() {
        return window.y();
    }
}
