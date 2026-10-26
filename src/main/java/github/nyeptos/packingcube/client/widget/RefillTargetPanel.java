package github.nyeptos.packingcube.client.widget;

import com.mojang.blaze3d.systems.RenderSystem;

import github.nyeptos.packingcube.bag.refill.RefillSlotSettings;
import github.nyeptos.packingcube.bag.refill.RefillTargets;
import github.nyeptos.packingcube.gui.animation.SlotButtonAnimation;
import github.nyeptos.packingcube.gui.input.RefillTargetInteraction;
import github.nyeptos.packingcube.gui.skin.WidgetSkin;
import github.nyeptos.packingcube.menu.BagMenu;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Shared equipment side panel. Normal clicks use synchronized menu slots; selection is opt-in. */
public final class RefillTargetPanel {
    private record Target(String id, int x, int y, ItemStack stack, ResourceLocation icon) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
        }
    }

    private static final ResourceLocation EQUIPMENT = texture("equipment_panel.png");
    private static final ResourceLocation CURIOS = texture("curios_panel.png");
    private static final ResourceLocation SELECTED = texture("refill_target_selected.png");
    private static final ResourceLocation HOVERED = texture("refill_target_hovered.png");
    private static final ResourceLocation PRESSED = texture("refill_target_pressed.png");
    private static final int[] EQUIPMENT_SLOTS = {39, 38, 37, 36, 40};
    private static final ResourceLocation[] ICONS = {
        InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
        InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
        InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
        InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
        InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD
    };
    private final RefillTargetInteraction interaction = new RefillTargetInteraction();
    private List<Target> targets = List.of();
    private int left, top, curiosLeft, curiosWidth, scroll, total;
    private boolean scrollbarDragging, scrollbarHovered;
    private String hovered;
    private int source;

    public void source(int value) {
        source = value;
    }

    private boolean selected(BagMenu menu, String id) {
        return RefillSlotSettings.get(menu.filterSnapshot(), source).destination().equals(id);
    }

    private final Map<String, SlotButtonAnimation> animations = new HashMap<>();

    private SlotButtonAnimation animation(String id) {
        return animations.computeIfAbsent(id, ignored -> new SlotButtonAnimation());
    }

    private static ResourceLocation texture(String file) {
        return ResourceLocation.fromNamespaceAndPath("packingcube", "textures/gui/" + file);
    }

    public boolean selecting() {
        return interaction.selecting();
    }

    public void beginSelection(int source) {
        interaction.begin(source);
        hovered = null;
        animations.clear();
    }

    public int selectionSource() {
        return interaction.source();
    }

    public void selecting(boolean value) {
        interaction.selecting(value);
        hovered = null;
    }

    public void update(BagMenu menu, int left, int top) {
        this.left = left;
        this.top = top;
        var player = Minecraft.getInstance().player;
        if (player == null) {
            targets = List.of();
            return;
        }
        List<Target> current = new ArrayList<>();
        // The source pouch's nine slots deliberately never become destinations.
        for (int i = 9; i < menu.visibleSlotCount(); i++) {
            var slot = menu.getSlot(i);
            current.add(
                    new Target(
                            RefillTargets.inventory(slot.getContainerSlot()),
                            left + slot.x - 1,
                            top + slot.y - 1,
                            slot.getItem(),
                            null));
        }
        for (int i = 0; i < 5; i++)
            current.add(
                    new Target(
                            RefillTargets.inventory(EQUIPMENT_SLOTS[i]),
                            left + 184,
                            top + 34 + 18 * i,
                            menu.sideSlot(RefillTargets.inventory(EQUIPMENT_SLOTS[i])).getItem(),
                            ICONS[i]));
        var curios = menu.curiosTargets();
        total = curios.size();
        scroll = Math.clamp(scroll, 0, Math.max(0, total - 5));
        curiosWidth = total > 5 ? 34 : 26;
        curiosLeft = left - curiosWidth - 4;
        for (int i = 0; i < Math.min(5, total - scroll); i++) {
            var t = curios.get(scroll + i);
            current.add(
                    new Target(
                            t.id(),
                            curiosLeft + 4,
                            top + 34 + i * 18,
                            menu.sideSlot(t.id()).getItem(),
                            t.icon()));
        }
        targets = List.copyOf(current);
    }

    private Target at(double x, double y) {
        return targets.stream().filter(t -> t.contains(x, y)).findFirst().orElse(null);
    }

    public net.minecraft.world.inventory.Slot hoveredSlot(BagMenu menu, double x, double y) {
        if (!overSide(x, y) || overBar(x, y)) return null;
        Target target = at(x, y);
        return target == null ? null : menu.sideSlot(target.id());
    }

    public boolean overSide(double x, double y) {
        return y >= top + 30
                && y < top + 128
                && ((x >= left + 180 && x < left + 206)
                        || (total > 0 && x >= curiosLeft && x < curiosLeft + curiosWidth));
    }

    public void renderBackground(GuiGraphics g) {
        draw(g, EQUIPMENT, left + 180, top + 30, 26, 98);
        if (total > 0) {
            // Fixed artist borders; only the plain interior is extended for the scrollbar.
            WidgetTextures.draw(g, CURIOS, 26, 98, 3, 3, curiosLeft, top + 30, curiosWidth, 98);
        }
        for (Target t : targets) {
            if (t.icon() == null) continue; // Central inventory already rendered by the container.
            if (t.id().startsWith("curios:"))
                WidgetTextures.draw(g, WidgetSkin.GHOST_SLOT, t.x(), t.y(), 18, 18);
            if (t.stack().isEmpty()) {
                var sprite =
                        Minecraft.getInstance()
                                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                                .apply(t.icon());
                g.blit(t.x() + 1, t.y() + 1, 0, 16, 16, sprite);
            } else {
                g.renderItem(t.stack(), t.x() + 1, t.y() + 1);
                g.renderItemDecorations(
                        Minecraft.getInstance().font, t.stack(), t.x() + 1, t.y() + 1);
            }
        }
        if (total > 5) {
            WidgetTextures.draw(g, WidgetSkin.SCROLLBAR_TRACK, curiosLeft + 26, top + 34, 6, 90);
            WidgetTextures.draw(
                    g,
                    WidgetSkin.thumb(scrollbarDragging, scrollbarHovered),
                    curiosLeft + 26,
                    thumbY(),
                    6,
                    12);
        }
    }

    public void renderMarks(
            GuiGraphics g,
            BagMenu menu,
            int mouseX,
            int mouseY,
            boolean obstructed,
            boolean configuring) {
        Target hit =
                !obstructed && (selecting() || overSide(mouseX, mouseY))
                        ? at(mouseX, mouseY)
                        : null;
        String next = hit == null ? null : hit.id();
        if (!Objects.equals(hovered, next)) {
            hovered = next;
            if (hovered != null) UiInteractionSounds.hover();
        }
        long now = Util.getMillis();
        g.pose().pushPose();
        g.pose().translate(0, 0, 210);
        for (Target target : targets) {
            if (configuring && selected(menu, target.id())) drawAlpha(g, SELECTED, target, 1);
            var frame =
                    animation(target.id())
                            .sample(
                                    now,
                                    target.id().equals(hovered),
                                    interaction.pressed(target.id()));
            if (frame.hover() > .001F) drawAlpha(g, HOVERED, target, .7F * frame.hover());
            if (frame.press() > .001F) drawAlpha(g, PRESSED, target, .85F * frame.press());
        }
        g.pose().popPose();
        boolean bar = !obstructed && overBar(mouseX, mouseY);
        if (bar && !scrollbarHovered) UiInteractionSounds.hover();
        scrollbarHovered = bar;
    }

    private int thumbY() {
        return top + 34 + (total <= 5 ? 0 : scroll * 78 / (total - 5));
    }

    private boolean overBar(double x, double y) {
        return total > 5
                && x >= curiosLeft + 26
                && x < curiosLeft + 32
                && y >= top + 34
                && y < top + 124;
    }

    private void scrollTo(double y) {
        scroll = Math.clamp((int) Math.round((y - top - 40) * (total - 5) / 78.0), 0, total - 5);
    }

    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && overBar(x, y)) {
            scrollbarDragging = true;
            scrollTo(y);
            UiInteractionSounds.grab();
            return true;
        }
        if (selecting()) {
            Target target = at(x, y);
            interaction.press(target == null ? null : target.id(), button);
            if (target != null && button == 0) {
                animation(target.id()).press(Util.getMillis());
                UiInteractionSounds.grab();
            }
            return true; // Also suppress pickup, quick craft and drop outside the inventory.
        }
        Target target = overSide(x, y) ? at(x, y) : null;
        if (target != null && (button == 0 || button == 1)) {
            animation(target.id()).press(Util.getMillis());
            UiInteractionSounds.grab();
        }
        return false;
    }

    public String mouseReleased(double x, double y, int button) {
        if (button == 0) scrollbarDragging = false;
        Target target = at(x, y);
        String result = interaction.release(target == null ? null : target.id(), button);
        if (result != null) animation(result).press(Util.getMillis());
        return result;
    }

    public void cancelPress() {
        interaction.press(null, 0);
        scrollbarDragging = false;
    }

    public boolean dragging() {
        return scrollbarDragging;
    }

    public boolean mouseDragged(double y) {
        if (!scrollbarDragging) return false;
        scrollTo(y);
        return true;
    }

    public boolean mouseScrolled(double x, double y, double delta) {
        if (total <= 5
                || x < curiosLeft
                || x >= curiosLeft + curiosWidth
                || y < top + 30
                || y >= top + 128) return false;
        scroll = Math.clamp(scroll - (int) Math.signum(delta), 0, total - 5);
        return true;
    }

    public void renderTooltip(GuiGraphics g, BagMenu menu, int x, int y) {
        Target target = at(x, y);
        if (target == null || (!selecting() && (!overSide(x, y) || !target.stack().isEmpty())))
            return;
        var font = Minecraft.getInstance().font;
        var lines = new ArrayList<Component>();
        if (!target.stack().isEmpty()) lines.add(target.stack().getHoverName());
        lines.add(targetName(target.id()));
        if (!selecting()) {
            lines.add(Component.translatable("gui.packingcube.equipment_controls"));
            g.renderComponentTooltip(font, lines, x, y);
            return;
        }
        lines.add(
                Component.translatable(
                        "gui.packingcube."
                                + (selected(menu, target.id())
                                        ? "refill_selected"
                                        : "refill_unselected")));
        g.renderComponentTooltip(font, lines, x, y);
    }

    public static Component targetName(String id) {
        if (id.startsWith("curios:")) {
            int split = id.lastIndexOf(':');
            String type = id.substring(7, split);
            String key = "curios.identifier." + type;
            Component name =
                    net.minecraft.client.resources.language.I18n.exists(key)
                            ? Component.translatable(key)
                            : Component.literal(type);
            return name.copy().append(" " + (Integer.parseInt(id.substring(split + 1)) + 1));
        }
        int slot = Integer.parseInt(id.substring(10));
        if (slot < 9)
            return Component.translatable("gui.packingcube.refill_hotbar_index", slot + 1);
        if (slot < 36)
            return Component.translatable("gui.packingcube.refill_inventory_index", slot - 8);
        String[] names = {"feet", "legs", "chest", "head", "offhand"};
        return Component.translatable("gui.packingcube.refill_slot_" + names[slot - 36]);
    }

    private static void drawAlpha(GuiGraphics g, ResourceLocation texture, Target t, float alpha) {
        g.flush();
        g.setColor(1, 1, 1, alpha);
        draw(g, texture, t.x(), t.y(), 18, 18);
        g.setColor(1, 1, 1, 1);
    }

    private static void draw(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h) {
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(texture, x, y, 0, 0, w, h, w, h);
    }
}
