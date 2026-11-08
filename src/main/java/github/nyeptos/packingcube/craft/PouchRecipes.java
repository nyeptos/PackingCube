package github.nyeptos.packingcube.craft;

import github.nyeptos.packingcube.registry.ModRecipes;

import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.List;

/**
 * The bills the station offers, read from the recipe manager. Both sides sort by the bill's own
 * order, so the row the player clicks is the row the server crafts.
 */
public final class PouchRecipes {
    private static final Comparator<RecipeHolder<PouchRecipe>> BILL_ORDER =
            Comparator.comparingInt((RecipeHolder<PouchRecipe> holder) -> holder.value().order())
                    .thenComparing(holder -> holder.id().toString());

    private PouchRecipes() {}

    public static List<PouchRecipe> all(Level level) {
        if (level == null) return List.of();
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.POUCH_TYPE.get()).stream()
                .sorted(BILL_ORDER)
                .map(RecipeHolder::value)
                .toList();
    }

    /** The recipe at {@code index}, or null when the index is out of range. */
    public static PouchRecipe get(Level level, int index) {
        List<PouchRecipe> recipes = all(level);
        return index >= 0 && index < recipes.size() ? recipes.get(index) : null;
    }

    /** How many of {@code item} the material slots hold, wherever they sit. */
    public static int held(Container materials, Item item) {
        int held = 0;
        for (int slot = 0; slot < materials.getContainerSize(); slot++) {
            ItemStack stack = materials.getItem(slot);
            if (stack.is(item)) held += stack.getCount();
        }
        return held;
    }

    public static boolean canCraft(PouchRecipe recipe, Container materials) {
        if (recipe == null) return false;
        for (PouchRecipe.Material material : recipe.materials())
            if (held(materials, material.item()) < material.count()) return false;
        return true;
    }

    /** Pays the bill out of the material slots. Refuses and changes nothing when short. */
    public static boolean consume(PouchRecipe recipe, Container materials) {
        if (!canCraft(recipe, materials)) return false;
        for (PouchRecipe.Material material : recipe.materials()) {
            int left = material.count();
            for (int slot = 0; slot < materials.getContainerSize() && left > 0; slot++) {
                ItemStack stack = materials.getItem(slot);
                if (!stack.is(material.item())) continue;
                int taken = Math.min(left, stack.getCount());
                stack.shrink(taken);
                left -= taken;
                if (stack.isEmpty()) materials.setItem(slot, ItemStack.EMPTY);
            }
        }
        materials.setChanged();
        return true;
    }
}
