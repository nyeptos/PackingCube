package github.nyeptos.packingcube.gametest.bag;

import github.nyeptos.packingcube.PackingCube;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** The table only offers the station; every bag is one of the station's own bills. */
@GameTestHolder(PackingCube.MODID)
@PrefixGameTestTemplate(false)
public final class RecipeGameTests {
    private static boolean exists(GameTestHelper h, String path) {
        var id = ResourceLocation.fromNamespaceAndPath(PackingCube.MODID, path);
        return h.getLevel()
                .getRecipeManager()
                .byKey(id)
                .isPresent();
    }

    @GameTest(template = "empty")
    public static void theStationIsTheOnlyTableRecipe(GameTestHelper h) {
        h.assertTrue(exists(h, "pouch_station"), "Missing recipe: pouch_station");
        h.assertFalse(exists(h, "backpack"), "the table still makes a backpack");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theStationBillsCoverEveryBag(GameTestHelper h) {
        for (String path :
                List.of(
                        "pouch/storage_pouch",
                        "pouch/refill_pouch",
                        "pouch/bulk_pouch",
                        "pouch/backpack"))
            h.assertTrue(exists(h, path), "Missing bill: " + path);
        h.assertFalse(exists(h, "storage_pouch"), "the table still makes a storage pouch");
        h.assertFalse(exists(h, "refill_pouch"), "the table still makes a refill pouch");
        h.assertFalse(exists(h, "bulk_pouch"), "the table still makes a bulk pouch");
        h.succeed();
    }
}
