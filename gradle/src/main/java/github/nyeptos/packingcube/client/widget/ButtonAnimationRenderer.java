package github.nyeptos.packingcube.client.widget;

import github.nyeptos.packingcube.gui.ButtonAnimation;

import net.minecraft.client.gui.GuiGraphics;

/** Balanced local transform for a button and its label/icon, not its input bounds. */
public final class ButtonAnimationRenderer {
    private ButtonAnimationRenderer() {}

    public static void draw(
            GuiGraphics graphics,
            ButtonAnimation.Frame frame,
            int x,
            int y,
            int width,
            int height,
            Runnable contents) {
        graphics.flush();
        graphics.pose().pushPose();
        try {
            float cx = x + width / 2f, cy = y + height / 2f;
            graphics.pose().translate(cx, cy + frame.offsetY(), 0);
            graphics.pose().scale(frame.scale(), frame.scale(), 1);
            graphics.pose().translate(-cx, -cy, 0);
            contents.run();
        } finally {
            graphics.flush();
            graphics.pose().popPose();
        }
    }
}
