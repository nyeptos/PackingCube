package github.nyeptos.packingcube.client;

import github.nyeptos.packingcube.bag.BagKeyActions;
import github.nyeptos.packingcube.bag.RadialGeometry;
import github.nyeptos.packingcube.bag.RadialKeyState;
import github.nyeptos.packingcube.bag.RadialSelectionFeedback;
import github.nyeptos.packingcube.client.widget.ButtonAnimationRenderer;
import github.nyeptos.packingcube.client.widget.WidgetTextures;
import github.nyeptos.packingcube.gui.ButtonAnimation;
import github.nyeptos.packingcube.gui.GuiSurface;
import github.nyeptos.packingcube.gui.RadialLayout;
import github.nyeptos.packingcube.gui.RadialSlotSkin;
import github.nyeptos.packingcube.gui.WidgetSkin;
import github.nyeptos.packingcube.menu.BagRadialMenu;
import github.nyeptos.packingcube.network.RadialSelectPayload;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import org.lwjgl.glfw.GLFW;

/**
 * Artist-background radial selector. Icons are sanitized server-synchronized menu slots, not real
 * items.
 */
public final class BagRadialScreen extends AbstractContainerScreen<BagRadialMenu> {
    private final BagCursorMemory cursorMemory = new BagCursorMemory();
    private int cx, cy, radius, hovered = -1;
    private RadialLayout backdrop;
    private RadialKeyState trigger;
    private boolean sent;
    private int pressedSlot = -1;
    private RadialSelectionFeedback feedback;
    private int manageX, manageY;
    private final ButtonAnimation[] slotAnimations = new ButtonAnimation[RadialGeometry.COUNT];
    private final ButtonAnimation managementAnimation = new ButtonAnimation();

    {
        for (int i = 0; i < slotAnimations.length; i++) slotAnimations[i] = new ButtonAnimation();
    }

    public BagRadialScreen(BagRadialMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void added() {
        super.added();
        cursorMemory.added();
    }

    @Override
    public void removed() {
        // Capture before vanilla grabs the mouse and centers it for camera control.
        cursorMemory.remember();
        super.removed();
    }

    private static Component text(String suffix) {
        return Component.translatable("gui.packingcube.radial_" + suffix);
    }

    @Override
    protected void init() {
        super.init();
        backdrop = RadialLayout.fit(width, height);
        cx = backdrop.centerX();
        cy = backdrop.centerY();
        radius = backdrop.radius();
        manageX = backdrop.manageX();
        manageY = backdrop.manageY();
        hovered = -1;
        feedback = new RadialSelectionFeedback();
        trigger = new RadialKeyState(BagKeyHandler.bindingDown());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!minecraft.isWindowActive()) {
            onClose();
            return;
        }
        if (trigger.held() && !BagKeyHandler.bindingDown()) releaseTrigger();
    }

    private void pressTrigger() {
        if (!sent && trigger.press() == RadialKeyState.Action.CLOSE) {
            BagKeyHandler.latch(BagKeyActions.Action.RADIAL);
            onClose();
        }
    }

    private void releaseTrigger() {
        trigger.release();
    }

    private int hoveredAt(double x, double y) {
        if (x >= manageX && x < manageX + 130 && y >= manageY && y < manageY + 18) return -1;
        return RadialGeometry.hit(x - cx, y - cy);
    }

    private void choose(int index) {
        if (sent) return;
        if (index < 0 || index >= RadialGeometry.COUNT || menu.icon(index).isEmpty()) {
            onClose();
            return;
        }
        send(index);
    }

    private void send(int index) {
        if (sent) return;
        sent = true;
        pressedSlot = index;
        if (index >= 0 && index < slotAnimations.length)
            slotAnimations[index].press(Util.getMillis());
        else managementAnimation.press(Util.getMillis());
        clickSound(1.8F);
        PacketDistributor.sendToServer(new RadialSelectPayload(menu.containerId, index));
        // Keep this menu open until the server opens the selected bag. Never send a close first.
    }

    private boolean overManagement(double x, double y) {
        return x >= manageX && x < manageX + 130 && y >= manageY && y < manageY + 18;
    }

    private void clickSound(float pitch) {
        minecraft
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.25F));
    }

    private void updateHover(double x, double y) {
        hovered = hoveredAt(x, y);
        int target =
                overManagement(x, y)
                        ? RadialGeometry.COUNT
                        : hovered >= 0 && !menu.icon(hovered).isEmpty() ? hovered : -1;
        if (!sent && feedback != null && feedback.moveTo(target)) clickSound(1.6F);
    }

    @Override
    public void mouseMoved(double x, double y) {
        updateHover(x, y);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (sent) return true;
        if (BagKeyHandler.OPEN_RADIAL.matchesMouse(button)
                && BagKeyHandler.radialModifierActive()) {
            pressTrigger();
            return true;
        }
        if (button == 1) {
            onClose();
            return true;
        }
        if (button == 0) {
            if (x >= manageX && x < manageX + 130 && y >= manageY && y < manageY + 18) send(-1);
            else choose(hoveredAt(x, y));
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (BagKeyHandler.OPEN_RADIAL.matchesMouse(button)) releaseTrigger();
        return true;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE || minecraft.options.keyInventory.matches(key, scan)) {
            onClose();
            return true;
        }
        if (BagKeyHandler.OPEN_RADIAL.matches(key, scan) && BagKeyHandler.radialModifierActive()) {
            pressTrigger();
            return true;
        }
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) choose(key - GLFW.GLFW_KEY_1);
        return true;
    }

    @Override
    public boolean keyReleased(int key, int scan, int modifiers) {
        if (BagKeyHandler.OPEN_RADIAL.matches(key, scan)) releaseTrigger();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // Avoid the base container renderer: it implements inventory actions and real slot
        // tooltips.
        g.fill(0, 0, width, height, 0xB0101420);
        WidgetTextures.draw(
                g,
                GuiSurface.RADIAL_BACKGROUND,
                backdrop.left(),
                backdrop.top(),
                backdrop.side(),
                backdrop.side());
        updateHover(mouseX, mouseY);
        g.drawCenteredString(font, title, cx, backdrop.titleY(), 0xFFFFFF);
        boolean any = false;
        for (int i = 0; i < RadialGeometry.COUNT; i++) {
            var icon = menu.icon(i);
            any |= !icon.isEmpty();
            int x = RadialGeometry.x(cx, radius, i), y = RadialGeometry.y(cy, radius, i);
            boolean active = i == hovered && !icon.isEmpty();
            int number = i + 1;
            var skin = RadialSlotSkin.select(!icon.isEmpty(), active, sent && pressedSlot == i);
            ButtonAnimationRenderer.draw(
                    g,
                    slotAnimations[i].sample(Util.getMillis(), active, !icon.isEmpty()),
                    x - 18,
                    y - 18,
                    36,
                    36,
                    () -> {
                        WidgetTextures.draw(g, skin, x - 18, y - 18);
                        if (!icon.isEmpty()) g.renderItem(icon, x - 8, y - 11);
                        else g.drawCenteredString(font, "—", x, y - 9, 0xFFFFFF);
                        g.drawCenteredString(font, Integer.toString(number), x, y + 7, 0xFFFFFF);
                    });
        }
        Component label =
                !any
                        ? text("empty")
                        : hovered < 0
                                ? text("cancel")
                                : menu.icon(hovered).isEmpty()
                                        ? text("empty_slot")
                                        : menu.icon(hovered).getHoverName();
        String shortLabel = font.plainSubstrByWidth(label.getString(), 86);
        g.drawCenteredString(font, shortLabel, cx, cy - 10, 0xFFFFFF);
        g.drawCenteredString(
                font,
                text(hovered < 0 || menu.icon(hovered).isEmpty() ? "toggle_hint" : "click"),
                cx,
                cy + 4,
                0xFFFFFF);
        boolean manageHover =
                mouseX >= manageX
                        && mouseX < manageX + 130
                        && mouseY >= manageY
                        && mouseY < manageY + 18;
        ButtonAnimationRenderer.draw(
                g,
                managementAnimation.sample(Util.getMillis(), manageHover, true),
                manageX,
                manageY,
                130,
                18,
                () -> {
                    WidgetTextures.draw(
                            g,
                            WidgetSkin.button(true, false, manageHover),
                            manageX,
                            manageY,
                            130,
                            18);
                    g.drawCenteredString(font, text("manage"), cx, manageY + 5, 0xFFFFFF);
                });
        // The same Item tooltip path as inventory stacks, backed only by sanitized filter preview
        // data.
        if (!sent && hovered >= 0 && !menu.icon(hovered).isEmpty()) {
            g.renderTooltip(font, menu.icon(hovered), mouseX, mouseY);
        }
    }
}
