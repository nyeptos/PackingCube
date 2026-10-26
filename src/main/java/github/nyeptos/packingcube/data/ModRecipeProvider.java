package github.nyeptos.packingcube.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.concurrent.CompletableFuture;

/** Recipe authoring entry point. No crafting costs are invented before the design is decided. */
public final class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(
            PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // Add ShapedRecipeBuilder / ShapelessRecipeBuilder definitions here.
        // A copyable example and runData instructions are in docs/DATAGEN.md.
    }
}
