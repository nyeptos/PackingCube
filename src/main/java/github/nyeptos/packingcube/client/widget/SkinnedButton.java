package github.nyeptos.packingcube.client.widget;

import github.nyeptos.packingcube.gui.animation.ButtonAnimation;
import github.nyeptos.packingcube.gui.animation.ButtonHoverState;
import github.nyeptos.packingcube.gui.skin.GuiSurface;
import github.nyeptos.packingcube.gui.skin.WidgetSkin;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.util.Mth;

import java.util.function.BooleanSupplier;

/** Artist skins with vanilla click, keyboard, sound, tooltip and narration behavior. */
public final class SkinnedButton extends Button {
    private enum Style {
        COMMON,
        CLOSE,
        TOGGLE
    }

    private final Style style;

    public record Feedback(ButtonAnimation animation, ButtonHoverState hover) {}

    private Feedback feedback = new Feedback(new ButtonAnimation(), new ButtonHoverState());
    private boolean pointerHeld;

    public String feedbackStyle() {
        return style.name();
    }

    public Feedback feedback() {
        return feedback;
    }

    public void feedback(Feedback value) {
        feedback = value;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (active && visible && isValidClickButton(button) && isMouseOver(mouseX, mouseY))
            pointerHeld = true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        if (pointerHeld) feedback.animation().press(Util.getMillis());
        pointerHeld = false;
        super.onRelease(mouseX, mouseY);
    }

    private BooleanSupplier selected = () -> false;

    public SkinnedButton(Button.Builder builder) {
        this(builder, Style.COMMON);
    }

    private SkinnedButton(Button.Builder builder, Style style) {
        super(builder);
        this.style = style;
    }

    public SkinnedButton selected(BooleanSupplier state) {
        selected = state;
        return this;
    }

    public static SkinnedButton close(Button.Builder builder) {
        return new SkinnedButton(builder, Style.CLOSE);
    }

    public static SkinnedButton toggle(Button.Builder builder, BooleanSupplier state) {
        return new SkinnedButton(builder, Style.TOGGLE).selected(state);
    }

    @Override
    public void playDownSound(net.minecraft.client.sounds.SoundManager ignored) {
        UiInteractionSounds.grab();
    }

    @Override
    public void onPress() {
        feedback.animation().press(Util.getMillis());
        // A selected tab still animates, but must not reset the current page when clicked again.
        if (style == Style.COMMON && selected.getAsBoolean()) return;
        super.onPress();
    }

    /** A scroll-clipped widget cannot keep a stale pointer entry across reappearance. */
    public void suspendHover() {
        feedback.hover().reset();
        feedback.animation().sample(Util.getMillis(), false, active);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        var minecraft = Minecraft.getInstance();
        var feedback =
                this.feedback
                        .hover()
                        .update(
                                active,
                                isMouseOver(mouseX, mouseY),
                                isFocused(),
                                minecraft.getLastInputType().isKeyboard());
        boolean on = selected.getAsBoolean(), hovered = feedback.highlighted();
        long now = Util.getMillis();
        if (!active
                || org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                                minecraft.getWindow().getWindow(),
                                org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT)
                        != org.lwjgl.glfw.GLFW.GLFW_PRESS) pointerHeld = false;
        var frame = this.feedback.animation().sample(now, hovered, active, pointerHeld);
        boolean pressed = active && (pointerHeld || this.feedback.animation().pressing(now));
        if (feedback.playSound()) UiInteractionSounds.hover();
        WidgetSkin skin =
                switch (style) {
                    case CLOSE -> WidgetSkin.close(active, hovered);
                    case TOGGLE -> WidgetSkin.toggle(on, active, hovered);
                    case COMMON -> WidgetSkin.button(active, on, hovered);
                };
        String overrideFile =
                pressed
                        ? skin.pressedFileName()
                        : style == Style.COMMON && hovered && !on ? "button_hovered.png" : null;
        WidgetSkin drawSkin = pressed ? skin.pressedFallback() : skin;
        ButtonAnimationRenderer.draw(
                g,
                frame,
                getX(),
                getY(),
                getWidth(),
                getHeight(),
                () -> {
                    g.setColor(1, 1, 1, alpha);
                    WidgetTextures.drawButton(
                            g, drawSkin, overrideFile, getX(), getY(), getWidth(), getHeight());
                    g.setColor(1, 1, 1, 1);
                    if (style != Style.CLOSE) {
                        int color = GuiSurface.TEXT_COLOR;
                        renderString(
                                g,
                                Minecraft.getInstance().font,
                                color | Mth.ceil(alpha * 255) << 24);
                    }
                });
    }
}
