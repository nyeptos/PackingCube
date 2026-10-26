package github.nyeptos.packingcube.client.widget;

import github.nyeptos.packingcube.gui.skin.GuiRenderLayers;
import github.nyeptos.packingcube.gui.skin.GuiSurface;
import github.nyeptos.packingcube.gui.skin.TooltipQueue;
import github.nyeptos.packingcube.gui.window.FloatingWindowState;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Reusable, opt-in floating chrome. Hosts provide content and block their own covered controls. */
public final class FloatingWindow {
    public static final int OVERLAY_Z = GuiRenderLayers.WINDOW;
    private final FloatingWindowState state = new FloatingWindowState();

    private record TooltipRequest(
            List<FormattedCharSequence> lines, ClientTooltipPositioner positioner) {}

    private final TooltipQueue<TooltipRequest> tooltips = new TooltipQueue<>();

    public void queueTooltip(
            List<FormattedCharSequence> lines,
            ClientTooltipPositioner positioner,
            boolean override) {
        tooltips.offer(new TooltipRequest(List.copyOf(lines), positioner), override);
    }

    public void clearTooltips() {
        tooltips.clear();
    }

    /**
     * Render after window/content/carried items, outside clipping. Vanilla tooltips add their own
     * Z=400.
     */
    public void renderTooltips(
            GuiGraphics graphics, Font font, int mouseX, int mouseY, Runnable directTooltips) {
        var deferred = tooltips.take();
        graphics.flush();
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, GuiRenderLayers.TOOLTIP_LIFT);
            directTooltips.run();
            if (deferred != null)
                graphics.renderTooltip(
                        font, deferred.lines(), deferred.positioner(), mouseX, mouseY);
            graphics.flush();
        } finally {
            graphics.pose().popPose();
        }
    }

    /**
     * Call ONCE at the end of an AbstractContainerScreen host's renderLabels, after all windows.
     * Intentionally lifts the remainder of that container-owned pose (carried item + snapback).
     * Vanilla's render() pop restores the pose. Never call from an ordinary Screen render pass.
     */
    public static void prepareContainerCursorLayer(GuiGraphics graphics) {
        graphics.flush();
        graphics.pose().translate(0, 0, GuiRenderLayers.CARRIED_LIFT);
    }

    public FloatingWindowState state() {
        return state;
    }

    public boolean contains(double x, double y) {
        return state.contains(x, y);
    }

    public boolean mouseClicked(double x, double y, int button, boolean allowTransform) {
        return state.press(x, y, button, allowTransform);
    }

    public boolean mouseDragged(double x, double y, int button) {
        if (button != 0 || !state.interacting()) return false;
        state.drag(x, y);
        return true;
    }

    public boolean mouseReleased(int button) {
        return state.release(button);
    }

    /**
     * Screen coordinates. Flush both sides: deferred item/text batches must not cross this layer.
     */
    public void render(
            GuiGraphics graphics,
            Font font,
            Component title,
            int mouseX,
            int mouseY,
            Runnable content) {
        graphics.flush();
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, OVERLAY_Z);
            int x = state.x(),
                    y = state.y(),
                    right = x + state.width(),
                    bottom = y + state.height();
            WidgetTextures.draw(
                    graphics, GuiSurface.SETTINGS_PANEL, x, y, state.width(), state.height());
            graphics.enableScissor(x, y, right, bottom);
            try {
                content.run();
            } finally {
                graphics.disableScissor();
            }
            // Preserve the artist's gold border; edge hit-testing does not need a gray overlay.
            graphics.flush();
        } finally {
            graphics.pose().popPose();
        }
    }
}
