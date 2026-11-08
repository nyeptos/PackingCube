package github.nyeptos.packingcube.client;

import com.mojang.blaze3d.systems.RenderSystem;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.client.widget.SkinnedButton;
import github.nyeptos.packingcube.client.widget.UiInteractionSounds;
import github.nyeptos.packingcube.client.widget.WidgetTextures;
import github.nyeptos.packingcube.craft.PouchRecipe;
import github.nyeptos.packingcube.craft.PouchRecipes;
import github.nyeptos.packingcube.gui.animation.ScrollbarSoundState;
import github.nyeptos.packingcube.gui.animation.SlotButtonAnimation;
import github.nyeptos.packingcube.gui.animation.SlotFeedback;
import github.nyeptos.packingcube.gui.input.SettingsScrollState;
import github.nyeptos.packingcube.gui.skin.WidgetSkin;
import github.nyeptos.packingcube.menu.PouchStationMenu;
import github.nyeptos.packingcube.network.PouchStationActionPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The station panel. The artist owns the surfaces; this class places them, scrolls the pouch list
 * and draws the words, the numbers and the item icons.
 */
public final class PouchStationScreen extends AbstractContainerScreen<PouchStationMenu> {
    private static final ResourceLocation CRAFTING_PANEL =
            texture("gui/container/pouch_station_crafting.png");
    private static final ResourceLocation ROW_NORMAL =
            texture("gui/container/pouch_station_row_normal.png");
    private static final ResourceLocation ROW_SELECTED =
            texture("gui/container/pouch_station_row_selected.png");
    private static final ResourceLocation ROW_HOVERED =
            texture("gui/container/pouch_station_row_hovered.png");
    private static final ResourceLocation ROW_PRESSED =
            texture("gui/container/pouch_station_row_pressed.png");
    private static final ResourceLocation ROW_SELECTED_HOVERED =
            texture("gui/container/pouch_station_row_selected_hovered.png");
    private static final ResourceLocation SLOT_HOVERED = texture("gui/refill_target_hovered.png");
    private static final ResourceLocation SLOT_PRESSED = texture("gui/refill_target_pressed.png");
    private static final int FALLBACK_PANEL = 0xffc6c6c6;
    /** The artist's panel is dark, so every word on it goes light. */
    private static final int TEXT = 0xf2f2f2;
    private static final int ROW_TEXT = 0xf2f2f2;
    private static final int ROW_TEXT_DIM = 0xd6d6d6;
    private static final int ROW_TEXT_SHORT = 0xff8a8a;
    private static final int TEXT_DIM = 0xd6d6d6;
    private static final int TEXT_READY = 0x9fe07a;
    private static final int TEXT_SHORT = 0xff8a8a;
    private static final int TITLE_LEFT = 8;
    private static final int TITLE_TOP = 12;
    /** The legend rides the title line, right-aligned over the list. */
    private static final int LEGEND_RIGHT = PouchStationMenu.LIST_LEFT + PouchStationMenu.LIST_WIDTH;
    private static final int LEGEND_TOP = 12;
    private static final int RESULT_LABEL_TOP = 26;
    private static final int STATUS_TOP = 94;
    private static final float ROW_HOVER_ALPHA = 1F;
    private static final float SLOT_HOVER_ALPHA = .7F;
    private static final float SLOT_PRESS_ALPHA = .85F;

    private final SettingsScrollState scroll = new SettingsScrollState();
    private final SlotFeedback rowFeedback = new SlotFeedback();
    private final SlotFeedback slotFeedback = new SlotFeedback();
    private final ScrollbarSoundState scrollbarSounds = new ScrollbarSoundState();
    private List<PouchRecipe> recipes = List.of();
    private Button craftButton;
    private boolean thumbDragging;
    private double thumbGrab;
    private int pressedRow = -1;
    private int hoveredRow = -1;

    public PouchStationScreen(PouchStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PouchStationMenu.PANEL_WIDTH;
        this.imageHeight = PouchStationMenu.PANEL_HEIGHT;
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(PackingCube.MODID, "textures/" + path);
    }

    private static boolean present(ResourceLocation texture) {
        return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
    }

    @Override
    protected void init() {
        super.init();
        rowFeedback.clear();
        slotFeedback.clear();
        pressedRow = -1;
        hoveredRow = -1;
        refreshRecipes();
        craftButton =
                addRenderableWidget(
                        Button.builder(
                                        Component.translatable(
                                                "gui.packingcube.pouch_station.craft"),
                                        button ->
                                                send(
                                                        PouchStationActionPayload.ACTION_CRAFT,
                                                        0))
                                .bounds(
                                        leftPos + PouchStationMenu.BUTTON_LEFT,
                                        topPos + PouchStationMenu.BUTTON_TOP,
                                        PouchStationMenu.BUTTON_WIDTH,
                                        PouchStationMenu.BUTTON_HEIGHT)
                                .build(SkinnedButton::new));
        craftButton.active = menu.canCraftSelected();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (craftButton == null) return;
        refreshRecipes();
        craftButton.active = menu.canCraftSelected();
    }

    /** The bills arrive with the recipe sync, so the list can grow after the panel opens. */
    private void refreshRecipes() {
        recipes = PouchRecipes.all(minecraft == null ? null : minecraft.level);
        scroll.configure(
                PouchStationMenu.LIST_HEIGHT,
                recipes.size() * PouchStationMenu.ROW_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawPanel(graphics);
        hoveredRow = rowAt(mouseX - leftPos, mouseY - topPos);
        drawCraftingTab(graphics, mouseX, mouseY);
        drawSlotFeedback(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, TITLE_LEFT, TITLE_TOP, TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int localX = (int) mouseX - leftPos;
            int localY = (int) mouseY - topPos;
            if (scrollbarAt(localX, localY)) {
                thumbDragging = true;
                double thumbTop = PouchStationMenu.LIST_TOP + scroll.thumbTop();
                double inside = localY - thumbTop;
                thumbGrab =
                        inside >= 0 && inside < scroll.thumbHeight()
                                ? inside
                                : scroll.thumbHeight() / 2.0;
                scroll.dragThumb(localY - PouchStationMenu.LIST_TOP - thumbGrab);
                if (scrollbarSounds.grab(scroll.scrollable())) UiInteractionSounds.grab();
                return true;
            }
            int row = rowAt(localX, localY);
            if (row >= 0) {
                pressRow(row);
                send(PouchStationActionPayload.ACTION_SELECT, row);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX, double mouseY, double scrollX, double scrollY) {
        int localX = (int) mouseX - leftPos;
        int localY = (int) mouseY - topPos;
        if (scroll.scrollable()
                && localX >= PouchStationMenu.LIST_LEFT
                && localX < PouchStationMenu.LIST_LEFT + PouchStationMenu.LIST_WIDTH
                && localY >= PouchStationMenu.LIST_TOP
                && localY < PouchStationMenu.LIST_TOP + PouchStationMenu.LIST_HEIGHT) {
            scroll.wheel(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(
            double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (thumbDragging) {
            scroll.dragThumb(mouseY - topPos - PouchStationMenu.LIST_TOP - thumbGrab);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        thumbDragging = false;
        pressedRow = -1;
        scrollbarSounds.release();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** Slots answer a click the way every other panel does: a press pulse and the shared click. */
    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slot != null) {
            slotFeedback.press("slot" + slot.index);
            UiInteractionSounds.grab();
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    private void pressRow(int row) {
        pressedRow = row;
        rowFeedback.press("row" + row);
        UiInteractionSounds.grab();
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Component> bill = billTooltip(mouseX, mouseY);
        if (bill != null) {
            graphics.renderTooltip(font, bill, Optional.empty(), mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private void send(int action, int value) {
        PacketDistributor.sendToServer(
                new PouchStationActionPayload(menu.containerId, action, value));
    }

    private void drawPanel(GuiGraphics graphics) {
        ResourceLocation plate = CRAFTING_PANEL;
        if (present(plate)) {
            graphics.blit(
                    plate,
                    leftPos,
                    topPos,
                    imageWidth,
                    imageHeight,
                    0.0F,
                    0.0F,
                    imageWidth,
                    imageHeight,
                    PouchStationMenu.PANEL_WIDTH,
                    PouchStationMenu.PANEL_HEIGHT);
            return;
        }
        // Only a flat plate, so the panel is readable before the artist's texture lands.
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, FALLBACK_PANEL);
    }

    private void drawCraftingTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = leftPos + PouchStationMenu.LIST_LEFT;
        int top = topPos + PouchStationMenu.LIST_TOP;
        int right = left + PouchStationMenu.LIST_WIDTH;
        int bottom = top + PouchStationMenu.LIST_HEIGHT;
        if (rowFeedback.enter(hoveredRow < 0 ? null : "row" + hoveredRow))
            UiInteractionSounds.hover();
        long now = Util.getMillis();
        graphics.enableScissor(left, top, right, bottom);
        for (int index = 0; index < recipes.size(); index++) {
            int rowTop = top + index * PouchStationMenu.ROW_HEIGHT - scroll.offset();
            if (rowTop + PouchStationMenu.ROW_HEIGHT <= top || rowTop >= bottom) continue;
            drawRow(graphics, left, rowTop, index, recipes.get(index), now);
        }
        graphics.disableScissor();
        drawScrollbar(graphics, mouseX, mouseY);
        drawColumn(graphics);
    }

    private void drawRow(
            GuiGraphics graphics,
            int left,
            int top,
            int index,
            PouchRecipe recipe,
            long now) {
        int width = PouchStationMenu.LIST_WIDTH;
        int height = PouchStationMenu.ROW_HEIGHT;
        SlotButtonAnimation.Frame frame = rowFeedback.frame("row" + index, now, false);
        ItemStack icon = recipe.resultStack();
        String name = font.plainSubstrByWidth(icon.getHoverName().getString(), width - 26);
        blitRow(graphics, ROW_NORMAL, left, top, 1F);
        if (index == menu.selected()) {
            blitRow(graphics, ROW_SELECTED, left, top, 1F);
            if (frame.hover() > .001F)
                blitRow(
                        graphics,
                        ROW_SELECTED_HOVERED,
                        left,
                        top,
                        ROW_HOVER_ALPHA * frame.hover());
        } else if (frame.hover() > .001F)
            blitRow(graphics, ROW_HOVERED, left, top, ROW_HOVER_ALPHA * frame.hover());
        if (frame.press() > .001F)
            blitRow(graphics, ROW_HOVERED, left, top, ROW_HOVER_ALPHA * frame.press());
        graphics.renderItem(icon, left + 2, top + 4);
        graphics.drawString(font, name, left + 22, top + 3, ROW_TEXT, false);
        drawCost(graphics, left + 22, top + 13, recipe);
    }

    private void blitRow(
            GuiGraphics graphics, ResourceLocation plate, int left, int top, float alpha) {
        if (plate == null || !present(plate)) return;
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1F, 1F, 1F, Math.clamp(alpha, 0F, 1F));
        graphics.blit(
                plate,
                left,
                top,
                PouchStationMenu.LIST_WIDTH,
                PouchStationMenu.ROW_HEIGHT,
                0.0F,
                0.0F,
                PouchStationMenu.LIST_WIDTH,
                PouchStationMenu.ROW_HEIGHT,
                PouchStationMenu.LIST_WIDTH,
                PouchStationMenu.ROW_HEIGHT);
        graphics.setColor(1F, 1F, 1F, 1F);
    }

    /** "held/need" next to a half-size icon, so three lines of cost fit one row. */
    private void drawCost(GuiGraphics graphics, int x, int y, PouchRecipe recipe) {
        int cursor = x;
        for (PouchRecipe.Material material : recipe.materials()) {
            int held = PouchRecipes.held(menu.inventory(), material.item());
            boolean enough = held >= material.count();
            drawSmallItem(graphics, material.stack(), cursor, y);
            String text = held + "/" + material.count();
            graphics.drawString(
                    font, text, cursor + 9, y, enough ? ROW_TEXT_DIM : ROW_TEXT_SHORT, false);
            cursor += 8 + font.width(text) + 3;
        }
    }

    private void drawSmallItem(GuiGraphics graphics, ItemStack stack, int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
    }

    private void drawScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!scroll.scrollable()) return;
        int x = leftPos + PouchStationMenu.SCROLLBAR_LEFT;
        int y = topPos + PouchStationMenu.LIST_TOP;
        int width = PouchStationMenu.SCROLLBAR_WIDTH;
        WidgetTextures.draw(
                graphics, WidgetSkin.SCROLLBAR_TRACK, x, y, width, PouchStationMenu.LIST_HEIGHT);
        int thumbY = y + scroll.thumbTop();
        boolean hovered =
                mouseX >= x
                        && mouseX < x + width
                        && mouseY >= thumbY
                        && mouseY < thumbY + scroll.thumbHeight();
        if (scrollbarSounds.hover(scroll.scrollable(), hovered)) UiInteractionSounds.hover();
        WidgetTextures.draw(
                graphics,
                WidgetSkin.thumb(thumbDragging, hovered),
                x,
                thumbY,
                width,
                scroll.thumbHeight());
    }

    /** Every slot in the panel fades its hover in and pulses on the click, like the bag panels. */
    private void drawSlotFeedback(GuiGraphics graphics, int mouseX, int mouseY) {
        int hovered = slotAt(mouseX - leftPos, mouseY - topPos);
        if (slotFeedback.enter(hovered < 0 ? null : "slot" + hovered)) UiInteractionSounds.hover();
        long now = Util.getMillis();
        for (int index = 0; index < menu.slots.size(); index++) {
            SlotButtonAnimation.Frame frame = slotFeedback.frame("slot" + index, now, false);
            if (frame.hover() <= .001F && frame.press() <= .001F) continue;
            Slot slot = menu.slots.get(index);
            int x = leftPos + slot.x - 1;
            int y = topPos + slot.y - 1;
            if (frame.hover() > .001F)
                drawSlotPlate(graphics, SLOT_HOVERED, x, y, SLOT_HOVER_ALPHA * frame.hover());
            if (frame.press() > .001F)
                drawSlotPlate(graphics, SLOT_PRESSED, x, y, SLOT_PRESS_ALPHA * frame.press());
        }
    }

    private static void drawSlotPlate(
            GuiGraphics graphics, ResourceLocation texture, int x, int y, float alpha) {
        if (!present(texture)) return;
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1F, 1F, 1F, Math.clamp(alpha, 0F, 1F));
        graphics.blit(texture, x, y, 0, 0, 18, 18, 18, 18);
        graphics.setColor(1F, 1F, 1F, 1F);
    }

    private int slotAt(int localX, int localY) {
        for (int index = 0; index < menu.slots.size(); index++) {
            Slot slot = menu.slots.get(index);
            if (localX >= slot.x - 1
                    && localX < slot.x + 17
                    && localY >= slot.y - 1
                    && localY < slot.y + 17) return index;
        }
        return -1;
    }

    private void drawColumn(GuiGraphics graphics) {
        int column = leftPos + PouchStationMenu.BUTTON_LEFT;
        int width = PouchStationMenu.BUTTON_WIDTH;
        Component legend = Component.translatable("gui.packingcube.pouch_station.need_held");
        graphics.drawString(
                font,
                legend,
                leftPos + LEGEND_RIGHT - font.width(legend),
                topPos + LEGEND_TOP,
                TEXT_DIM,
                false);
        Component label = Component.translatable("gui.packingcube.pouch_station.result");
        graphics.drawString(
                font,
                label,
                column + (width - font.width(label)) / 2,
                topPos + RESULT_LABEL_TOP,
                TEXT,
                false);
        boolean ready = menu.canCraftSelected();
        Component status =
                Component.translatable(
                        ready
                                ? "gui.packingcube.pouch_station.ready"
                                : "gui.packingcube.pouch_station.missing");
        graphics.drawString(
                font,
                status,
                column + (width - font.width(status)) / 2,
                topPos + STATUS_TOP,
                ready ? TEXT_READY : TEXT_SHORT,
                false);
    }

    private List<Component> billTooltip(int mouseX, int mouseY) {
        int row = rowAt(mouseX - leftPos, mouseY - topPos);
        return row >= 0 ? bill(recipes.get(row)) : null;
    }

    private List<Component> bill(PouchRecipe recipe) {
        if (recipe == null) return null;
        List<Component> lines = new ArrayList<>();
        lines.add(recipe.resultStack().getHoverName().copy().withStyle(ChatFormatting.BOLD));
        lines.add(
                Component.translatable("gui.packingcube.pouch_station.need")
                        .withStyle(ChatFormatting.GRAY));
        for (PouchRecipe.Material material : recipe.materials()) {
            int held = PouchRecipes.held(menu.inventory(), material.item());
            boolean enough = held >= material.count();
            lines.add(
                    Component.literal(
                                    material.stack().getHoverName().getString()
                                            + " "
                                            + held
                                            + "/"
                                            + material.count())
                            .withStyle(enough ? ChatFormatting.GRAY : ChatFormatting.RED));
        }
        lines.add(
                Component.translatable("gui.packingcube.pouch_station.craft_hint")
                        .withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    private int rowAt(int localX, int localY) {
        if (localX < PouchStationMenu.LIST_LEFT
                || localX >= PouchStationMenu.LIST_LEFT + PouchStationMenu.LIST_WIDTH) return -1;
        if (localY < PouchStationMenu.LIST_TOP
                || localY >= PouchStationMenu.LIST_TOP + PouchStationMenu.LIST_HEIGHT) return -1;
        int row = (localY - PouchStationMenu.LIST_TOP + scroll.offset())
                / PouchStationMenu.ROW_HEIGHT;
        return row < recipes.size() ? row : -1;
    }

    private boolean scrollbarAt(int localX, int localY) {
        return scroll.scrollable()
                && localX >= PouchStationMenu.SCROLLBAR_LEFT
                && localX < PouchStationMenu.SCROLLBAR_LEFT + PouchStationMenu.SCROLLBAR_WIDTH
                && localY >= PouchStationMenu.LIST_TOP
                && localY < PouchStationMenu.LIST_TOP + PouchStationMenu.LIST_HEIGHT;
    }
}
