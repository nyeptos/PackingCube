package github.nyeptos.packingcube.gui.skin;

/** Artist-supplied backgrounds. GUI label text is white in every interaction state. */
public enum GuiSurface {
    BAG_INVENTORY("gui/container/bag_inventory.png", 176, 132, 0),
    SETTINGS_PANEL("gui/container/settings_panel.png", 244, 132, 4),
    RADIAL_BACKGROUND("gui/radial/background.png", 220, 220, 4);
    public static final int TEXT_COLOR = 0xffffff;
    public final String path;
    public final int width, height, border;

    GuiSurface(String path, int width, int height, int border) {
        this.path = "textures/" + path;
        this.width = width;
        this.height = height;
        this.border = border;
    }
}
