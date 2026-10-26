package github.nyeptos.packingcube.gametest.gui;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.gui.layout.BagSettingsLayout;
import github.nyeptos.packingcube.gui.input.FilterEditorState;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class SettingsLayoutGameTests {
    private static ItemStack settings(int size) {
        // A different item type proves that the state projection is not tied to a filter-pouch
        // item.
        ItemStack settings = new ItemStack(Items.PAPER);
        for (int i = 0; i < size; i++)
            BagFilter.add(
                    settings, "items", ResourceLocation.fromNamespaceAndPath("test", "item_" + i));
        return settings;
    }

    private static void check(boolean condition, GameTestHelper h, String message) {
        h.assertTrue(condition, message);
    }

    @GameTest(template = "empty")
    public static void leftInventoryAndRightSettingsAreSeparated(GameTestHelper h) {
        var layout = new BagSettingsLayout(420, 260, 2);
        check(
                layout.inventory().equals(new BagSettingsLayout.Rect(0, 0, 176, 132)),
                h,
                "Inventory unchanged");
        check(
                layout.optionRow(0).x() >= 180
                        && layout.optionRow(1).x() >= 180
                        && layout.sampleSlot().x() >= 180,
                h,
                "Every setting including sample is on right");
        check(
                layout.optionRow(1).bottom() <= layout.modeY()
                        && layout.modeY() + 20 <= layout.sampleLabelY()
                        && layout.sampleHelpY() + 18 <= layout.addItemY(),
                h,
                "Logical sections do not overlap");
        check(
                layout.content().right() <= layout.viewport().right() - 10,
                h,
                "Scrollbar gutter reserved");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void differentInventoryAndOptionCountsDoNotOverlap(GameTestHelper h) {
        for (int inventoryHeight : new int[] {0, 132, 186, 222}) {
            for (int options : new int[] {0, 1, 2, 4, 16}) {
                for (int width : new int[] {300, 312, 420}) {
                    int height = BagSettingsLayout.preferredHeight(240, inventoryHeight, options);
                    var layout = new BagSettingsLayout(width, height, inventoryHeight, options);
                    for (int row = 0; row < options; row++) {
                        var bounds = layout.optionRow(row);
                        check(
                                bounds.x() >= layout.inventory().right()
                                        && bounds.right() <= layout.content().right(),
                                h,
                                "All options on right");
                        check(bounds.bottom() <= layout.modeY(), h, "Options do not overlap mode");
                        if (row > 0)
                            check(
                                    layout.optionRow(row - 1).bottom() <= bounds.y(),
                                    h,
                                    "Option rows separated");
                    }
                    check(
                            layout.ruleRowY(layout.rulePageSize() - 1) + 18 <= layout.pagerY(),
                            h,
                            "Rules do not overlap pager");
                    check(
                            layout.pagerY() + 18 <= layout.contentBottom(),
                            h,
                            "Pager fits logical canvas");
                    check(
                            height <= 232 || inventoryHeight > 232,
                            h,
                            "Extra settings scroll rather than enlarge viewport");
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void sampleHitRegionHasStableExclusiveEdges(GameTestHelper h) {
        var slot = new BagSettingsLayout(312, 232, 2).sampleSlot();
        check(
                slot.contains(slot.x(), slot.y())
                        && slot.contains(slot.right() - 0.01, slot.bottom() - 0.01),
                h,
                "Inside accepted");
        check(
                !slot.contains(slot.right(), slot.y())
                        && !slot.contains(slot.x(), slot.bottom())
                        && !slot.contains(slot.x() - 0.01, slot.y()),
                h,
                "Outer edges rejected");
        check(
                !slot.contains(8, slot.y()),
                h,
                "Former left-side location never accepts sample input");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void badLayoutSizesAreRejected(GameTestHelper h) {
        boolean tooNarrow = false, tooShort = false, badRows = false, badIndex = false;
        try {
            new BagSettingsLayout(239, 260, 2);
        } catch (IllegalArgumentException expected) {
            tooNarrow = true;
        }
        try {
            new BagSettingsLayout(420, 47, 2);
        } catch (IllegalArgumentException expected) {
            tooShort = true;
        }
        try {
            BagSettingsLayout.minimumHeight(-1);
        } catch (IllegalArgumentException expected) {
            badRows = true;
        }
        try {
            new BagSettingsLayout(420, 260, 0).optionRow(0);
        } catch (IndexOutOfBoundsException expected) {
            badIndex = true;
        }
        check(tooNarrow && tooShort && badRows && badIndex, h, "Invalid geometry rejected");
        check(
                BagSettingsLayout.preferredWidth(320) == 312
                        && BagSettingsLayout.preferredHeight(240, 2) == 232,
                h,
                "Small GUI viewport fits");
        check(
                BagSettingsLayout.preferredHeight(540, 2) == 373,
                h,
                "Tall screen shows complete settings form");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rightSettingsExpandWithoutStretchingInventory(GameTestHelper h) {
        for (int screenHeight : new int[] {140, 240, 540, 1080}) {
            var layout =
                    new BagSettingsLayout(
                            420, BagSettingsLayout.preferredHeight(screenHeight, 2), 2);
            int expectedHeight = Math.max(132, Math.min(373, screenHeight - 8));
            check(
                    layout.height() == expectedHeight && layout.inventory().height() == 132,
                    h,
                    "Only right settings expands with screen space");
            var close = layout.closeButton();
            check(
                    close.x() >= 0
                            && close.y() >= 0
                            && close.right() <= layout.inventory().right()
                            && close.bottom() <= 17,
                    h,
                    "Close remains inside inventory header above slots");
            var background = layout.settingsBackground();
            check(
                    background.x() == 176 && background.height() == expectedHeight,
                    h,
                    "Tall backdrop still excludes left inventory");
            check(
                    layout.viewport().bottom() <= layout.height(),
                    h,
                    "Viewport fits settings height");
            var scroll = new github.nyeptos.packingcube.gui.input.SettingsScrollState();
            scroll.configure(layout.viewport().height(), layout.content().height());
            check(
                    scroll.scrollable() == (screenHeight < 381),
                    h,
                    "Overflow scrolls on small screens; complete form fits on tall screens");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void sharedFilterStateSupportsNonPouchSettingsAndKeepsInput(GameTestHelper h) {
        ItemStack settings = settings(20);
        var state = new FilterEditorState();
        state.setRegistered(true);
        state.setPageSize(3);
        state.setQuery("ITEM_");
        state.refresh(settings, ItemStack.EMPTY);
        state.next();
        state.next();
        ItemStack before = settings.copy();
        check(
                state.page() == 2 && state.visibleRules().size() == 3,
                h,
                "Case insensitive query and paging");
        BagFilter.setExcluding(settings, true);
        state.refresh(settings, ItemStack.EMPTY);
        check(
                state.excluding()
                        && state.page() == 2
                        && state.registered()
                        && state.query().equals("ITEM_"),
                h,
                "Mode sync never resets local input");
        ItemStack afterMode = settings.copy();
        state.refresh(settings, ItemStack.EMPTY);
        check(
                ItemStack.matches(settings, afterMode) && !ItemStack.matches(before, afterMode),
                h,
                "Projection does not rewrite settings");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void sharedFilterStateClampsPagesAfterRemovalAndResize(GameTestHelper h) {
        ItemStack settings = settings(12);
        var state = new FilterEditorState();
        state.setRegistered(true);
        state.setPageSize(2);
        state.refresh(settings, ItemStack.EMPTY);
        for (int i = 0; i < 20; i++) state.next();
        check(state.page() == 5 && !state.hasNext(), h, "No page overflow");
        state.setPageSize(5);
        check(
                state.page() == 2 && state.visibleRules().size() == 2,
                h,
                "Rebuild at a new size remains safe");
        BagFilter.clear(settings);
        state.refresh(settings, ItemStack.EMPTY);
        check(
                state.page() == 0
                        && state.pageCount() == 1
                        && state.empty()
                        && state.visibleRules().isEmpty(),
                h,
                "Removing all rules clamps to empty page");
        state.previous();
        check(state.page() == 0, h, "No negative page");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void filterStateInstancesDoNotLeakBetweenBags(GameTestHelper h) {
        ItemStack first = settings(5), second = settings(1);
        var a = new FilterEditorState();
        var b = new FilterEditorState();
        a.setRegistered(true);
        a.setQuery("item_4");
        a.refresh(first, ItemStack.EMPTY);
        b.refresh(second, ItemStack.EMPTY);
        check(
                a.visibleRules().size() == 1 && !b.registered() && b.query().isEmpty() && b.empty(),
                h,
                "Each screen owns local state");
        check(
                BagFilter.entries(second, "items").size() == 1,
                h,
                "One editor never changes another bag");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void candidateTagsUseSampleAndNeverConsumeIt(GameTestHelper h) {
        ItemStack sample = new ItemStack(Items.OAK_LOG, 17);
        var state = new FilterEditorState();
        state.setPageSize(256);
        state.refresh(settings(2), sample);
        check(
                !state.empty()
                        && state.visibleRules().stream()
                                .allMatch(rule -> rule.field().equals("tags")),
                h,
                "Sample tag candidates are generic");
        check(
                state.visibleRules().stream()
                        .anyMatch(rule -> rule.value().equals("minecraft:logs")),
                h,
                "Log tag offered");
        check(sample.getCount() == 17, h, "Read-only sample projection never consumes items");
        h.succeed();
    }
}
