package github.nyeptos.packingcube.gui;

/** Local render-layer contract for opt-in floating containers (Minecraft 1.21.1). */
public final class GuiRenderLayers {
    private GuiRenderLayers() {}

    public static final int WINDOW = 400;
    public static final int CARRIED_LIFT = 250;
    public static final int TOOLTIP_LIFT = 400;
    // Vanilla GuiGraphics / AbstractContainerScreen offsets, kept explicit for upgrade checks.
    public static final int ITEM = 150;
    public static final int DECORATION = 200;
    public static final int VANILLA_CARRIED = 232;
    public static final int VANILLA_TOOLTIP = 400;
}
