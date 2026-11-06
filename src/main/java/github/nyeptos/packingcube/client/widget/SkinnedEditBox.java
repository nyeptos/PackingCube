package github.nyeptos.packingcube.client.widget;

import github.nyeptos.packingcube.gui.animation.SlotFeedback;
import github.nyeptos.packingcube.gui.skin.GuiSurface;
import github.nyeptos.packingcube.gui.skin.WidgetSkin;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Only substitutes the border sprite; text selection, cursor, scrolling and input stay vanilla. */
public final class SkinnedEditBox extends EditBox {
    private static final String HOVER_ID = "search";
    private final SlotFeedback feedback = new SlotFeedback();
    private boolean suppressVanillaBorder;
    private boolean editable = true;

    public SkinnedEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
        setTextColor(GuiSurface.TEXT_COLOR);
        setTextColorUneditable(GuiSurface.TEXT_COLOR);
        setTextShadow(true);
    }

    @Override
    public boolean isBordered() {
        return !suppressVanillaBorder && super.isBordered();
    }

    // EditBox asks the virtual isBordered() for text width; preserve the real 4px insets.
    @Override
    public int getInnerWidth() {
        return super.isBordered() ? getWidth() - 8 : getWidth();
    }

    @Override
    public void setEditable(boolean value) {
        super.setEditable(value);
        editable = value;
    }

    /**
     * The owning panel resolves the pointer in the space it hit-tests clicks, so a field that is
     * scrolled or clipped away never reacts. Returns true only on the frame the pointer enters.
     */
    public boolean setPointerOver(boolean over) {
        return feedback.enter(over ? HOVER_ID : null);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (isVisible() && super.isBordered()) {
            boolean active = isActive() && editable;
            WidgetTextures.draw(
                    g,
                    WidgetSkin.search(active, isFocused()),
                    getX(),
                    getY(),
                    getWidth(),
                    getHeight());
            // The focused artwork doubles as the hover highlight, faded in like the sample slots.
            var frame = feedback.frame(HOVER_ID, Util.getMillis(), false);
            if (active && !isFocused() && frame.hover() > .001F)
                drawAlpha(g, WidgetSkin.SEARCH_FOCUSED, .7F * frame.hover());
        }
        suppressVanillaBorder = true;
        try {
            super.renderWidget(g, mouseX, mouseY, partialTick);
        } finally {
            suppressVanillaBorder = false;
        }
    }

    private void drawAlpha(GuiGraphics g, WidgetSkin skin, float alpha) {
        g.flush();
        g.setColor(1, 1, 1, alpha);
        try {
            WidgetTextures.draw(g, skin, getX(), getY(), getWidth(), getHeight());
        } finally {
            g.setColor(1, 1, 1, 1);
        }
    }
}
