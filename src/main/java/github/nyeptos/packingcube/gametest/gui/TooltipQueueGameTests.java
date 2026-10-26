package github.nyeptos.packingcube.gametest.gui;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.gui.skin.TooltipQueue;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class TooltipQueueGameTests {
    @GameTest(template = "empty")
    public static void tooltipPriorityMatchesScreenSemantics(GameTestHelper h) {
        var q = new TooltipQueue<String>();
        q.offer("first", false);
        q.offer("other", false);
        h.assertTrue("first".equals(q.take()), "First ordinary tooltip wins");
        q.offer("first", false);
        q.offer("focused", true);
        q.offer("other", false);
        h.assertTrue("focused".equals(q.take()), "Explicit override replaces pending tooltip");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tooltipDrainsOnceAndClearRemovesStaleRequests(GameTestHelper h) {
        var q = new TooltipQueue<String>();
        q.offer("popup", false);
        h.assertTrue("popup".equals(q.take()) && q.take() == null, "No repeated popup next frame");
        q.offer("stale", false);
        q.clear();
        h.assertTrue(q.take() == null, "Rebuild or closing clears popup");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tooltipQueuesAreHostLocal(GameTestHelper h) {
        var a = new TooltipQueue<String>();
        var b = new TooltipQueue<String>();
        a.offer("a", false);
        b.offer("b", false);
        a.clear();
        h.assertTrue(
                a.take() == null && "b".equals(b.take()),
                "One window cannot replace another popup");
        h.succeed();
    }
}
