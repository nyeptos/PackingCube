package github.nyeptos.packingcube.datagen;

import github.nyeptos.packingcube.registry.ModBlocks;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

/**
 * The table's own costs: the station, and nothing else. The pouches and the backpack are the
 * station's bills, so no table recipe may ask for one of them.
 */
public final class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(
            PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // No pouch here: the station is what makes pouches, so it must not ask for one.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.POUCH_STATION_ITEM.get())
                .pattern("LLL")
                .pattern("PTP")
                .pattern("CCC")
                .define('L', Items.LEATHER)
                .define('P', ItemTags.PLANKS)
                .define('T', Items.CRAFTING_TABLE)
                .define('C', Items.COBBLESTONE)
                .unlockedBy("has_leather", has(Items.LEATHER))
                .save(output);

        PouchRecipeData.write(output);
    }
}
