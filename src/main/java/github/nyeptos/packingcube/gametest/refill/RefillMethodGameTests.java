package github.nyeptos.packingcube.gametest.refill;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.bag.refill.RefillPouchSettings;
import github.nyeptos.packingcube.bag.refill.RefillSlotDestinations;
import github.nyeptos.packingcube.bag.refill.RefillTargets;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Unified "refill up to N when at most M remain" rule and its migration from PC-45 timings. */
@GameTestHolder(PackingCube.MODID)
@PrefixGameTestTemplate(false)
public final class RefillMethodGameTests {
    private static final String KEY = "packingcube_refill";
    private static final String SOURCES = "packingcube_refill_sources";

    private static ItemStack pouch() {
        return ModItems.REFILL_POUCH.get().getDefaultInstance();
    }

    private static void write(
            ItemStack pouch,
            String key,
            java.util.function.Consumer<net.minecraft.nbt.CompoundTag> edit) {
        var data = pouch.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var tag = data.getCompound(key);
        edit.accept(tag);
        data.put(key, tag);
        pouch.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    private static ItemStack legacy(String timing, int target) {
        var pouch = pouch();
        write(
                pouch,
                KEY,
                tag -> {
                    tag.putString("timing", timing);
                    if (target > 0) tag.putInt("target", target);
                });
        return pouch;
    }

    @GameTest(template = "empty")
    public static void legacyTimingsMigrateToThresholds(GameTestHelper h) {
        var fresh = pouch();
        h.assertTrue(
                RefillPouchSettings.threshold(fresh) == 0
                        && RefillPouchSettings.target(fresh) == RefillPouchSettings.MAX_TARGET,
                "A new pouch waits for an empty hand and refills a full stack");
        h.assertTrue(
                RefillPouchSettings.threshold(legacy("empty", 0)) == 0,
                "Empty timing becomes threshold 0");
        h.assertTrue(
                RefillPouchSettings.threshold(legacy("threshold", 0)) == 16,
                "Threshold timing keeps the stored count");
        var use = legacy("use", 32);
        h.assertTrue(
                RefillPouchSettings.threshold(use) == 31 && RefillPouchSettings.target(use) == 32,
                "Use timing becomes one below the target");
        h.assertTrue(RefillPouchSettings.set(use, "target", "8"), "Target stays editable");
        h.assertTrue(
                RefillPouchSettings.threshold(use) == 7 && !RefillPouchSettings.set(use, "timing", "use"),
                "Writing migrates the legacy value and drops the old field");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void targetAndThresholdStayOrdered(GameTestHelper h) {
        var pouch = pouch();
        h.assertTrue(
                RefillPouchSettings.set(pouch, "target", "16")
                        && RefillPouchSettings.set(pouch, "threshold", "12"),
                "Both sliders save");
        h.assertTrue(
                RefillPouchSettings.target(pouch) == 16 && RefillPouchSettings.threshold(pouch) == 12,
                "Chosen counts are kept");
        h.assertTrue(
                RefillPouchSettings.set(pouch, "target", "5")
                        && RefillPouchSettings.threshold(pouch) == 4,
                "Lowering the target lowers the threshold");
        h.assertTrue(
                RefillPouchSettings.set(pouch, "threshold", "63")
                        && RefillPouchSettings.threshold(pouch) == 4,
                "The threshold can never reach the target");
        h.assertTrue(
                !RefillPouchSettings.set(pouch, "target", "0")
                        && !RefillPouchSettings.set(pouch, "target", "65")
                        && !RefillPouchSettings.set(pouch, "threshold", "64"),
                "Out-of-range slider values are rejected");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void desiredFollowsTheUnifiedRule(GameTestHelper h) {
        var pouch = pouch();
        RefillPouchSettings.set(pouch, "target", "16");
        RefillPouchSettings.set(pouch, "threshold", "4");
        h.assertTrue(
                RefillPouchSettings.desired(pouch, 5, 64) == 5
                        && RefillPouchSettings.desired(pouch, 4, 64) == 16
                        && RefillPouchSettings.desired(pouch, 0, 64) == 16,
                "Refills only at or below the threshold");
        h.assertTrue(
                RefillPouchSettings.desired(pouch, 0, 8) == 8,
                "The item's own stack limit still wins");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void slotRulesUseTheSameMigrationAndClamp(GameTestHelper h) {
        var pouch = pouch();
        RefillSlotDestinations.set(pouch, 0, "destination", RefillTargets.inventory(0));
        RefillSlotDestinations.set(pouch, 0, "target", "3");
        h.assertTrue(
                RefillSlotDestinations.get(pouch, 0).threshold() == 0,
                "A new slot rule waits for an empty destination");
        RefillSlotDestinations.set(pouch, 0, "threshold", "2");
        h.assertTrue(
                RefillSlotDestinations.get(pouch, 0).threshold() == 2
                        && RefillSlotDestinations.get(pouch, 0).desired(3, 64) == 3,
                "Above the threshold the destination is left alone");
        RefillSlotDestinations.set(pouch, 0, "target", "5");
        h.assertTrue(
                RefillSlotDestinations.get(pouch, 0).desired(2, 64) == 5,
                "At the threshold the slot tops up to its target");
        RefillSlotDestinations.set(pouch, 0, "target", "1");
        h.assertTrue(
                RefillSlotDestinations.get(pouch, 0).threshold() == 0,
                "Lowering the slot target lowers its threshold");
        h.assertTrue(
                !RefillSlotDestinations.set(pouch, 0, "target", "0")
                        && !RefillSlotDestinations.set(pouch, 0, "timing", "use"),
                "Zero targets and the removed timing field are rejected");

        var saved = pouch();
        write(
                saved,
                SOURCES,
                sources -> {
                    var slot = sources.getCompound("0");
                    slot.putString("timing", "use");
                    slot.putInt("target", 5);
                    sources.put("0", slot);
                });
        h.assertTrue(
                RefillSlotDestinations.get(saved, 0).threshold() == 4,
                "Legacy per-slot use timing becomes one below its target");
        h.succeed();
    }
}
