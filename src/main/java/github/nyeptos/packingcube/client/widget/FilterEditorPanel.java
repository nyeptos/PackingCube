package github.nyeptos.packingcube.client.widget;

import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.gui.input.FilterEditorState;
import github.nyeptos.packingcube.gui.layout.BagSettingsLayout;
import github.nyeptos.packingcube.gui.skin.WidgetSkin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Reusable filter widgets. The host supplies synchronized data and validates all requests. */
public final class FilterEditorPanel {
    public interface Access {
        ItemStack settings();

        ItemStack sample();

        void setExcluding(boolean excluding);

        void addRule(String field, String value);

        void removeRule(String field, String value);

        void sampleCursor(boolean clear);

        /** Override contextual wording for a non-storage bag without rewriting the widgets. */
        default Component modeHelp() {
            return Component.translatable("gui.packingcube.rules_help");
        }

        default Component emptyRules(boolean excluding) {
            return Component.translatable(
                    excluding ? "gui.packingcube.no_exclude_rules" : "gui.packingcube.no_rules");
        }
    }

    private final Font font;
    private final Access access;
    private final FilterEditorState state;
    private final List<Button> rowButtons = new ArrayList<>();
    private BagSettingsLayout layout;
    private Consumer<AbstractWidget> widgets;
    private int left, top, panelX, panelWidth;
    private EditBox search;
    private Button modeToggle, candidateTab, registeredTab, addItem, previous, next;
    private boolean ghostPressed;

    public FilterEditorPanel(Font font, Access access, FilterEditorState state) {
        this.font = font;
        this.access = access;
        this.state = state;
    }

    private static Component text(String key) {
        return Component.translatable("gui.packingcube." + key);
    }

    private <T extends AbstractWidget> T addWidget(T widget) {
        widgets.accept(widget);
        return widget;
    }

    public void build(
            BagSettingsLayout layout, int left, int top, Consumer<AbstractWidget> widgets) {
        this.layout = layout;
        this.left = left;
        this.top = top;
        this.widgets = widgets;
        ghostPressed = false;
        rowButtons.clear();
        panelX = layout.content().x();
        panelWidth = layout.content().width();
        int half = (panelWidth - 4) / 2;
        modeToggle =
                addWidget(
                        Button.builder(
                                        Component.empty(),
                                        button ->
                                                access.setExcluding(
                                                        !BagFilter.isExcluding(access.settings())))
                                .bounds(left + panelX, top + layout.modeY(), panelWidth, 20)
                                .build(SkinnedButton::new));
        addItem =
                addWidget(
                        Button.builder(
                                        text("add_item"),
                                        button -> {
                                            if (!access.sample().isEmpty())
                                                access.addRule(
                                                        "items",
                                                        BuiltInRegistries.ITEM
                                                                .getKey(access.sample().getItem())
                                                                .toString());
                                        })
                                .bounds(left + panelX, top + layout.addItemY(), panelWidth, 20)
                                .build(SkinnedButton::new));
        candidateTab =
                addWidget(
                        Button.builder(
                                        text("tags"),
                                        button -> {
                                            state.setRegistered(false);
                                            updateEditor();
                                        })
                                .bounds(left + panelX, top + layout.tabsY(), half, 20)
                                .build(
                                        b ->
                                                new SkinnedButton(b)
                                                        .selected(() -> !state.registered())));
        registeredTab =
                addWidget(
                        Button.builder(
                                        text("registered"),
                                        button -> {
                                            state.setRegistered(true);
                                            updateEditor();
                                        })
                                .bounds(left + panelX + half + 4, top + layout.tabsY(), half, 20)
                                .build(b -> new SkinnedButton(b).selected(state::registered)));
        search =
                new SkinnedEditBox(
                        font,
                        left + panelX,
                        top + layout.searchY(),
                        panelWidth,
                        18,
                        text("search"));
        search.setMaxLength(256);
        search.setHint(text("search"));
        search.setValue(state.query());
        search.setResponder(
                value -> {
                    state.setQuery(value);
                    updateEditor();
                });
        addWidget(search);
        int pageSize = layout.rulePageSize();
        state.setPageSize(pageSize);
        for (int i = 0; i < pageSize; i++) {
            final int row = i;
            rowButtons.add(
                    addWidget(
                            Button.builder(
                                            Component.empty(),
                                            button -> {
                                                var visible = state.visibleRules();
                                                if (row >= visible.size()) return;
                                                var rule = visible.get(row);
                                                if (state.registered())
                                                    access.removeRule(rule.field(), rule.value());
                                                else access.addRule(rule.field(), rule.value());
                                            })
                                    .bounds(left + panelX, top + layout.ruleRowY(i), panelWidth, 18)
                                    .build(SkinnedButton::new)));
        }
        previous =
                addWidget(
                        Button.builder(
                                        Component.literal("<"),
                                        button -> {
                                            state.previous();
                                            updateEditor();
                                        })
                                .bounds(left + panelX, top + layout.pagerY(), 24, 18)
                                .build(SkinnedButton::new));
        next =
                addWidget(
                        Button.builder(
                                        Component.literal(">"),
                                        button -> {
                                            state.next();
                                            updateEditor();
                                        })
                                .bounds(
                                        left + panelX + panelWidth - 24,
                                        top + layout.pagerY(),
                                        24,
                                        18)
                                .build(SkinnedButton::new));
        updateEditor();
    }

    public void tick() {
        updateEditor();
    }

    private void updateEditor() {
        if (search == null) return;
        state.refresh(access.settings(), access.sample());
        modeToggle.setMessage(
                Component.translatable(
                        "gui.packingcube.filter_mode",
                        text(state.excluding() ? "exclude" : "allow")));
        candidateTab.active = true;
        registeredTab.active = true;
        addItem.active = !access.sample().isEmpty();
        previous.active = state.hasPrevious();
        next.active = state.hasNext();
        var visible = state.visibleRules();
        for (int i = 0; i < rowButtons.size(); i++) {
            Button button = rowButtons.get(i);
            button.visible = i < visible.size();
            if (!button.visible) continue;
            var rule = visible.get(i);
            boolean tag = rule.field().endsWith("tags");
            boolean already =
                    BagFilter.entries(access.settings(), rule.field()).contains(rule.value());
            String label =
                    (state.registered() ? "× " : already ? "✓ " : "+ ")
                            + (tag ? "#" : "")
                            + rule.value();
            button.setMessage(Component.literal(font.plainSubstrByWidth(label, panelWidth - 10)));
            button.active = state.registered() || !already;
            String tooltip = (tag ? "#" : "") + rule.value();
            if (tag) tooltip += tagPreview(rule.value());
            button.setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
    }

    private String tagPreview(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) return "";
        var tag = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, id));
        if (tag.isEmpty()) return "\n" + text("empty_tag").getString();
        List<String> names =
                tag.get().stream()
                        .limit(5)
                        .map(holder -> holder.value().getDescription().getString())
                        .toList();
        return "\n"
                + Component.translatable("gui.packingcube.tag_count", tag.get().size()).getString()
                + "\n"
                + String.join(", ", names)
                + (tag.get().size() > 5 ? " …" : "");
    }

    public void renderLabels(GuiGraphics graphics) {
        graphics.drawString(
                font,
                font.plainSubstrByWidth(text("sample").getString(), panelWidth),
                panelX,
                layout.sampleLabelY(),
                0xffffff,
                true);
        ItemStack sample = access.sample();
        if (!sample.isEmpty()) {
            graphics.renderItem(sample, layout.sampleSlot().x() + 1, layout.sampleSlot().y() + 1);
            graphics.drawString(
                    font,
                    font.plainSubstrByWidth(sample.getHoverName().getString(), panelWidth - 24),
                    layout.sampleSlot().right() + 4,
                    layout.sampleSlot().y() + 5,
                    0xffffff,
                    true);
        } else
            graphics.drawString(
                    font,
                    text("no_sample"),
                    layout.sampleSlot().right() + 4,
                    layout.sampleSlot().y() + 5,
                    0xffffff,
                    true);
        graphics.drawWordWrap(
                font,
                text("sample_short_help"),
                panelX,
                layout.sampleHelpY(),
                panelWidth,
                0xffffff);
        int x = panelX;
        int pages = state.pageCount();
        String paging = (state.page() + 1) + " / " + pages;
        graphics.drawString(
                font,
                paging,
                x + (panelWidth - font.width(paging)) / 2,
                layout.pagerY() + 5,
                0xffffff,
                true);
        if (state.empty())
            graphics.drawWordWrap(
                    font,
                    state.registered() ? access.emptyRules(state.excluding()) : text("no_tags"),
                    x,
                    layout.ruleRowY(0) + 2,
                    panelWidth,
                    0xffffff);
    }

    public boolean searchFocused() {
        return search != null && search.isFocused();
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!searchFocused()) return false;
        if (keyCode == 256) {
            search.setFocused(false);
            return true;
        }
        return search.keyPressed(keyCode, scanCode, modifiers);
    }

    public void moveOrigin(int left, int top) {
        this.left = left;
        this.top = top;
    }

    public boolean overSample(double mouseX, double mouseY) {
        return layout != null && layout.sampleSlot().contains(mouseX - left, mouseY - top);
    }

    public boolean samplePressed(double mouseX, double mouseY, int button) {
        if (!overSample(mouseX, mouseY) || (button != 0 && button != 1)) return false;
        access.sampleCursor(button == 1);
        ghostPressed = true;
        return true;
    }

    public boolean sampleReleased(double mouseX, double mouseY, int button) {
        if (ghostPressed) {
            ghostPressed = false;
            return true;
        }
        if (!overSample(mouseX, mouseY) || (button != 0 && button != 1)) return false;
        access.sampleCursor(button == 1);
        return true;
    }

    public void renderBackground(GuiGraphics graphics) {
        var slot = layout.sampleSlot();
        WidgetTextures.draw(
                graphics, WidgetSkin.GHOST_SLOT, slot.x(), slot.y(), slot.width(), slot.height());
    }

    public void renderTooltip(
            GuiGraphics graphics,
            int screenX,
            int screenY,
            double unscrolledX,
            double unscrolledY) {
        if (overSample(unscrolledX, unscrolledY))
            graphics.renderTooltip(font, text("ghost_help"), screenX, screenY);
    }
}
