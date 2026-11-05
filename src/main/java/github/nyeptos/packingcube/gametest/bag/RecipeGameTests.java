package github.nyeptos.packingcube.gametest.bag;

import github.nyeptos.packingcube.Packingcube;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Every bag has a crafting recipe the recipe book can offer. */
@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class RecipeGameTests {
    private static boolean exists(GameTestHelper h, String path) {
        var id = ResourceLocation.fromNamespaceAndPath(Packingcube.MODID, path);
        return h.getLevel()
                .getRecipeManager()
                .byKey(id)
                .isPresent();
    }

    @GameTest(template = "empty")
    public static void everyBagIsCraftable(GameTestHelper h) {
        for (String path : List.of("storage_pouch", "refill_pouch", "large_bag"))
            h.assertTrue(exists(h, path), "Missing recipe: " + path);
        h.succeed();
    }
}
