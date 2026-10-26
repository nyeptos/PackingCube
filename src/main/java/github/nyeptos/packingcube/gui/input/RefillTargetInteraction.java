package github.nyeptos.packingcube.gui.input;

/** Input state for an opt-in slot selection mode. Does not move or own any ItemStack. */
public final class RefillTargetInteraction {
    private boolean selecting;
    private String pressed;
    private int source;

    /** Pin the source for one modal gesture; later page navigation cannot retarget its packet. */
    public void begin(int source) {
        if (source < 0 || source >= 9) throw new IllegalArgumentException("Invalid source slot");
        this.source = source;
        selecting(true);
    }

    public int source() {
        return source;
    }

    public boolean selecting() {
        return selecting;
    }

    public void selecting(boolean value) {
        selecting = value;
        pressed = null;
    }

    public void press(String id, int button) {
        pressed = selecting && button == 0 ? id : null;
    }

    public String release(String id, int button) {
        String result =
                selecting && button == 0 && pressed != null && pressed.equals(id) ? pressed : null;
        pressed = null;
        if (result != null) selecting = false;
        return result;
    }

    public boolean pressed(String id) {
        return selecting && id != null && id.equals(pressed);
    }
}
