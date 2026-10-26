package github.nyeptos.packingcube.client.widget;

import github.nyeptos.packingcube.gui.skin.GuiSurface;
import github.nyeptos.packingcube.gui.skin.WidgetSkin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Only substitutes the border sprite; text selection, cursor, scrolling and input stay vanilla. */
public final class SkinnedEditBox extends EditBox {
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

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (isVisible() && super.isBordered())
            WidgetTextures.draw(
                    g,
                    WidgetSkin.search(isActive() && editable, isFocused()),
                    getX(),
                    getY(),
                    getWidth(),
                    getHeight());
        suppressVanillaBorder = true;
        try {
            super.renderWidget(g, mouseX, mouseY, partialTick);
        } finally {
            suppressVanillaBorder = false;
        }
    }
}
