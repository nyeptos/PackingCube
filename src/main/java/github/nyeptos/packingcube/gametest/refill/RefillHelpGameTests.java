package github.nyeptos.packingcube.gametest.refill;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.bag.refill.RefillSlotSettings;
import github.nyeptos.packingcube.gui.input.RefillSettingsView;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Help mode explains the control under the cursor, never the whole page at once. */
@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class RefillHelpGameTests {
    private static boolean explained(RefillSettingsView view, ItemStack pouch, int row) {
        for (var helpRow : view.helpRows(pouch)) if (helpRow.row() == row) return true;
        return false;
    }

    @GameTest(template = "empty")
    public static void destinationRowsExplainTheirOwnButton(GameTestHelper h) {
        var pouch = ModItems.REFILL_POUCH.get().getDefaultInstance();
        var view = new RefillSettingsView();
        for (int row : new int[] {0, 1, 2, 3, 6, 7, 8})
            h.assertTrue(explained(view, pouch, row), "Row " + row + " explains its own control");
        h.assertTrue(!explained(view, pouch, 4), "A plain label row has no explanation");
        h.assertTrue(!explained(view, pouch, 5), "A spacer row has no explanation");
        h.assertTrue(!explained(view, pouch, 9), "Hidden toggles are not explained");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void replacementTogglesOnlyExistWhileReplaceIsOn(GameTestHelper h) {
        var pouch = ModItems.REFILL_POUCH.get().getDefaultInstance();
        var view = new RefillSettingsView();
        h.assertTrue(
                view.priorityRow(pouch) == 8
                        && !explained(view, pouch, 9)
                        && !explained(view, pouch, 10),
                "Replace OFF keeps priority at row 8 with no replacement rows");
        RefillSlotSettings.set(pouch, 0, "replace", "true");
        h.assertTrue(
                view.priorityRow(pouch) == 10
                        && explained(view, pouch, 8)
                        && explained(view, pouch, 9)
                        && explained(view, pouch, 10),
                "Replace ON explains collect, lock and priority without gaps");
        h.assertTrue(view.rows() == 11, "The destination page keeps a fixed row count");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void methodAndFilterRowsExplainTheirOwnButton(GameTestHelper h) {
        var pouch = ModItems.REFILL_POUCH.get().getDefaultInstance();
        var view = new RefillSettingsView();
        view.page(RefillSettingsView.METHOD);
        h.assertTrue(
                explained(view, pouch, 3)
                        && explained(view, pouch, 4)
                        && explained(view, pouch, 7)
                        && !explained(view, pouch, 5),
                "Method explains both sliders and the durability switch");
        RefillSlotSettings.set(pouch, 0, "durabilityMode", "percent");
        h.assertTrue(explained(view, pouch, 5), "An enabled limit explains its own slider");
        view.page(RefillSettingsView.FILTER);
        h.assertTrue(
                explained(view, pouch, 2)
                        && explained(view, pouch, 3)
                        && explained(view, pouch, 4)
                        && !explained(view, pouch, 5),
                "Shared explains its switches and the pouch-wide pause mode");
        h.succeed();
    }
}
