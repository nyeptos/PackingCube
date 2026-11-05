package github.nyeptos.packingcube.gui.layout;

/** Local radial backdrop geometry, measured in GUI pixels. Real slot icons remain 16px. */
public record RadialLayout(
        int centerX, int centerY, int side, int radius, int titleY, int manageX, int manageY) {
    public static final int MANAGE_WIDTH = 130;
    public static final int MANAGE_HEIGHT = 18;

    public static RadialLayout fit(int screenWidth, int screenHeight) {
        int side = Math.min(220, Math.max(64, Math.min(screenWidth - 8, screenHeight - 40)));
        int left = (screenWidth - side) / 2;
        int top = Math.max(0, (screenHeight - side - 26) / 2);
        int cx = left + side / 2, cy = top + side / 2;
        int radius = Math.max(18, Math.min(72, side / 2 - 37));
        return new RadialLayout(
                cx,
                cy,
                side,
                radius,
                top + 6,
                cx - 65,
                Math.min(screenHeight - 24, top + side + 4));
    }

    public int left() {
        return centerX - side / 2;
    }

    public int top() {
        return centerY - side / 2;
    }

    /** The manage button uses the same rectangle for drawing and for input. */
    public boolean containsManage(double x, double y) {
        return x >= manageX
                && x < manageX + MANAGE_WIDTH
                && y >= manageY
                && y < manageY + MANAGE_HEIGHT;
    }
}
