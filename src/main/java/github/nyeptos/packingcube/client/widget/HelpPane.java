package github.nyeptos.packingcube.client.widget;

import github.nyeptos.packingcube.gui.skin.GuiRenderLayers;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Read-only explanation pane beside a settings window; it never takes input. */
public final class HelpPane {
    public static final int PADDING = 5;
    public static final int LINE_HEIGHT = 10;
    private static final int TITLE_GAP = 3;
    private static final int SCREEN_MARGIN = 2;
    private static final int WINDOW_GAP = 4;

    public record Placement(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }

    private HelpPane() {}

    /** Wrapped body lines for the given pane width. Blank entries become blank rows. */
    public static List<FormattedCharSequence> wrap(
            Font font, int width, List<Component> lines) {
        int textWidth = Math.max(1, width - PADDING * 2);
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (var line : lines) wrapped.addAll(font.split(line, textWidth));
        return wrapped;
    }

    public static int height(int wrappedLines) {
        return PADDING * 2 + LINE_HEIGHT + TITLE_GAP + Math.max(0, wrappedLines) * LINE_HEIGHT;
    }

    /**
     * Beside the window, never over it: the right side when it fits, otherwise the left, and always
     * inside the screen.
     */
    public static Placement place(
            int windowX,
            int windowY,
            int windowWidth,
            int screenWidth,
            int screenHeight,
            int width,
            int height) {
        int x = windowX + windowWidth + WINDOW_GAP;
        if (x + width > screenWidth - SCREEN_MARGIN) x = windowX - WINDOW_GAP - width;
        int maxX = Math.max(SCREEN_MARGIN, screenWidth - width - SCREEN_MARGIN);
        x = Math.max(SCREEN_MARGIN, Math.min(x, maxX));
        int maxY = Math.max(SCREEN_MARGIN, screenHeight - height - SCREEN_MARGIN);
        int y = Math.max(SCREEN_MARGIN, Math.min(windowY, maxY));
        return new Placement(x, y, width, height);
    }

    public static void render(
            GuiGraphics graphics,
            Font font,
            Placement placement,
            Component title,
            List<FormattedCharSequence> wrapped) {
        graphics.flush();
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, GuiRenderLayers.WINDOW + 1);
            graphics.fill(
                    placement.x(),
                    placement.y(),
                    placement.right(),
                    placement.bottom(),
                    0xF0101010);
            graphics.fill(
                    placement.x(),
                    placement.y(),
                    placement.right(),
                    placement.y() + 1,
                    0xFFB08A4A);
            graphics.fill(
                    placement.x(),
                    placement.bottom() - 1,
                    placement.right(),
                    placement.bottom(),
                    0xFFB08A4A);
            graphics.enableScissor(
                    placement.x(), placement.y(), placement.right(), placement.bottom());
            try {
                int x = placement.x() + PADDING;
                int y = placement.y() + PADDING;
                graphics.drawString(font, title, x, y, 0xFFF3D9A4, false);
                y += LINE_HEIGHT + TITLE_GAP;
                for (var line : wrapped) {
                    graphics.drawString(font, line, x, y, 0xFFE6E6E6, false);
                    y += LINE_HEIGHT;
                }
            } finally {
                graphics.disableScissor();
            }
            graphics.flush();
        } finally {
            graphics.pose().popPose();
        }
    }
}
