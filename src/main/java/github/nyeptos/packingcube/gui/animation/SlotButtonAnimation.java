package github.nyeptos.packingcube.gui.animation;

/** Opacity-only slot feedback. Texture pixels and the 18x18 input rectangle never move/scale. */
public final class SlotButtonAnimation {
    public static final long PRESS_MILLIS = 180;
    private long last = Long.MIN_VALUE;
    private long pressed = Long.MIN_VALUE;
    private double hover;

    public record Frame(float hover, float press) {}

    public void press(long now) {
        pressed = now;
    }

    public Frame sample(long now, boolean hovered, boolean held) {
        long elapsed = last == Long.MIN_VALUE ? 0 : Math.clamp(now - last, 0, 100);
        last = now;
        double target = hovered ? 1 : 0;
        hover = target + (hover - target) * Math.exp(-elapsed / 45.0);
        float pulse = 0;
        if (pressed != Long.MIN_VALUE) {
            long age = Math.max(0, now - pressed);
            if (age >= PRESS_MILLIS) pressed = Long.MIN_VALUE;
            else pulse = age < 30 ? 1 : (float) Math.pow(1 - (age - 30) / 150.0, 2);
        }
        return new Frame((float) hover, held ? 1 : pulse);
    }
}
