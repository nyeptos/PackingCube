package github.nyeptos.packingcube.gui;

import java.util.Locale;

/** Only artist-supplied assets. Missing common-button variants deliberately reuse normal. */
public enum WidgetSkin {
    BUTTON_NORMAL(20, 20, 3, 3),
    BUTTON_SELECTED(20, 20, 3, 3),
    BUTTON_SELECTED_HOVERED(20, 20, 3, 3),
    CLOSE_NORMAL(14, 14, 0, 0),
    CLOSE_HOVERED(14, 14, 0, 0),
    CLOSE_DISABLED(14, 14, 0, 0),
    TOGGLE_ON_NORMAL(40, 12, 3, 3),
    TOGGLE_ON_HOVERED(40, 12, 3, 3),
    TOGGLE_ON_DISABLED(40, 12, 3, 3),
    TOGGLE_OFF_NORMAL(40, 12, 3, 3),
    TOGGLE_OFF_HOVERED(40, 12, 3, 3),
    TOGGLE_OFF_DISABLED(40, 12, 3, 3),
    SEARCH_NORMAL(20, 18, 3, 3),
    SEARCH_FOCUSED(20, 18, 3, 3),
    SEARCH_DISABLED(20, 18, 3, 3),
    GHOST_SLOT(18, 18, 0, 0),
    SCROLLBAR_TRACK(6, 12, 0, 2),
    SCROLLBAR_THUMB_NORMAL(6, 12, 0, 2),
    SCROLLBAR_THUMB_HOVERED(6, 12, 0, 2),
    SCROLLBAR_THUMB_DRAGGING(6, 12, 0, 2);
    public final int width, height, borderX, borderY;

    WidgetSkin(int width, int height, int borderX, int borderY) {
        this.width = width;
        this.height = height;
        this.borderX = borderX;
        this.borderY = borderY;
    }

    public String fileName() {
        return name().toLowerCase(Locale.ROOT) + ".png";
    }

    public static WidgetSkin button(boolean active, boolean selected, boolean hovered) {
        if (active && selected) return hovered ? BUTTON_SELECTED_HOVERED : BUTTON_SELECTED;
        return BUTTON_NORMAL;
    }

    public static WidgetSkin toggle(boolean enabled, boolean active, boolean hovered) {
        if (enabled)
            return !active ? TOGGLE_ON_DISABLED : hovered ? TOGGLE_ON_HOVERED : TOGGLE_ON_NORMAL;
        return !active ? TOGGLE_OFF_DISABLED : hovered ? TOGGLE_OFF_HOVERED : TOGGLE_OFF_NORMAL;
    }

    public static WidgetSkin close(boolean active, boolean hovered) {
        return !active ? CLOSE_DISABLED : hovered ? CLOSE_HOVERED : CLOSE_NORMAL;
    }

    public static WidgetSkin search(boolean active, boolean focused) {
        return !active ? SEARCH_DISABLED : focused ? SEARCH_FOCUSED : SEARCH_NORMAL;
    }

    public static WidgetSkin thumb(boolean dragging, boolean hovered) {
        return dragging
                ? SCROLLBAR_THUMB_DRAGGING
                : hovered ? SCROLLBAR_THUMB_HOVERED : SCROLLBAR_THUMB_NORMAL;
    }
}
