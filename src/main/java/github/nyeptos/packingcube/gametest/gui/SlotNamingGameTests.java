package github.nyeptos.packingcube.gametest.gui;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.client.widget.RefillTargetPanel;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(PackingCube.MODID)
@PrefixGameTestTemplate(false)
public final class SlotNamingGameTests {
    @GameTest(template = "empty")
    public static void onlyRepeatedCuriosTypesKeepTheirNumber(GameTestHelper h) {
        var single = List.of("curios:ring:0");
        h.assertTrue(
                !RefillTargetPanel.needsIndex("curios:ring:0", single),
                "A single ring slot drops the number");
        var pair = List.of("curios:ring:0", "curios:ring:1");
        h.assertTrue(
                RefillTargetPanel.needsIndex("curios:ring:1", pair),
                "Two ring slots keep their numbers");
        var mixed = List.of("curios:ring:0", "curios:belt:0");
        h.assertTrue(
                !RefillTargetPanel.needsIndex("curios:belt:0", mixed),
                "Different types with one slot each stay unnumbered");
        var equipment = List.of("inventory:36", "inventory:39", "inventory:40");
        h.assertTrue(
                !RefillTargetPanel.needsIndex("inventory:36", equipment),
                "Equipment and offhand slots are never numbered");
        h.succeed();
    }
}
