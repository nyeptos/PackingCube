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
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Source 1–9 pages, per-source destination/method, and explicitly shared pouch settings. */
public final class RefillSettingsPanel {
    public interface Access {
        ItemStack settings();

        void set(String field, String value);

        void setSource(String field, String value);

        void rebuild();

        void toggleTargets();
    }

    private record LabelledButton(
            Button button, Supplier<Component> label, BooleanSupplier enabled) {}

    private final Access access;
    private final RefillSettingsView view;
    private final List<LabelledButton> live = new ArrayList<>();
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
        String[] tabs = {"refill_page_destinations", "refill_page_method", "refill_page_filter"};
        for (int i = 0; i < 3; i++) {
            final int page = i;
            int x = layout.content().width() * i / 3, next = layout.content().width() * (i + 1) / 3;
            fullTab(
                    x,
                    next - x - 1,
                    () -> text(tabs[page]),
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
        number(8, () -> rule().priority(), v -> setRule("priority", v), 0, 9);
    }

    private void buildMethod() {
        full(
                2,
                () -> text("refill_timing_" + rule().timing()),
                () -> {
                    var all = RefillSettings.TIMINGS;
                    setRule("timing", all.get((all.indexOf(rule().timing()) + 1) % all.size()));
                },
                null);
        if (view.thresholdVisible(settings()))
            number(4, () -> rule().threshold(), v -> setRule("threshold", v), 0, 63);
        full(
                5,
                () -> text(rule().target() == 0 ? "refill_amount_full" : "refill_amount_custom"),
                () -> setRule("target", rule().target() == 0 ? 16 : 0),
                null);
        if (view.targetVisible(settings()))
            number(7, () -> rule().target(), v -> setRule("target", v), 1, 64);
    }

    public void tick() {
        if (shape != null && !shape.equals(view.shape(settings()))) {
            access.rebuild();
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

    private void value(GuiGraphics graphics, Font font, int row, Component text) {
        var rect = layout.optionRow(row);
        graphics.drawCenteredString(font, text, rect.centerX(), rect.y() + 5, 0xffffff);
    }

    public void renderLabels(GuiGraphics graphics, Font font) {
        if (!view.shape(settings()).equals(shape)) return;
        switch (view.page()) {
            case RefillSettingsView.DESTINATIONS -> {
                Component destination =
                        rule().destination().isEmpty()
                                ? text("refill_destination_none")
                                : RefillTargetPanel.targetName(rule().destination());
                label(
                        graphics,
                        font,
                        4,
                        format("refill_source_destination", view.source() + 1, destination),
                        3);
                value(graphics, font, 8, format("pickup_priority", rule().priority()));
                label(graphics, font, 9, text("refill_source_priority_help"), 4);
            }
            case RefillSettingsView.METHOD -> {
                if (view.thresholdVisible(settings())) {
                    label(graphics, font, 3, text("refill_threshold_label"), 2);
                    value(graphics, font, 4, format("refill_threshold_count", rule().threshold()));
                }
                if (view.targetVisible(settings())) {
                    label(graphics, font, 6, text("refill_target_label"), 2);
                    value(graphics, font, 7, format("refill_item_count", rule().target()));
                }
                label(graphics, font, 8, text("refill_source_amount_help"), 4);
            }
            case RefillSettingsView.FILTER ->
                    label(graphics, font, 4, text("refill_shared_help"), 4);
            default -> throw new IllegalStateException();
        }
    }
}
