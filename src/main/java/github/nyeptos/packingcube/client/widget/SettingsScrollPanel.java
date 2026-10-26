package github.nyeptos.packingcube.client.widget;

import github.nyeptos.packingcube.gui.BagSettingsLayout;
import github.nyeptos.packingcube.gui.ButtonAnimation;
import github.nyeptos.packingcube.gui.ScrollbarSoundState;
import github.nyeptos.packingcube.gui.SettingsScrollState;
import github.nyeptos.packingcube.gui.WidgetSkin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Owns all right-hand widgets, clips drawing, and prevents off-viewport interaction. */
public final class SettingsScrollPanel extends AbstractContainerEventHandler
        implements Renderable, NarratableEntry {
    private record Viewport(int x, int y, int width, int height) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx < right() && my >= y && my < bottom();
        }
    }

    private record Entry(AbstractWidget widget, int localX, int originalY) {}

    private final List<Entry> entries = new ArrayList<>();
    private final SettingsScrollState scroll;
    private Viewport viewport;
    private int originX, originY;
    private int canvasTop;
    private Consumer<GuiGraphics> contentRenderer = graphics -> {};
    private boolean scrollbarDragging;
    private final ScrollbarSoundState scrollbarSounds = new ScrollbarSoundState();
    private boolean hovered;
    private double thumbGrab;

    public SettingsScrollPanel(
            BagSettingsLayout layout, int left, int top, SettingsScrollState scroll) {
        this(
                left,
                top,
                layout.viewport().x(),
                layout.viewport().y(),
                layout.viewport().width(),
                layout.viewport().height(),
                layout.content().height(),
                scroll);
    }

    /** Generic viewport constructor: no bag layout or menu is required. */
    public SettingsScrollPanel(
            int left,
            int top,
            int viewportX,
            int viewportY,
            int viewportWidth,
            int viewportHeight,
            int contentHeight,
            SettingsScrollState scroll) {
        if (viewportWidth < 1 || viewportHeight < 1 || contentHeight < 0)
            throw new IllegalArgumentException("Invalid viewport geometry");
        this.scroll = scroll;
        originX = left;
        originY = top;
        canvasTop = top + viewportY;
        viewport = new Viewport(left + viewportX, canvasTop, viewportWidth, viewportHeight);
        scroll.configure(viewportHeight, contentHeight);
    }

    public void moveTo(int left, int top) {
        int dx = left - originX, dy = top - originY;
        originX = left;
        originY = top;
        canvasTop += dy;
        viewport =
                new Viewport(
                        viewport.x() + dx, viewport.y() + dy, viewport.width(), viewport.height());
        for (Entry entry : entries) entry.widget().setX(originX + entry.localX());
        updatePositions();
    }

    public void setContentRenderer(Consumer<GuiGraphics> renderer) {
        contentRenderer = renderer;
    }

    public void add(AbstractWidget widget) {
        entries.add(new Entry(widget, widget.getX() - originX, widget.getY() - originY));
        updatePositions();
    }

    private void updatePositions() {
        for (Entry entry : entries)
            entry.widget().setY(originY + entry.originalY() - scroll.offset());
        if (getFocused() instanceof AbstractWidget focused) {
            Entry entry =
                    entries.stream().filter(e -> e.widget() == focused).findFirst().orElse(null);
            if (entry != null
                    && !scroll.fullyVisible(
                            entry.originalY() + originY - canvasTop,
                            entry.originalY() + originY - canvasTop + focused.getHeight()))
                setFocused(null);
        }
    }

    public int offset() {
        return scroll.offset();
    }

    public double unscrolledY(double mouseY) {
        return scroll.unscrolledY(mouseY);
    }

    public boolean contains(double mouseX, double mouseY) {
        return viewport.contains(mouseX, mouseY);
    }

    private boolean intersects(Entry entry) {
        return entry.widget().visible
                && scroll.intersects(
                        entry.originalY() + originY - canvasTop,
                        entry.originalY() + originY - canvasTop + entry.widget().getHeight());
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return entries.stream().filter(this::intersects).map(Entry::widget).toList();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return contains(mouseX, mouseY);
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(viewport.x(), viewport.y(), viewport.width(), viewport.height());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updatePositions();
        hovered = contains(mouseX, mouseY);
        // At either end, allow local hover/press motion into the unused inner panel margin.
        // The enclosing floating-window scissor still protects its frame and other GUIs.
        graphics.enableScissor(
                viewport.x(),
                scroll.visualClipTop(viewport.y(), ButtonAnimation.VISUAL_MARGIN),
                viewport.right(),
                scroll.visualClipBottom(viewport.bottom(), ButtonAnimation.VISUAL_MARGIN));
        graphics.pose().pushPose();
        graphics.pose().translate(originX, originY - scroll.offset(), 0);
        contentRenderer.accept(graphics);
        graphics.pose().popPose();
        for (Entry entry : entries) {
            if (intersects(entry))
                entry.widget()
                        .render(
                                graphics,
                                hovered ? mouseX : Integer.MIN_VALUE,
                                hovered ? mouseY : Integer.MIN_VALUE,
                                partialTick);
            else if (entry.widget() instanceof SkinnedButton button) button.suspendHover();
        }
        graphics.disableScissor();
        if (scroll.scrollable()) {
            int x = viewport.right() - 6;
            WidgetTextures.draw(
                    graphics, WidgetSkin.SCROLLBAR_TRACK, x, viewport.y(), 6, viewport.height());
            int thumbY = viewport.y() + scroll.thumbTop();
            boolean thumbHovered =
                    mouseX >= x
                            && mouseX < viewport.right()
                            && mouseY >= thumbY
                            && mouseY < thumbY + scroll.thumbHeight();
            if (scrollbarSounds.hover(true, thumbHovered)) UiInteractionSounds.hover();
            WidgetTextures.draw(
                    graphics,
                    WidgetSkin.thumb(scrollbarDragging, thumbHovered),
                    x,
                    thumbY,
                    6,
                    scroll.thumbHeight());
        } else scrollbarSounds.hover(false, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) return false;
        updatePositions();
        if (button == 0 && scroll.scrollable() && mouseX >= viewport.right() - 6) {
            scrollbarDragging = true;
            if (scrollbarSounds.grab(true)) UiInteractionSounds.grab();
            double thumbTop = viewport.y() + scroll.thumbTop();
            thumbGrab =
                    mouseY >= thumbTop && mouseY < thumbTop + scroll.thumbHeight()
                            ? mouseY - thumbTop
                            : scroll.thumbHeight() / 2.0;
            scroll.dragThumb(mouseY - viewport.y() - thumbGrab);
            updatePositions();
            return true;
        }
        boolean accepted = super.mouseClicked(mouseX, mouseY, button);
        if (!accepted) setFocused(null);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (scrollbarDragging && button == 0) {
            scroll.dragThumb(mouseY - viewport.y() - thumbGrab);
            updatePositions();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scrollbarDragging) {
            stopDragging();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public void stopDragging() {
        scrollbarSounds.release();
        scrollbarDragging = false;
        setDragging(false);
    }

    public boolean dragging() {
        return scrollbarDragging || isDragging();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (!contains(mouseX, mouseY)) return false;
        scroll.wheel(vertical);
        updatePositions();
        return true;
    }

    @Override
    public void setFocused(GuiEventListener listener) {
        if (listener instanceof AbstractWidget widget) {
            for (Entry entry : entries)
                if (entry.widget() == widget) {
                    scroll.ensureVisible(
                            entry.originalY() + originY - canvasTop,
                            entry.originalY() + originY - canvasTop + widget.getHeight());
                    break;
                }
        }
        super.setFocused(listener);
        for (Entry entry : entries)
            entry.widget().setY(originY + entry.originalY() - scroll.offset());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!(getFocused() instanceof EditBox)) {
            if (keyCode == 266) scroll.setOffset(scroll.offset() - viewport.height());
            else if (keyCode == 267) scroll.setOffset(scroll.offset() + viewport.height());
            else if (keyCode == 268) scroll.setOffset(0);
            else if (keyCode == 269) scroll.setOffset(scroll.maxOffset());
            else return super.keyPressed(keyCode, scanCode, modifiers);
            updatePositions();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public NarrationPriority narrationPriority() {
        return getFocused() != null
                ? NarrationPriority.FOCUSED
                : hovered ? NarrationPriority.HOVERED : NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.translatable("gui.packingcube.settings"));
        if (getFocused() instanceof NarratableEntry entry) entry.updateNarration(output.nest());
    }
}
