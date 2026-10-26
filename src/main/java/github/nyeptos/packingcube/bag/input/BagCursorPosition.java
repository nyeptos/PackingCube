package github.nyeptos.packingcube.bag.input;

import java.util.Optional;

/** Last visible cursor position in window coordinates, independent of GUI scale. */
public final class BagCursorPosition {
    public record Point(double x, double y) {}

    private Point fraction;

    public boolean remember(double x, double y, int width, int height) {
        if (width <= 0 || height <= 0 || !Double.isFinite(x) || !Double.isFinite(y)) return false;
        fraction = new Point(clamp(x, width) / width, clamp(y, height) / height);
        return true;
    }

    public Optional<Point> resolve(int width, int height) {
        if (fraction == null || width <= 0 || height <= 0) return Optional.empty();
        return Optional.of(
                new Point(
                        clamp(fraction.x() * width, width), clamp(fraction.y() * height, height)));
    }

    public void clear() {
        fraction = null;
    }

    private static double clamp(double value, int size) {
        return Math.max(0, Math.min(size - 1, value));
    }
}
