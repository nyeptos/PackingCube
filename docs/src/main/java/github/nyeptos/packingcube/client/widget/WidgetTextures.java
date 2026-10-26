package github.nyeptos.packingcube.client.widget;

import com.mojang.blaze3d.systems.RenderSystem;

import github.nyeptos.packingcube.gui.GuiSurface;
import github.nyeptos.packingcube.gui.NineSliceAxis;
import github.nyeptos.packingcube.gui.RadialSlotSkin;
import github.nyeptos.packingcube.gui.WidgetSkin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;

/** Raw PNG nine-slicing; no vanilla atlas replacement or resource-pack-wide overrides. */
public final class WidgetTextures {
    private static final EnumMap<WidgetSkin, ResourceLocation> TEXTURES =
            new EnumMap<>(WidgetSkin.class);

    static {
        for (var skin : WidgetSkin.values())
            TEXTURES.put(
                    skin,
                    ResourceLocation.fromNamespaceAndPath(
                            "packingcube", "textures/gui/widgets/" + skin.fileName()));
    }

    private static final EnumMap<GuiSurface, ResourceLocation> SURFACES =
            new EnumMap<>(GuiSurface.class);

    static {
        for (var surface : GuiSurface.values())
            SURFACES.put(
                    surface, ResourceLocation.fromNamespaceAndPath("packingcube", surface.path));
    }

    private static final EnumMap<RadialSlotSkin, ResourceLocation> RADIAL_SLOTS =
            new EnumMap<>(RadialSlotSkin.class);

    static {
        for (var skin : RadialSlotSkin.values())
            RADIAL_SLOTS.put(skin, ResourceLocation.fromNamespaceAndPath("packingcube", skin.path));
    }

    private WidgetTextures() {}

    public static void draw(GuiGraphics g, RadialSlotSkin skin, int x, int y) {
        draw(
                g,
                RADIAL_SLOTS.get(skin),
                RadialSlotSkin.SIZE,
                RadialSlotSkin.SIZE,
                0,
                0,
                x,
                y,
                RadialSlotSkin.SIZE,
                RadialSlotSkin.SIZE);
    }

    public static void draw(
            GuiGraphics g, GuiSurface surface, int x, int y, int width, int height) {
        draw(
                g,
                SURFACES.get(surface),
                surface.width,
                surface.height,
                surface.border,
                surface.border,
                x,
                y,
                width,
                height);
    }

    public static void draw(GuiGraphics g, WidgetSkin skin, int x, int y, int width, int height) {
        draw(
                g,
                TEXTURES.get(skin),
                skin.width,
                skin.height,
                skin.borderX,
                skin.borderY,
                x,
                y,
                width,
                height);
    }

    private static void draw(
            GuiGraphics g,
            ResourceLocation texture,
            int sourceWidth,
            int sourceHeight,
            int borderX,
            int borderY,
            int x,
            int y,
            int width,
            int height) {
        if (width <= 0 || height <= 0) return;
        // Raw PNG blits draw immediately; settle earlier fill/text batches before drawing them.
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        var horizontal = new NineSliceAxis(sourceWidth, width, borderX);
        var vertical = new NineSliceAxis(sourceHeight, height, borderY);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++) {
                int w = horizontal.targetLength(col), h = vertical.targetLength(row);
                if (w == 0 || h == 0) continue;
                g.blit(
                        texture,
                        x + horizontal.targetStart(col),
                        y + vertical.targetStart(row),
                        w,
                        h,
                        horizontal.sourceStart(col),
                        vertical.sourceStart(row),
                        horizontal.sourceLength(col),
                        vertical.sourceLength(row),
                        sourceWidth,
                        sourceHeight);
            }
    }
}
