package github.nyeptos.packingcube.client.widget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import github.nyeptos.packingcube.bag.pickup.BagPickupSettings;
import github.nyeptos.packingcube.gui.layout.BagSettingsLayout;
import github.nyeptos.packingcube.gui.input.FilterEditorState;
import github.nyeptos.packingcube.gui.input.SettingsScrollState;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.network.BagStowPayload;
import github.nyeptos.packingcube.network.FilterActions;
import github.nyeptos.packingcube.gui.input.RefillSettingsView;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Owns the settings side panel: the close/help buttons, the scroll panel, the pickup rows, and the
 * filter editor or refill controls that fill it. It also draws the panel and takes its pointer and
 * key events, so the screen only forwards the calls and asks for the result.
 */
public final class BagSettingsView {
    /** What the panel needs from the screen: the menu, its callbacks, and widget registration. */
    public interface Host {
        BagMenu menu();

        boolean editing();

        boolean selectingTargets();

        RefillSettingsView refillView();

        Font font();

        <T extends GuiEventListener & Renderable & NarratableEntry> T addWidget(T listener);

        <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget);

        void beginTargetSelection();

        void beginTargetPause();

        void rebuild();

        boolean beginModal();

        void requestRefillRebuild();

        void sendFilterAction(int operation, int sampleSlot, String field, String value);

        ItemStack snapshot();

        int destinationLimit(String destination);

        void closeSettings();

        int leftPos();

        int topPos();

        int screenWidth();

        int screenHeight();

        FloatingWindow settingsWindow();

        void focusPanel();

        boolean panelFocused();

        void cancelTargetPress();

        void cancelInventoryDrag();
    }

    /** One explanation: the button's name and the line help mode shows beside the panel. */
    public record HelpText(Component name, Component body) {}

    private final Host host;
    private final FilterEditorState filterState = new FilterEditorState();
    private final SettingsScrollState scroll = new SettingsScrollState();
    private final Map<String, SkinnedButton.Feedback> buttonFeedback = new HashMap<>();
    private final Map<AbstractWidget, HelpText> helpByWidget = new HashMap<>();
    private BagSettingsLayout layout;
    private int left;
    private int top;
    private SettingsScrollPanel panel;
    private FilterEditorPanel editor;
    private RefillSettingsPanel refill;
    private Button pickupToggle;
    private Button priorityDown;
    private Button priorityUp;
    private Button stowButton;
    private SkinnedButton close;
    private SkinnedButton help;
    private boolean helpMode;
    private boolean pointerDown;
    private HelpPane.Placement helpPane;

    public BagSettingsView(Host host) {
        this.host = host;
    }

    private static Component text(String key) {
        return Component.translatable("gui.packingcube." + key);
    }

    /** Drops every widget so the next build starts clean. */
    public void reset() {
        editor = null;
        refill = null;
        panel = null;
        pointerDown = false;
        pickupToggle = null;
        priorityDown = null;
        priorityUp = null;
        stowButton = null;
        close = null;
        help = null;
        helpByWidget.clear();
    }

    /** Records where the panel sits; a null layout means the panel is closed. */
    public void place(BagSettingsLayout layout, int left, int top) {
        this.layout = layout;
        this.left = left;
        this.top = top;
    }

    public void moveTo(int left, int top) {
        this.left = left;
        this.top = top;
        if (panel != null) panel.moveTo(left, top);
        if (editor != null) editor.moveOrigin(left, top);
    }

    public BagSettingsLayout layout() {
        return layout;
    }

    public int left() {
        return left;
    }

    public int top() {
        return top;
    }

    public SettingsScrollPanel panel() {
        return panel;
    }

    public FilterEditorPanel editor() {
        return editor;
    }

    public RefillSettingsPanel refill() {
        return refill;
    }

    public Button stowButton() {
        return stowButton;
    }

    public SkinnedButton close() {
        return close;
    }

    public SkinnedButton help() {
        return help;
    }

    public Map<AbstractWidget, HelpText> helpByWidget() {
        return helpByWidget;
    }

    public boolean helpMode() {
        return helpMode;
    }

    public void toggleHelpMode() {
        helpMode = !helpMode;
    }

    public void clearHelp() {
        helpMode = false;
        helpPane = null;
    }

    public HelpPane.Placement helpPane() {
        return helpPane;
    }

    public void helpPane(HelpPane.Placement placement) {
        this.helpPane = placement;
    }

    public boolean pointerDown() {
        return pointerDown;
    }

    public void pointerDown(boolean down) {
        this.pointerDown = down;
    }

    public SettingsScrollState scroll() {
        return scroll;
    }

    public void build() {
        buildChromeButtons();
        buildStowButton();
        buildScrollPanel();
    }

    /** The close and help buttons, which only the settings panel shows. */
    private void buildChromeButtons() {
        if (!host.editing() || host.selectingTargets()) return;
        var close = layout.settingsClose();
        this.close =
                SkinnedButton.close(
                        Button.builder(Component.literal("×"), button -> host.closeSettings())
                                .bounds(
                                        left + close.x(),
                                        top + close.y(),
                                        close.width(),
                                        close.height()));
        var help = layout.settingsHelp();
        this.help =
                SkinnedButton.toggle(
                        Button.builder(Component.literal("?"), button -> toggleHelpMode())
                                .bounds(
                                        left + help.x(),
                                        top + help.y(),
                                        help.width(),
                                        help.height()),
                        () -> helpMode);
    }

    /** The bulk pouch has no filter, so it gets the stow-all button instead. */
    private void buildStowButton() {
        if (!host.menu().isModules()) return;
        stowButton =
                host.addRenderableWidget(
                        Button.builder(
                                        text("stow_all"),
                                        button -> {
                                            if (host.menu().getCarried().isEmpty())
                                                PacketDistributor.sendToServer(
                                                        new BagStowPayload(
                                                                host.menu().containerId));
                                        })
                                .bounds(host.leftPos() + 115, host.topPos() + 2, 55, 14)
                                .build(SkinnedButton::new));
        stowButton.setTooltip(Tooltip.create(text("stow_all_help")));
    }

    /** The scrolling panel that holds the page. */
    private void buildScrollPanel() {
        if (!host.editing() || host.selectingTargets()) return;
        var viewport = layout.viewport();
        panel =
                new SettingsScrollPanel(
                        left,
                        top,
                        viewport.x(),
                        viewport.y(),
                        viewport.width(),
                        viewport.height(),
                        host.menu().isRefill()
                                ? host.refillView().contentHeight(layout, host.snapshot())
                                : layout.content().height(),
                        scroll);
        panel.feedback(buttonFeedback);
        host.addWidget(panel);
    }

    public void bindContent() {
        if (!host.editing() || host.selectingTargets()) return;
        boolean hiddenRefill =
                host.menu().isRefill()
                        && !host.refillView().editorVisible(host.snapshot());
        if (hiddenRefill) {
            bindRefillPlaceholder();
            return;
        }
        if (host.menu().isBulk()) {
            bindBulkContent();
            return;
        }
        bindFilterEditor();
        bindFilterContent();
    }

    private void bindRefillPlaceholder() {
        panel.setContentRenderer(
                (graphics, mouseX, mouseY) -> refill.renderLabels(graphics, host.font()));
    }

    private void bindBulkContent() {
        // A bulk pouch holds one kind, so it has no filter rules; the shared rows plus the
        // lock are all it shows.
        panel.setContentRenderer(
                (graphics, mouseX, mouseY) -> {
                    var row = layout.optionRow(1);
                    graphics.drawCenteredString(
                            host.font(),
                            Component.translatable(
                                    "gui.packingcube.pickup_priority",
                                    BagPickupSettings.priority(host.snapshot())),
                            row.centerX(),
                            row.y() + 5,
                            0xffffff);
                });
    }

    private void bindFilterEditor() {
        editor = new FilterEditorPanel(host.font(), newFilterAccess(), filterState);
        editor.build(layout, left, top, panel::add);
        for (var control : editor.helpControls())
            helpByWidget.put(
                    control.widget(),
                    new HelpText(
                            text("filter_help_name_" + control.key()),
                            text("filter_help_body_" + control.key())));
    }

    /** What the filter editor reads and writes, routed through the host. */
    private FilterEditorPanel.Access newFilterAccess() {
        return new FilterEditorPanel.Access() {
            @Override
            public ItemStack settings() {
                return host.snapshot();
            }

            @Override
            public Component emptyRules(boolean excluding) {
                return host.menu().isRefill()
                        ? text(
                                excluding
                                        ? "refill_empty_exclude"
                                        : "refill_empty_include")
                        : FilterEditorPanel.Access.super.emptyRules(excluding);
            }

            @Override
            public ItemStack sample() {
                return host.menu().sample();
            }

            @Override
            public void setExcluding(boolean excluding) {
                host.sendFilterAction(
                        FilterActions.SET_MODE,
                        -1,
                        "mode",
                        excluding ? "exclude" : "include");
            }

            @Override
            public void addRule(String field, String value) {
                host.sendFilterAction(FilterActions.ADD_RULE, -1, field, value);
            }

            @Override
            public void removeRule(String field, String value) {
                host.sendFilterAction(FilterActions.REMOVE_RULE, -1, field, value);
            }

            @Override
            public void sampleCursor(boolean clear) {
                String value = clear ? "clear" : "copy";
                host.sendFilterAction(FilterActions.SAMPLE_CURSOR, -1, "", value);
            }
        };
    }

    private void bindFilterContent() {
        panel.setContentRenderer(
                (graphics, mouseX, mouseY) -> {
                    editor.renderBackground(graphics);
                    editor.renderLabels(graphics);
                    editor.renderMarks(
                            graphics,
                            mouseX,
                            panel.unscrolledY(mouseY),
                            panel.contains(mouseX, mouseY));
                    if (refill != null) refill.renderLabels(graphics, host.font());
                    if (host.menu().isRefill()) return;
                    var row = layout.optionRow(1);
                    graphics.drawCenteredString(
                            host.font(),
                            Component.translatable(
                                    "gui.packingcube.pickup_priority",
                                    BagPickupSettings.priority(host.snapshot())),
                            row.centerX(),
                            row.y() + 5,
                            0xffffff);
                });
    }

    public void buildControls() {
        if (!host.editing() || host.selectingTargets()) return;
        if (host.menu().isRefill()) {
            buildRefillControls();
            return;
        }
        buildPickupToggle();
        buildPrioritySteppers();
        tick();
    }

    private void buildRefillControls() {
        refill =
                new RefillSettingsPanel(
                        new RefillSettingsPanel.Access() {
                            public ItemStack settings() {
                                return host.snapshot();
                            }

                            public void set(String field, String value) {
                                host.sendFilterAction(
                                        FilterActions.SET_REFILL_OPTION, -1, field, value);
                            }

                            public void setSource(String field, String value) {
                                host.sendFilterAction(
                                        FilterActions.SET_REFILL_SOURCE,
                                        host.refillView().source(),
                                        field,
                                        value);
                            }

                            public void rebuild() {
                                host.requestRefillRebuild();
                            }

                            public List<String> sideIds() {
                                return RefillTargetPanel.sideIds(host.menu());
                            }

                            public int destinationLimit(String destination) {
                                return host.destinationLimit(destination);
                            }

                            public void toggleTargets() {
                                if (!host.beginModal()) return;
                                host.beginTargetSelection();
                                host.rebuild();
                            }

                            public void togglePause() {
                                if (!host.beginModal()) return;
                                host.beginTargetPause();
                                host.rebuild();
                            }
                        },
                        host.refillView());
        refill.build(layout, left, top, panel::add);
    }

    private void buildPickupToggle() {
        var row = layout.optionRow(0);
        pickupToggle =
                addSettingWidget(
                        Button.builder(
                                        Component.empty(),
                                        ignored ->
                                                host.sendFilterAction(
                                                        FilterActions.SET_PICKUP_ENABLED,
                                                        -1,
                                                        "pickup",
                                                        Boolean.toString(
                                                                !BagPickupSettings.enabled(
                                                                        host.snapshot()))))
                                .bounds(
                                        left + row.x(),
                                        top + row.y(),
                                        row.width(),
                                        row.height())
                                .build(
                                        b ->
                                                SkinnedButton.toggle(
                                                        b,
                                                        () ->
                                                                BagPickupSettings.enabled(
                                                                        host.snapshot()))));
        helpByWidget.put(
                pickupToggle,
                new HelpText(text("pickup_help_name"), text("pickup_pouch_help")));
    }

    private void buildPrioritySteppers() {
        var row = layout.optionRow(1);
        priorityDown =
                addSettingWidget(
                        Button.builder(
                                        Component.literal("-"),
                                        ignored ->
                                                host.sendFilterAction(
                                                        FilterActions.SET_PICKUP_PRIORITY,
                                                        -1,
                                                        "priority",
                                                        Integer.toString(
                                                                BagPickupSettings.priority(
                                                                                host.snapshot())
                                                                        - 1)))
                                .bounds(
                                        left + row.x(),
                                        top + row.y(),
                                        20,
                                        row.height())
                                .build(SkinnedButton::new));
        priorityUp =
                addSettingWidget(
                        Button.builder(
                                        Component.literal("+"),
                                        ignored ->
                                                host.sendFilterAction(
                                                        FilterActions.SET_PICKUP_PRIORITY,
                                                        -1,
                                                        "priority",
                                                        Integer.toString(
                                                                BagPickupSettings.priority(
                                                                                host.snapshot())
                                                                        + 1)))
                                .bounds(
                                        left + row.right() - 20,
                                        top + row.y(),
                                        20,
                                        row.height())
                                .build(SkinnedButton::new));
        priorityDown.setTooltip(Tooltip.create(text("pickup_priority_help")));
        priorityUp.setTooltip(Tooltip.create(text("pickup_priority_help")));
    }

    private Button addSettingWidget(Button button) {
        if (panel != null) panel.add(button);
        else host.addRenderableWidget(button);
        return button;
    }

    public void tick() {
        if (refill != null) refill.tick();
        if (pickupToggle != null)
            pickupToggle.setMessage(
                    text(
                            "pickup_pouch_"
                                    + (BagPickupSettings.enabled(host.snapshot())
                                            ? "on"
                                            : "off")));
        int priority = BagPickupSettings.priority(host.snapshot());
        if (priorityDown != null) priorityDown.active = priority > BagPickupSettings.MIN_PRIORITY;
        if (priorityUp != null) priorityUp.active = priority < BagPickupSettings.MAX_PRIORITY;
    }

    /** True when the pointer is over the settings window or its help pane. */
    public boolean over(double mouseX, double mouseY) {
        if (!host.editing() || host.selectingTargets()) return false;
        return host.settingsWindow().contains(mouseX, mouseY)
                || (helpPane != null && helpPane.contains(mouseX, mouseY));
    }

    public boolean overClose(double mouseX, double mouseY) {
        if (!host.editing() || host.selectingTargets() || layout == null) return false;
        return layout.settingsClose().contains(mouseX - left, mouseY - top);
    }

    public boolean overHelp(double mouseX, double mouseY) {
        if (!host.editing() || host.selectingTargets() || layout == null) return false;
        return layout.settingsHelp().contains(mouseX - left, mouseY - top);
    }

    /**
     * Takes a click that lands on the settings window: the close and help buttons, the window
     * chrome, and the scroll panel. The screen forwards the event and stops when this returns true.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button, boolean carriedEmpty) {
        if (overClose(mouseX, mouseY)) {
            if (close != null) close.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (overHelp(mouseX, mouseY)) {
            if (help != null) help.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (host.editing()
                && !host.selectingTargets()
                && host.settingsWindow().mouseClicked(mouseX, mouseY, button, carriedEmpty)) {
            panel.setFocused(null);
            pointerDown = true;
            return true;
        }
        if (!host.selectingTargets() && panel != null && panel.contains(mouseX, mouseY)) {
            host.focusPanel();
            if (editor != null && editor.samplePressed(mouseX, panel.unscrolledY(mouseY), button)) {
                panel.setFocused(null);
                return true;
            }
            pointerDown = true;
            panel.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        return over(mouseX, mouseY);
    }

    /** Ends a press that started on the window, the filter sample, or the scroll panel. */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (host.editing()
                && !host.selectingTargets()
                && host.settingsWindow().mouseReleased(button)) {
            endPress();
            return true;
        }
        boolean inside = panel != null && panel.contains(mouseX, mouseY);
        if (editor != null
                && editor.sampleReleased(
                        inside ? mouseX : Double.NEGATIVE_INFINITY,
                        inside ? panel.unscrolledY(mouseY) : Double.NEGATIVE_INFINITY,
                        button)) {
            panel.stopDragging();
            endPress();
            return true;
        }
        if (panel != null && (pointerDown || panel.dragging() || inside)) {
            panel.mouseReleased(mouseX, mouseY, button);
            panel.stopDragging();
            endPress();
            return true;
        }
        if (over(mouseX, mouseY)) {
            endPress();
            return true;
        }
        return false;
    }

    private void endPress() {
        pointerDown = false;
        host.cancelTargetPress();
        host.cancelInventoryDrag();
    }

    /** Moves or resizes the window while its title bar is held, then drags the scroll panel. */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (host.editing() && !host.selectingTargets()) {
            int oldWidth = host.settingsWindow().state().width();
            int oldHeight = host.settingsWindow().state().height();
            if (host.settingsWindow().mouseDragged(mouseX, mouseY, button)) {
                if (oldWidth != host.settingsWindow().state().width()
                        || oldHeight != host.settingsWindow().state().height()) {
                    // Rebuild widths/heights from shared geometry. State/query/scroll remain
                    // screen-local.
                    host.rebuild();
                } else {
                    moveTo(
                            host.settingsWindow().state().x() - 176,
                            host.settingsWindow().state().y());
                }
                return true;
            }
        }
        if (panel != null && (pointerDown || panel.dragging())) {
            panel.mouseDragged(mouseX, mouseY, button, dx, dy);
            return true;
        }
        return false;
    }

    /** Scrolls the panel, and swallows the wheel over the window so the bag grid stays still. */
    public boolean mouseScrolled(
            double mouseX, double mouseY, double horizontal, double vertical) {
        if (!host.selectingTargets()
                && panel != null
                && panel.mouseScrolled(mouseX, mouseY, horizontal, vertical)) return true;
        return over(mouseX, mouseY);
    }

    /** True when the filter editor's search box owns the keyboard. */
    public boolean searchFocused() {
        return editor != null && editor.searchFocused();
    }

    /** Forwards a key to the search box; call this only when {@link #searchFocused()} is true. */
    public boolean searchKeyPressed(int keyCode, int scanCode, int modifiers) {
        return editor != null && editor.keyPressed(keyCode, scanCode, modifiers);
    }

    /** True when the focused scroll panel used the key. */
    public boolean panelKeyPressed(int keyCode, int scanCode, int modifiers) {
        return panel != null
                && host.panelFocused()
                && panel.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Draws the settings window and its close/help buttons, then the help pane beside it. */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (layout == null || host.selectingTargets()) return;
        graphics.pose().pushPose();
        graphics.pose().translate(-host.leftPos(), -host.topPos(), 0);
        host.settingsWindow()
                .render(
                        graphics,
                        host.font(),
                        text("settings"),
                        mouseX,
                        mouseY,
                        () -> {
                            panel.render(graphics, mouseX, mouseY, partialTick);
                            if (close == null) return;
                            var closeBounds = layout.settingsClose();
                            close.setX(left + closeBounds.x());
                            close.setY(top + closeBounds.y());
                            close.render(graphics, mouseX, mouseY, partialTick);
                            if (help == null) return;
                            var helpBounds = layout.settingsHelp();
                            help.setX(left + helpBounds.x());
                            help.setY(top + helpBounds.y());
                            help.render(graphics, mouseX, mouseY, partialTick);
                        });
        graphics.pose().popPose();
        renderHelp(graphics, mouseX, mouseY);
    }

    /** Help mode: the explanation of the control under the cursor, beside the settings window. */
    private void renderHelp(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!helpMode || !host.editing() || host.selectingTargets()) {
            helpPane = null;
            return;
        }
        HelpText entry = helpAt(mouseX, mouseY);
        Component title = entry == null ? text("help") : entry.name();
        List<Component> body =
                entry == null ? List.of(text("refill_help_hint")) : List.of(entry.body());
        int paneWidth = Math.max(90, Math.min(150, host.screenWidth() - 8));
        var wrapped = HelpPane.wrap(host.font(), paneWidth, body);
        int paneHeight = Math.min(HelpPane.height(wrapped.size()), host.screenHeight() - 4);
        var window = host.settingsWindow().state();
        var placement =
                HelpPane.place(
                        window.x(),
                        window.y(),
                        window.width(),
                        host.screenWidth(),
                        host.screenHeight(),
                        paneWidth,
                        paneHeight);
        helpPane = placement;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(-host.leftPos(), -host.topPos(), 0);
            HelpPane.render(graphics, host.font(), placement, title, wrapped);
        } finally {
            graphics.pose().popPose();
        }
    }

    /** The explanation of the control under the cursor; the refill panel keys off its rows. */
    private HelpText helpAt(double mouseX, double mouseY) {
        if (refill != null) {
            var entry = refill.helpAt(hoveredRow(mouseX, mouseY));
            return entry == null ? null : new HelpText(entry.name(), entry.body());
        }
        if (panel == null) return null;
        return helpByWidget.get(panel.widgetAt(mouseX, mouseY));
    }

    /** Row under the cursor in settings-content space, or -1 when outside the viewport. */
    private int hoveredRow(double mouseX, double mouseY) {
        if (panel == null || layout == null) return -1;
        if (!panel.contains(mouseX, mouseY)) return -1;
        int row = Math.floorDiv((int) (panel.unscrolledY(mouseY) - top - 24), 20);
        return row >= 0 && row < host.refillView().rows() ? row : -1;
    }
}
