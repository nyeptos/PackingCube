package github.nyeptos.packingcube.gui;

/** Shared artwork for all pouch types; empty slots use the same normal frame. */
public enum RadialSlotSkin {
    NORMAL("slot.png"),
    HOVERED("slot_hovered.png"),
    PRESSED("slot_pressed.png");

    public static final int SIZE = 36;
    public final String path;

    RadialSlotSkin(String file) {
        path = "textures/gui/radial/" + file;
    }

    public static RadialSlotSkin select(boolean occupied, boolean hovered, boolean pressed) {
        if (!occupied) return NORMAL;
        if (pressed) return PRESSED;
        return hovered ? HOVERED : NORMAL;
    }
}
