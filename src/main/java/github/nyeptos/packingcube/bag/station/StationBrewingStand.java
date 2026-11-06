package github.nyeptos.packingcube.bag.station;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * A brewing stand carried in a module slot. Three bottles, the ingredient and the blaze powder
 * live on the block stack together with the brew timer, so it keeps brewing while the GUI is shut.
 */
public final class StationBrewingStand implements TickingStation {
    public static final int SLOTS = 5;
    private static final int BOTTLES = 3;
    private static final int INGREDIENT = 3;
    private static final int FUEL = 4;
    private static final int FUEL_PER_POWDER = 20;
    private static final int BREW_TICKS = 400;
    private static final String TAG = "packingcube_station_brewing";
    private static final String BREW_TIME = "brew_time";
    private static final String FUEL_LEFT = "fuel";

    private final Player player;
    private final Supplier<ItemStack> root;
    private final int moduleSlot;
    private final ItemStack block;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int brewTime;
    private int fuel;
    private Item ingredient = Items.AIR;

    private StationBrewingStand(
            Player player, Supplier<ItemStack> root, int moduleSlot, ItemStack block) {
        this.player = player;
        this.root = root;
        this.moduleSlot = moduleSlot;
        this.block = block.copy();
        List<ItemStack> saved = StationItems.stored(this.block, SLOTS);
        for (int i = 0; i < SLOTS; i++) items.set(i, saved.get(i).copy());
        CompoundTag tag =
                this.block.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(TAG)) {
            CompoundTag state = tag.getCompound(TAG);
            brewTime = state.getInt(BREW_TIME);
            fuel = state.getInt(FUEL_LEFT);
        }
        if (brewTime > 0) ingredient = items.get(INGREDIENT).getItem();
    }

    public static StationBrewingStand of(
            Player player, Supplier<ItemStack> root, int moduleSlot, ItemStack block) {
        return new StationBrewingStand(player, root, moduleSlot, block);
    }

    public static StationBrewingStand of(
            Player player, UUID rootId, int moduleSlot, ItemStack block) {
        return of(player, () -> StationStorage.root(player, rootId), moduleSlot, block);
    }

    /** Vanilla brewing stand tick without a block: bottles, powder and timer all land in the stack. */
    @Override
    public boolean tick(Level level) {
        boolean changed = false;
        ItemStack powder = items.get(FUEL);
        if (fuel <= 0 && powder.is(Items.BLAZE_POWDER)) {
            fuel = FUEL_PER_POWDER;
            powder.shrink(1);
            changed = true;
        }
        PotionBrewing brewing = level.potionBrewing();
        boolean brewable = brewable(brewing);
        ItemStack reagent = items.get(INGREDIENT);
        if (brewTime > 0) {
            brewTime--;
            changed = true;
            if (brewTime == 0 && brewable) doBrew(level, brewing);
            else if (!brewable || !reagent.is(ingredient)) brewTime = 0;
        } else if (brewable && fuel > 0) {
            fuel--;
            brewTime = BREW_TICKS;
            ingredient = reagent.getItem();
            changed = true;
        }
        return changed;
    }

    private boolean brewable(PotionBrewing brewing) {
        ItemStack reagent = items.get(INGREDIENT);
        if (reagent.isEmpty() || !brewing.isIngredient(reagent)) return false;
        for (int i = 0; i < BOTTLES; i++) {
            ItemStack bottle = items.get(i);
            if (!bottle.isEmpty() && brewing.hasMix(bottle, reagent)) return true;
        }
        return false;
    }

    private void doBrew(Level level, PotionBrewing brewing) {
        if (EventHooks.onPotionAttemptBrew(items)) return;
        ItemStack reagent = items.get(INGREDIENT);
        for (int i = 0; i < BOTTLES; i++) items.set(i, brewing.mix(reagent, items.get(i)));
        EventHooks.onPotionBrewed(items);
        if (reagent.hasCraftingRemainingItem()) {
            ItemStack remainder = reagent.getCraftingRemainingItem();
            reagent.shrink(1);
            if (reagent.isEmpty()) reagent = remainder;
            else if (!player.getInventory().add(remainder)) player.drop(remainder, false);
        } else {
            reagent.shrink(1);
        }
        items.set(INGREDIENT, reagent);
        level.levelEvent(1035, player.blockPosition(), 0);
    }

    /** Writes the slots and the counters back into the block that carries them. */
    @Override
    public boolean flush() {
        if (player.level().isClientSide) return false;
        StationItems.store(block, List.copyOf(items));
        CompoundTag tag =
                block.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag state = new CompoundTag();
        state.putInt(BREW_TIME, brewTime);
        state.putInt(FUEL_LEFT, fuel);
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
        return 2;
    }

    @Override
    public int get(int index) {
        return index == 0 ? brewTime : fuel;
    }

    @Override
    public void set(int index, int value) {
        if (index == 0) brewTime = value;
        else fuel = value;
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
