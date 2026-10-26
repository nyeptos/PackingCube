package github.nyeptos.packingcube.bag.input;

/** Publish the saved logical position before making the native cursor visible at that position. */
public final class BagCursorRelease {
    public interface Backend {
        void hide();

        void synchronize(BagCursorPosition.Point point);

        void position(BagCursorPosition.Point point);

        void show();
    }

    private BagCursorRelease() {}

    public static boolean restore(
            BagCursorPosition position, boolean grabbed, int width, int height, Backend backend) {
        if (!grabbed) return false;
        var saved = position.resolve(width, height);
        if (saved.isEmpty()) return false;
        var point = saved.get();
        // Leaving disabled mode can restore a platform's old position. Keep that invisible.
        backend.hide();
        backend.synchronize(point);
        backend.position(point);
        backend.show();
        return true;
    }
}
