package github.nyeptos.packingcube.bag.input;

/** Prevent a held/repeating close key from reopening a screen after its close notification. */
public final class BagKeyLatch {
    private boolean radial, direct;

    public void refresh(boolean radialDown, boolean directDown) {
        if (!radialDown) radial = false;
        if (!directDown) direct = false;
    }

    public boolean accept(BagKeyActions.Action action, boolean sameBinding) {
        if (action == BagKeyActions.Action.NONE) return false;
        boolean wasHeld = action == BagKeyActions.Action.RADIAL ? radial : direct;
        if (sameBinding) {
            radial = true;
            direct = true;
        } else if (action == BagKeyActions.Action.RADIAL) radial = true;
        else direct = true;
        return !wasHeld;
    }
}
