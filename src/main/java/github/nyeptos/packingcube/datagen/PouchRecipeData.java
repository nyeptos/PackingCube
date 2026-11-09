package github.nyeptos.packingcube.datagen;

import github.nyeptos.packingcube.craft.PouchRecipeBuilder;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;

/**
 * The station's own bills: the pouches and the backpack that carries them. They are written as
 * data so a new entry is a line here rather than a code change.
 */
public final class PouchRecipeData {
    private PouchRecipeData() {}

    public static void write(RecipeOutput output) {
        PouchRecipeBuilder.pouch(ModItems.STORAGE_POUCH.get())
                .order(10)
                .needs(Items.LEATHER, 1)
                .needs(Items.STRING, 2)
                .needs(Items.CHEST)
                .save(output, "pouch/storage_pouch");

        PouchRecipeBuilder.pouch(ModItems.REFILL_POUCH.get())
                .order(20)
                .needs(Items.IRON_INGOT, 2)
                .needs(Items.HOPPER)
                .needs(ModItems.STORAGE_POUCH.get())
                .needs(Items.REDSTONE)
                .save(output, "pouch/refill_pouch");

        PouchRecipeBuilder.pouch(ModItems.BULK_POUCH.get())
                .order(30)
                .needs(Items.IRON_INGOT, 4)
                .needs(Items.CHEST, 4)
                .needs(ModItems.STORAGE_POUCH.get())
                .save(output, "pouch/bulk_pouch");

        PouchRecipeBuilder.pouch(ModItems.BACKPACK.get())
                .order(40)
                .needs(Items.LEATHER, 2)
                .needs(ModItems.STORAGE_POUCH.get(), 2)
                .needs(Items.CHEST)
                .save(output, "pouch/backpack");
    }
}
