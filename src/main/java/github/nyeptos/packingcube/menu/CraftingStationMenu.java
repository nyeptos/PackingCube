package github.nyeptos.packingcube.menu;

import github.nyeptos.packingcube.bag.BagSounds;
import github.nyeptos.packingcube.bag.station.StationStorage;
import github.nyeptos.packingcube.menu.station.CarriedStationMenu;
import github.nyeptos.packingcube.registry.ModMenus;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The 3x3 grid of a crafting table carried in a module slot. The grid is stored on the block
 * stack itself, so the table keeps its contents while it moves in and out of the bag.
 */
public final class CraftingStationMenu extends AbstractContainerMenu
        implements CarriedStationMenu {
    public static final int RESULT_SLOT = 0;
    private static final int GRID_SIZE = 9;
    private static final int INV_SLOT_START = 10;
    private static final int INV_SLOT_END = 37;
    private static final int USE_ROW_SLOT_START = 37;
    private static final int USE_ROW_SLOT_END = 46;

    private final Player player;
    private final UUID rootId;
    private final int moduleSlot;
    private final ItemStack block;
    private final CraftingContainer grid;
    private final ResultContainer resultSlots = new ResultContainer();

    public CraftingStationMenu(
            int id, Inventory inventory, UUID rootId, int moduleSlot, ItemStack block) {
        super(ModMenus.CRAFTING_STATION_MENU.get(), id);
        this.player = inventory.player;
        this.rootId = rootId;
        this.moduleSlot = moduleSlot;
        this.block = block.copy();
        this.grid = new TransientCraftingContainer(this, 3, 3, storedGrid(this.block));
        addSlot(new ResultSlot(inventory.player, grid, resultSlots, RESULT_SLOT, 124, 35));
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 3; column++)
                addSlot(new Slot(grid, column + row * 3, 30 + column * 18, 17 + row * 18));
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
    }

    public static CraftingStationMenu fromNetwork(
            int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        UUID rootId = data.readUUID();
        int moduleSlot = data.readVarInt();
        return new CraftingStationMenu(id, inventory, rootId, moduleSlot, ItemStack.EMPTY);
    }

    /** Opens the carried station without placing any block in the world. */
    public static boolean open(Player player, UUID rootId, int moduleSlot, ItemStack block) {
        if (player.level().isClientSide) return false;
        boolean opened =
                player.openMenu(
                                new SimpleMenuProvider(
                                        (id, inventory, ignored) ->
                                                new CraftingStationMenu(
                                                        id,
                                                        inventory,
                                                        rootId,
                                                        moduleSlot,
                                                        block),
                                        block.getHoverName()),
                                data -> {
                                    data.writeUUID(rootId);
                                    data.writeVarInt(moduleSlot);
                                })
                        .isPresent();
        if (opened) BagSounds.opened(player);
        return opened;
    }

    private static NonNullList<ItemStack> storedGrid(ItemStack block) {
        NonNullList<ItemStack> items = NonNullList.withSize(GRID_SIZE, ItemStack.EMPTY);
        ItemContainerContents contents =
                block.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        List<ItemStack> saved = contents.stream().toList();
        for (int i = 0; i < Math.min(saved.size(), GRID_SIZE); i++)
            items.set(i, saved.get(i).copy());
        return items;
    }

    /** Read-only view of the grid, used by tests and the result preview. */
    public CraftingContainer grid() {
        return grid;
    }

    @Override
    public void slotsChanged(Container container) {
        if (container != grid) return;
        save();
        updateResult();
    }

    /** Persist the grid into the block, then into the module slot that owns it. */
    private boolean save() {
        if (player.level().isClientSide) return false;
        List<ItemStack> items = new ArrayList<>(GRID_SIZE);
        for (int i = 0; i < GRID_SIZE; i++) items.add(grid.getItem(i).copy());
        block.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        return StationStorage.store(player, rootId, moduleSlot, block);
    }

    private void updateResult() {
        if (!(player instanceof ServerPlayer server)) return;
        Level level = server.level();
        CraftingInput input = grid.asCraftInput();
        ItemStack crafted = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> found =
                level.getServer()
                        .getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, input, level);
        if (found.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = found.get();
            if (resultSlots.setRecipeUsed(level, server, holder)) {
                ItemStack assembled = holder.value().assemble(input, level.registryAccess());
                if (assembled.isItemEnabled(level.enabledFeatures())) crafted = assembled;
            }
        }
        resultSlots.setItem(RESULT_SLOT, crafted);
        setRemoteSlot(RESULT_SLOT, crafted);
        server.connection.send(
                new ClientboundContainerSetSlotPacket(
                        containerId, incrementStateId(), RESULT_SLOT, crafted));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // The client mirrors the grid; only the server may hand items back.
        if (player.level().isClientSide) return;
        if (!save()) clearContainer(player, grid);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return moved;
        ItemStack current = slot.getItem();
        moved = current.copy();
        if (index == RESULT_SLOT) {
            current.getItem().onCraftedBy(current, player.level(), player);
            if (!moveItemStackTo(current, INV_SLOT_START, USE_ROW_SLOT_END, true))
                return ItemStack.EMPTY;
            slot.onQuickCraft(current, moved);
        } else if (index >= INV_SLOT_START && index < USE_ROW_SLOT_END) {
            if (!moveItemStackTo(current, 1, INV_SLOT_START, false)) {
                if (index < USE_ROW_SLOT_START) {
                    if (!moveItemStackTo(current, USE_ROW_SLOT_START, USE_ROW_SLOT_END, false))
                        return ItemStack.EMPTY;
                } else if (!moveItemStackTo(current, INV_SLOT_START, INV_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (!moveItemStackTo(current, INV_SLOT_START, USE_ROW_SLOT_END, false)) {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (current.getCount() == moved.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, current);
        if (index == RESULT_SLOT) player.drop(current, false);
        return moved;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
    }
}
