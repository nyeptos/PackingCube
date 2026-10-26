package github.nyeptos.packingcube.gui;

/** Real-time visual feedback only; never delays or changes an action/hit box. */
public final class ButtonAnimation {
    public static final long PRESS_MILLIS = 180;
    public static final int VISUAL_MARGIN = 2;
    private long lastMillis = Long.MIN_VALUE;
    private long pressedMillis = Long.MIN_VALUE;
    private double hover;

    public record Frame(float scale, float offsetY) {}

    public void press(long nowMillis) {
        pressedMillis = nowMillis;
    }

    public Frame sample(long nowMillis, boolean hovered, boolean enabled) {
        if (!enabled) {
            hover = 0;
            pressedMillis = Long.MIN_VALUE;
            lastMillis = nowMillis;
            return new Frame(1, 0);
        }
        double elapsed = lastMillis == Long.MIN_VALUE ? 0 : Math.max(0, nowMillis - lastMillis);
        lastMillis = nowMillis;
        double target = hovered ? 1 : 0;
        hover = target + (hover - target) * Math.exp(-elapsed / 45.0);
        double press = 0;
        if (pressedMillis != Long.MIN_VALUE) {
            long age = Math.max(0, nowMillis - pressedMillis);
            if (age >= PRESS_MILLIS) pressedMillis = Long.MIN_VALUE;
            else if (age < 30) press = age / 30.0;
            else {
                double remaining = 1 - (age - 30) / (double) (PRESS_MILLIS - 30);
                press = remaining * remaining;
            }
        }
        return new Frame((float) (1 - .065 * press), (float) (-hover + 2 * press));
    }
}
