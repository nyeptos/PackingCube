package github.nyeptos.packingcube.gui.input;

/** Pure scrolling policy shared by drawing, pointer translation and scrollbar dragging. */
public final class SettingsScrollState {
    private int offset;
    private int viewportHeight = 1;
    private int contentHeight = 1;

    public void configure(int viewportHeight, int contentHeight) {
        if (viewportHeight <= 0 || contentHeight < 0)
            throw new IllegalArgumentException("Invalid scroll dimensions");
        this.viewportHeight = viewportHeight;
        this.contentHeight = contentHeight;
        setOffset(offset);
    }

    public int offset() {
        return offset;
    }

    public int maxOffset() {
        return Math.max(0, contentHeight - viewportHeight);
    }

    public boolean scrollable() {
        return maxOffset() > 0;
    }

    public void setOffset(double offset) {
        if (!Double.isFinite(offset)) return;
        this.offset = (int) Math.round(Math.clamp(offset, 0, maxOffset()));
    }

    public void wheel(double vertical) {
        setOffset(offset - vertical * 24);
    }

    public int thumbHeight() {
        if (!scrollable()) return viewportHeight;
        return Math.min(
                viewportHeight,
                Math.max(12, (int) ((long) viewportHeight * viewportHeight / contentHeight)));
    }

    public int thumbTop() {
        return scrollable()
                ? (int) Math.round((double) offset / maxOffset() * (viewportHeight - thumbHeight()))
                : 0;
    }

    public void dragThumb(double top) {
        int travel = viewportHeight - thumbHeight();
        if (travel > 0) setOffset(top / travel * maxOffset());
    }

    /** End overscan is visual only; scrolling, hit boxes and thumb geometry stay unchanged. */
    public int visualClipTop(int viewportTop, int margin) {
        return viewportTop - (offset == 0 ? Math.max(0, margin) : 0);
    }

    public int visualClipBottom(int viewportBottom, int margin) {
        return viewportBottom + (offset == maxOffset() ? Math.max(0, margin) : 0);
    }

    public double unscrolledY(double screenY) {
        return screenY + offset;
    }

    public void ensureVisible(int top, int bottom) {
        if (top < offset) setOffset(top);
        else if (bottom > offset + viewportHeight) setOffset(bottom - viewportHeight);
    }

    public boolean intersects(int top, int bottom) {
        return bottom > offset && top < offset + viewportHeight;
    }

    public boolean fullyVisible(int top, int bottom) {
        return top >= offset && bottom <= offset + viewportHeight;
    }
}
