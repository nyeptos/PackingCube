package github.nyeptos.packingcube.client.widget;

import github.nyeptos.packingcube.bag.refill.RefillSettings;
import github.nyeptos.packingcube.bag.refill.RefillSlotSettings;
import github.nyeptos.packingcube.gui.input.RefillSettingsView;
import github.nyeptos.packingcube.gui.layout.BagSettingsLayout;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Source 1–9 pages, per-source destination/method, and explicitly shared pouch settings. */
public final class RefillSettingsPanel {
    public interface Access {
        ItemStack settings();

        void set(String field, String value);

        void setSource(String field, String value);

        void rebuild();

        void toggleTargets();

        void togglePause();

        /** Side-slot ids of the open menu; used only to decide slot numbering. */
        default List<String> sideIds() {
            return List.of();
        }

        /** Real stack limit of the item this destination holds or would receive; 0 if unknown. */
        default int destinationLimit(String destination) {
            return 0;
        }
    }

    private record LabelledButton(
            Button button, Supplier<Component> label, BooleanSupplier enabled) {}

    private static final String[] TAB_KEYS = {
        "refill_page_destinations", "refill_page_method", "refill_page_filter"
    };

    private final Access access;
    private final RefillSettingsView view;
    private final List<LabelledButton> live = new ArrayList<>();
    private final Map<Integer, SkinnedSlider> sliders = new HashMap<>();
    private String shape;
    private BagSettingsLayout layout;
    private Consumer<AbstractWidget> widgets;
    private int left, top;

    public RefillSettingsPanel(Access access, RefillSettingsView view) {
        this.access = access;
        this.view = view;
    }

    private static Component text(String key) {
        return Component.translatable("gui.packingcube." + key);
    }

    private static String tabKey(int page) {
        return TAB_KEYS[page];
    }

    private static Component format(String key, Object... args) {
        return Component.translatable("gui.packingcube." + key, args);
    }

    private void set(String key, Object value) {
        access.set(key, value.toString());
    }

    private RefillSlotSettings.Rule rule() {
        return view.rule(settings());
    }

    private void setRule(String key, Object value) {
        access.setSource(key, value.toString());
    }

    private ItemStack settings() {
        return access.settings();
    }

    private Button button(
            int row,
            int offset,
            int width,
            Supplier<Component> label,
            Runnable action,
            BooleanSupplier selected,
            BooleanSupplier enabled) {
        return button(row, offset, width, label, action, selected, enabled, false);
    }

    private Button button(
            int row,
            int offset,
            int width,
            Supplier<Component> label,
            Runnable action,
            BooleanSupplier selected,
            BooleanSupplier enabled,
            boolean toggle) {
        var rect = layout.optionRow(row);
        var b =
                Button.builder(label.get(), ignored -> action.run())
                        .bounds(left + rect.x() + offset, top + rect.y(), width, rect.height())
                        .build(
                                builder ->
                                        toggle
                                                ? SkinnedButton.toggle(builder, selected)
                                                : new SkinnedButton(builder)
                                                        .selected(
                                                                selected == null
                                                                        ? () -> false
                                                                        : selected));
        widgets.accept(b);
        live.add(new LabelledButton(b, label, enabled));
        return b;
    }

    private Button full(
            int row, Supplier<Component> label, Runnable action, BooleanSupplier selected) {
        return button(
                row,
                0,
                layout.content().width(),
                label,
                action,
                selected,
                () -> true,
                selected != null);
    }

    private void number(
            int row, Supplier<Integer> value, Consumer<Integer> change, int min, int max) {
        button(
                row,
                0,
                20,
                () -> Component.literal("-"),
                () -> change.accept(value.get() - 1),
                null,
                () -> value.get() > min);
        button(
                row,
                layout.content().width() - 20,
                20,
                () -> Component.literal("+"),
                () -> change.accept(value.get() + 1),
                null,
                () -> value.get() < max);
    }

    public void build(
            BagSettingsLayout layout, int left, int top, Consumer<AbstractWidget> widgets) {
        this.layout = layout;
        this.left = left;
        this.top = top;
        this.widgets = widgets;
        live.clear();
        sliders.clear();
        shape = view.shape(settings());
        for (int i = 0; i < 9; i++) {
            int source = i;
            int x = layout.content().width() * i / 9, next = layout.content().width() * (i + 1) / 9;
            button(
                    0,
                    x,
                    Math.max(1, next - x - 1),
                    () -> Component.literal(Integer.toString(source + 1)),
                    () -> {
                        view.source(source);
                        access.rebuild();
                    },
                    () -> view.source() == source,
                    () -> true);
        }
        for (int i = 0; i < 3; i++) {
            final int page = i;
            int x = layout.content().width() * i / 3, next = layout.content().width() * (i + 1) / 3;
            fullTab(
                    x,
                    next - x - 1,
                    () -> text(tabKey(page)),
                    () -> {
                        view.page(page);
                        access.rebuild();
                    },
                    () -> view.page() == page);
        }
        switch (view.page()) {
            case RefillSettingsView.DESTINATIONS -> buildDestinations();
            case RefillSettingsView.METHOD -> buildMethod();
            case RefillSettingsView.FILTER -> {
                full(
                        2,
                        () ->
                                text(
                                        "refill_filter_"
                                                + (RefillSettings.filterEnabled(settings())
                                                        ? "on"
                                                        : "off")),
                        () -> set("filter", !RefillSettings.filterEnabled(settings())),
                        () -> RefillSettings.filterEnabled(settings()));
                full(
                        3,
                        () ->
                                text(
                                        "refill_trade_"
                                                + (RefillSettings.enabled(settings(), true)
                                                        ? "on"
                                                        : "off")),
                        () -> set("trade", !RefillSettings.enabled(settings(), true)),
                        () -> RefillSettings.enabled(settings(), true));
                full(4, () -> text("refill_pause_start"), access::togglePause, null);
            }
            default -> throw new IllegalStateException();
        }
        tick();
    }

    private void fullTab(
            int offset,
            int width,
            Supplier<Component> label,
            Runnable action,
            BooleanSupplier selected) {
        // Tabs are navigation, not item settings. Re-selecting one leaves configuration intact.
        button(1, offset, width, label, action, selected, () -> true);
    }

    private void buildDestinations() {
        full(
                2,
                () ->
                        format(
                                "refill_source_enabled_" + (rule().enabled() ? "on" : "off"),
                                view.source() + 1),
                () -> setRule("enabled", !rule().enabled()),
                () -> rule().enabled());
        full(3, () -> text("refill_select_start"), access::toggleTargets, null);
        full(6, () -> text("refill_clear_destination"), () -> setRule("destination", ""), null);
        full(
                7,
                () -> text(rule().replace() ? "refill_replace_on" : "refill_replace_off"),
                () -> setRule("replace", !rule().replace()),
                () -> rule().replace());
        if (rule().replace()) {
            full(
                    8,
                    () -> text("refill_collect_" + (rule().collect() ? "on" : "off")),
                    () -> setRule("collect", !rule().collect()),
                    () -> rule().collect());
            full(
                    9,
                    () -> text("refill_lock_" + (rule().lock() ? "on" : "off")),
                    () -> setRule("lock", !rule().lock()),
                    () -> rule().lock());
        }
        number(priorityRow(), () -> rule().priority(), v -> setRule("priority", v), 0, 9);
    }

    private int priorityRow() {
        return view.priorityRow(settings());
    }

    private Component durabilityLabel(RefillSlotSettings.Rule rule) {
        int value = sliderValue(5, rule.durability());
        return switch (rule.durabilityMode()) {
            case "percent" -> format("refill_durability_percent", value);
            case "count" -> format("refill_durability_count", value);
            default -> text("refill_durability_off");
        };
    }

    /** Off, percent and count share one row so the method page stays two sliders wide. */
    private void cycleDurability() {
        setRule(
                "durabilityMode",
                switch (rule().durabilityMode()) {
                    case "percent" -> "count";
                    case "count" -> "off";
                    default -> "percent";
                });
    }

    private void buildMethod() {
        slider(
                3,
                () -> 1,
                this::stackLimit,
                this::effectiveTarget,
                value -> setRule("target", value),
                "refill_target_label");
        full(
                4,
                () -> durabilityLabel(rule()),
                this::cycleDurability,
                () -> !rule().durabilityOff());
        if (!rule().durabilityOff())
            slider(
                    5,
                    () -> 1,
                    () -> 100,
                    () -> Math.clamp(rule().durability(), 1, 100),
                    value -> setRule("durability", value),
                    "refill_durability_label");
        slider(
                7,
                () -> 0,
                () -> Math.max(0, effectiveTarget() - 1),
                this::effectiveThreshold,
                value -> setRule("threshold", value),
                "refill_threshold_label");
    }

    /** Real stack limit of the refilled item, so the slider never offers more than one stack. */
    private int stackLimit() {
        int limit = access.destinationLimit(rule().destination());
        return limit > 0 ? limit : RefillSettings.MAX_TARGET;
    }

    private int effectiveTarget() {
        return Math.min(rule().target(), stackLimit());
    }

    private int effectiveThreshold() {
        return Math.min(rule().threshold(), Math.max(0, effectiveTarget() - 1));
    }

    private void slider(
            int row,
            IntSupplier min,
            IntSupplier max,
            IntSupplier value,
            IntConsumer change,
            String labelKey) {
        var rect = layout.optionRow(row);
        var slider =
                new SkinnedSlider(
                        left + rect.x(),
                        top + rect.y(),
                        rect.width(),
                        rect.height(),
                        min,
                        max,
                        value,
                        change,
                        text(labelKey));
        sliders.put(row, slider);
        widgets.accept(slider);
    }

    /** Live slider value, so labels follow the knob instead of the last server snapshot. */
    private int sliderValue(int row, int fallback) {
        var slider = sliders.get(row);
        return slider == null ? fallback : slider.current();
    }

    private boolean draggingSlider() {
        for (var slider : sliders.values()) if (slider.dragging()) return true;
        return false;
    }

    public void tick() {
        if (shape != null && !shape.equals(view.shape(settings()))) {
            // Rebuilding mid-drag would replace the grabbed widget and drop the pointer.
            if (!draggingSlider()) access.rebuild();
            return;
        }
        for (var entry : live) {
            entry.button().setMessage(entry.label().get());
            entry.button().active = entry.enabled().getAsBoolean();
        }
    }

    private void label(GuiGraphics graphics, Font font, int row, Component text, int maxLines) {
        var rect = layout.optionRow(row);
        var lines = font.split(text, rect.width());
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++)
            graphics.drawString(
                    font, lines.get(i), rect.x(), rect.y() + 2 + i * font.lineHeight, 0xffffff);
    }

    private void valueRight(GuiGraphics graphics, Font font, int row, Component text) {
        var rect = layout.optionRow(row);
        graphics.drawString(
                font, text, rect.right() - font.width(text), rect.y() + 5, 0xffffff);
    }

    private void value(GuiGraphics graphics, Font font, int row, Component text) {
        var rect = layout.optionRow(row);
        graphics.drawCenteredString(font, text, rect.centerX(), rect.y() + 5, 0xffffff);
    }

    /** One explanation per control row, shown only while the help mode is on. */
    public record HelpEntry(int row, Component name, Component body) {}

    public HelpEntry helpAt(int row) {
        if (row < 0) return null;
        for (var helpRow : view.helpRows(settings()))
            if (helpRow.row() == row)
                return new HelpEntry(
                        row,
                        text("refill_help_name_" + helpRow.key()),
                        text("refill_help_body_" + helpRow.key()));
        return null;
    }

    public void renderLabels(GuiGraphics graphics, Font font) {
        if (!view.shape(settings()).equals(shape)) return;
        switch (view.page()) {
            case RefillSettingsView.DESTINATIONS -> {
                Component destination =
                        rule().destination().isEmpty()
                                ? text("refill_destination_none")
                                : RefillTargetPanel.targetName(
                                        rule().destination(), access.sideIds());
                label(
                        graphics,
                        font,
                        4,
                        format("refill_source_destination", view.source() + 1, destination),
                        2);
                value(graphics, font, priorityRow(), format("pickup_priority", rule().priority()));
            }
            case RefillSettingsView.METHOD -> {
                label(graphics, font, 2, text("refill_target_label"), 1);
                valueRight(
                        graphics,
                        font,
                        2,
                        format("refill_item_count", sliderValue(3, effectiveTarget())));
                label(graphics, font, 6, text("refill_threshold_label"), 1);
                valueRight(
                        graphics,
                        font,
                        6,
                        format("refill_threshold_count", sliderValue(7, effectiveThreshold())));
            }
            case RefillSettingsView.FILTER -> {}
            default -> throw new IllegalStateException();
        }
    }
}
