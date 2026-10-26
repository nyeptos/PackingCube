package github.nyeptos.packingcube.client;

import github.nyeptos.packingcube.bag.input.BagKeyActions;
import github.nyeptos.packingcube.bag.pickup.BagPickupSettings;
import github.nyeptos.packingcube.client.widget.FilterEditorPanel;
import github.nyeptos.packingcube.client.widget.FloatingWindow;
import github.nyeptos.packingcube.client.widget.RefillSettingsPanel;
import github.nyeptos.packingcube.client.widget.RefillTargetPanel;
import github.nyeptos.packingcube.client.widget.SettingsScrollPanel;
import github.nyeptos.packingcube.client.widget.SkinnedButton;
import github.nyeptos.packingcube.client.widget.WidgetTextures;
import github.nyeptos.packingcube.gui.input.FilterEditorState;
import github.nyeptos.packingcube.gui.input.RefillSettingsView;
import github.nyeptos.packingcube.gui.input.SettingsScrollState;
import github.nyeptos.packingcube.gui.layout.BagSettingsLayout;
import github.nyeptos.packingcube.gui.skin.GuiSurface;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.network.BagNavigationPayload;
import github.nyeptos.packingcube.network.BagStowPayload;
import github.nyeptos.packingcube.network.FilterActionPayload;
import github.nyeptos.packingcube.network.FilterActions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
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

import java.util.List;

/**
 * Container/navigation host. Filter widgets and their layout are reusable independent components.
 */
public final class BagScreen extends AbstractContainerScreen<BagMenu> {
    private final BagCursorMemory cursorMemory = new BagCursorMemory();
    private final FilterEditorState filterState = new FilterEditorState();
    private final SettingsScrollState settingsScroll = new SettingsScrollState();
    private SettingsScrollPanel settingsPanel;
    private final java.util.Map<String, SkinnedButton.Feedback> buttonFeedback =
            new java.util.HashMap<>();
    private boolean settingsPointerDown;
    private boolean editing;
    private boolean directHeld;
    private boolean radialHeld;
    private BagSettingsLayout settingsLayout;
    private int settingsTop;
    private int settingsLeft;
    private final FloatingWindow settingsWindow = new FloatingWindow();
    private float renderPartialTick;
    private FilterEditorPanel filterEditor;
    private RefillSettingsPanel refillControls;
    private final RefillSettingsView refillView = new RefillSettingsView();
    private final RefillTargetPanel targetPanel = new RefillTargetPanel();
    private boolean refillRebuildRequested;
    private Button pickupToggle;
    private Button priorityDown;
    private Button priorityUp;
    private Button stowButton;
    private Button settingsButton;

    public BagScreen(BagMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 132;
        inventoryLabelY = 38;
    }

    @Override
    public void added() {
        super.added();
        cursorMemory.added();
    }

    @Override
    public void removed() {
        settingsWindow.state().stop();
        settingsWindow.clearTooltips();
        cursorMemory.remember();
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
        settingsWindow.clearTooltips();
        filterEditor = null;
        refillControls = null;
        settingsPanel = null;
        settingsPointerDown = false;
        pickupToggle = null;
        priorityDown = null;
        priorityUp = null;
        stowButton = null;
        settingsButton = null;
        imageWidth = 176;
        imageHeight = 132;
        inventoryLabelY = 38;
        super.init();
        if (!editing || refillView.page() != RefillSettingsView.DESTINATIONS)
            targetPanel.selecting(false);
        var anchor = BagSettingsLayout.inventoryAnchor(width, height);
        leftPos = anchor.x();
        topPos = anchor.y();
        {
            targetPanel.source(refillView.source());
            targetPanel.update(menu, leftPos, topPos);
        }
        settingsLayout = null;
        settingsLeft = leftPos;
        settingsTop = topPos;
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
            settingsLeft = settingsWindow.state().x() - 176;
            settingsTop = settingsWindow.state().y();
            settingsLayout =
                    BagSettingsLayout.floating(
                            settingsWindow.state().width(),
                            settingsWindow.state().height(),
                            menu.isRefill() ? refillView.rows() : 2);
        }
        directHeld = BagKeyHandler.bindingDown(BagKeyHandler.OPEN_BIG_BAG);
        radialHeld = BagKeyHandler.bindingDown();
        if (!menu.isModules()) {
            settingsButton =
                    addRenderableWidget(
                            Button.builder(
                                            targetPanel.selecting()
                                                    ? text("refill_select_cancel")
                                                    : editing
                                                            ? Component.literal("×")
                                                            : text("settings"),
                                            button -> {
                                                if (targetPanel.selecting())
                                                    targetPanel.selecting(false);
                                                else editing = !editing;
                                                rebuildWidgets();
                                            })
                                    .bounds(
                                            editing && !targetPanel.selecting()
                                                    ? leftPos + settingsLayout.closeButton().x()
                                                    : leftPos + 115,
                                            topPos + 2,
                                            editing && !targetPanel.selecting() ? 14 : 55,
                                            14)
                                    .build(
                                            b ->
                                                    editing && !targetPanel.selecting()
                                                            ? SkinnedButton.close(b)
                                                            : new SkinnedButton(b)));
        }
        if (menu.isModules()) {
            stowButton =
                    addRenderableWidget(
                            Button.builder(
                                            text("stow_all"),
                                            button -> {
                                                if (menu.getCarried().isEmpty())
                                                    PacketDistributor.sendToServer(
                                                            new BagStowPayload(menu.containerId));
                                            })
                                    .bounds(leftPos + 115, topPos + 2, 55, 14)
                                    .build(SkinnedButton::new));
            stowButton.setTooltip(Tooltip.create(text("stow_all_help")));
        }
        if (editing && !targetPanel.selecting()) {
            var viewport = settingsLayout.viewport();
            settingsPanel =
                    new SettingsScrollPanel(
                            settingsLeft,
                            settingsTop,
                            viewport.x(),
                            viewport.y(),
                            viewport.width(),
                            viewport.height(),
                            menu.isRefill()
                                    ? refillView.contentHeight(
                                            settingsLayout, menu.filterSnapshot())
                                    : settingsLayout.content().height(),
                            settingsScroll);
            settingsPanel.feedback(buttonFeedback);
            addWidget(settingsPanel);
        }
        buildPickupControls();
        if (!editing || targetPanel.selecting()) return;
        if (menu.isRefill() && !refillView.editorVisible(menu.filterSnapshot())) {
            settingsPanel.setContentRenderer(
                    graphics -> refillControls.renderLabels(graphics, font));
            return;
        }
        // This adapter is the only place where the generic editor knows the current menu's
        // protocol.
        filterEditor =
                new FilterEditorPanel(
                        font,
                        new FilterEditorPanel.Access() {
                            @Override
                            public ItemStack settings() {
                                return menu.filterSnapshot();
                            }

                            @Override
                            public Component emptyRules(boolean excluding) {
                                return menu.isRefill()
                                        ? text(
                                                excluding
                                                        ? "refill_empty_exclude"
                                                        : "refill_empty_include")
                                        : FilterEditorPanel.Access.super.emptyRules(excluding);
                            }

                            @Override
                            public ItemStack sample() {
                                return menu.sample();
                            }

                            @Override
                            public void setExcluding(boolean excluding) {
                                send(
                                        FilterActions.SET_MODE,
                                        -1,
                                        "mode",
                                        excluding ? "exclude" : "include");
                            }

                            @Override
                            public void addRule(String field, String value) {
                                send(FilterActions.ADD_RULE, -1, field, value);
                            }

                            @Override
                            public void removeRule(String field, String value) {
                                send(FilterActions.REMOVE_RULE, -1, field, value);
                            }

                            @Override
                            public void sampleCursor(boolean clear) {
                                send(FilterActions.SAMPLE_CURSOR, -1, "", clear ? "clear" : "copy");
                            }
                        },
                        filterState);
        filterEditor.build(settingsLayout, settingsLeft, settingsTop, settingsPanel::add);
        settingsPanel.setContentRenderer(
                graphics -> {
                    filterEditor.renderBackground(graphics);
                    filterEditor.renderLabels(graphics);
                    if (refillControls != null) refillControls.renderLabels(graphics, font);
                    if (menu.isRefill()) return;
                    var row = settingsLayout.optionRow(1);
                    graphics.drawCenteredString(
                            font,
                            Component.translatable(
                                    "gui.packingcube.pickup_priority",
                                    BagPickupSettings.priority(menu.filterSnapshot())),
                            row.centerX(),
                            row.y() + 5,
                            0xffffff);
                });
    }

    @Override
    protected boolean hasClickedOutside(
            double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        return !targetPanel.overSide(mouseX, mouseY)
                && !overSettings(mouseX, mouseY)
                && super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, button);
    }

    private void buildPickupControls() {
        if (!editing || targetPanel.selecting()) return;
        if (menu.isRefill()) {
            refillControls =
                    new RefillSettingsPanel(
                            new RefillSettingsPanel.Access() {
                                public ItemStack settings() {
                                    return menu.filterSnapshot();
                                }

                                public void set(String field, String value) {
                                    send(FilterActions.SET_REFILL_OPTION, -1, field, value);
                                }

                                public void setSource(String field, String value) {
                                    send(
                                            FilterActions.SET_REFILL_SOURCE,
                                            refillView.source(),
                                            field,
                                            value);
                                }

                                public void rebuild() {
                                    refillRebuildRequested = true;
                                }

                                public void toggleTargets() {
                                    if (!menu.getCarried().isEmpty()) {
                                        minecraft.player.displayClientMessage(
                                                Component.translatable(
                                                        "message.packingcube.empty_cursor"),
                                                true);
                                        return;
                                    }
                                    cancelInventoryDrag();
                                    settingsPointerDown = false;
                                    if (settingsPanel != null) {
                                        settingsPanel.stopDragging();
                                        settingsPanel.setFocused(null);
                                    }
                                    setFocused(null);
                                    settingsWindow.state().stop();
                                    targetPanel.beginSelection(refillView.source());
                                    // Remove the old settings listeners now, not on the next tick.
                                    // A fast press/release must not be consumed by the hidden
                                    // panel.
                                    refillRebuildRequested = false;
                                    rebuildWidgets();
                                }
                            },
                            refillView);
            refillControls.build(settingsLayout, settingsLeft, settingsTop, settingsPanel::add);
            return;
        }
        var row = settingsLayout.optionRow(0);
        pickupToggle =
                addSettingWidget(
                        Button.builder(
                                        Component.empty(),
                                        ignored ->
                                                send(
                                                        FilterActions.SET_PICKUP_ENABLED,
                                                        -1,
                                                        "pickup",
                                                        Boolean.toString(
                                                                !BagPickupSettings.enabled(
                                                                        menu.filterSnapshot()))))
                                .bounds(
                                        settingsLeft + row.x(),
                                        settingsTop + row.y(),
                                        row.width(),
                                        row.height())
                                .build(
                                        b ->
                                                SkinnedButton.toggle(
                                                        b,
                                                        () ->
                                                                BagPickupSettings.enabled(
                                                                        menu.filterSnapshot()))));
        row = settingsLayout.optionRow(1);
        priorityDown =
                addSettingWidget(
                        Button.builder(
                                        Component.literal("-"),
                                        ignored ->
                                                send(
                                                        FilterActions.SET_PICKUP_PRIORITY,
                                                        -1,
                                                        "priority",
                                                        Integer.toString(
                                                                BagPickupSettings.priority(
                                                                                menu
                                                                                        .filterSnapshot())
                                                                        - 1)))
                                .bounds(
                                        settingsLeft + row.x(),
                                        settingsTop + row.y(),
                                        20,
                                        row.height())
                                .build(SkinnedButton::new));
        priorityUp =
                addSettingWidget(
                        Button.builder(
                                        Component.literal("+"),
                                        ignored ->
                                                send(
                                                        FilterActions.SET_PICKUP_PRIORITY,
                                                        -1,
                                                        "priority",
                                                        Integer.toString(
                                                                BagPickupSettings.priority(
                                                                                menu
                                                                                        .filterSnapshot())
                                                                        + 1)))
                                .bounds(
                                        settingsLeft + row.right() - 20,
                                        settingsTop + row.y(),
                                        20,
                                        row.height())
                                .build(SkinnedButton::new));
        priorityDown.setTooltip(Tooltip.create(text("pickup_priority_help")));
        priorityUp.setTooltip(Tooltip.create(text("pickup_priority_help")));
        updatePickupSettings();
    }

    private Button addSettingWidget(Button button) {
        if (settingsPanel != null) settingsPanel.add(button);
        else addRenderableWidget(button);
        return button;
    }

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
            targetPanel.update(menu, leftPos, topPos);
            targetPanel.mouseClicked(mouseX, mouseY, button);
            cancelInventoryDrag();
            return true;
        }
        if (editing
                && !targetPanel.selecting()
                && settingsWindow.mouseClicked(
                        mouseX, mouseY, button, menu.getCarried().isEmpty())) {
            settingsPanel.setFocused(null);
            settingsPointerDown = true;
            return true;
        }
        if (!targetPanel.selecting()
                && settingsPanel != null
                && settingsPanel.contains(mouseX, mouseY)) {
            if (getFocused() != settingsPanel) setFocused(settingsPanel);
            if (filterEditor != null
                    && filterEditor.samplePressed(
                            mouseX, settingsPanel.unscrolledY(mouseY), button)) {
                settingsPanel.setFocused(null);
                return true;
            }
            settingsPointerDown = true;
            settingsPanel.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (overSettings(mouseX, mouseY)) return true;
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

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (BagKeyHandler.OPEN_BIG_BAG.matchesMouse(button)) directHeld = false;
        if (BagKeyHandler.OPEN_RADIAL.matchesMouse(button)) radialHeld = false;
        if (targetPanel.selecting()) {
            targetPanel.update(menu, leftPos, topPos);
            int source = targetPanel.selectionSource();
            String selected = targetPanel.mouseReleased(mouseX, mouseY, button);
            settingsPointerDown = false;
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
        if (editing && !targetPanel.selecting() && settingsWindow.mouseReleased(button)) {
            settingsPointerDown = false;
            targetPanel.cancelPress();
            cancelInventoryDrag();
            return true;
        }
        boolean inside = settingsPanel != null && settingsPanel.contains(mouseX, mouseY);
        if (filterEditor != null
                && filterEditor.sampleReleased(
                        inside ? mouseX : Double.NEGATIVE_INFINITY,
                        inside ? settingsPanel.unscrolledY(mouseY) : Double.NEGATIVE_INFINITY,
                        button)) {
            settingsPanel.stopDragging();
            settingsPointerDown = false;
            targetPanel.cancelPress();
            cancelInventoryDrag();
            return true;
        }
        if (settingsPanel != null && (settingsPointerDown || settingsPanel.dragging() || inside)) {
            settingsPanel.mouseReleased(mouseX, mouseY, button);
            settingsPanel.stopDragging();
            settingsPointerDown = false;
            targetPanel.cancelPress();
            cancelInventoryDrag();
            return true;
        }
        if (overSettings(mouseX, mouseY)) {
            targetPanel.cancelPress();
            cancelInventoryDrag();
            return true;
        }
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
        if (editing && !targetPanel.selecting()) {
            int oldWidth = settingsWindow.state().width(),
                    oldHeight = settingsWindow.state().height();
            if (settingsWindow.mouseDragged(mouseX, mouseY, button)) {
                if (oldWidth != settingsWindow.state().width()
                        || oldHeight != settingsWindow.state().height()) {
                    // Rebuild widths/heights from shared geometry. State/query/scroll remain
                    // screen-local.
                    rebuildWidgets();
                } else {
                    settingsLeft = settingsWindow.state().x() - 176;
                    settingsTop = settingsWindow.state().y();
                    settingsPanel.moveTo(settingsLeft, settingsTop);
                    if (filterEditor != null) filterEditor.moveOrigin(settingsLeft, settingsTop);
                }
                return true;
            }
        }
        if (settingsPanel != null && (settingsPointerDown || settingsPanel.dragging())) {
            settingsPanel.mouseDragged(mouseX, mouseY, button, dx, dy);
            return true;
        }
        if (targetPanel.mouseDragged(mouseY) || targetPanel.selecting()) return true;
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (targetPanel.selecting()) {
            targetPanel.mouseScrolled(mouseX, mouseY, vertical);
            return true;
        }
        if (!targetPanel.selecting()
                && settingsPanel != null
                && settingsPanel.mouseScrolled(mouseX, mouseY, horizontal, vertical)) return true;
        if (overSettings(mouseX, mouseY)) return true;
        if (targetPanel.mouseScrolled(mouseX, mouseY, vertical)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (filterEditor != null && filterEditor.searchFocused())
            return filterEditor.keyPressed(keyCode, scanCode, modifiers);
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && targetPanel.selecting()) {
            targetPanel.selecting(false);
            rebuildWidgets();
            return true;
        }
        if (navigate(BagKeyHandler.keyAction(keyCode, scanCode))) return true;
        if (settingsPanel != null
                && getFocused() == settingsPanel
                && settingsPanel.keyPressed(keyCode, scanCode, modifiers)) return true;
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
        updatePickupSettings();
        if (filterEditor != null) filterEditor.tick();
        if (refillRebuildRequested) {
            refillRebuildRequested = false;
            settingsScroll.setOffset(0);
            rebuildWidgets();
        }
    }

    private void updatePickupSettings() {
        if (refillControls != null) refillControls.tick();
        if (pickupToggle != null)
            pickupToggle.setMessage(
                    text(
                            "pickup_pouch_"
                                    + (BagPickupSettings.enabled(menu.filterSnapshot())
                                            ? "on"
                                            : "off")));
        int priority = BagPickupSettings.priority(menu.filterSnapshot());
        if (priorityDown != null) priorityDown.active = priority > BagPickupSettings.MIN_PRIORITY;
        if (priorityUp != null) priorityUp.active = priority < BagPickupSettings.MAX_PRIORITY;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPartialTick = partialTick;
        {
            targetPanel.source(refillView.source());
            targetPanel.update(menu, leftPos, topPos);
        }
        if (stowButton != null) stowButton.active = menu.getCarried().isEmpty();
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!targetPanel.selecting() && !overSettings(mouseX, mouseY)) {
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
        if (!overSettings(mouseX, mouseY)) {
            if (!targetPanel.selecting()) renderTooltip(graphics, mouseX, mouseY);
            targetPanel.renderTooltip(graphics, menu, mouseX, mouseY);
        }
        if (filterEditor != null && settingsPanel != null && settingsPanel.contains(mouseX, mouseY))
            filterEditor.renderTooltip(
                    graphics, mouseX, mouseY, mouseX, settingsPanel.unscrolledY(mouseY));
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
        graphics.drawString(font, playerInventoryTitle, 8, 38, GuiSurface.TEXT_COLOR, true);
        {
            graphics.pose().pushPose();
            graphics.pose().translate(-leftPos, -topPos, 0);
            targetPanel.renderMarks(
                    graphics,
                    menu,
                    mouseX,
                    mouseY,
                    overSettings(mouseX, mouseY),
                    menu.isRefill() && editing);
            graphics.pose().popPose();
        }
        if (menu.isRefill() && editing) {
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
            if (targetPanel.selecting())
                graphics.drawCenteredString(
                        font,
                        Component.translatable(
                                "gui.packingcube.refill_selecting_source", refillView.source() + 1),
                        88,
                        -12,
                        0xffffff);
        }
        renderSettings(graphics, mouseX, mouseY);
        if (editing) FloatingWindow.prepareContainerCursorLayer(graphics);
    }

    private boolean overSettings(double mouseX, double mouseY) {
        return editing && !targetPanel.selecting() && settingsWindow.contains(mouseX, mouseY);
    }

    @Override
    protected boolean isHovering(int x, int y, int w, int h, double mouseX, double mouseY) {
        return !overSettings(mouseX, mouseY) && super.isHovering(x, y, w, h, mouseX, mouseY);
    }

    private void renderSettings(GuiGraphics graphics, int mouseX, int mouseY) {
        if (settingsLayout == null || targetPanel.selecting()) return;
        graphics.pose().pushPose();
        graphics.pose().translate(-leftPos, -topPos, 0);
        settingsWindow.render(
                graphics,
                font,
                text("settings"),
                mouseX,
                mouseY,
                () -> settingsPanel.render(graphics, mouseX, mouseY, renderPartialTick));
        graphics.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        WidgetTextures.draw(graphics, GuiSurface.BAG_INVENTORY, leftPos, topPos, 176, 132);
        targetPanel.renderBackground(graphics);
    }
}
