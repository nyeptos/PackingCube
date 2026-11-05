package github.nyeptos.packingcube.data;

import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

/**
 * Crafting costs for the three bags. A pouch is the cheaper entry point, and the backpack asks
 * for pouches so the pouch work carries over.
 */
public final class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(
            PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.STORAGE_POUCH.get())
                .pattern("SLS")
                .pattern("LCL")
                .pattern(" L ")
                .define('S', Items.STRING)
                .define('L', Items.LEATHER)
                .define('C', Items.CHEST)
                .unlockedBy("has_chest", has(Items.CHEST))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.REFILL_POUCH.get())
                .pattern("IHI")
                .pattern(" P ")
                .pattern(" R ")
                .define('I', Items.IRON_INGOT)
                .define('H', Items.HOPPER)
                .define('P', ModItems.STORAGE_POUCH.get())
                .define('R', Items.REDSTONE)
                .unlockedBy("has_storage_pouch", has(ModItems.STORAGE_POUCH.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.LARGE_BAG.get())
                .pattern("LLL")
                .pattern("LCL")
                .pattern(" PP")
                .define('L', Items.LEATHER)
                .define('C', Items.CHEST)
                .define('P', ModItems.STORAGE_POUCH.get())
                .unlockedBy("has_storage_pouch", has(ModItems.STORAGE_POUCH.get()))
                .save(output);
    }
}
