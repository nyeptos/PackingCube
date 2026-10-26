package github.nyeptos.packingcube.bag.radial;

/** One hover click per new nonempty target; the center/empty targets remain silent. */
public final class RadialSelectionFeedback {
    private int previous = -1;

    public boolean moveTo(int target) {
        if (target < 0 || target > RadialGeometry.COUNT) target = -1;
        if (target == previous) return false;
        previous = target;
        return target >= 0;
    }
}
