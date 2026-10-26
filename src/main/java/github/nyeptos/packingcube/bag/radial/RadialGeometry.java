package github.nyeptos.packingcube.bag.radial;

import github.nyeptos.packingcube.bag.BagInventory;

/** Stable positions preserve the Big Bag's left-to-right module slot numbers, including gaps. */
public final class RadialGeometry {
    public static final int COUNT = BagInventory.MODULE_SLOTS;
    public static final int DEAD_ZONE = 30;

    private RadialGeometry() {}

    public static double angle(int index) {
        return -Math.PI / 2 + index * Math.PI * 2 / COUNT;
    }

    public static int x(int center, int radius, int index) {
        // Widen the tight lower diagonal gaps (4–5 and 6–7) without moving other slots.
        int outward = Math.max(0, radius) / 9;
        int offset = index == 3 ? outward : index == 6 ? -outward : 0;
        return center + (int) Math.round(Math.cos(angle(index)) * radius) + offset;
    }

    public static int y(int center, int radius, int index) {
        return center + (int) Math.round(Math.sin(angle(index)) * radius);
    }

    public static int hit(double dx, double dy) {
        if (!Double.isFinite(dx)
                || !Double.isFinite(dy)
                || dx * dx + dy * dy < DEAD_ZONE * DEAD_ZONE) return -1;
        double clockwise = Math.atan2(dy, dx) + Math.PI / 2;
        if (clockwise < 0) clockwise += Math.PI * 2;
        return ((int) Math.floor(clockwise / (Math.PI * 2 / COUNT) + 0.5)) % COUNT;
    }
}
