package github.nyeptos.packingcube.bag.station;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Crafting blocks that can ride in a module slot and open their GUI from the radial menu. */
public final class StationBlocks {
    private StationBlocks() {}

    /**
     * Each kind names the block, the slots its GUI keeps inside the block, and where those slots
     * start in the menu. The furnace family and the brewing stand also keep their progress there,
     * so they keep working while the GUI is closed.
     */
    public enum Kind {
        CRAFTING_TABLE(Items.CRAFTING_TABLE, 1, 9),
        STONECUTTER(Items.STONECUTTER, 0, 1),
        GRINDSTONE(Items.GRINDSTONE, 0, 2),
        CARTOGRAPHY_TABLE(Items.CARTOGRAPHY_TABLE, 0, 2),
        LOOM(Items.LOOM, 0, 3),
        SMITHING_TABLE(Items.SMITHING_TABLE, 0, 3),
        ANVIL(Items.ANVIL, 0, 2),
        FURNACE(Items.FURNACE, 0, 3),
        SMOKER(Items.SMOKER, 0, 3),
        BLAST_FURNACE(Items.BLAST_FURNACE, 0, 3),
        BREWING_STAND(Items.BREWING_STAND, 0, 5);

        private final Item item;
        private final int firstInput;
        private final int inputs;

        Kind(Item item, int firstInput, int inputs) {
            this.item = item;
            this.firstInput = firstInput;
            this.inputs = inputs;
        }

        public Item item() {
            return item;
        }

        /** Index of the first menu slot this kind keeps inside the block. */
        public int firstInput() {
            return firstInput;
        }

        public int inputs() {
            return inputs;
        }
    }

    /** Furnaces and the brewing stand keep running while the GUI is closed, so they need a tick. */
    public static boolean ticking(Kind kind) {
        return kind == Kind.FURNACE
                || kind == Kind.SMOKER
                || kind == Kind.BLAST_FURNACE
                || kind == Kind.BREWING_STAND;
    }

    public static RecipeType<? extends AbstractCookingRecipe> cooking(Kind kind) {
        return switch (kind) {
            case FURNACE -> RecipeType.SMELTING;
            case SMOKER -> RecipeType.SMOKING;
            case BLAST_FURNACE -> RecipeType.BLASTING;
            default -> null;
        };
    }

    public static Kind kind(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (Kind kind : Kind.values()) if (stack.is(kind.item)) return kind;
        return null;
    }

    public static boolean isStation(ItemStack stack) {
        return kind(stack) != null;
    }

    /** One block per module slot keeps the radial icon and the stored GUI unambiguous. */
    public static boolean fitsModule(ItemStack stack) {
        return isStation(stack) && stack.getCount() == 1;
    }
}
