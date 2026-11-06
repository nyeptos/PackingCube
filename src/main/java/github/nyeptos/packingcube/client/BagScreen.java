package github.nyeptos.packingcube.client;

import github.nyeptos.packingcube.bag.BulkStore;
import github.nyeptos.packingcube.bag.refill.RefillPouchSettings;
import github.nyeptos.packingcube.client.widget.BagSettingsView;
import github.nyeptos.packingcube.client.widget.FloatingWindow;
import github.nyeptos.packingcube.client.widget.RefillTargetPanel;
import github.nyeptos.packingcube.client.widget.SkinnedButton;
import github.nyeptos.packingcube.client.widget.UiInteractionSounds;
import github.nyeptos.packingcube.client.widget.WidgetTextures;
import github.nyeptos.packingcube.gui.input.BagKeyActions;
import github.nyeptos.packingcube.gui.input.RefillSettingsView;
import github.nyeptos.packingcube.gui.layout.BagSettingsLayout;
import github.nyeptos.packingcube.gui.skin.GuiSurface;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.network.BagNavigationPayload;
import github.nyeptos.packingcube.network.FilterActionPayload;
import github.nyeptos.packingcube.network.FilterActions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Container/navigation host. Filter widgets and their layout are reusable independent components.
 */
public final class BagScreen extends AbstractContainerScreen<BagMenu> {
    private boolean editing;
    private boolean directHeld;
    private boolean radialHeld;
    private final FloatingWindow settingsWindow = new FloatingWindow();
    private float renderPartialTick;
    private final RefillSettingsView refillView = new RefillSettingsView();
    private final RefillTargetPanel targetPanel = new RefillTargetPanel();
    private boolean refillRebuildRequested;
    private Button settingsButton;

    private final BagSettingsView settings =
            new BagSettingsView(
                    new BagSettingsView.Host() {
                        @Override
                        public BagMenu menu() {
                            return menu;
                        }

                        @Override
                        public boolean editing() {
                            return editing;
                        }

                        @Override
                        public boolean selectingTargets() {
                            return targetPanel.selecting();
                        }

                        @Override
                        public RefillSettingsView refillView() {
                            return refillView;
                        }

                        @Override
                        public Font font() {
                            return font;
                        }

                        @Override
                        public <T extends GuiEventListener & Renderable & NarratableEntry>
                                T addWidget(T listener) {
                            return BagScreen.this.addWidget(listener);
                        }

                        @Override
                        public <T extends GuiEventListener & Renderable & NarratableEntry>
                                T addRenderableWidget(T widget) {
                            return BagScreen.this.addRenderableWidget(widget);
                        }

                        @Override
                        public void beginTargetSelection() {
                            targetPanel.beginSelection(refillView.source());
                        }

                        @Override
                        public void beginTargetPause() {
                            targetPanel.beginPause(refillView.source());
                        }

                        @Override
                        public void rebuild() {
                            rebuildWidgets();
                        }

                        @Override
                        public boolean beginModal() {
                            return beginModalMode();
                        }

                        @Override
                        public void requestRefillRebuild() {
                            refillRebuildRequested = true;
                        }

                        @Override
                        public void sendFilterAction(
                                int operation, int sampleSlot, String field, String value) {
                            send(operation, sampleSlot, field, value);
                        }

                        @Override
                        public ItemStack snapshot() {
                            return menu.filterSnapshot();
                        }

                        @Override
                        public int destinationLimit(String destination) {
                            return refillDestinationLimit(destination);
                        }

                        @Override
                        public void closeSettings() {
                            BagScreen.this.closeSettings();
                        }

                        @Override
                        public int leftPos() {
                            return leftPos;
                        }

                        @Override
                        public int topPos() {
                            return topPos;
                        }

                        @Override
                        public int screenWidth() {
                            return width;
                        }

                        @Override
                        public int screenHeight() {
                            return height;
                        }

                        @Override
                        public FloatingWindow settingsWindow() {
                            return settingsWindow;
                        }

                        @Override
                        public void focusPanel() {
                            setFocused(settings.panel());
                        }

                        @Override
                        public boolean panelFocused() {
                            return getFocused() == settings.panel();
                        }

                        @Override
                        public void cancelTargetPress() {
                            targetPanel.cancelPress();
                        }

                        @Override
                        public void cancelInventoryDrag() {
                            BagScreen.this.cancelInventoryDrag();
                        }
                    });

    public BagScreen(BagMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = menu.isBulk() ? 140 : 132;
        inventoryLabelY = 38;
    }

    @Override
    public void removed() {
        settingsWindow.state().stop();
        settingsWindow.clearTooltips();
        super.removed();
    }

    private static Component text(String key) {
        return Component.translatable("gui.packingcube." + key);
    }

    private int preferredRefillHeight() {
        var probe = BagSettingsLayout.floating(244, 200, refillView.rows());
        return Math.max(
                80,
                Math.min(height - 8, refillView.contentHeight(probe, menu.filterSnapshot()) + 32));
    }

    @Override
    protected void init() {
        resetControls();
        super.init();
        restoreInteractionMode();
        layoutWindow();
        directHeld = BagKeyHandler.bindingDown(BagKeyHandler.OPEN_BIG_BAG);
        radialHeld = BagKeyHandler.bindingDown();
        buildChrome();
        settings.build();
        settings.buildControls();
        settings.bindContent();
    }

    private void resetControls() {
        settingsWindow.clearTooltips();
        settings.reset();
        settingsButton = null;
        imageWidth = 176;
        imageHeight = menu.isBulk() ? 140 : 132;
        inventoryLabelY = 38;
    }

    /** A running slot mode owns the screen until it ends, whichever page started it. */
    private void restoreInteractionMode() {
        // A running slot mode owns the screen until it ends, whichever page started it. Only
        // leaving the settings, or rebuilding from another page outside a mode, cancels it.
        boolean modal = targetPanel.selecting();
        if (!editing || (!modal && refillView.page() != RefillSettingsView.DESTINATIONS))
            targetPanel.selecting(false);
    }

    private void layoutWindow() {
        var anchor = BagSettingsLayout.inventoryAnchor(width, height, imageHeight);
        leftPos = anchor.x();
        topPos = anchor.y();
        {
            targetPanel.source(refillView.source());
            targetPanel.update(menu, leftPos, topPos);
        }
        settings.place(null, leftPos, topPos);
        if (editing) {
            // On narrow screens the panel may overlap the inventory instead of refusing to open.
            int preferredWidth = Math.max(124, Math.min(244, width - leftPos - 184));
            settingsWindow
                    .state()
                    .configure(
                            leftPos + 176,
                            (height - (menu.isRefill() ? preferredRefillHeight() : 373)) / 2,
                            preferredWidth,
                            menu.isRefill()
                                    ? preferredRefillHeight()
                                    : BagSettingsLayout.preferredHeight(height, 2),
                            124,
                            80,
                            width,
                            height);
            settings.place(
                    BagSettingsLayout.floating(
                            settingsWindow.state().width(),
                            settingsWindow.state().height(),
                            menu.isRefill() ? refillView.rows() : 2),
                    settingsWindow.state().x() - 176,
                    settingsWindow.state().y());
        }
    }

    private void buildChrome() {
        if (!menu.isModules() && (!editing || targetPanel.selecting())) {
            settingsButton =
                    addRenderableWidget(
                            Button.builder(
                                            targetPanel.selecting()
                                                    ? text(
                                                            targetPanel.pausing()
                                                                    ? "refill_select_done"
                                                                    : "refill_select_cancel")
                                                    : text("settings"),
                                            button -> {
                                                if (targetPanel.selecting())
                                                    targetPanel.selecting(false);
                                                else editing = !editing;
                                                rebuildWidgets();
                                            })
                                    .bounds(
                                            leftPos + (menu.isBulk() ? 136 : 115),
                                            topPos + 2,
                                            menu.isBulk() ? 36 : 55,
                                            14)
                                    .build(SkinnedButton::new));
        }
    }

    /** Hidden editor: the refill panel draws its own labels over the settings panel. */
    /** A bulk pouch holds one kind, so it has no filter rules; the shared rows plus the lock. */
    /** This adapter is the only place where the generic editor knows the current protocol. */
    /** Paints the editor layers, then the shared priority row underneath. */

    @Override
    protected boolean hasClickedOutside(
            double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        return !targetPanel.overSide(mouseX, mouseY)
                && !settings.over(mouseX, mouseY)
                && super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, button);
    }

    /** Real stack limit of the item a refill destination holds, or of the pouch stock it takes. */
    private int refillDestinationLimit(String destination) {
        var held = destinationStack(destination);
        if (!held.isEmpty()) return held.getMaxStackSize();
        for (int i = 0; i < menu.visibleSlotCount(); i++) {
            var stored = menu.getSlot(i).getItem();
            if (!stored.isEmpty()) return stored.getMaxStackSize();
        }
        return 0;
    }

    private ItemStack destinationStack(String destination) {
        if (!destination.startsWith("inventory:")) return ItemStack.EMPTY;
        int index;
        try {
            index = Integer.parseInt(destination.substring("inventory:".length()));
        } catch (NumberFormatException ignored) {
            return ItemStack.EMPTY;
        }
        var inventory = minecraft.player.getInventory();
        return index >= 0 && index < inventory.getContainerSize()
                ? inventory.getItem(index)
                : ItemStack.EMPTY;
    }

    /** Builds the refill settings panel that replaces the filter editor on refill pouches. */
    /** The enable toggle on the first shared row. */
    /** The -/+ priority steppers on the second shared row. */
    private void send(int operation, int sampleSlot, String field, String value) {
        PacketDistributor.sendToServer(
                new FilterActionPayload(menu.containerId, operation, sampleSlot, field, value));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (navigate(BagKeyHandler.mouseAction(button))) return true;
        // Modal destination selection owns pointer events before ANY old settings listener.
        // Rebuilds and fast clicks can leave a previous panel's focus/drag state alive.
        if (targetPanel.selecting()) {
            if (settingsButton != null && settingsButton.isMouseOver(mouseX, mouseY))
                return super.mouseClicked(mouseX, mouseY, button);
            if (targetPanel.pausing()) {
                if (button == 0) togglePausedSlot(mouseX, mouseY);
                cancelInventoryDrag();
                return true;
            }
            targetPanel.update(menu, leftPos, topPos);
            targetPanel.mouseClicked(mouseX, mouseY, button);
            cancelInventoryDrag();
            return true;
        }
        if (settings.mouseClicked(mouseX, mouseY, button, menu.getCarried().isEmpty())) {
            return true;
        }
        if (targetPanel.mouseClicked(mouseX, mouseY, button)) {
            cancelInventoryDrag();
            return true;
        }
        var equipment = targetPanel.hoveredSlot(menu, mouseX, mouseY);
        if (equipment != null && (button == 0 || button == 1)) {
            cancelInventoryDrag();
            slotClicked(
                    equipment,
                    equipment.index,
                    button,
                    hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP);
            return true;
        }
        if (targetPanel.overSide(mouseX, mouseY)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void cancelInventoryDrag() {
        isQuickCrafting = false;
        quickCraftSlots.clear();
    }

    /**
     * Shared prelude for the modal slot modes: empty cursor, no live drag and no listeners left on
     * the settings panel that is about to be hidden. Returns false when the gesture is refused.
     */
    private boolean beginModalMode() {
        if (!menu.getCarried().isEmpty()) {
            minecraft.player.displayClientMessage(
                    Component.translatable("message.packingcube.empty_cursor"), true);
            return false;
        }
        cancelInventoryDrag();
        settings.pointerDown(false);
        if (settings.panel() != null) {
            settings.panel().stopDragging();
            settings.panel().setFocused(null);
        }
        setFocused(null);
        settingsWindow.state().stop();
        // Remove the old settings listeners now, not on the next tick. A fast press/release must
        // not be consumed by the hidden panel.
        refillRebuildRequested = false;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (BagKeyHandler.OPEN_BIG_BAG.matchesMouse(button)) directHeld = false;
        if (BagKeyHandler.OPEN_RADIAL.matchesMouse(button)) radialHeld = false;
        if (targetPanel.selecting()) {
            targetPanel.update(menu, leftPos, topPos);
            int source = targetPanel.selectionSource();
            String selected = targetPanel.mouseReleased(mouseX, mouseY, button);
            settings.pointerDown(false);
            cancelInventoryDrag();
            if (selected != null) {
                send(FilterActions.SET_REFILL_SOURCE, source, "destination", selected);
                targetPanel.selecting(false);
                refillRebuildRequested = false;
                rebuildWidgets();
                Minecraft.getInstance()
                        .getSoundManager()
                        .play(
                                SimpleSoundInstance.forUI(
                                        SoundEvents.UI_BUTTON_CLICK.value(), 1.4F, .25F));
            }
            return true;
        }
        if (settings.mouseReleased(mouseX, mouseY, button)) return true;
        boolean sideConsumed = targetPanel.dragging() || targetPanel.overSide(mouseX, mouseY);
        targetPanel.mouseReleased(mouseX, mouseY, button);
        if (sideConsumed) {
            cancelInventoryDrag();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (targetPanel.selecting()) {
            targetPanel.mouseDragged(mouseY);
            cancelInventoryDrag();
            return true;
        }
        if (settings.mouseDragged(mouseX, mouseY, button, dx, dy)) return true;
        if (targetPanel.mouseDragged(mouseY) || targetPanel.selecting()) return true;
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (targetPanel.selecting()) {
            targetPanel.mouseScrolled(mouseX, mouseY, vertical);
            return true;
        }
        if (settings.mouseScrolled(mouseX, mouseY, horizontal, vertical)) return true;
        if (targetPanel.mouseScrolled(mouseX, mouseY, vertical)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (settings.searchFocused())
            return settings.searchKeyPressed(keyCode, scanCode, modifiers);
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && targetPanel.selecting()) {
            targetPanel.selecting(false);
            rebuildWidgets();
            return true;
        }
        if (navigate(BagKeyHandler.keyAction(keyCode, scanCode))) return true;
        if (settings.panelKeyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (!targetPanel.selecting()) super.slotClicked(slot, slotId, mouseButton, type);
    }

    private boolean navigate(BagKeyActions.Action action) {
        if (action == BagKeyActions.Action.NONE) return false;
        boolean radial = action == BagKeyActions.Action.RADIAL;
        boolean held = radial ? radialHeld : directHeld;
        if (radial) radialHeld = true;
        else directHeld = true;
        if (held) return true; // Ignore key repeat and the key that opened this screen.
        BagKeyHandler.latch(action);
        if (!menu.getCarried().isEmpty()) {
            minecraft.player.displayClientMessage(
                    Component.translatable("message.packingcube.empty_cursor"), true);
            return true;
        }
        PacketDistributor.sendToServer(new BagNavigationPayload(menu.containerId, radial));
        return true; // Do not send a close packet before the server switches menus.
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (BagKeyHandler.OPEN_BIG_BAG.matches(keyCode, scanCode)) directHeld = false;
        if (BagKeyHandler.OPEN_RADIAL.matches(keyCode, scanCode)) radialHeld = false;
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!BagKeyHandler.bindingDown(BagKeyHandler.OPEN_BIG_BAG)) directHeld = false;
        if (!BagKeyHandler.bindingDown()) radialHeld = false;
        settings.tick();
        if (settings.editor() != null) settings.editor().tick();
        if (refillRebuildRequested) {
            refillRebuildRequested = false;
            settings.scroll().setOffset(0);
            rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPartialTick = partialTick;
        {
            targetPanel.source(refillView.source());
            targetPanel.update(menu, leftPos, topPos);
        }
        if (settings.stowButton() != null)
            settings.stowButton().active = menu.getCarried().isEmpty();
        super.render(graphics, mouseX, mouseY, partialTick);
        renderLockedSlots(graphics);
        if (!targetPanel.selecting() && !settings.over(mouseX, mouseY)) {
            var equipment = targetPanel.hoveredSlot(menu, mouseX, mouseY);
            if (equipment != null) hoveredSlot = equipment;
        }
        if (editing)
            settingsWindow.renderTooltips(
                    graphics,
                    font,
                    mouseX,
                    mouseY,
                    () -> renderDirectTooltips(graphics, mouseX, mouseY));
        else renderDirectTooltips(graphics, mouseX, mouseY);
    }

    private void renderDirectTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!settings.over(mouseX, mouseY)) {
            if (!targetPanel.selecting()) renderTooltip(graphics, mouseX, mouseY);
            targetPanel.renderTooltip(graphics, menu, mouseX, mouseY);
        }
    }

    /** Returned items keep their slot locked; dim it and show a lock so the reason is visible. */
    private void renderLockedSlots(GuiGraphics graphics) {
        var pouch = menu.filterSnapshot();
        for (int i = 0; i < menu.visibleSlotCount(); i++) {
            if (!RefillPouchSettings.locked(pouch, i)) continue;
            var slot = menu.getSlot(i);
            int x = leftPos + slot.x, y = topPos + slot.y;
            graphics.fill(x, y, x + 16, y + 16, 0x8C000000);
            drawLock(graphics, x + 2, y + 2);
        }
    }

    private static void drawLock(GuiGraphics graphics, int x, int y) {
        int color = 0xFFE8E8E8;
        graphics.fill(x + 3, y + 1, x + 9, y + 2, color);
        graphics.fill(x + 3, y + 1, x + 4, y + 5, color);
        graphics.fill(x + 8, y + 1, x + 9, y + 5, color);
        graphics.fill(x + 2, y + 5, x + 10, y + 11, color);
        graphics.fill(x + 5, y + 7, x + 7, y + 9, 0xFF303030);
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        if (hoveredSlot != null
                && hoveredSlot.index < menu.visibleSlotCount()
                && RefillPouchSettings.locked(menu.filterSnapshot(), hoveredSlot.index)) {
            lines.add(Component.translatable("gui.packingcube.refill_slot_locked"));
            lines.add(Component.translatable("gui.packingcube.refill_slot_unlock_hint"));
        }
        return lines;
    }

    @Override
    public void setTooltipForNextRenderPass(
            List<FormattedCharSequence> lines,
            ClientTooltipPositioner positioner,
            boolean override) {
        if (editing) settingsWindow.queueTooltip(lines, positioner, override);
        else super.setTooltipForNextRenderPass(lines, positioner, override);
    }

    @Override
    protected void clearTooltipForNextRenderPass() {
        super.clearTooltipForNextRenderPass();
        if (settingsWindow != null) settingsWindow.clearTooltips();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font,
                font.plainSubstrByWidth(title.getString(), menu.isModules() ? 160 : 103),
                8,
                6,
                GuiSurface.TEXT_COLOR,
                true);
        // The bulk pouch uses this row for the amount slider instead of the inventory label.
        if (!menu.isBulk())
            graphics.drawString(font, playerInventoryTitle, 8, 38, GuiSurface.TEXT_COLOR, true);
        {
            graphics.pose().pushPose();
            graphics.pose().translate(-leftPos, -topPos, 0);
            targetPanel.renderMarks(
                    graphics,
                    menu,
                    mouseX,
                    mouseY,
                    settings.over(mouseX, mouseY),
                    menu.isRefill() && editing);
            graphics.pose().popPose();
        }
        if (menu.isRefill() && editing && !targetPanel.pausing()) {
            int sourceX = 7 + refillView.source() * 18;
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 210);
            WidgetTextures.draw(
                    graphics,
                    ResourceLocation.fromNamespaceAndPath(
                            "packingcube", "textures/gui/refill_target_selected.png"),
                    18,
                    18,
                    0,
                    0,
                    sourceX,
                    17,
                    18,
                    18);
            graphics.pose().popPose();
        }
        if (menu.isRefill() && editing && targetPanel.selecting())
            graphics.drawCenteredString(
                    font,
                    targetPanel.pausing()
                            ? Component.translatable("gui.packingcube.refill_pause_banner")
                            : Component.translatable(
                                    "gui.packingcube.refill_selecting_source",
                                    refillView.source() + 1),
                    88,
                    -12,
                    0xffffff);
        settings.render(graphics, mouseX, mouseY, renderPartialTick);
        if (editing) FloatingWindow.prepareContainerCursorLayer(graphics);
    }

    /** Pause mode: one click on a supply slot flips its lock and leaves the stack alone. */
    private void togglePausedSlot(double mouseX, double mouseY) {
        int slots = Math.min(RefillPouchSettings.LOCK_SLOTS, menu.visibleSlotCount());
        for (int i = 0; i < slots; i++) {
            var slot = menu.getSlot(i);
            double x = leftPos + slot.x, y = topPos + slot.y;
            if (mouseX < x || mouseX >= x + 16 || mouseY < y || mouseY >= y + 16) continue;
            boolean locked = RefillPouchSettings.locked(menu.filterSnapshot(), i);
            if (locked) {
                UiInteractionSounds.unlock();
            } else {
                UiInteractionSounds.lock();
            }
            send(FilterActions.SET_REFILL_SLOT_LOCK, i, "locked", Boolean.toString(!locked));
            return;
        }
    }

    @Override
    protected boolean isHovering(int x, int y, int w, int h, double mouseX, double mouseY) {
        return !settings.over(mouseX, mouseY) && super.isHovering(x, y, w, h, mouseX, mouseY);
    }

    private void closeSettings() {
        editing = false;
        settings.clearHelp();
        rebuildWidgets();
    }

    /** Help mode: the explanation of the button under the cursor, beside the settings window. */
    /** The explanation of the control under the cursor; the refill panel keys off its rows. */
    @Nullable
    /** Row under the cursor in settings-content space, or -1 when outside the viewport. */
    /** The bulk window's top row: the two slots and the amount between them. */
    private void renderBulk(GuiGraphics graphics) {
        // Name the two slots so the direction reads without hovering them.
        Component in = text("bulk_slot_in");
        graphics.drawString(
                font, in, leftPos + 52 - font.width(in) / 2, topPos + 36, 0xFFFFFFFF, true);
        Component out = text("bulk_slot_out");
        graphics.drawString(
                font, out, leftPos + 124 - font.width(out) / 2, topPos + 36, 0xFFFFFFFF, true);
        Component amount = Component.literal(menu.bulkAmount() + "/" + BulkStore.LIMIT);
        int x = leftPos + 88 - font.width(amount) / 2;
        graphics.drawString(font, amount, x, topPos + 45, 0xFFFFFFFF, true);
    }

    /** The bulk output slot shows the kind only; the amount sits between the two slots. */
    @Override
    protected void renderSlotContents(
            GuiGraphics graphics, ItemStack stack, Slot slot, @Nullable String countString) {
        if (menu.isBulk() && slot.index == 0 && !stack.isEmpty()) {
            graphics.renderItem(stack.copyWithCount(1), slot.x, slot.y);
            return;
        }
        super.renderSlotContents(graphics, stack, slot, countString);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (menu.isBulk()) {
            WidgetTextures.draw(graphics, GuiSurface.BULK_POUCH, leftPos, topPos, 176, 140);
            renderBulk(graphics);
        } else {
            WidgetTextures.draw(graphics, GuiSurface.BAG_INVENTORY, leftPos, topPos, 176, 132);
        }
        targetPanel.renderBackground(graphics);
    }
}
