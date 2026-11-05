package github.nyeptos.packingcube.gametest.gui;

import com.google.gson.JsonParser;

import github.nyeptos.packingcube.Packingcube;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class SampleSlotGameTests {
    @GameTest(template = "empty")
    public static void hoverDescriptionIsGoneAndTheShortHelpStays(GameTestHelper h) throws Exception {
        for (String lang : new String[] {"en_us", "ja_jp"}) {
            try (var reader =
                    new InputStreamReader(
                            Objects.requireNonNull(
                                    SampleSlotGameTests.class.getResourceAsStream(
                                            "/assets/packingcube/lang/" + lang + ".json")),
                            StandardCharsets.UTF_8)) {
                var json = JsonParser.parseReader(reader).getAsJsonObject();
                h.assertTrue(
                        !json.has("gui.packingcube.ghost_help"),
                        "Sample hover description removed for " + lang);
                h.assertTrue(
                        json.has("gui.packingcube.sample_short_help"),
                        "The one-line sample help stays for " + lang);
            }
        }
        h.succeed();
    }
}
