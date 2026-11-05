package github.nyeptos.packingcube.client.widget;

import github.nyeptos.packingcube.gui.animation.SliderSoundState;
import github.nyeptos.packingcube.gui.input.SliderGeometry;
import github.nyeptos.packingcube.gui.skin.WidgetSkin;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Integer slider drawn with the artist-supplied track and knob. */
public final class SkinnedSlider extends AbstractWidget {
    private static final int TRACK_HEIGHT = 12;

    private final IntSupplier min;
    private final IntSupplier max;
    private final IntSupplier value;
    private final IntConsumer change;
    private final SliderSoundState sounds = new SliderSoundState();
    private boolean dragging;
    private boolean hovered;
    private int dragValue;

    public SkinnedSlider(
            int x,
            int y,
            int width,
            int height,
            IntSupplier min,
            IntSupplier max,
            IntSupplier value,
            IntConsumer change,
            Component narration) {
        super(x, y, width, height, narration);
        this.min = min;
        this.max = max;
        this.value = value;
        this.change = change;
    }

    private int minimum() {
        return min.getAsInt();
    }

    private int maximum() {
        return Math.max(minimum(), max.getAsInt());
    }

    public int current() {
        return dragging
                ? dragValue
                : SliderGeometry.clamp(value.getAsInt(), minimum(), maximum());
    }

    private int trackY() {
        return getY() + (getHeight() - TRACK_HEIGHT) / 2;
    }

    private int knobX() {
        return SliderGeometry.knobX(current(), minimum(), maximum(), getX(), getWidth());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || button != 0 || !isMouseOver(mouseX, mouseY)) return false;
        dragging = true;
        sounds.reset();
        dragValue = SliderGeometry.valueAt(mouseX, minimum(), maximum(), getX(), getWidth());
        change.accept(dragValue);
        UiInteractionSounds.grab();
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (!dragging || button != 0) return false;
        int next = SliderGeometry.valueAt(mouseX, minimum(), maximum(), getX(), getWidth());
        if (next != dragValue) {
            dragValue = next;
            change.accept(next);
            if (sounds.step(Util.getMillis())) UiInteractionSounds.slide();
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!dragging || button != 0) return false;
        dragging = false;
        return true;
    }

    /** Wheel steps by one, or ten with Shift held. Returns false when the value cannot move. */
    public boolean scrolled(double delta, boolean fast) {
        if (!active || !visible || delta == 0) return false;
        int next =
                SliderGeometry.clamp(
                        current() + (fast ? 10 : 1) * (delta > 0 ? 1 : -1),
                        minimum(),
                        maximum());
        if (next == current()) return false;
        change.accept(next);
        if (sounds.step(Util.getMillis())) UiInteractionSounds.slide();
        return true;
    }

    public boolean dragging() {
        return dragging;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean over = active && visible && isMouseOver(mouseX, mouseY);
        if (over && !hovered) UiInteractionSounds.hover();
        hovered = over;
        WidgetTextures.draw(
                g,
                WidgetSkin.SLIDER_TRACK,
                SliderGeometry.trackX(getX()),
                trackY(),
                SliderGeometry.trackWidth(getWidth()),
                TRACK_HEIGHT);
        WidgetTextures.draw(
                g, WidgetSkin.knob(dragging, over), knobX(), trackY(), SliderGeometry.KNOB_WIDTH,
                TRACK_HEIGHT);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
        output.add(NarratedElementType.USAGE, Component.literal(Integer.toString(current())));
    }
}
