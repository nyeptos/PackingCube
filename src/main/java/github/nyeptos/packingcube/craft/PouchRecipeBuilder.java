package github.nyeptos.packingcube.craft;

import github.nyeptos.packingcube.PackingCube;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/** Writes one station bill, so the datagen never hand-builds the record. */
public final class PouchRecipeBuilder {
    private final Item result;
    private final List<PouchRecipe.Material> materials = new ArrayList<>();
    private int order;

    private PouchRecipeBuilder(Item result) {
        this.result = result;
    }

    public static PouchRecipeBuilder pouch(Item result) {
        return new PouchRecipeBuilder(result);
    }

    /** Where the pouch sits in the list; lower comes first. Ties fall back to the file name. */
    public PouchRecipeBuilder order(int order) {
        this.order = order;
        return this;
    }

    /** Adds one bill line; naming the same item again just grows that line. */
    public PouchRecipeBuilder needs(Item item, int count) {
        for (int index = 0; index < materials.size(); index++) {
            PouchRecipe.Material line = materials.get(index);
            if (!line.item().equals(item)) continue;
            materials.set(index, new PouchRecipe.Material(item, line.count() + count));
            return this;
        }
        materials.add(new PouchRecipe.Material(item, count));
        return this;
    }

    public PouchRecipeBuilder needs(Item item) {
        return needs(item, 1);
    }

    public PouchRecipe build() {
        return new PouchRecipe(result, List.copyOf(materials), order);
    }

    public void save(RecipeOutput output, String path) {
        save(output, ResourceLocation.fromNamespaceAndPath(PackingCube.MODID, path));
    }

    public void save(RecipeOutput output, ResourceLocation id) {
        output.accept(id, build(), null);
    }
}
