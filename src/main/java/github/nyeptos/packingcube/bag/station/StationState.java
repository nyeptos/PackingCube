package github.nyeptos.packingcube.bag.station;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Keeps the input slots of a carried station inside the block stack. The slots are refilled once
 * the menu is really open on both sides, because anything written during construction would be
 * sent before the client has the menu and would be dropped.
 */
public final class StationState {
    private final Player player;
    private final UUID rootId;
    private final int moduleSlot;
    private final ItemStack block;
    private final StationBlocks.Kind kind;

    public StationState(
            Player player, UUID rootId, int moduleSlot, ItemStack block, StationBlocks.Kind kind) {
        this.player = player;
        this.rootId = rootId;
        this.moduleSlot = moduleSlot;
        this.block = block.copy();
        this.kind = kind;
    }

    /**
     * Fills the slots from the block as soon as the menu exists. Slots written here are sent
     * before the client owns the menu, so the remote copies are cleared again to make the first
     * broadcast after opening carry them.
     */
    public void restore(AbstractContainerMenu menu) {
        if (player.level().isClientSide || block.isEmpty()) return;
        List<ItemStack> saved = StationItems.stored(block, kind.inputs());
        for (int i = 0; i < kind.inputs(); i++)
            menu.getSlot(kind.firstInput() + i).set(saved.get(i).copy());
        for (int i = 0; i < kind.inputs(); i++)
            menu.setRemoteSlot(kind.firstInput() + i, ItemStack.EMPTY);
    }

    /** Closing keeps the contents in the block; the carried stack still goes back to the player. */
    public void closed(AbstractContainerMenu menu, Player player) {
        if (player.level().isClientSide) return;
        ItemStack carried = menu.getCarried();
        if (!carried.isEmpty() && player instanceof ServerPlayer server) {
            if (server.isAlive() && !server.hasDisconnected())
                server.getInventory().placeItemBackInInventory(carried);
            else player.drop(carried, false);
            menu.setCarried(ItemStack.EMPTY);
        }
        if (save(menu)) return;
        // The block left the module slot: hand the contents back instead of dropping them.
        for (int i = 0; i < kind.inputs(); i++) {
            ItemStack stack = menu.getSlot(kind.firstInput() + i).getItem();
            if (stack.isEmpty()) continue;
            player.getInventory().placeItemBackInInventory(stack.copy());
            menu.getSlot(kind.firstInput() + i).set(ItemStack.EMPTY);
        }
    }

    private boolean save(AbstractContainerMenu menu) {
        if (player.level().isClientSide || block.isEmpty()) return false;
        List<ItemStack> items = new ArrayList<>(kind.inputs());
        for (int i = 0; i < kind.inputs(); i++)
            items.add(menu.getSlot(kind.firstInput() + i).getItem().copy());
        StationItems.store(block, items);
        return StationStorage.store(player, rootId, moduleSlot, block);
    }
}
