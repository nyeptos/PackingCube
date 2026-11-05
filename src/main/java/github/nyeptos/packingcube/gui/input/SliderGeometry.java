package github.nyeptos.packingcube.gui.input;

/** Pure slider math shared by the widget and the game tests. */
public final class SliderGeometry {
    /** Inset kept clear inside the track so the knob never covers the track ends. */
    public static final int TRACK_INSET = 4;

    public static final int KNOB_WIDTH = 8;

    private SliderGeometry() {}

    /** The groove starts where the knob's left edge stops, so both ends line up exactly. */
    public static int trackX(int x) {
        return x + TRACK_INSET;
    }

    /** Groove length equals the span the knob covers, not the whole widget width. */
    public static int trackWidth(int width) {
        return Math.max(0, width - TRACK_INSET * 2);
    }

    public static int travel(int width) {
        return Math.max(0, width - TRACK_INSET * 2 - KNOB_WIDTH);
    }

    public static int knobX(int value, int min, int max, int x, int width) {
        int span = max - min;
        if (span <= 0) return x + TRACK_INSET;
        int offset =
                (int)
                        Math.round(
                                travel(width)
                                        * (double) (clamp(value, min, max) - min)
                                        / (double) span);
        return x + TRACK_INSET + offset;
    }

    public static int valueAt(double mouseX, int min, int max, int x, int width) {
        int span = max - min;
        int travel = travel(width);
        if (span <= 0 || travel <= 0) return min;
        double ratio = (mouseX - x - TRACK_INSET) / (double) travel;
        return min + (int) Math.round(span * Math.max(0, Math.min(1, ratio)));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
