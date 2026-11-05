package github.nyeptos.packingcube.gui.animation;

/** At most one step tick per short interval, so dragging never machine-guns the click sound. */
public final class SliderSoundState {
    public static final long INTERVAL_MS = 60;
    private long last = Long.MIN_VALUE;

    /** True when the step should be audible; false keeps fast drags and wheel repeats quiet. */
    public boolean step(long now) {
        if (last != Long.MIN_VALUE && now - last < INTERVAL_MS) return false;
        last = now;
        return true;
    }

    public void reset() {
        last = Long.MIN_VALUE;
    }
}
