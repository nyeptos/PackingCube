package github.nyeptos.packingcube.client.widget;

import github.nyeptos.packingcube.gui.ButtonAnimation;
import github.nyeptos.packingcube.gui.GuiSurface;
import github.nyeptos.packingcube.gui.WidgetSkin;

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
    private final ButtonAnimation animation = new ButtonAnimation();
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
    public void onPress() {
        animation.press(Util.getMillis());
        // A selected tab still animates, but must not reset the current page when clicked again.
        if (style == Style.COMMON && selected.getAsBoolean()) return;
        super.onPress();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean on = selected.getAsBoolean(), hovered = isHoveredOrFocused();
        WidgetSkin skin =
                switch (style) {
                    case CLOSE -> WidgetSkin.close(active, hovered);
                    case TOGGLE -> WidgetSkin.toggle(on, active, hovered);
                    case COMMON -> WidgetSkin.button(active, on, hovered);
                };
        ButtonAnimationRenderer.draw(
                g,
                animation.sample(Util.getMillis(), hovered, active),
                getX(),
                getY(),
                getWidth(),
                getHeight(),
                () -> {
                    g.setColor(1, 1, 1, alpha);
                    WidgetTextures.draw(g, skin, getX(), getY(), getWidth(), getHeight());
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
