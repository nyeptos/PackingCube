package github.nyeptos.packingcube.gametest.gui;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.gui.animation.SlotFeedback;

import net.minecraft.Util;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class SlotFeedbackGameTests {
    @GameTest(template = "empty")
    public static void hoverSoundFiresOncePerSlotEntry(GameTestHelper h) {
        var feedback = new SlotFeedback();
        h.assertTrue(feedback.enter("slot:3"), "Entering a slot plays the hover sound");
        h.assertFalse(feedback.enter("slot:3"), "Staying on the same slot stays silent");
        h.assertTrue(feedback.enter("inventory:12"), "Entering another slot plays again");
        h.assertFalse(feedback.enter(null), "Leaving every slot is silent");
        h.assertTrue(feedback.enter("inventory:12"), "Re-entering a slot plays again");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void pressPulsesOnlyThePressedSlot(GameTestHelper h) {
        var feedback = new SlotFeedback();
        long start = Util.getMillis();
        feedback.enter("inventory:0");
        feedback.press("inventory:0");
        feedback.frame("inventory:0", start, false);
        var hovered = feedback.frame("inventory:0", start + 45, false);
        h.assertTrue(hovered.hover() > .6F && hovered.hover() < .7F, "Hover opacity eases in");
        h.assertTrue(hovered.press() > 0, "The pressed slot keeps its pulse");
        var other = feedback.frame("inventory:1", start + 45, false);
        h.assertTrue(
                other.hover() == 0 && other.press() == 0, "Untouched slots draw nothing");
        feedback.clear();
        h.assertTrue(
                feedback.frame("inventory:0", start + 200, false).press() == 0,
                "Clearing drops the press state");
        h.succeed();
    }
}
