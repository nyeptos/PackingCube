package github.nyeptos.packingcube.gametest.gui;

import com.google.gson.JsonParser;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.gui.layout.RadialLayout;
import github.nyeptos.packingcube.gui.skin.GuiSurface;
import github.nyeptos.packingcube.gui.skin.NineSliceAxis;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class GuiSurfaceGameTests {
    private static InputStream resource(String path) {
        return Objects.requireNonNull(
                GuiSurfaceGameTests.class.getResourceAsStream("/assets/packingcube/" + path), path);
    }

    @GameTest(template = "empty")
    public static void artistBackgroundBytesAndWhiteTextContractArePreserved(GameTestHelper h)
            throws Exception {
        try (var reader =
                new InputStreamReader(resource("gui_surfaces.json"), StandardCharsets.UTF_8)) {
            var entries = JsonParser.parseReader(reader).getAsJsonArray();
            h.assertTrue(
                    entries.size() == 3 && GuiSurface.values().length == 3,
                    "All three background files present");
            for (var entry : entries) {
                var asset = entry.getAsJsonObject();
                GuiSurface surface = null;
                for (var candidate : GuiSurface.values())
                    if (candidate.path.equals(asset.get("path").getAsString())) surface = candidate;
                h.assertTrue(surface != null, "Background has an actual render catalog entry");
                byte[] bytes;
                try (var input = resource(surface.path)) {
                    bytes = input.readAllBytes();
                }
                h.assertTrue(
                        HexFormat.of()
                                .formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
                                .equals(asset.get("sha256").getAsString()),
                        "Artist's original background PNG is unmodified");
                try (var png = new DataInputStream(new ByteArrayInputStream(bytes))) {
                    h.assertTrue(
                            png.readLong() == 0x89504e470d0a1a0aL
                                    && png.readInt() == 13
                                    && png.readInt() == 0x49484452,
                            "PNG IHDR valid");
                    h.assertTrue(
                            png.readInt() == surface.width && png.readInt() == surface.height,
                            "Source dimensions match renderer");
                }
            }
        }
        h.assertTrue(
                GuiSurface.TEXT_COLOR == 0xffffff,
                "White text is the shared default in every state");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void inventoryStaysNativeAndPanelBordersNeverStretch(GameTestHelper h) {
        h.assertTrue(
                GuiSurface.BAG_INVENTORY.width == 176
                        && GuiSurface.BAG_INVENTORY.height == 132
                        && GuiSurface.BAG_INVENTORY.border == 0,
                "Inventory image is used at original geometry");
        for (int width : new int[] {124, 244, 640})
            for (int height : new int[] {80, 132, 373, 540}) {
                var horizontal =
                        new NineSliceAxis(
                                GuiSurface.SETTINGS_PANEL.width,
                                width,
                                GuiSurface.SETTINGS_PANEL.border);
                var vertical =
                        new NineSliceAxis(
                                GuiSurface.SETTINGS_PANEL.height,
                                height,
                                GuiSurface.SETTINGS_PANEL.border);
                h.assertTrue(
                        horizontal.targetLength(0) == 4
                                && horizontal.targetLength(2) == 4
                                && vertical.targetLength(0) == 4
                                && vertical.targetLength(2) == 4,
                        "Gold frame corners keep four pixels");
                h.assertTrue(
                        horizontal.targetStart(2) + horizontal.targetLength(2) == width
                                && vertical.targetStart(2) + vertical.targetLength(2) == height,
                        "Panel covers the resized window");
            }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialBackgroundContainsSlotsAndKeepsManagementBelow(GameTestHelper h) {
        for (int width : new int[] {320, 400, 640, 960})
            for (int height : new int[] {240, 300, 540, 1080}) {
                var layout = RadialLayout.fit(width, height);
                h.assertTrue(
                        layout.left() >= 0
                                && layout.left() + layout.side() <= width
                                && layout.top() >= 0
                                && layout.top() + layout.side() <= height,
                        "Background stays on screen");
                h.assertTrue(
                        layout.manageY() >= layout.top() + layout.side() + 4
                                && layout.manageY() + 18 <= height
                                && layout.manageX() >= 0
                                && layout.manageX() + 130 <= width,
                        "Management button does not cover the artist's panel frame");
                h.assertTrue(
                        layout.titleY() + 9 <= layout.centerY() - layout.radius() - 18,
                        "Title does not cover the upper bag slot");
                h.assertTrue(
                        layout.radius() + 18 < layout.side() / 2,
                        "36px bag slots fit inside the backdrop");
            }
        h.succeed();
    }
}
