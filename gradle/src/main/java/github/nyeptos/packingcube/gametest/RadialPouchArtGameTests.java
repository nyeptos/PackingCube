package github.nyeptos.packingcube.gametest;

import com.google.gson.JsonParser;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.gui.RadialSlotSkin;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class RadialPouchArtGameTests {
    private static InputStream resource(String path) {
        return Objects.requireNonNull(
                RadialPouchArtGameTests.class.getResourceAsStream("/assets/packingcube/" + path),
                path);
    }

    @GameTest(template = "empty")
    public static void originalPouchAndFrameImagesArePreserved(GameTestHelper h) throws Exception {
        try (var reader =
                new InputStreamReader(
                        resource("radial_pouch_assets.json"), StandardCharsets.UTF_8)) {
            var entries = JsonParser.parseReader(reader).getAsJsonArray();
            h.assertTrue(entries.size() == 4, "Four original images present");
            for (var entry : entries) {
                var asset = entry.getAsJsonObject();
                byte[] bytes;
                try (var input = resource(asset.get("path").getAsString())) {
                    bytes = input.readAllBytes();
                }
                h.assertTrue(
                        HexFormat.of()
                                .formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
                                .equals(asset.get("sha256").getAsString()),
                        "Artist PNG bytes unchanged");
                try (var png = new DataInputStream(new ByteArrayInputStream(bytes))) {
                    h.assertTrue(
                            png.readLong() == 0x89504e470d0a1a0aL
                                    && png.readInt() == 13
                                    && png.readInt() == 0x49484452,
                            "PNG IHDR valid");
                    h.assertTrue(
                            png.readInt() == asset.get("width").getAsInt()
                                    && png.readInt() == asset.get("height").getAsInt(),
                            "Native image dimensions match");
                }
            }
            for (var skin : RadialSlotSkin.values()) {
                boolean found = false;
                for (var entry : entries) {
                    var asset = entry.getAsJsonObject();
                    if (skin.path.equals(asset.get("path").getAsString()))
                        found =
                                asset.get("width").getAsInt() == RadialSlotSkin.SIZE
                                        && asset.get("height").getAsInt() == RadialSlotSkin.SIZE;
                }
                h.assertTrue(found, "Every rendered frame references a supplied 36px PNG");
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyHoverAndPressedStatesUseSharedFrames(GameTestHelper h) {
        for (boolean hovered : new boolean[] {false, true})
            for (boolean pressed : new boolean[] {false, true}) {
                h.assertTrue(
                        RadialSlotSkin.select(false, hovered, pressed) == RadialSlotSkin.NORMAL,
                        "Empty frame always shares normal artwork");
                h.assertTrue(
                        RadialSlotSkin.select(true, hovered, pressed)
                                == (pressed
                                        ? RadialSlotSkin.PRESSED
                                        : hovered ? RadialSlotSkin.HOVERED : RadialSlotSkin.NORMAL),
                        "Occupied pressed wins over hover");
            }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void pouchItemModelReferencesArtistTexture(GameTestHelper h) throws Exception {
        try (var reader =
                new InputStreamReader(
                        resource("models/item/filter_bag.json"), StandardCharsets.UTF_8)) {
            var model = JsonParser.parseReader(reader).getAsJsonObject();
            h.assertTrue(
                    model.get("parent").getAsString().equals("minecraft:item/generated"),
                    "Pouch remains a flat item");
            h.assertTrue(
                    model.getAsJsonObject("textures")
                            .get("layer0")
                            .getAsString()
                            .equals("packingcube:item/filter_bag"),
                    "Pouch no longer renders vanilla leather");
        }
        h.succeed();
    }
}
