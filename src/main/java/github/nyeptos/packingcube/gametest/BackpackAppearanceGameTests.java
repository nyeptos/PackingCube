package github.nyeptos.packingcube.gametest;

import com.google.gson.JsonParser;

import github.nyeptos.packingcube.Packingcube;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Resource integrity checks, not a substitute for a rendered client test. */
@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class BackpackAppearanceGameTests {
    private static InputStream resource(String path) {
        return Objects.requireNonNull(
                BackpackAppearanceGameTests.class.getResourceAsStream(
                        "/assets/packingcube/" + path),
                "Missing backpack resource: " + path);
    }

    @GameTest(template = "empty")
    public static void equippedModelAndTextureArePackagedTogether(GameTestHelper h)
            throws Exception {
        try (var reader =
                new InputStreamReader(
                        resource("models/item/backpack_equipped.json"), StandardCharsets.UTF_8)) {
            var model = JsonParser.parseReader(reader).getAsJsonObject();
            h.assertTrue(
                    model.getAsJsonArray("elements").size() == 7,
                    "Artist's seven elements preserved");
            var size = model.getAsJsonArray("texture_size");
            h.assertTrue(
                    size.get(0).getAsInt() == 32 && size.get(1).getAsInt() == 32,
                    "Model uses 32px UV texture");
            var textures = model.getAsJsonObject("textures");
            h.assertTrue(
                    textures.get("2").getAsString().equals("packingcube:item/backpack")
                            && textures.get("particle")
                                    .getAsString()
                                    .equals("packingcube:item/backpack"),
                    "Both texture references resolve in PackingCube namespace");
            for (var element : model.getAsJsonArray("elements")) {
                var faces = element.getAsJsonObject().getAsJsonObject("faces");
                h.assertTrue(faces.size() == 6, "Every cube has six textured faces");
                for (var face : faces.entrySet()) {
                    h.assertTrue(
                            face.getValue()
                                    .getAsJsonObject()
                                    .get("texture")
                                    .getAsString()
                                    .equals("#2"),
                            "Face resolves to artist texture");
                }
            }
        }
        try (var png = new DataInputStream(resource("textures/item/backpack.png"))) {
            h.assertTrue(png.readLong() == 0x89504e470d0a1a0aL, "Valid PNG signature");
            h.assertTrue(png.readInt() == 13 && png.readInt() == 0x49484452, "PNG IHDR present");
            h.assertTrue(png.readInt() == 32 && png.readInt() == 32, "Texture is 32 by 32 pixels");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void backpackItemReusesEquippedGeometryWithoutDuplicatingIt(GameTestHelper h)
            throws Exception {
        try (var reader =
                new InputStreamReader(
                        resource("models/item/large_bag.json"), StandardCharsets.UTF_8)) {
            var item = JsonParser.parseReader(reader).getAsJsonObject();
            h.assertTrue(
                    item.get("parent").getAsString().equals("packingcube:item/backpack_equipped"),
                    "Item inherits the same equipped model, including its UV/texture");
            h.assertTrue(
                    !item.has("elements") && !item.has("textures"),
                    "Geometry and texture remain single-source");
            h.assertTrue(
                    !item.has("display"),
                    "Blockbench-authored inventory/frame/hand display transforms are not"
                            + " overridden");
        }
        h.succeed();
    }
}
