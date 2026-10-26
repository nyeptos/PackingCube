package github.nyeptos.packingcube.bag.radial;

/** C toggles a sticky radial; release only rearms the key, never selects a bag. */
public final class RadialKeyState {
    public enum Action {
        NONE,
        CLOSE
    }

    private boolean held;

    public RadialKeyState(boolean heldOnOpen) {
        held = heldOnOpen;
    }

    public boolean held() {
        return held;
    }

    public Action press() {
        if (held) return Action.NONE; // Keyboard auto-repeat is not a second press.
        held = true;
        return Action.CLOSE;
    }

    public void release() {
        held = false;
    }
}
