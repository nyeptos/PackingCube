package github.nyeptos.packingcube.gui;

/** One pointer-entry sound and one grab sound, never a per-frame/per-drag-event sound. */
public final class ScrollbarSoundState {
    private boolean pointerOver;
    private boolean grabbed;

    public boolean hover(boolean scrollable, boolean overThumb) {
        if (!scrollable) {
            pointerOver = false;
            grabbed = false;
            return false;
        }
        boolean play = overThumb && !pointerOver && !grabbed;
        pointerOver = overThumb;
        return play;
    }

    public boolean grab(boolean scrollable) {
        if (!scrollable || grabbed) return false;
        grabbed = true;
        // A track click can put the thumb under the pointer: don't add an entry sound on top.
        pointerOver = true;
        return true;
    }

    public void release() {
        grabbed = false;
    }
}
