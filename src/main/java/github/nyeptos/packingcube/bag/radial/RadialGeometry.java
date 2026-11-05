package github.nyeptos.packingcube.bag.radial;

import github.nyeptos.packingcube.bag.BagInventory;

/**
 * Stable positions preserve the Big Bag's left-to-right module slot numbers, including gaps. Every
 * slot is a {@link #FRAME_SIZE}px frame, and only that frame is selectable, so the drawing and the
 * input rectangle come from the same numbers.
 */
public final class RadialGeometry {
    public static final int COUNT = BagInventory.MODULE_SLOTS;
    public static final int FRAME_SIZE = 36;
    /** Center cancel area; the two label lines stay a cancel target instead of a slot. */
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

    /** Top-left corner of the frame drawn for one slot; icon and number use its center. */
    public static int frameLeft(int center, int radius, int index) {
        return x(center, radius, index) - FRAME_SIZE / 2;
    }

    public static int frameTop(int center, int radius, int index) {
        return y(center, radius, index) - FRAME_SIZE / 2;
    }

    public static boolean frameContains(
            int centerX, int centerY, int radius, int index, double mouseX, double mouseY) {
        int left = frameLeft(centerX, radius, index), top = frameTop(centerY, radius, index);
        return mouseX >= left
                && mouseX < left + FRAME_SIZE
                && mouseY >= top
                && mouseY < top + FRAME_SIZE;
    }

    /**
     * Selects the frame under the cursor. Gaps between frames, the area outside the ring and the
     * center cancel area select nothing, so hover, sound and click always agree with the drawing.
     * Frames that overlap in a very small window resolve to the nearest frame center.
     */
    public static int hit(double mouseX, double mouseY, int centerX, int centerY, int radius) {
        if (!Double.isFinite(mouseX) || !Double.isFinite(mouseY)) return -1;
        double dx = mouseX - centerX, dy = mouseY - centerY;
        if (dx * dx + dy * dy < DEAD_ZONE * DEAD_ZONE) return -1;
        int nearest = -1;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < COUNT; i++) {
            if (!frameContains(centerX, centerY, radius, i, mouseX, mouseY)) continue;
            double fx = frameLeft(centerX, radius, i) + FRAME_SIZE / 2.0;
            double fy = frameTop(centerY, radius, i) + FRAME_SIZE / 2.0;
            double distance = (mouseX - fx) * (mouseX - fx) + (mouseY - fy) * (mouseY - fy);
            if (distance < nearestDistance) {
                nearest = i;
                nearestDistance = distance;
            }
        }
        return nearest;
    }
}
