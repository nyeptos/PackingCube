package github.nyeptos.packingcube.bag.station;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * A furnace, smoker or blast furnace carried in a module slot. The three slots and the heating
 * counters live on the block stack, so the station keeps cooking while its GUI is closed.
 */
public final class StationFurnace implements TickingStation {
    public static final int SLOTS = 3;
    private static final String TAG = "packingcube_station_furnace";
    private static final String LIT = "lit";
    private static final String LIT_TOTAL = "lit_total";
    private static final String COOKING = "cooking";
    private static final String COOKING_TOTAL = "cooking_total";

    private final Player player;
    private final Supplier<ItemStack> root;
    private final int moduleSlot;
    private final ItemStack block;
    private final RecipeType<? extends AbstractCookingRecipe> recipeType;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int litTime;
    private int litDuration;
    private int cookingProgress;
    private int cookingTotalTime = 200;

    private StationFurnace(
            Player player,
            Supplier<ItemStack> root,
            int moduleSlot,
            ItemStack block,
            StationBlocks.Kind kind) {
        this.player = player;
        this.root = root;
        this.moduleSlot = moduleSlot;
        this.block = block.copy();
        this.recipeType = StationBlocks.cooking(kind);
        List<ItemStack> saved = StationItems.stored(this.block, SLOTS);
        for (int i = 0; i < SLOTS; i++) items.set(i, saved.get(i).copy());
        CompoundTag tag =
                this.block
                        .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag()
                        .getCompound(TAG);
        litTime = tag.getInt(LIT);
        litDuration = tag.getInt(LIT_TOTAL);
        cookingProgress = tag.getInt(COOKING);
        cookingTotalTime = tag.contains(COOKING_TOTAL) ? tag.getInt(COOKING_TOTAL) : 200;
    }

    public static StationFurnace of(
            Player player,
            Supplier<ItemStack> root,
            int moduleSlot,
            ItemStack block,
            StationBlocks.Kind kind) {
        return new StationFurnace(player, root, moduleSlot, block, kind);
    }

    /** Vanilla furnace tick without a block: fuel, progress and output all land in the stack. */
    @Override
    public boolean tick(Level level) {
        boolean changed = false;
        if (litTime > 0) {
            litTime--;
            changed = true;
        }
        ItemStack fuel = items.get(1);
        ItemStack input = items.get(0);
        boolean hasInput = !input.isEmpty();
        boolean hasFuel = !fuel.isEmpty();
        if (litTime > 0 || (hasFuel && hasInput)) {
            RecipeHolder<? extends AbstractCookingRecipe> recipe = hasInput ? recipe(level) : null;
            if (litTime <= 0 && canBurn(level, recipe)) {
                litTime = fuel.getBurnTime(recipeType);
                litDuration = litTime;
                if (litTime > 0) {
                    changed = true;
                    if (fuel.hasCraftingRemainingItem()) items.set(1, fuel.getCraftingRemainingItem());
                    else {
                        fuel.shrink(1);
                        if (fuel.isEmpty()) items.set(1, fuel.getCraftingRemainingItem());
                    }
                }
            }
            if (litTime > 0 && canBurn(level, recipe)) {
                cookingProgress++;
                changed = true;
                if (cookingProgress == cookingTotalTime) {
                    cookingProgress = 0;
                    cookingTotalTime = cookTime(level);
                    burn(level, recipe);
                }
            } else if (cookingProgress != 0) {
                cookingProgress = 0;
                changed = true;
            }
        } else if (cookingProgress > 0) {
            cookingProgress = Mth.clamp(cookingProgress - 2, 0, cookingTotalTime);
            changed = true;
        }
        return changed;
    }

    private RecipeHolder<? extends AbstractCookingRecipe> recipe(Level level) {
        SingleRecipeInput input = new SingleRecipeInput(items.get(0));
        return cookingRecipe(level, input).orElse(null);
    }

    @SuppressWarnings("unchecked")
    private java.util.Optional<RecipeHolder<? extends AbstractCookingRecipe>> cookingRecipe(
            Level level, SingleRecipeInput input) {
        return level.getRecipeManager()
                .getRecipeFor((RecipeType<AbstractCookingRecipe>) recipeType, input, level)
                .map(holder -> (RecipeHolder<? extends AbstractCookingRecipe>) holder);
    }

    private int cookTime(Level level) {
        return cookingRecipe(level, new SingleRecipeInput(items.get(0)))
                .map(holder -> holder.value().getCookingTime())
                .orElse(200);
    }

    private boolean canBurn(Level level, RecipeHolder<? extends AbstractCookingRecipe> recipe) {
        if (items.get(0).isEmpty() || recipe == null) return false;
        ItemStack result =
                recipe.value()
                        .assemble(
                                new SingleRecipeInput(items.get(0)), level.registryAccess());
        if (result.isEmpty()) return false;
        ItemStack output = items.get(2);
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(output, result)) return false;
        int total = output.getCount() + result.getCount();
        return total <= output.getMaxStackSize() && total <= result.getMaxStackSize();
    }

    private void burn(Level level, RecipeHolder<? extends AbstractCookingRecipe> recipe) {
        if (!canBurn(level, recipe)) return;
        ItemStack result =
                recipe.value()
                        .assemble(
                                new SingleRecipeInput(items.get(0)), level.registryAccess());
        ItemStack output = items.get(2);
        if (output.isEmpty()) items.set(2, result.copy());
        else if (ItemStack.isSameItemSameComponents(output, result)) output.grow(result.getCount());
        ItemStack input = items.get(0);
        if (input.is(net.minecraft.world.level.block.Blocks.WET_SPONGE.asItem())
                && items.get(1).is(net.minecraft.world.item.Items.BUCKET))
            items.set(1, new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
        input.shrink(1);
    }

    /** Writes the slots and the counters back into the block that carries them. */
    @Override
    public boolean flush() {
        if (player.level().isClientSide) return false;
        StationItems.store(block, List.copyOf(items));
        CompoundTag tag =
                block.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag state = new CompoundTag();
        state.putInt(LIT, litTime);
        state.putInt(LIT_TOTAL, litDuration);
        state.putInt(COOKING, cookingProgress);
        state.putInt(COOKING_TOTAL, cookingTotalTime);
        tag.put(TAG, state);
        block.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        ItemStack target = root.get();
        if (target.isEmpty()) return false;
        return StationStorage.store(target, moduleSlot, block);
    }

    /** Items still in the block go back to the player when the block itself left the bag. */
    @Override
    public void spill(Player player) {
        for (int i = 0; i < SLOTS; i++) {
            if (items.get(i).isEmpty()) continue;
            player.getInventory().placeItemBackInInventory(items.get(i).copy());
            items.set(i, ItemStack.EMPTY);
        }
    }

    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public int get(int index) {
        return switch (index) {
            case 0 -> litTime;
            case 1 -> litDuration;
            case 2 -> cookingProgress;
            default -> cookingTotalTime;
        };
    }

    @Override
    public void set(int index, int value) {
        switch (index) {
            case 0 -> litTime = value;
            case 1 -> litDuration = value;
            case 2 -> cookingProgress = value;
            default -> cookingTotalTime = value;
        }
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    @Override
    public void clearContent() {
        items.clear();
    }
}
