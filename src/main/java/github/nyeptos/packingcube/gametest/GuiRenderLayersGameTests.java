package github.nyeptos.packingcube.gametest;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.gui.GuiRenderLayers;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class GuiRenderLayersGameTests {
    @GameTest(template = "empty")
    public static void inventoryWindowCarriedAndTooltipLayersStayOrdered(GameTestHelper h) {
        int windowTop = GuiRenderLayers.WINDOW + GuiRenderLayers.DECORATION;
        int cursorItem =
                GuiRenderLayers.CARRIED_LIFT
                        + GuiRenderLayers.VANILLA_CARRIED
                        + GuiRenderLayers.ITEM;
        int cursorCount =
                GuiRenderLayers.CARRIED_LIFT
                        + GuiRenderLayers.VANILLA_CARRIED
                        + GuiRenderLayers.DECORATION;
        int tooltip = GuiRenderLayers.TOOLTIP_LIFT + GuiRenderLayers.VANILLA_TOOLTIP;
        h.assertTrue(
                GuiRenderLayers.WINDOW > GuiRenderLayers.DECORATION,
                "Window covers inventory items and text");
        h.assertTrue(
                cursorItem > windowTop,
                "Carried item itself, not just its count, covers all window content");
        h.assertTrue(
                tooltip > cursorItem && tooltip > cursorCount && tooltip > windowTop,
                "Popup background and text cover window items, widgets, and carried decorations");
        h.succeed();
    }
}
