package github.nyeptos.packingcube.gametest.refill;

import com.google.gson.JsonParser;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.bag.refill.RefillDiagnostics;
import github.nyeptos.packingcube.bag.refill.RefillSettings;
import github.nyeptos.packingcube.gametest.support.LegacyRefillFixture;
import github.nyeptos.packingcube.gui.input.RefillSettingsView;
import github.nyeptos.packingcube.gui.layout.BagSettingsLayout;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStreamReader;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class RefillClarityGameTests {
    @GameTest(template = "empty")
    public static void pagesOnlyShowRelevantSettingsAndDoNotChangeItem(GameTestHelper h) {
        var bag = ModItems.REFILL_POUCH.get().getDefaultInstance();
        var saved = bag.copy();
        var view = new RefillSettingsView();
        h.assertTrue(
                view.page() == RefillSettingsView.DESTINATIONS && !view.editorVisible(bag),
                "First page is destinations without a filter editor");
        view.page(RefillSettingsView.METHOD);
        h.assertTrue(!view.priorityExpanded(), "Advanced priority hidden by default");
        view.togglePriority();
        view.page(RefillSettingsView.FILTER);
        h.assertTrue(
                !view.editorVisible(bag) && ItemStack.matches(saved, bag),
                "Navigation does not mutate configuration");
        RefillSettings.set(bag, "filter", "true");
        h.assertTrue(view.editorVisible(bag), "Only enabled filter page exposes shared editor");
        view.page(RefillSettingsView.METHOD);
        RefillSettings.set(bag, "target", "16");
        h.assertTrue(
                RefillSettings.target(bag) == 16 && RefillSettings.threshold(bag) <= 15,
                "Counts stay within the item-independent slider range");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void shortPagesDoNotReserveHiddenEditorSpace(GameTestHelper h) {
        var bag = ModItems.REFILL_POUCH.get().getDefaultInstance();
        var view = new RefillSettingsView();
        var layout = BagSettingsLayout.floating(124, 80, view.rows());
        h.assertTrue(
                view.contentHeight(layout, bag) < layout.content().height(),
                "Destination page has no blank filter-editor scroll area");
        view.page(RefillSettingsView.FILTER);
        layout = BagSettingsLayout.floating(124, 80, view.rows());
        h.assertTrue(
                view.rows() == 6 && view.contentHeight(layout, bag) == 148,
                "Filter OFF has a compact help page");
        RefillSettings.set(bag, "filter", "true");
        h.assertTrue(
                view.contentHeight(layout, bag) == layout.content().height()
                        && layout.modeY() >= layout.optionRow(5).bottom(),
                "Enabled filter reserves complete editor below its help");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void diagnosticsAreReadOnlyAndDistinguishMissingSupplyFiltersAndCreative(
            GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        p.getInventory().selected = 0;
        h.assertTrue(
                RefillDiagnostics.reason(p, 0).equals("pouch"),
                "Missing installed/enabled pouch explained");
        var supply = LegacyRefillFixture.pouch();
        new BagInventory(supply, false, () -> {}).setItem(0, new ItemStack(Items.STONE, 8));
        var root = ModItems.LARGE_BAG.get().getDefaultInstance();
        new BagInventory(root, true, () -> {}).setItem(0, supply);
        p.getInventory().setItem(8, root);
        h.assertTrue(
                RefillDiagnostics.reason(p, 0).equals("empty"),
                "Empty hand cannot identify a template outside a use event");
        p.getInventory().setItem(0, new ItemStack(Items.DIRT));
        h.assertTrue(RefillDiagnostics.reason(p, 0).equals("stock"), "No matching stock explained");
        p.getInventory().setItem(0, new ItemStack(Items.STONE));
        var before = root.copy();
        h.assertTrue(
                RefillDiagnostics.reason(p, 0).equals("use") && ItemStack.matches(before, root),
                "Diagnostic does not refill, consume or save anything");
        var modules = new BagInventory(root, true, () -> {});
        RefillSettings.set(modules.getItem(0), "filter", "true");
        BagFilter.add(modules.getItem(0), "items", ResourceLocation.parse("minecraft:stone"));
        modules.setChanged();
        h.assertTrue(
                RefillDiagnostics.reason(p, 0).equals("filter"),
                "Excluded stock distinguished from missing stock");
        var creative = h.makeMockPlayer(GameType.CREATIVE);
        h.assertTrue(
                RefillDiagnostics.reason(creative, 0).equals("creative"),
                "Creative mode limitation explicit");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void japaneseTerminologyAndNumberLabelsAreExplicit(GameTestHelper h)
            throws Exception {
        try (var input =
                new InputStreamReader(
                        RefillClarityGameTests.class.getResourceAsStream(
                                "/assets/packingcube/lang/ja_jp.json"))) {
            var json = JsonParser.parseReader(input).getAsJsonObject();
            for (var entry : json.entrySet())
                h.assertTrue(
                        !entry.getValue().getAsString().contains("搬出フィルター"),
                        "Player-facing name consistently refill filter");
            for (var entry : json.entrySet())
                h.assertTrue(
                        !entry.getValue().getAsString().contains("旧品"),
                        "Collected items are not called 旧品");
            h.assertTrue(
                    json.get("gui.packingcube.refill_filter_heading")
                            .getAsString()
                            .equals("補充フィルター"),
                    "Header matches toggle name");
            h.assertTrue(
                    json.get("gui.packingcube.refill_threshold_label")
                                    .getAsString()
                                    .contains("残り個数")
                            && json.get("gui.packingcube.refill_target_label")
                                    .getAsString()
                                    .contains("補充後"),
                    "Different counts have explicit purposes");
            h.assertTrue(
                    json.get("gui.packingcube.refill_help_name_target")
                            .getAsString()
                            .equals("補充後の個数"),
                    "Help names the final count explicitly");
            h.assertTrue(
                    json.get("gui.packingcube.refill_help_body_threshold")
                            .getAsString()
                            .contains("空になったとき"),
                    "The empty-hand case is explained in the help line");
            for (var key :
                    new String[] {
                        "source", "pages", "enabled", "select", "clear", "replace", "collect",
                        "lock", "priority", "target", "threshold", "filter", "trade"
                    })
                h.assertTrue(
                        !json.get("gui.packingcube.refill_help_name_" + key).getAsString().isEmpty()
                                && !json.get("gui.packingcube.refill_help_body_" + key)
                                        .getAsString()
                                        .isEmpty(),
                        "Each control has its own explanation: " + key);
        }
        h.succeed();
    }
}
