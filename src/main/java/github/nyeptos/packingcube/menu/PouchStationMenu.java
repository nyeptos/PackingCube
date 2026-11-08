package github.nyeptos.packingcube.menu;

import github.nyeptos.packingcube.bag.BagSounds;
import github.nyeptos.packingcube.craft.PouchRecipe;
import github.nyeptos.packingcube.craft.PouchRecipes;
import github.nyeptos.packingcube.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The station menu. Crafting charges the player's own inventory, so the panel needs no material
 * slots: a pouch is paid for wherever it sits in the backpack.
 */
public final class PouchStationMenu extends AbstractContainerMenu {
    public static final int RESULT_SLOT = 0;
    public static final int PANEL_WIDTH = 216;
    public static final int PANEL_HEIGHT = 190;
    /** The scrolling pouch list: three 24px rows, scrolled by the bar on its right. */
    public static final int LIST_LEFT = 8;
    public static final int LIST_TOP = 26;
    public static final int LIST_WIDTH = 144;
    public static final int LIST_HEIGHT = 72;
    public static final int ROW_HEIGHT = 24;
    public static final int SCROLLBAR_LEFT = 154;
    public static final int SCROLLBAR_WIDTH = 6;
    public static final int RESULT_LEFT = 172;
    public static final int RESULT_TOP = 45;
    public static final int BUTTON_LEFT = 161;
    public static final int BUTTON_TOP = 74;
    public static final int BUTTON_WIDTH = 40;
    public static final int BUTTON_HEIGHT = 16;
    public static final int INVENTORY_LEFT = 28;
    public static final int INVENTORY_TOP = 106;
    public static final int USE_ROW_TOP = 164;

    private static final int INV_SLOT_START = RESULT_SLOT + 1;
    private static final int INV_SLOT_END = INV_SLOT_START + 27;
    private static final int USE_ROW_START = INV_SLOT_END;
    private static final int USE_ROW_END = USE_ROW_START + 9;
    private static final int DATA_SELECTED = 0;
    private static final double REACH = 64.0;

    private final Container result;
    private final Inventory inventory;
    private final ContainerData data;
    private final BlockPos origin;

    public PouchStationMenu(int id, Inventory inventory, BlockPos origin) {
        this(id, inventory, origin, new SimpleContainer(1), new SimpleContainerData(1));
    }

    private PouchStationMenu(
            int id, Inventory inventory, BlockPos origin, Container result, ContainerData data) {
        super(ModMenus.POUCH_STATION_MENU.get(), id);
        this.origin = origin;
        this.inventory = inventory;
        this.result = result;
        this.data = data;
        addSlot(new ResultSlot(result, 0, RESULT_LEFT, RESULT_TOP));
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(
                        new Slot(
                                inventory,
                                column + row * 9 + 9,
                                INVENTORY_LEFT + column * 18,
                                INVENTORY_TOP + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, INVENTORY_LEFT + column * 18, USE_ROW_TOP));
        addDataSlots(data);
    }

    public static PouchStationMenu fromNetwork(
            int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        return new PouchStationMenu(id, inventory, data.readBlockPos());
    }

    /** The finished pouch waits here; the player only takes, never puts. */
    private static final class ResultSlot extends Slot {
        ResultSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

    }

    public int selected() {
        return data.get(DATA_SELECTED);
    }

    public Inventory inventory() {
        return inventory;
    }

    /** The bills live in the recipe manager, so both sides read the same list. */
    private Level level() {
        return inventory.player.level();
    }

    public PouchRecipe selectedRecipe() {
        return PouchRecipes.get(level(), selected());
    }

    public void select(int index) {
        if (PouchRecipes.get(level(), index) == null) return;
        data.set(DATA_SELECTED, index);
        broadcastChanges();
    }

    /** True when the bill is covered by the inventory and the result slot is free. */
    public boolean canCraftSelected() {
        return result.getItem(0).isEmpty() && PouchRecipes.canCraft(selectedRecipe(), inventory);
    }

    /** Pays the bill out of the player's inventory and puts the pouch in the result slot. */
    public boolean craft(Player player) {
        if (player.level().isClientSide) return false;
        if (!canCraftSelected()) return false;
        PouchRecipe recipe = selectedRecipe();
        if (!PouchRecipes.consume(recipe, inventory)) return false;
        result.setItem(0, recipe.resultStack());
        player.playNotifySound(
                BagSounds.leather(), SoundSource.PLAYERS, BagSounds.VOLUME, BagSounds.PITCH);
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == RESULT_SLOT) {
            if (!moveItemStackTo(stack, INV_SLOT_START, USE_ROW_END, true)) return ItemStack.EMPTY;
        } else if (index < INV_SLOT_END) {
            if (!moveItemStackTo(stack, USE_ROW_START, USE_ROW_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, INV_SLOT_START, INV_SLOT_END, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(
                        origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5)
                <= REACH;
    }

    /** A pouch left in the result slot goes back to the player, never into the void. */
    @Override
    public void removed(Player player) {
        super.removed(player);
        ItemStack stack = result.getItem(0);
        if (stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        result.setItem(0, ItemStack.EMPTY);
    }
}
