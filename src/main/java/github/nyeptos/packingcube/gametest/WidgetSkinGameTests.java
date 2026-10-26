package github.nyeptos.packingcube.gametest;

import com.google.gson.JsonParser;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.gui.NineSliceAxis;
import github.nyeptos.packingcube.gui.WidgetSkin;

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
public final class WidgetSkinGameTests {
    private static InputStream resource(String file) {
        return Objects.requireNonNull(
                WidgetSkinGameTests.class.getResourceAsStream("/assets/packingcube/" + file), file);
    }

    @GameTest(template = "empty")
    public static void artistPngsKeepOriginalBytesDimensionsAndNames(GameTestHelper h)
            throws Exception {
        try (var reader =
                new InputStreamReader(resource("widget_assets.json"), StandardCharsets.UTF_8)) {
            var assets = JsonParser.parseReader(reader).getAsJsonArray();
            h.assertTrue(
                    assets.size() == 20 && WidgetSkin.values().length == 20,
                    "All twenty supplied assets are packaged");
            for (var entry : assets) {
                var item = entry.getAsJsonObject();
                String name = item.get("file").getAsString();
                WidgetSkin skin = null;
                for (var candidate : WidgetSkin.values())
                    if (candidate.fileName().equals(name)) skin = candidate;
                h.assertTrue(skin != null, "Manifest asset is used by the skin catalog");
                byte[] bytes;
                try (var input = resource("textures/gui/widgets/" + name)) {
                    bytes = input.readAllBytes();
                }
                h.assertTrue(
                        HexFormat.of()
                                .formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
                                .equals(item.get("sha256").getAsString()),
                        "Artist's PNG bytes remain unmodified: " + name);
                try (var png = new DataInputStream(new ByteArrayInputStream(bytes))) {
                    h.assertTrue(
                            png.readLong() == 0x89504e470d0a1a0aL
                                    && png.readInt() == 13
                                    && png.readInt() == 0x49484452,
                            "Valid PNG IHDR: " + name);
                    int width = png.readInt(), height = png.readInt();
                    h.assertTrue(
                            width == skin.width
                                    && height == skin.height
                                    && width == item.get("width").getAsInt()
                                    && height == item.get("height").getAsInt(),
                            "Expected texture size: " + name);
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void widgetStatesUseSuppliedVariantsAndExplicitFallbacks(GameTestHelper h) {
        h.assertTrue(
                WidgetSkin.button(true, true, true) == WidgetSkin.BUTTON_SELECTED_HOVERED
                        && WidgetSkin.button(true, true, false) == WidgetSkin.BUTTON_SELECTED,
                "Selected tabs use cyan variants");
        h.assertTrue(
                WidgetSkin.button(false, false, false) == WidgetSkin.BUTTON_NORMAL
                        && WidgetSkin.button(true, false, true)
                                == WidgetSkin.BUTTON_SELECTED_HOVERED,
                "Ordinary hover shares supplied highlighted artwork; disabled keeps normal");
        h.assertTrue(
                WidgetSkin.toggle(true, true, true) == WidgetSkin.TOGGLE_ON_HOVERED
                        && WidgetSkin.toggle(true, false, true) == WidgetSkin.TOGGLE_ON_DISABLED
                        && WidgetSkin.toggle(false, true, false) == WidgetSkin.TOGGLE_OFF_NORMAL
                        && WidgetSkin.toggle(false, false, false) == WidgetSkin.TOGGLE_OFF_DISABLED,
                "Switch preserves on/off while selecting interaction state");
        h.assertTrue(
                WidgetSkin.close(false, true) == WidgetSkin.CLOSE_DISABLED
                        && WidgetSkin.close(true, true) == WidgetSkin.CLOSE_HOVERED
                        && WidgetSkin.search(true, true) == WidgetSkin.SEARCH_FOCUSED
                        && WidgetSkin.search(false, true) == WidgetSkin.SEARCH_DISABLED,
                "Disabled and focused states stay distinct");
        h.assertTrue(
                WidgetSkin.thumb(true, true) == WidgetSkin.SCROLLBAR_THUMB_DRAGGING
                        && WidgetSkin.thumb(false, true) == WidgetSkin.SCROLLBAR_THUMB_HOVERED
                        && WidgetSkin.thumb(false, false) == WidgetSkin.SCROLLBAR_THUMB_NORMAL,
                "Dragging thumb wins over hover");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void nineSliceKeepsCornersAndCoversEveryDestinationPixel(GameTestHelper h) {
        for (int length : new int[] {6, 14, 18, 20, 55, 102, 222, 900}) {
            var axis = new NineSliceAxis(20, length, 3);
            int cursor = 0;
            for (int part = 0; part < 3; part++) {
                h.assertTrue(
                        axis.targetStart(part) == cursor && axis.targetLength(part) >= 0,
                        "No overlaps or gaps");
                h.assertTrue(
                        axis.sourceStart(part) >= 0
                                && axis.sourceStart(part) + axis.sourceLength(part) <= 20,
                        "Sample stays inside source PNG");
                cursor += axis.targetLength(part);
            }
            h.assertTrue(
                    cursor == length && axis.targetLength(0) == 3 && axis.targetLength(2) == 3,
                    "Fixed three-pixel corners, full target covered");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tinySlicesAndVerticalScrollbarCapsRemainSafe(GameTestHelper h) {
        for (int length : new int[] {1, 2, 3, 4, 5}) {
            var axis = new NineSliceAxis(20, length, 3);
            h.assertTrue(
                    axis.targetLength(0) + axis.targetLength(1) + axis.targetLength(2) == length
                            && axis.targetLength(1) >= 0,
                    "Small target compresses borders without negative rectangles");
        }
        var vertical = new NineSliceAxis(12, 100, 2);
        var horizontal = new NineSliceAxis(6, 6, 0);
        h.assertTrue(
                vertical.targetLength(0) == 2
                        && vertical.targetLength(2) == 2
                        && vertical.targetLength(1) == 96
                        && horizontal.targetLength(1) == 6,
                "Scrollbar stretches only its middle and keeps original width");
        h.succeed();
    }
}
