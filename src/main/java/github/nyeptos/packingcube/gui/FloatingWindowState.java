package github.nyeptos.packingcube.gui;

/** Opt-in window geometry/input state. No dependency on bags, screens, or vanilla modifications. */
public final class FloatingWindowState {
    public static final int HEADER_HEIGHT = 20;
    public static final int EDGE_SIZE = 4;
    public static final int LEFT = 1, RIGHT = 2, TOP = 4, BOTTOM = 8;
    private int x, y, width, height, screenWidth, screenHeight, minWidth, minHeight;
    private boolean initialized, interacting;
    private int edges, startX, startY, startWidth, startHeight;
    private double mouseStartX, mouseStartY;

    public void configure(
            int defaultX,
            int defaultY,
            int defaultWidth,
            int defaultHeight,
            int minimumWidth,
            int minimumHeight,
            int screenWidth,
            int screenHeight) {
        if (screenWidth < 1 || screenHeight < 1 || minimumWidth < 1 || minimumHeight < 1)
            throw new IllegalArgumentException("Invalid window constraints");
        if (this.screenWidth != screenWidth || this.screenHeight != screenHeight) stop();
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        minWidth = Math.min(minimumWidth, screenWidth);
        minHeight = Math.min(minimumHeight, screenHeight);
        if (!initialized) {
            x = defaultX;
            y = defaultY;
            width = defaultWidth;
            height = defaultHeight;
            initialized = true;
        }
        width = clamp(width, minWidth, screenWidth);
        height = clamp(height, minHeight, screenHeight);
        x = clamp(x, 0, screenWidth - width);
        y = clamp(y, 0, screenHeight - height);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }

    public int edgeAt(double mx, double my) {
        if (!contains(mx, my)) return 0;
        int result = 0;
        if (mx < x + EDGE_SIZE) result |= LEFT;
        else if (mx >= x + width - EDGE_SIZE) result |= RIGHT;
        if (my < y + EDGE_SIZE) result |= TOP;
        else if (my >= y + height - EDGE_SIZE) result |= BOTTOM;
        return result;
    }

    /** Returns true for chrome even when transforms are disallowed (e.g. a carried item). */
    public boolean press(double mx, double my, int button, boolean allowTransform) {
        if (button != 0 || !contains(mx, my)) return false;
        int hit = edgeAt(mx, my);
        if (hit == 0 && my >= y + HEADER_HEIGHT) return false;
        if (allowTransform) startInteraction(mx, my, hit);
        return true;
    }

    /** Programmatic move support for legacy hosts with their own header hit testing. */
    public void beginMove(double mx, double my) {
        if (Double.isFinite(mx) && Double.isFinite(my)) startInteraction(mx, my, 0);
    }

    private void startInteraction(double mx, double my, int hit) {
        edges = hit;
        interacting = true;
        mouseStartX = mx;
        mouseStartY = my;
        startX = x;
        startY = y;
        startWidth = width;
        startHeight = height;
    }

    public void drag(double mx, double my) {
        if (!interacting || !Double.isFinite(mx) || !Double.isFinite(my)) return;
        int dx = (int) Math.max(-screenWidth, Math.min(screenWidth, mx - mouseStartX));
        int dy = (int) Math.max(-screenHeight, Math.min(screenHeight, my - mouseStartY));
        if (edges == 0) {
            x = clamp(startX + dx, 0, screenWidth - width);
            y = clamp(startY + dy, 0, screenHeight - height);
            return;
        }
        if ((edges & LEFT) != 0) {
            x = clamp(startX + dx, 0, startX + startWidth - minWidth);
            width = startX + startWidth - x;
        } else if ((edges & RIGHT) != 0) width = clamp(startWidth + dx, minWidth, screenWidth - x);
        if ((edges & TOP) != 0) {
            y = clamp(startY + dy, 0, startY + startHeight - minHeight);
            height = startY + startHeight - y;
        } else if ((edges & BOTTOM) != 0)
            height = clamp(startHeight + dy, minHeight, screenHeight - y);
    }

    public boolean release(int button) {
        if (button != 0 || !interacting) return false;
        stop();
        return true;
    }

    public void stop() {
        interacting = false;
        edges = 0;
    }

    public boolean interacting() {
        return interacting;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
